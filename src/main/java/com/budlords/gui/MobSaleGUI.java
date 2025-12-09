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
 * PROFESSIONAL PREMIUM SALE GUI - Schedule 1 style selling interface
 * Features: Enhanced visuals, bulk selling, quick actions, detailed analytics
 * Quality of Life: Auto-sort, quick-fill, price preview, bonus indicators
 */
public class MobSaleGUI implements InventoryHolder, Listener {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final PackagingManager packagingManager;
    private final StrainManager strainManager;
    
    // Active sale sessions with enhanced tracking
    private final Map<UUID, SaleSession> activeSessions;
    
    // Per-entity cooldowns: EntityUUID -> expiry time (prevents selling to same entity repeatedly)
    private final Map<UUID, Long> entityCooldowns;
    
    // Cooldown time in milliseconds (30 seconds default)
    private static final long ENTITY_COOLDOWN_MS = 30000L;
    
    // ENHANCED: More sale slots for bulk operations (6 slots instead of 4)
    private static final int[] SALE_SLOTS = {19, 20, 21, 22, 23, 24};
    private static final int CONFIRM_SLOT = 40;
    private static final int CANCEL_SLOT = 38;
    private static final int INFO_SLOT = 4;
    private static final int QUICK_FILL_SLOT = 10; // NEW: Auto-fill button
    private static final int SORT_SLOT = 16; // NEW: Sort items button
    
    // Dose-based success chance penalty constants
    // Large sales (more grams) are riskier
    private static final int DOSE_PENALTY_THRESHOLD = 15;  // Grams before penalty applies
    private static final double DOSE_PENALTY_DIVISOR = 5.0; // Grams per penalty increment
    private static final double DOSE_PENALTY_RATE = 0.01;   // 1% penalty per increment

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
     * Opens the PROFESSIONAL PREMIUM sale GUI for a player with a specific entity buyer.
     * ENHANCED: Better visuals, animations, and user feedback
     */
    @SuppressWarnings("deprecation")
    public void open(Player player, Entity buyer, NPCManager.NPCType buyerType) {
        // Check if this entity is on cooldown
        if (isEntityOnCooldown(buyer.getUniqueId())) {
            long remaining = getEntityCooldownRemaining(buyer.getUniqueId()) / 1000;
            player.sendMessage("");
            player.sendMessage("§c§l⏰ BUYER COOLDOWN");
            player.sendMessage("§7" + getBuyerName(buyerType) + " §cis still recovering from the last deal!");
            player.sendMessage("§7Wait §e" + remaining + "s §7before selling to them again.");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            player.spawnParticle(Particle.SMOKE_NORMAL, buyer.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.01);
            return;
        }
        
        SaleSession session = new SaleSession(player.getUniqueId(), buyer.getUniqueId(), buyerType);
        activeSessions.put(player.getUniqueId(), session);
        
        String buyerName = getBuyerName(buyerType);
        // ENHANCED: Larger inventory for more features (54 slots)
        Inventory inv = Bukkit.createInventory(this, 54, "§6§l✦ §a§lPREMIUM TRADE §6§l✦ §f" + buyerName);
        updateInventory(inv, session, player);
        player.openInventory(inv);
        
        // PROFESSIONAL: Better opening sound and visual effects
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.3f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.4f, 1.5f);
        player.spawnParticle(Particle.VILLAGER_HAPPY, buyer.getLocation().add(0, 1.5, 0), 8, 0.3, 0.3, 0.3, 0.01);
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

        // PROFESSIONAL: Premium animated border with better colors
        ItemStack borderGold = createItem(Material.YELLOW_STAINED_GLASS_PANE, "§6✦", null);
        ItemStack borderEmerald = createItem(Material.LIME_STAINED_GLASS_PANE, "§a✦", null);
        ItemStack borderDark = createItem(Material.BLACK_STAINED_GLASS_PANE, "§0✦", null);
        
        // Top border - premium gradient
        for (int i = 0; i < 9; i++) {
            if (i == 0 || i == 8) {
                inv.setItem(i, borderGold);
            } else if (i % 2 == 0) {
                inv.setItem(i, borderEmerald);
            } else {
                inv.setItem(i, borderDark);
            }
        }
        
        // Bottom border - matching gradient
        for (int i = 45; i < 54; i++) {
            if (i == 45 || i == 53) {
                inv.setItem(i, borderGold);
            } else if (i % 2 == 0) {
                inv.setItem(i, borderEmerald);
            } else {
                inv.setItem(i, borderDark);
            }
        }
        
        // Side borders
        for (int i = 9; i < 45; i += 9) {
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
        double successChance = calculateSuccessChance(player, session);
        String chanceColor = getSuccessChanceColor(successChance);
        
        // Price display with detailed bonuses
        List<String> priceInfo = new ArrayList<>();
        priceInfo.add("");
        priceInfo.add("§7Items: §e" + countItems(session));
        priceInfo.add("§7Base Total: §e" + economyManager.formatMoney(calculateBaseTotalValue(session)));
        priceInfo.add("");
        
        // Show active bonuses
        UUID playerId = player.getUniqueId();
        boolean hasBonuses = false;
        
        if (plugin.getSkillManager() != null) {
            double skillBonus = plugin.getSkillManager().getTotalBonus(playerId, 
                com.budlords.skills.Skill.BonusType.PRICE_BONUS);
            if (skillBonus > 0) {
                priceInfo.add("§a✦ Skills: §e+" + String.format("%.0f%%", skillBonus * 100));
                hasBonuses = true;
            }
        }
        
        if (plugin.getPrestigeManager() != null && plugin.getStatsManager() != null) {
            PlayerStats stats = plugin.getStatsManager().getStats(player);
            if (stats != null && stats.getPrestigeLevel() > 0) {
                double prestigeBonus = (plugin.getPrestigeManager().getEarningsMultiplier(stats.getPrestigeLevel()) - 1.0);
                priceInfo.add("§d✦ Prestige: §e+" + String.format("%.0f%%", prestigeBonus * 100));
                hasBonuses = true;
            }
        }
        
        if (plugin.getReputationManager() != null) {
            int rep = plugin.getReputationManager().getReputation(playerId, session.buyerType.name());
            double repBonus = (plugin.getReputationManager().getReputationMultiplier(rep) - 1.0);
            if (repBonus != 0) {
                String repColor = repBonus > 0 ? "§a" : "§c";
                priceInfo.add(repColor + "✦ Reputation: §e" + (repBonus > 0 ? "+" : "") + String.format("%.0f%%", repBonus * 100));
                hasBonuses = true;
            }
        }
        
        if (hasBonuses) {
            priceInfo.add("");
        }
        
        priceInfo.add("§7Final Total: §a§l" + economyManager.formatMoney(totalValue));
        priceInfo.add("");
        priceInfo.add(getPriceBreakdown(session));
        
        inv.setItem(13, createItem(Material.GOLD_INGOT, "§e§lTotal Value", priceInfo));

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
                "§7Success chance: " + chanceColor + String.format("%.0f%%", successChance * 100),
                "",
                successChance < 0.5 ? "§c⚠ Low chance! Build more reputation." : "§a▶ Click to confirm"
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

        // NEW: Quick Fill Button - Auto-fill slots with sellable items
        int sellableCount = countSellableItemsInInventory(player);
        inv.setItem(QUICK_FILL_SLOT, createItem(Material.HOPPER, 
            "§b§l⚡ QUICK FILL",
            Arrays.asList(
                "",
                "§7Automatically fill sale slots",
                "§7with items from your inventory",
                "",
                "§7Found: §e" + sellableCount + " §7sellable items",
                "",
                "§a▶ Click to auto-fill"
            )));
        
        // NEW: Sort Button - Organize items by value
        boolean hasItemsToSort = countItems(session) > 1;
        inv.setItem(SORT_SLOT, createItem(
            hasItemsToSort ? Material.COMPARATOR : Material.GRAY_DYE,
            hasItemsToSort ? "§d§l⚙ SORT BY VALUE" : "§7§l⚙ Sort",
            hasItemsToSort ? Arrays.asList(
                "",
                "§7Sort items by value",
                "§7(highest to lowest)",
                "",
                "§a▶ Click to sort"
            ) : Arrays.asList(
                "",
                "§7Add multiple items to sort"
            )));

        // Enhanced Tips with more info
        inv.setItem(44, createItem(Material.ENCHANTED_BOOK, "§6§l★ PRO TIPS",
            Arrays.asList(
                "",
                "§e✦ QUALITY MATTERS:",
                "§7  Higher star ratings = more money",
                "",
                "§e✦ STRAIN RARITY:",
                "§7  Legendary strains pay 2.5x more!",
                "",
                "§e✦ BUYER TYPES:",
                "§a  • Market Joe: §7Standard prices",
                "§5  • BlackMarket Joe: §7+50% premium",
                "§e  • Village Vendor: §7-20% discount",
                "",
                "§e✦ BULK SELLING:",
                "§7  Use Quick Fill for faster trades!"
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

    private double calculateBaseTotalValue(SaleSession session) {
        double total = 0;
        double multiplier = getPriceMultiplier(session.buyerType);
        
        for (ItemStack item : session.itemsToSell) {
            if (item == null) continue;
            total += calculateItemPrice(item, session.buyerType) * item.getAmount();
        }
        return total;
    }
    
    private double calculateTotalValue(SaleSession session) {
        double total = 0;
        double multiplier = getPriceMultiplier(session.buyerType);
        
        for (ItemStack item : session.itemsToSell) {
            if (item == null) continue;
            total += calculateItemPrice(item, session.buyerType) * item.getAmount();
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
    
    /**
     * Counts the total doses (grams) in a sale session.
     * This is used for success chance calculation - larger deals are riskier.
     * @param session The sale session
     * @return Total grams being sold
     */
    private int countTotalDoses(SaleSession session) {
        int totalDoses = 0;
        for (ItemStack item : session.itemsToSell) {
            if (item == null) continue;
            
            if (packagingManager.isPackagedProduct(item)) {
                // Get grams from package and multiply by item count
                int gramsPerPackage = packagingManager.getWeightFromPackage(item);
                totalDoses += gramsPerPackage * item.getAmount();
            } else if (JointItems.isJoint(item)) {
                // Joints count as 1 dose per joint
                totalDoses += item.getAmount();
            }
        }
        return totalDoses;
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
     * Calculates the price of a single item (used for buyer purchase tracking).
     */
    private double calculateItemPrice(ItemStack item, NPCManager.NPCType buyerType) {
        double multiplier = getPriceMultiplier(buyerType);
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
            return 0; // Not sellable
        }
        
        // Apply black market rarity bonus
        if (strain != null && buyerType == NPCManager.NPCType.BLACKMARKET_JOE) {
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
        
        return value * multiplier;
    }
    
    /**
     * Calculates the success chance for a sale to display in the UI.
     */
    private double calculateSuccessChance(Player player, SaleSession session) {
        UUID playerId = player.getUniqueId();
        String buyerTypeName = session.buyerType.name();
        
        int reputation = 0;
        if (plugin.getReputationManager() != null) {
            reputation = plugin.getReputationManager().getReputation(playerId, buyerTypeName);
        }
        
        double successChance;
        if (reputation >= com.budlords.economy.ReputationManager.REPUTATION_LEGENDARY) {
            successChance = 0.98;
        } else if (reputation >= com.budlords.economy.ReputationManager.REPUTATION_VIP) {
            successChance = 0.90;
        } else if (reputation >= com.budlords.economy.ReputationManager.REPUTATION_TRUSTED) {
            successChance = 0.80;
        } else if (reputation >= com.budlords.economy.ReputationManager.REPUTATION_FRIENDLY) {
            successChance = 0.65;
        } else if (reputation > com.budlords.economy.ReputationManager.REPUTATION_SUSPICIOUS) {
            successChance = 0.50;
        } else {
            successChance = 0.30;
        }
        
        // Apply skill bonus
        if (plugin.getSkillManager() != null) {
            double skillBonus = plugin.getSkillManager().getBonusMultiplier(playerId, 
                com.budlords.skills.Skill.BonusType.TRADE_SUCCESS);
            successChance = Math.min(0.99, successChance + (skillBonus - 1.0));
        }
        
        // Apply prestige bonus
        if (plugin.getPrestigeManager() != null && plugin.getStatsManager() != null) {
            com.budlords.stats.PlayerStats stats = plugin.getStatsManager().getStats(player);
            if (stats != null && stats.getPrestigeLevel() > 0) {
                double prestigeBonus = stats.getPrestigeLevel() * 0.02;
                successChance = Math.min(0.99, successChance + prestigeBonus);
            }
        }
        
        // Penalty for large sales - based on total doses (grams) being sold
        // This makes selling larger packages riskier than many small ones
        int totalDoses = countTotalDoses(session);
        if (totalDoses > DOSE_PENALTY_THRESHOLD) {
            double penalty = ((totalDoses - DOSE_PENALTY_THRESHOLD) / DOSE_PENALTY_DIVISOR) * DOSE_PENALTY_RATE;
            successChance = Math.max(0.1, successChance - penalty);
        }
        
        return successChance;
    }
    
    /**
     * Gets the color code for a success chance value.
     */
    private String getSuccessChanceColor(double chance) {
        if (chance >= 0.90) return "§a";
        if (chance >= 0.70) return "§e";
        if (chance >= 0.50) return "§6";
        return "§c";
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

        // Player inventory - allow picking up items (54-slot GUI)
        if (slot >= 54) {
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
                
                // Handle shift-click to remove item from sale slot
                if (event.isShiftClick() && session.itemsToSell[i] != null) {
                    // Return item to player inventory
                    ItemStack itemToReturn = session.itemsToSell[i];
                    session.itemsToSell[i] = null;
                    
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemToReturn);
                    if (!leftover.isEmpty()) {
                        // If inventory full, drop at player's location
                        leftover.values().forEach(item -> 
                            player.getWorld().dropItemNaturally(player.getLocation(), item)
                        );
                    }
                    
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 0.8f);
                    updateInventory(event.getInventory(), session, player);
                    return;
                }
                
                // Placing item with cursor
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
                } else if (session.itemsToSell[i] != null && !event.isShiftClick()) {
                    // Picking up item with cursor (regular click)
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
            return;
        }
        
        // NEW: Quick Fill Button
        if (slot == QUICK_FILL_SLOT) {
            int sellableCount = countSellableItemsInInventory(player);
            if (sellableCount == 0) {
                player.sendMessage("§cNo sellable items found in your inventory!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            quickFillSaleSlots(session, player);
            updateInventory(event.getInventory(), session, player);
            player.sendMessage("§a§l✓ §aAuto-filled sale slots!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.3f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
            return;
        }
        
        // NEW: Sort Button
        if (slot == SORT_SLOT) {
            if (countItems(session) <= 1) {
                player.sendMessage("§cNeed at least 2 items to sort!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            sortItemsByValue(session);
            updateInventory(event.getInventory(), session, player);
            player.sendMessage("§a§l✓ §aSorted by value (highest first)!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.4f);
            return;
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
        
        // ========== SALE SUCCESS CHECK ==========
        // Use the same calculation as displayed in the GUI
        double successChance = calculateSuccessChance(player, session);
        
        // Get reputation for failure message
        int reputation = 0;
        if (plugin.getReputationManager() != null) {
            reputation = plugin.getReputationManager().getReputation(playerId, buyerTypeName);
        }
        
        // Roll for success
        if (ThreadLocalRandom.current().nextDouble() > successChance) {
            // SALE FAILED!
            handleFailedSale(player, session, successChance, reputation);
            return;
        }
        
        // ========== SALE SUCCESS - Continue with normal processing ==========
        
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
            // Get the active order to check against
            var activeOrder = plugin.getBulkOrderManager().getActiveOrder(playerId);
            if (activeOrder != null) {
                // Count only items matching the order's strain
                int matchingItems = countItemsOfStrain(session, activeOrder.strainId);
                if (matchingItems > 0) {
                    bulkOrderBonus = plugin.getBulkOrderManager().checkOrderFulfillment(playerId, activeOrder.strainId, matchingItems);
                    total *= bulkOrderBonus;
                }
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
        
        // NEW: Individual Buyer Integration with Intelligent Matching
        if (plugin.getBuyerRegistry() != null && plugin.getBuyerMatcher() != null) {
            // Use intelligent matching to find best buyer for this transaction
            List<ItemStack> itemsList = new ArrayList<>(Arrays.asList(session.itemsToSell));
            com.budlords.npc.IndividualBuyer buyer = plugin.getBuyerMatcher().findBestMatch(itemsList, playerId);
            
            if (buyer != null) {
                // Record all purchases with this buyer (including joints!)
                for (ItemStack item : session.itemsToSell) {
                    if (item != null && !item.getType().equals(Material.AIR)) {
                        String strainId = null;
                        int amount = 0;
                        
                        // Get strain ID from packaged products
                        if (packagingManager.isPackagedProduct(item)) {
                            strainId = packagingManager.getStrainIdFromPackage(item);
                            amount = packagingManager.getWeightFromPackage(item) * item.getAmount();
                        }
                        // Get strain ID from joints
                        else if (JointItems.isJoint(item)) {
                            strainId = JointItems.getJointStrainId(item);
                            amount = item.getAmount(); // Each joint = 1g
                        }
                        
                        if (strainId != null) {
                            double itemPrice = calculateItemPrice(item, session.buyerType) * item.getAmount();
                            plugin.getBuyerRegistry().recordPurchase(buyer.getId(), strainId, amount, itemPrice);
                        }
                    }
                }
                
                // Check if this sale fulfills any requests
                if (plugin.getBuyerRequestManager() != null) {
                    for (ItemStack item : session.itemsToSell) {
                        if (item != null && !item.getType().equals(Material.AIR)) {
                            String strainId = null;
                            com.budlords.strain.Strain strain = null;
                            com.budlords.quality.StarRating rating = null;
                            int quantity = 0;
                            
                            // Check packaged products
                            if (packagingManager.isPackagedProduct(item)) {
                                strainId = packagingManager.getStrainIdFromPackage(item);
                                strain = strainManager.getStrain(strainId);
                                rating = packagingManager.getStarRatingFromPackage(item);
                                quantity = packagingManager.getWeightFromPackage(item) * item.getAmount();
                            }
                            // Check joints
                            else if (JointItems.isJoint(item)) {
                                strainId = JointItems.getJointStrainId(item);
                                strain = strainManager.getStrain(strainId);
                                rating = JointItems.getJointRating(item);
                                quantity = item.getAmount();
                            }
                            
                            if (strainId != null) {
                                com.budlords.npc.BuyerRequest fulfilledRequest = 
                                    plugin.getBuyerRequestManager().checkAndFulfillRequest(
                                        buyer.getId(), strainId, 
                                        strain != null ? strain.getRarity() : com.budlords.strain.Strain.Rarity.COMMON,
                                        rating, quantity
                                    );
                                
                                if (fulfilledRequest != null) {
                                    // Request fulfilled! Give bonus
                                    economyManager.addBalance(player, fulfilledRequest.getBonusPayment());
                                    player.sendMessage("");
                                    player.sendMessage("§6§l✦ REQUEST FULFILLED! ✦");
                                    player.sendMessage("§eBonus Payment: §a+$" + String.format("%.2f", fulfilledRequest.getBonusPayment()));
                                    player.sendMessage("§7" + buyer.getName() + " §7is very pleased!");
                                    player.sendMessage("");
                                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                                }
                            }
                        }
                    }
                }
                
                // Show buyer's comment
                String buyerComment = buyer.getPurchaseComment(
                    Arrays.stream(session.itemsToSell)
                        .filter(i -> i != null && !i.getType().equals(Material.AIR))
                        .findFirst()
                        .map(i -> {
                            if (packagingManager.isPackagedProduct(i)) {
                                return packagingManager.getStrainIdFromPackage(i);
                            } else if (JointItems.isJoint(i)) {
                                return JointItems.getJointStrainId(i);
                            }
                            return null;
                        })
                        .orElse(null)
                );
                
                // Check for network effect (referral opportunity)
                if (plugin.getBuyerNetworkEffect() != null) {
                    plugin.getBuyerNetworkEffect().checkForReferral(buyer, playerId);
                }
                
                // Check for special event bonus
                double eventBonus = 0;
                if (plugin.getSpecialBuyerEvent() != null && plugin.getSpecialBuyerEvent().isEventActive()) {
                    com.budlords.npc.SpecialBuyerEvent.SpecialBuyer specialBuyer = 
                        plugin.getSpecialBuyerEvent().getCurrentEvent();
                    if (specialBuyer != null) {
                        double baseTotal = total / reputationMultiplier; // Remove reputation to get base
                        eventBonus = baseTotal * (specialBuyer.getPriceMultiplier() - 1.0);
                        economyManager.addBalance(player, eventBonus);
                        
                        player.sendMessage("");
                        player.sendMessage("§6§l✦ SPECIAL EVENT BONUS! ✦");
                        player.sendMessage("§e" + specialBuyer.getName());
                        player.sendMessage("§aEvent Bonus: +$" + String.format("%.2f", eventBonus));
                        player.sendMessage("§7Time Remaining: §e" + plugin.getSpecialBuyerEvent().getTimeRemainingFormatted());
                        player.sendMessage("");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    }
                }
                
                player.sendMessage("");
                player.sendMessage("§6§l" + buyer.getName() + ":");
                player.sendMessage(buyerComment);
                
                // Show network tier progress
                if (plugin.getBuyerLeaderboard() != null) {
                    com.budlords.npc.BuyerLeaderboard.NetworkTier tier = 
                        plugin.getBuyerLeaderboard().getNetworkTier(playerId);
                    player.sendMessage("§7Your Network Tier: " + tier.getDisplay());
                }
            }
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
        
        // Update dynamic buyer demand status if applicable
        if (plugin.getDynamicBuyerManager() != null) {
            org.bukkit.entity.Entity buyerEntity = plugin.getServer().getEntity(session.buyerId);
            if (buyerEntity != null) {
                plugin.getDynamicBuyerManager().onSuccessfulSale(buyerEntity);
            }
        }
        
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
     * Counts items of a specific strain in the session.
     * This ensures bulk order fulfillment only counts matching strains.
     */
    private int countItemsOfStrain(SaleSession session, String targetStrainId) {
        if (targetStrainId == null) return 0;
        
        int totalGrams = 0;
        for (ItemStack item : session.itemsToSell) {
            if (item == null) continue;
            
            String strainId = null;
            int weightPerItem = 1; // Default to 1 gram per item
            
            if (packagingManager.isPackagedProduct(item)) {
                strainId = packagingManager.getStrainIdFromPackage(item);
                // Get the weight of each package (1g, 3g, 5g, or 10g)
                weightPerItem = packagingManager.getWeightFromPackage(item);
                if (weightPerItem <= 0) weightPerItem = 1; // Safety default
            } else if (JointItems.isJoint(item)) {
                strainId = JointItems.getJointStrainId(item);
                // Joints count as 1 gram each
                weightPerItem = 1;
            }
            
            if (targetStrainId.equals(strainId)) {
                // Total grams = weight per item * number of items in stack
                totalGrams += weightPerItem * item.getAmount();
            }
        }
        return totalGrams;
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
    
    // Reputation penalty for failed sales
    private static final int FAILED_SALE_REPUTATION_PENALTY = -5;
    
    /**
     * Handles a failed sale attempt.
     * Returns items to player and applies reputation penalty.
     */
    private void handleFailedSale(Player player, SaleSession session, double successChance, int reputation) {
        String buyerName = getBuyerName(session.buyerType);
        
        // Return all items
        returnItems(player, session);
        
        // Close GUI
        activeSessions.remove(player.getUniqueId());
        player.closeInventory();
        
        // Apply reputation penalty for failed sale
        if (plugin.getReputationManager() != null) {
            plugin.getReputationManager().addReputation(player.getUniqueId(), session.buyerType.name(), 
                FAILED_SALE_REPUTATION_PENALTY);
        }
        
        // Update stats - count as failed sale
        if (plugin.getStatsManager() != null) {
            PlayerStats stats = plugin.getStatsManager().getStats(player);
            if (stats != null) {
                stats.incrementFailedSales();
            }
        }
        
        // Apply entity cooldown (can't sell to this buyer for a while)
        applyEntityCooldown(session.buyerId);
        
        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
        player.spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0, 1.5, 0), 10, 0.5, 0.3, 0.5, 0);
        
        // Get failure message based on reputation
        String failureReason;
        if (reputation <= com.budlords.economy.ReputationManager.REPUTATION_SUSPICIOUS) {
            failureReason = "doesn't trust you";
        } else if (reputation < com.budlords.economy.ReputationManager.REPUTATION_FRIENDLY) {
            failureReason = "isn't interested right now";
        } else {
            failureReason = "changed their mind";
        }
        
        // Get buyer comment
        String buyerComment = getBuyerRejectComment(reputation);
        
        player.sendMessage("");
        player.sendMessage("§c§l✗ SALE FAILED!");
        player.sendMessage("§7" + buyerName + " §c" + failureReason + "!");
        player.sendMessage("§7Your items have been returned.");
        player.sendMessage("");
        player.sendMessage("§7Success chance was: §e" + String.format("%.0f%%", successChance * 100));
        player.sendMessage("");
        player.sendMessage("§7" + buyerName + " says: " + buyerComment);
        player.sendMessage("");
        player.sendMessage("§7§oTip: Improve your reputation by making successful smaller sales first!");
        player.sendMessage("");
    }
    
    /**
     * Gets a rejection comment from the buyer based on reputation.
     */
    private String getBuyerRejectComment(int reputation) {
        String[] comments;
        
        if (reputation <= com.budlords.economy.ReputationManager.REPUTATION_SUSPICIOUS) {
            comments = new String[] {
                "§c\"Get out of my face!\"",
                "§c\"I don't deal with strangers.\"",
                "§c\"Come back when you're more... established.\"",
                "§c\"Nice try, cop.\"",
                "§c\"I don't know you. Beat it.\""
            };
        } else if (reputation < com.budlords.economy.ReputationManager.REPUTATION_FRIENDLY) {
            comments = new String[] {
                "§e\"Not today.\"",
                "§e\"Maybe next time.\"",
                "§e\"I've got enough for now.\"",
                "§e\"Price doesn't seem right...\"",
                "§e\"Let me think about it.\""
            };
        } else {
            comments = new String[] {
                "§7\"Sorry, something came up.\"",
                "§7\"Bad timing, friend.\"",
                "§7\"I need to lay low for a bit.\"",
                "§7\"Heat is on, can't risk it.\"",
                "§7\"Come back later, okay?\""
            };
        }
        
        return comments[ThreadLocalRandom.current().nextInt(comments.length)];
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
     * NEW: Counts sellable items in player's inventory
     */
    private int countSellableItemsInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSellableItem(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * NEW: Quick fill - automatically add sellable items to sale slots
     */
    private void quickFillSaleSlots(SaleSession session, Player player) {
        for (int i = 0; i < session.itemsToSell.length; i++) {
            if (session.itemsToSell[i] != null) continue; // Skip filled slots
            
            // Find sellable item in inventory
            for (ItemStack item : player.getInventory().getContents()) {
                if (isSellableItem(item)) {
                    session.itemsToSell[i] = item.clone();
                    player.getInventory().remove(item);
                    break;
                }
            }
        }
    }
    
    /**
     * NEW: Sort items by value (highest to lowest)
     */
    private void sortItemsByValue(SaleSession session) {
        // Create list of non-null items with their indices
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : session.itemsToSell) {
            if (item != null) {
                items.add(item);
            }
        }
        
        // Sort by value (descending)
        items.sort((a, b) -> Double.compare(
            calculateItemPrice(b, session.buyerType),
            calculateItemPrice(a, session.buyerType)
        ));
        
        // Clear and refill array
        Arrays.fill(session.itemsToSell, null);
        for (int i = 0; i < Math.min(items.size(), session.itemsToSell.length); i++) {
            session.itemsToSell[i] = items.get(i);
        }
    }

    /**
     * Session data for a sale - ENHANCED with 6 slots
     */
    private static class SaleSession {
        final UUID playerId;
        final UUID buyerId;
        final NPCManager.NPCType buyerType;
        final ItemStack[] itemsToSell;
        final Map<Integer, ItemStack> items;

        SaleSession(UUID playerId, UUID buyerId, NPCManager.NPCType buyerType) {
            this.playerId = playerId;
            this.buyerId = buyerId;
            this.buyerType = buyerType;
            this.itemsToSell = new ItemStack[6]; // ENHANCED: 6 slots instead of 4
            this.items = new HashMap<>();
        }
    }
}
