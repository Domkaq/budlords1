package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.challenges.Challenge;
import com.budlords.economy.EconomyManager;
import com.budlords.joint.JointItems;
import com.budlords.npc.NPCManager;
import com.budlords.packaging.PackagingManager;
import com.budlords.quality.StarRating;
import com.budlords.stats.PlayerStats;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Schedule 1 style selling GUI for selling packaged products to mobs.
 * Provides a professional trading interface with item slots and pricing.
 */
public class MobSaleGUI implements InventoryHolder, Listener {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final PackagingManager packagingManager;
    private final StrainManager strainManager;
    
    // Active sale sessions
    private final Map<UUID, SaleSession> activeSessions;
    
    // Per-entity cooldowns: EntityUUID -> expiry time (prevents selling to same entity repeatedly)
    private final Map<UUID, Long> entityCooldowns;
    
    // Cooldown time in milliseconds (30 seconds default)
    private static final long ENTITY_COOLDOWN_MS = 30000L;
    
    // Slots for items to sell (4 slots in the middle)
    private static final int[] SALE_SLOTS = {20, 21, 22, 23};
    private static final int CONFIRM_SLOT = 31;
    private static final int CANCEL_SLOT = 27;
    private static final int INFO_SLOT = 4;

    public MobSaleGUI(BudLords plugin, EconomyManager economyManager, 
                      PackagingManager packagingManager, StrainManager strainManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.packagingManager = packagingManager;
        this.strainManager = strainManager;
        this.activeSessions = new ConcurrentHashMap<>();
        this.entityCooldowns = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the sale GUI for a player with a specific entity buyer.
     */
    @SuppressWarnings("deprecation")
    public void open(Player player, Entity buyer, NPCManager.NPCType buyerType) {
        // Check if this entity is on cooldown
        if (isEntityOnCooldown(buyer.getUniqueId())) {
            long remaining = getEntityCooldownRemaining(buyer.getUniqueId()) / 1000;
            player.sendMessage("§c" + getBuyerName(buyerType) + " is still recovering from the last deal!");
            player.sendMessage("§7Wait §e" + remaining + " seconds §7before selling to them again.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        
        SaleSession session = new SaleSession(player.getUniqueId(), buyer.getUniqueId(), buyerType);
        activeSessions.put(player.getUniqueId(), session);
        
        String buyerName = getBuyerName(buyerType);
        Inventory inv = Bukkit.createInventory(this, 45, "§a§l💰 Sell to " + buyerName);
        updateInventory(inv, session, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.5f, 1.2f);
    }
    
    /**
     * Checks if an entity is on sale cooldown.
     */
    private boolean isEntityOnCooldown(UUID entityId) {
        Long cooldownEnd = entityCooldowns.get(entityId);
        if (cooldownEnd == null) return false;
        if (System.currentTimeMillis() >= cooldownEnd) {
            entityCooldowns.remove(entityId);
            return false;
        }
        return true;
    }
    
    /**
     * Gets the remaining cooldown time for an entity.
     */
    private long getEntityCooldownRemaining(UUID entityId) {
        Long cooldownEnd = entityCooldowns.get(entityId);
        if (cooldownEnd == null) return 0;
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }
    
    /**
     * Applies a cooldown to an entity after a sale.
     */
    private void applyEntityCooldown(UUID entityId) {
        entityCooldowns.put(entityId, System.currentTimeMillis() + ENTITY_COOLDOWN_MS);
    }

    private String getBuyerName(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "Market Joe";
            case BLACKMARKET_JOE -> "BlackMarket Joe";
            case VILLAGE_VENDOR -> "Village Vendor";
            default -> "Customer";
        };
    }

    private void updateInventory(Inventory inv, SaleSession session, Player player) {
        inv.clear();

        // Border
        ItemStack borderGreen = createItem(Material.LIME_STAINED_GLASS_PANE, " ", null);
        ItemStack borderDark = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderGreen : borderDark);
            inv.setItem(36 + i, i % 2 == 0 ? borderGreen : borderDark);
        }
        // Side borders
        for (int i = 9; i < 36; i += 9) {
            inv.setItem(i, borderDark);
            inv.setItem(i + 8, borderDark);
        }

        // Header info
        String buyerName = getBuyerName(session.buyerType);
        double balance = economyManager.getBalance(player);
        ItemStack header = createItem(Material.EMERALD, 
            "§a§l💰 Sale to " + buyerName,
            Arrays.asList(
                "",
                "§7Your balance: §e" + economyManager.formatMoney(balance),
                "",
                "§7Place packaged products in the",
                "§7slots below to sell them.",
                "",
                "§7" + buyerName + " buys:",
                getPriceModifierText(session.buyerType)
            ));
        inv.setItem(INFO_SLOT, header);

        // Sale slots (empty or with items)
        for (int i = 0; i < SALE_SLOTS.length; i++) {
            int slot = SALE_SLOTS[i];
            ItemStack saleItem = session.itemsToSell[i];
            
            if (saleItem == null) {
                inv.setItem(slot, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 
                    "§7§l[ Empty Slot ]",
                    Arrays.asList(
                        "",
                        "§7Click here with a",
                        "§7packaged product to sell"
                    )));
            } else {
                inv.setItem(slot, saleItem);
            }
        }

        // Calculate total value
        double totalValue = calculateTotalValue(session);
        
        // Price display
        inv.setItem(13, createItem(Material.GOLD_INGOT, 
            "§e§lTotal Value",
            Arrays.asList(
                "",
                "§7Items: §e" + countItems(session),
                "§7Total: §a" + economyManager.formatMoney(totalValue),
                "",
                getPriceBreakdown(session)
            )));

        // Confirm button
        boolean hasItems = totalValue > 0;
        inv.setItem(CONFIRM_SLOT, createItem(
            hasItems ? Material.EMERALD_BLOCK : Material.COAL_BLOCK,
            hasItems ? "§a§l✓ CONFIRM SALE" : "§7§l✗ No Items",
            hasItems ? Arrays.asList(
                "",
                "§7Click to sell §e" + countItems(session) + " §7item(s)",
                "§7for §a" + economyManager.formatMoney(totalValue),
                "",
                "§a▶ Click to confirm"
            ) : Arrays.asList(
                "",
                "§7Place items in the slots",
                "§7above to sell them"
            )));

        // Cancel button
        inv.setItem(CANCEL_SLOT, createItem(Material.BARRIER, "§c§l✗ Cancel",
            Arrays.asList(
                "",
                "§7Close without selling",
                "§7Items will be returned"
            )));

        // Tips
        inv.setItem(35, createItem(Material.BOOK, "§e§l? Tips",
            Arrays.asList(
                "",
                "§7• Higher quality = higher price",
                "§7• Rare strains pay more",
                "§7• BlackMarket pays premium",
                "§7• Village vendors pay less"
            )));
    }

    private String getPriceModifierText(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "§a• Standard prices";
            case BLACKMARKET_JOE -> "§5• Premium prices (+50%)";
            case VILLAGE_VENDOR -> "§e• Lower prices (-20%)";
            default -> "§7• Standard prices";
        };
    }

    private double getPriceMultiplier(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> 1.0;
            case BLACKMARKET_JOE -> 1.5;
            case VILLAGE_VENDOR -> 0.8;
            default -> 1.0;
        };
    }

    private double calculateTotalValue(SaleSession session) {
        double total = 0;
        double multiplier = getPriceMultiplier(session.buyerType);
        
        for (ItemStack item : session.itemsToSell) {
            if (item == null) continue;
            
            double value = 0;
            Strain strain = null;
            String strainId = null;
            
            if (packagingManager.isPackagedProduct(item)) {
                value = packagingManager.getValueFromPackage(item);
                
                // Get strain for rarity bonus
                strainId = packagingManager.getStrainIdFromPackage(item);
                strain = strainManager.getStrain(strainId);
            } else if (JointItems.isJoint(item)) {
                // Calculate joint value
                strainId = JointItems.getJointStrainId(item);
                strain = strainManager.getStrain(strainId);
                int potency = JointItems.getJointPotency(item);
                StarRating rating = JointItems.getJointRating(item);
                
                // Base joint value = potency * quality multiplier * 2 (joints are premium)
                double qualityMult = rating != null ? rating.getQualityMultiplier() : 1.0;
                value = potency * qualityMult * 2.0;
                
                // Add strain rarity bonus
                if (strain != null) {
                    value *= switch (strain.getRarity()) {
                        case COMMON -> 1.0;
                        case UNCOMMON -> 1.2;
                        case RARE -> 1.5;
                        case LEGENDARY -> 2.5;
                    };
                }
            } else {
                continue; // Skip non-sellable items
            }
            
            // Apply black market rarity bonus
            if (strain != null && session.buyerType == NPCManager.NPCType.BLACKMARKET_JOE) {
                value *= switch (strain.getRarity()) {
                    case COMMON -> 1.0;
                    case UNCOMMON -> 1.1;
                    case RARE -> 1.3;
                    case LEGENDARY -> 1.5;
                };
            }
            
            // Apply dynamic market demand multiplier
            if (strainId != null && plugin.getMarketDemandManager() != null) {
                double demandMultiplier = plugin.getMarketDemandManager().getDemandMultiplier(strainId);
                value *= demandMultiplier;
            }
            
            total += value * multiplier * item.getAmount();
        }
        return total;
    }

    private int countItems(SaleSession session) {
        int count = 0;
        for (ItemStack item : session.itemsToSell) {
            if (item != null) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private String getPriceBreakdown(SaleSession session) {
        StringBuilder breakdown = new StringBuilder();
        double multiplier = getPriceMultiplier(session.buyerType);
        
        for (ItemStack item : session.itemsToSell) {
            if (item == null) continue;
            
            String name = "Unknown";
            double value = 0;
            
            if (packagingManager.isPackagedProduct(item)) {
                String strainId = packagingManager.getStrainIdFromPackage(item);
                Strain strain = strainManager.getStrain(strainId);
                name = strain != null ? strain.getName() : "Unknown";
                value = packagingManager.getValueFromPackage(item) * multiplier * item.getAmount();
            } else if (JointItems.isJoint(item)) {
                String strainId = JointItems.getJointStrainId(item);
                Strain strain = strainManager.getStrain(strainId);
                name = (strain != null ? strain.getName() : "Unknown") + " Joint";
                int potency = JointItems.getJointPotency(item);
                StarRating rating = JointItems.getJointRating(item);
                double qualityMult = rating != null ? rating.getQualityMultiplier() : 1.0;
                value = potency * qualityMult * 2.0;
                if (strain != null) {
                    value *= switch (strain.getRarity()) {
                        case COMMON -> 1.0;
                        case UNCOMMON -> 1.2;
                        case RARE -> 1.5;
                        case LEGENDARY -> 2.5;
                    };
                }
                value *= multiplier * item.getAmount();
            } else {
                continue;
            }
            
            breakdown.append("§7• ").append(name).append(": §e").append(economyManager.formatMoney(value)).append("\n");
        }
        
        return breakdown.length() > 0 ? breakdown.toString().trim() : "§7No items";
    }
    
    /**
     * Checks if an item is sellable (packaged product or joint).
     */
    private boolean isSellableItem(ItemStack item) {
        if (item == null) return false;
        return packagingManager.isPackagedProduct(item) || JointItems.isJoint(item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MobSaleGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        SaleSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        int slot = event.getRawSlot();

        // Player inventory - allow picking up items
        if (slot >= 45) {
            // Clicking in player inventory - allow unless shift-clicking non-sellable items
            ItemStack clicked = event.getCurrentItem();
            if (event.isShiftClick() && clicked != null && isSellableItem(clicked)) {
                // Find empty sale slot
                for (int i = 0; i < SALE_SLOTS.length; i++) {
                    if (session.itemsToSell[i] == null) {
                        session.itemsToSell[i] = clicked.clone();
                        clicked.setAmount(0);
                        event.setCancelled(true);
                        updateInventory(event.getInventory(), session, player);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.2f);
                        return;
                    }
                }
                event.setCancelled(true);
                player.sendMessage("§cAll sale slots are full!");
            }
            return;
        }

        event.setCancelled(true);

        // Sale slots - place or remove items
        for (int i = 0; i < SALE_SLOTS.length; i++) {
            if (slot == SALE_SLOTS[i]) {
                ItemStack cursor = event.getCursor();
                
                // Placing item
                if (cursor != null && cursor.getType() != Material.AIR) {
                    if (isSellableItem(cursor)) {
                        if (session.itemsToSell[i] != null) {
                            // Return existing item to cursor
                            player.setItemOnCursor(session.itemsToSell[i]);
                        }
                        session.itemsToSell[i] = cursor.clone();
                        player.setItemOnCursor(null);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.2f);
                    } else {
                        player.sendMessage("§cYou can only sell packaged products and joints!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    }
                } else if (session.itemsToSell[i] != null) {
                    // Picking up item
                    player.setItemOnCursor(session.itemsToSell[i]);
                    session.itemsToSell[i] = null;
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 0.8f);
                }
                
                updateInventory(event.getInventory(), session, player);
                return;
            }
        }

        // Confirm button
        if (slot == CONFIRM_SLOT) {
            double total = calculateTotalValue(session);
            if (total <= 0) {
                player.sendMessage("§cNo items to sell!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            completeSale(player, session, total);
            return;
        }

        // Cancel button
        if (slot == CANCEL_SLOT) {
            returnItems(player, session);
            player.closeInventory();
            player.sendMessage("§cSale cancelled.");
            player.playSound(player.getLocation(), Sound.BLOCK_BARREL_CLOSE, 0.5f, 0.8f);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MobSaleGUI)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        SaleSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            // Return any remaining items
            returnItems(player, session);
        }
    }

    private void completeSale(Player player, SaleSession session, double total) {
        UUID playerId = player.getUniqueId();
        String buyerTypeName = session.buyerType.name();
        
        // Track the base price before any bonuses for display
        double basePrice = total;
        
        // Apply skill PRICE_BONUS (e.g., Haggler +5%, Premium Prices +10%, Master Dealer +15%)
        double skillPriceBonus = 1.0;
        if (plugin.getSkillManager() != null) {
            skillPriceBonus = plugin.getSkillManager().getBonusMultiplier(playerId, 
                com.budlords.skills.Skill.BonusType.PRICE_BONUS);
            total *= skillPriceBonus;
        }
        
        // Apply black market skill bonus if selling to black market
        double blackMarketSkillBonus = 1.0;
        if (session.buyerType == NPCManager.NPCType.BLACKMARKET_JOE && plugin.getSkillManager() != null) {
            blackMarketSkillBonus = plugin.getSkillManager().getBonusMultiplier(playerId, 
                com.budlords.skills.Skill.BonusType.BLACK_MARKET_BONUS);
            total *= blackMarketSkillBonus;
        }
        
        // Apply prestige earnings bonus
        double prestigeMultiplier = 1.0;
        if (plugin.getPrestigeManager() != null && plugin.getStatsManager() != null) {
            com.budlords.stats.PlayerStats stats = plugin.getStatsManager().getStats(player);
            if (stats != null && stats.getPrestigeLevel() > 0) {
                prestigeMultiplier = plugin.getPrestigeManager().getEarningsMultiplier(stats.getPrestigeLevel());
                total *= prestigeMultiplier;
            }
        }
        
        // Apply reputation bonus
        double reputationMultiplier = 1.0;
        if (plugin.getReputationManager() != null) {
            reputationMultiplier = plugin.getReputationManager().getReputationMultiplier(playerId, buyerTypeName);
            total *= reputationMultiplier;
        }
        
        // Check for bulk order fulfillment bonus
        double bulkOrderBonus = 1.0;
        if (plugin.getBulkOrderManager() != null) {
            String strainId = getFirstStrainId(session);
            if (strainId != null) {
                bulkOrderBonus = plugin.getBulkOrderManager().checkOrderFulfillment(playerId, strainId, countItems(session));
                total *= bulkOrderBonus;
            }
        }
        
        // Process the base sale
        economyManager.addBalance(player, total);
        economyManager.recordEarnings(player, total);
        
        // Calculate and apply tip
        double tip = 0;
        if (plugin.getReputationManager() != null) {
            tip = plugin.getReputationManager().calculateTip(playerId, buyerTypeName, total);
            if (tip > 0) {
                economyManager.addBalance(player, tip);
                economyManager.recordEarnings(player, tip);
            }
        }
        
        // Update reputation
        if (plugin.getReputationManager() != null) {
            int repGain = plugin.getReputationManager().calculateReputationGain(total, true);
            plugin.getReputationManager().addReputation(playerId, buyerTypeName, repGain);
        }
        
        // Count sold items for stats
        int itemsSold = countItems(session);
        
        // Update stats - count this as a single successful trade/transaction
        if (plugin.getStatsManager() != null) {
            PlayerStats stats = plugin.getStatsManager().getStats(player);
            stats.incrementSuccessfulSales();
            stats.recordSale(total + tip);
        }
        
        // Update challenges
        if (plugin.getChallengeManager() != null) {
            plugin.getChallengeManager().updateProgress(player, Challenge.ChallengeType.SELL_PRODUCTS, itemsSold);
            plugin.getChallengeManager().updateProgress(player, Challenge.ChallengeType.SUCCESSFUL_TRADES, 1);
            plugin.getChallengeManager().updateProgress(player, Challenge.ChallengeType.EARN_MONEY, (int) (total + tip));
        }
        
        // Award Trading skill XP
        if (plugin.getSkillManager() != null) {
            com.budlords.skills.SkillManager skillManager = plugin.getSkillManager();
            
            // Base 10 XP per trade, plus bonus based on sale value
            int tradingXP = 10 + (int) (total / 100); // +1 XP per $100 earned
            tradingXP = Math.min(tradingXP, 50); // Cap at 50 XP per trade
            
            // Bonus for black market trades
            if (session.buyerType == NPCManager.NPCType.BLACKMARKET_JOE) {
                tradingXP += 5;
            }
            
            // Bonus for bulk order completion
            if (bulkOrderBonus > 1.0) {
                tradingXP += 10;
            }
            
            skillManager.addTreeXP(playerId, com.budlords.skills.Skill.SkillTree.TRADING, tradingXP);
        }
        
        // Sync achievements with stats
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().syncWithStats(player);
        }
        
        // Apply cooldown to this buyer entity (prevents selling to same entity repeatedly)
        applyEntityCooldown(session.buyerId);
        
        // Get strain info from sold items to apply effects to buyer
        Strain soldStrain = null;
        StarRating soldRating = null;
        
        for (ItemStack item : session.itemsToSell) {
            if (item == null) continue;
            
            // Try to get strain from packaged product
            if (packagingManager.isPackagedProduct(item)) {
                String strainId = packagingManager.getStrainIdFromPackage(item);
                if (strainId != null) {
                    soldStrain = strainManager.getStrain(strainId);
                    // Get rating from lore if available
                    if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                        for (String line : item.getItemMeta().getLore()) {
                            if (line.contains("★")) {
                                int stars = (int) line.chars().filter(ch -> ch == '★').count();
                                soldRating = StarRating.fromValue(Math.min(StarRating.MAX_STARS, 
                                    Math.max(StarRating.MIN_STARS, stars)));
                                break;
                            }
                        }
                    }
                    break;
                }
            }
            
            // Try to get strain from joint
            if (JointItems.isJoint(item)) {
                String strainId = JointItems.getJointStrainId(item);
                if (strainId != null) {
                    soldStrain = strainManager.getStrain(strainId);
                    soldRating = JointItems.getJointRating(item);
                    break;
                }
            }
        }
        
        // Apply strain effects to buyer entity (villager gets high from the product!)
        applyEffectsToBuyer(session.buyerId, soldStrain, soldRating);
        
        // Clear sold items
        for (int i = 0; i < session.itemsToSell.length; i++) {
            session.itemsToSell[i] = null;
        }
        
        // Close and celebrate
        activeSessions.remove(player.getUniqueId());
        player.closeInventory();
        
        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        player.spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
        
        // Get reputation comment
        String buyerComment = "";
        if (plugin.getReputationManager() != null) {
            int rep = plugin.getReputationManager().getReputation(playerId, buyerTypeName);
            buyerComment = plugin.getReputationManager().getReputationComment(rep);
        }
        
        player.sendMessage("");
        player.sendMessage("§a§l✓ SALE COMPLETE!");
        player.sendMessage("§7Sold §e" + itemsSold + " §7item(s) for §a" + economyManager.formatMoney(total));
        if (tip > 0) {
            player.sendMessage("§6§l★ TIP: §e+" + economyManager.formatMoney(tip) + " §7(Buyer was impressed!)");
        }
        if (skillPriceBonus > 1.0) {
            player.sendMessage("§b§l★ SKILL BONUS: §e+" + String.format("%.0f%%", (skillPriceBonus - 1) * 100) + " §7(Sale Prices)");
        }
        if (blackMarketSkillBonus > 1.0) {
            player.sendMessage("§8§l★ BLACK MARKET VIP: §e+" + String.format("%.0f%%", (blackMarketSkillBonus - 1) * 100));
        }
        if (prestigeMultiplier > 1.0) {
            player.sendMessage("§5§l★ PRESTIGE BONUS: §e+" + String.format("%.0f%%", (prestigeMultiplier - 1) * 100));
        }
        if (bulkOrderBonus > 1.0) {
            player.sendMessage("§d§l★ BULK BONUS: §e+" + String.format("%.0f%%", (bulkOrderBonus - 1) * 100));
        }
        if (reputationMultiplier > 1.0) {
            player.sendMessage("§a§l★ REP BONUS: §e+" + String.format("%.0f%%", (reputationMultiplier - 1) * 100));
        }
        player.sendMessage("§7New balance: §e" + economyManager.formatMoney(economyManager.getBalance(player)));
        if (!buyerComment.isEmpty()) {
            player.sendMessage("");
            player.sendMessage("§7Buyer says: " + buyerComment);
        }
        player.sendMessage("");
    }
    
    /**
     * Gets the first strain ID from items in the session.
     */
    private String getFirstStrainId(SaleSession session) {
        for (ItemStack item : session.itemsToSell) {
            if (item == null) continue;
            if (packagingManager.isPackagedProduct(item)) {
                return packagingManager.getStrainIdFromPackage(item);
            }
            if (JointItems.isJoint(item)) {
                return JointItems.getJointStrainId(item);
            }
        }
        return null;
    }
    
    /**
     * Applies strain effects to the buyer entity after purchase.
     * The villager/buyer "gets high" from the product they bought!
     */
    private void applyEffectsToBuyer(UUID buyerId, Strain strain, StarRating rating) {
        org.bukkit.entity.Entity buyer = Bukkit.getEntity(buyerId);
        if (buyer == null || !(buyer instanceof org.bukkit.entity.LivingEntity living)) return;
        
        // Effect duration - 10-20 seconds (200-400 ticks)
        int duration = 200 + (rating != null ? rating.getStars() * 40 : 0);
        
        // Apply effects via StrainEffectsManager
        if (plugin.getStrainEffectsManager() != null && strain != null) {
            plugin.getStrainEffectsManager().applyStrainEffectsToEntity(living, strain, rating, duration);
        } else {
            // Fallback - apply generic "high" effects with smoking animation
            playBuyerSmokingAnimation(buyerId, null);
        }
    }
    
    /**
     * Plays a smoking/high animation for the buyer entity after purchase.
     */
    private void playBuyerSmokingAnimation(UUID buyerId, Player player) {
        org.bukkit.entity.Entity buyer = Bukkit.getEntity(buyerId);
        if (buyer == null) return;
        
        org.bukkit.Location buyerLoc = buyer.getLocation();
        org.bukkit.World world = buyerLoc.getWorld();
        if (world == null) return;
        
        // Initial smoke puff when they receive the product
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, buyerLoc.clone().add(0, 1.5, 0), 10, 0.2, 0.2, 0.2, 0.02);
        world.playSound(buyerLoc, Sound.ENTITY_PLAYER_BREATH, 0.5f, 0.8f);
        
        // Schedule repeated smoke effects to simulate smoking
        for (int delay = 20; delay <= 100; delay += 20) {
            final int currentDelay = delay;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(buyerId);
                if (entity == null || !entity.isValid()) return;
                
                org.bukkit.Location loc = entity.getLocation();
                org.bukkit.World w = loc.getWorld();
                if (w == null) return;
                
                // Smoke particles from head
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 1.8, 0), 
                    5 + (currentDelay / 20), 0.15, 0.1, 0.15, 0.01);
                
                // Small cloud particles for "high" effect
                if (currentDelay >= 40) {
                    w.spawnParticle(Particle.CLOUD, loc.clone().add(0, 2.0, 0), 
                        3, 0.3, 0.2, 0.3, 0.01);
                }
                
                // Happy particles (they're enjoying it!)
                if (currentDelay >= 60) {
                    double offsetX = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
                    double offsetZ = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
                    w.spawnParticle(Particle.HEART, loc.clone().add(offsetX, 2.2, offsetZ), 1, 0, 0, 0, 0);
                }
                
                // Sound effects
                if (currentDelay == 40) {
                    w.playSound(loc, Sound.ENTITY_PLAYER_BREATH, 0.4f, 0.7f);
                } else if (currentDelay == 80) {
                    w.playSound(loc, Sound.ENTITY_VILLAGER_CELEBRATE, 0.5f, 1.0f);
                }
            }, delay);
        }
        
        // Final satisfied sound
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(buyerId);
            if (entity == null || !entity.isValid()) return;
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.7f, 1.2f);
        }, 120);
    }

    private void returnItems(Player player, SaleSession session) {
        for (ItemStack item : session.itemsToSell) {
            if (item != null) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(i -> 
                        player.getWorld().dropItemNaturally(player.getLocation(), i)
                    );
                }
            }
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    /**
     * Session data for a sale.
     */
    private static class SaleSession {
        final UUID playerId;
        final UUID buyerId;
        final NPCManager.NPCType buyerType;
        final ItemStack[] itemsToSell;

        SaleSession(UUID playerId, UUID buyerId, NPCManager.NPCType buyerType) {
            this.playerId = playerId;
            this.buyerId = buyerId;
            this.buyerType = buyerType;
            this.itemsToSell = new ItemStack[4];
        }
    }
}
