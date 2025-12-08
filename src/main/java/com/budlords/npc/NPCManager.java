package com.budlords.npc;

import com.budlords.BudLords;
import com.budlords.challenges.Challenge;
import com.budlords.economy.EconomyManager;
import com.budlords.packaging.PackagingManager;
import com.budlords.progression.RankManager;
import com.budlords.stats.PlayerStats;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class NPCManager {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final StrainManager strainManager;
    private final RankManager rankManager;
    private final PackagingManager packagingManager;
    
    private final NamespacedKey npcTypeKey;
    private final Map<UUID, Long> tradeCooldowns;

    public NPCManager(BudLords plugin, EconomyManager economyManager, 
                      StrainManager strainManager, RankManager rankManager,
                      PackagingManager packagingManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.strainManager = strainManager;
        this.rankManager = rankManager;
        this.packagingManager = packagingManager;
        this.npcTypeKey = new NamespacedKey(plugin, "npc_type");
        this.tradeCooldowns = new ConcurrentHashMap<>();
    }

    // Using deprecated setCustomName() for Bukkit/Spigot compatibility
    // Paper servers can replace with Adventure API's customName(Component) method
    @SuppressWarnings("deprecation")
    public void spawnMarketJoe(Location location) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setCustomName("§a§lMarket Joe");
        villager.setCustomNameVisible(true);
        villager.setProfession(Villager.Profession.FARMER);
        villager.setVillagerLevel(5);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        pdc.set(npcTypeKey, PersistentDataType.STRING, "market_joe");
    }

    // Using deprecated setCustomName() for Bukkit/Spigot compatibility
    @SuppressWarnings("deprecation")
    public void spawnBlackMarketJoe(Location location) {
        WanderingTrader trader = (WanderingTrader) location.getWorld().spawnEntity(location, EntityType.WANDERING_TRADER);
        trader.setCustomName("§5§lBlackMarket Joe");
        trader.setCustomNameVisible(true);
        trader.setAI(false);
        trader.setInvulnerable(true);
        trader.setSilent(true);
        
        PersistentDataContainer pdc = trader.getPersistentDataContainer();
        pdc.set(npcTypeKey, PersistentDataType.STRING, "blackmarket_joe");
    }

    public NPCType getNPCType(Entity entity) {
        if (entity == null) return NPCType.NONE;
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(npcTypeKey, PersistentDataType.STRING)) {
            String type = pdc.get(npcTypeKey, PersistentDataType.STRING);
            if ("market_joe".equals(type)) return NPCType.MARKET_JOE;
            if ("blackmarket_joe".equals(type)) return NPCType.BLACKMARKET_JOE;
        }
        
        // Check for regular villagers without profession
        if (entity instanceof Villager villager) {
            if (villager.getProfession() == Villager.Profession.NONE || 
                villager.getProfession() == Villager.Profession.NITWIT) {
                return NPCType.VILLAGE_VENDOR;
            }
        }
        
        // Check if entity type is allowed in config
        if (isEntityAllowedForSelling(entity)) {
            return NPCType.CONFIGURABLE_MOB;
        }
        
        return NPCType.NONE;
    }
    
    /**
     * Checks if an entity type is allowed for selling based on config.
     */
    public boolean isEntityAllowedForSelling(Entity entity) {
        if (entity == null) return false;
        
        String entityType = entity.getType().name().toLowerCase();
        return plugin.getConfig().getBoolean("trading.allowed-mobs." + entityType, false);
    }

    public TradeResult attemptTrade(Player player, Entity trader, ItemStack item) {
        NPCType npcType = getNPCType(trader);
        if (npcType == NPCType.NONE) {
            return new TradeResult(false, "§cThis trader doesn't buy weed.", 0);
        }

        // Check cooldown
        UUID playerId = player.getUniqueId();
        if (isOnCooldown(playerId)) {
            long remaining = getCooldownRemaining(playerId);
            return new TradeResult(false, "§cYou're too suspicious! Wait " + (remaining / 1000) + " seconds.", 0);
        }

        if (!packagingManager.isPackagedProduct(item)) {
            return new TradeResult(false, "§cYou can only sell packaged products!", 0);
        }

        String strainId = packagingManager.getStrainIdFromPackage(item);
        Strain strain = strainManager.getStrain(strainId);
        if (strain == null) {
            return new TradeResult(false, "§cUnknown strain!", 0);
        }

        int weight = packagingManager.getWeightFromPackage(item);
        double baseValue = packagingManager.getValueFromPackage(item);

        // Calculate success chance
        double successChance = calculateSuccessChance(player, strain, weight, npcType);
        
        boolean success = ThreadLocalRandom.current().nextDouble() < successChance;

        if (!success) {
            // Failed deal - apply cooldown
            applyCooldown(playerId);
            
            // Update failed sales stats
            if (plugin.getStatsManager() != null) {
                PlayerStats stats = plugin.getStatsManager().getStats(player);
                stats.incrementFailedSales();
            }
            
            return new TradeResult(false, "§cThe deal went wrong! You seem suspicious... Cool off for a bit.", 0);
        }

        // Calculate final price with prestige bonus
        double basePrice = calculateFinalPrice(baseValue, strain, npcType, player);
        double finalPrice = economyManager.applyPrestigeEarningsBonus(player, basePrice);
        
        // Show bonus if applicable
        boolean hasPrestigeBonus = finalPrice > basePrice;

        // Process transaction
        economyManager.addBalance(player, finalPrice);
        economyManager.recordEarnings(player, finalPrice);

        // Update stats and challenges
        if (plugin.getStatsManager() != null) {
            PlayerStats stats = plugin.getStatsManager().getStats(player);
            stats.incrementSuccessfulSales();
            stats.recordSale(finalPrice);
        }
        
        // Update challenge progress
        if (plugin.getChallengeManager() != null) {
            plugin.getChallengeManager().updateProgress(player, Challenge.ChallengeType.SELL_PRODUCTS, 1);
            plugin.getChallengeManager().updateProgress(player, Challenge.ChallengeType.SUCCESSFUL_TRADES, 1);
            plugin.getChallengeManager().updateProgress(player, Challenge.ChallengeType.EARN_MONEY, (int) finalPrice);
        }
        
        // Record purchase in BuyerRegistry for unified buyer tracking
        if (plugin.getBuyerRegistry() != null) {
            UUID buyerId = null;
            String buyerTypeName = npcType.name();
            
            // Map NPC type to buyer ID in registry
            if (npcType == NPCType.MARKET_JOE) {
                com.budlords.npc.IndividualBuyer marketJoe = plugin.getBuyerRegistry().getMarketJoe();
                if (marketJoe != null) {
                    buyerId = marketJoe.getId();
                }
            } else if (npcType == NPCType.BLACKMARKET_JOE) {
                com.budlords.npc.IndividualBuyer blackMarketJoe = plugin.getBuyerRegistry().getBlackMarketJoe();
                if (blackMarketJoe != null) {
                    buyerId = blackMarketJoe.getId();
                }
            }
            
            // Record the purchase
            if (buyerId != null) {
                plugin.getBuyerRegistry().recordPurchase(buyerId, strainId, weight, finalPrice);
            }
            
            // Also update old reputation system for backward compatibility
            if (plugin.getReputationManager() != null) {
                int repGain = plugin.getReputationManager().calculateReputationGain(finalPrice, true);
                plugin.getReputationManager().addReputation(playerId, buyerTypeName, repGain);
            }
        }

        // Remove item from hand
        if (item.getAmount() == 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        String message = "§aSold " + strain.getName() + " - " + weight + "g for §e" + 
                economyManager.formatMoney(finalPrice) + "§a!";
        if (hasPrestigeBonus) {
            message += " §5(+Prestige Bonus!)";
        }
        return new TradeResult(true, message, finalPrice);
    }

    private double calculateSuccessChance(Player player, Strain strain, int weight, NPCType npcType) {
        RankManager.Rank rank = rankManager.getRank(player);
        
        // Base chance from rank
        double chance = rank.successChanceBonus();
        
        // Potency affects chance (higher potency = slightly riskier but more valuable)
        chance -= (strain.getPotency() - 50) * 0.001;
        
        // Rarity affects chance
        chance -= switch (strain.getRarity()) {
            case COMMON -> 0;
            case UNCOMMON -> 0.02;
            case RARE -> 0.05;
            case LEGENDARY -> 0.08;
        };
        
        // Weight affects chance (larger deals more risky)
        chance -= weight * 0.005;
        
        // Black market bonus
        if (npcType == NPCType.BLACKMARKET_JOE) {
            chance += 0.1;
        }
        
        // Apply prestige success bonus
        if (plugin.getPrestigeManager() != null && plugin.getStatsManager() != null) {
            com.budlords.stats.PlayerStats stats = plugin.getStatsManager().getStats(player);
            if (stats != null && stats.getPrestigeLevel() > 0) {
                chance += plugin.getPrestigeManager().getSuccessBonus(stats.getPrestigeLevel());
            }
        }
        
        return Math.max(0.3, Math.min(0.98, chance));
    }

    private double calculateFinalPrice(double baseValue, Strain strain, NPCType npcType, Player player) {
        double multiplier = 1.0;
        
        // NPC type multiplier
        multiplier *= switch (npcType) {
            case MARKET_JOE -> 1.0;
            case BLACKMARKET_JOE -> 1.5; // Black market pays more for rare strains
            case VILLAGE_VENDOR -> 0.8; // Village vendors pay less
            default -> 1.0;
        };
        
        // Rarity bonus for black market
        if (npcType == NPCType.BLACKMARKET_JOE) {
            multiplier *= switch (strain.getRarity()) {
                case COMMON -> 1.0;
                case UNCOMMON -> 1.1;
                case RARE -> 1.3;
                case LEGENDARY -> 1.5;
            };
        }
        
        return baseValue * multiplier;
    }

    private boolean isOnCooldown(UUID playerId) {
        Long cooldownEnd = tradeCooldowns.get(playerId);
        if (cooldownEnd == null) return false;
        return System.currentTimeMillis() < cooldownEnd;
    }

    private long getCooldownRemaining(UUID playerId) {
        Long cooldownEnd = tradeCooldowns.get(playerId);
        if (cooldownEnd == null) return 0;
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }

    private void applyCooldown(UUID playerId) {
        int cooldownSeconds = plugin.getConfig().getInt("trading.failed-deal-cooldown-seconds", 30);
        tradeCooldowns.put(playerId, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    public enum NPCType {
        NONE,
        MARKET_JOE,
        BLACKMARKET_JOE,
        VILLAGE_VENDOR,
        CONFIGURABLE_MOB  // Entity types enabled in config trading.allowed-mobs
    }

    public record TradeResult(boolean success, String message, double amount) {
    }
}
