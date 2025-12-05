package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
import com.budlords.economy.ReputationManager;
import com.budlords.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Professional GUI for viewing buyer profiles and reputation.
 * Accessed by using the Dealer Phone on NPCs or in air.
 */
public class BuyerProfileGUI implements InventoryHolder, Listener {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    
    // Active sessions tracking which entity the player is viewing
    private final Map<UUID, NPCManager.NPCType> viewingSessions;

    public BuyerProfileGUI(BudLords plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.viewingSessions = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the contacts list GUI showing all buyer types.
     */
    @SuppressWarnings("deprecation")
    public void openContactsList(Player player) {
        Inventory inv = Bukkit.createInventory(this, 45, "§b§l📱 Dealer Phone - Contacts");
        updateContactsList(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.5f);
    }

    /**
     * Opens a specific buyer profile GUI.
     */
    @SuppressWarnings("deprecation")
    public void openBuyerProfile(Player player, NPCManager.NPCType buyerType, Entity entity) {
        viewingSessions.put(player.getUniqueId(), buyerType);
        
        String buyerName = getBuyerDisplayName(buyerType);
        Inventory inv = Bukkit.createInventory(this, 54, "§b§l📱 " + buyerName + " - Profile");
        updateBuyerProfile(inv, player, buyerType, entity);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
    }

    private void updateContactsList(Inventory inv, Player player) {
        inv.clear();
        ReputationManager repManager = plugin.getReputationManager();

        // Border - phone style
        ItemStack borderBlue = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", null);
        ItemStack borderCyan = createItem(Material.CYAN_STAINED_GLASS_PANE, " ", null);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderBlue : borderCyan);
            inv.setItem(36 + i, i % 2 == 0 ? borderBlue : borderCyan);
        }
        for (int i = 9; i < 36; i += 9) {
            inv.setItem(i, borderBlue);
            inv.setItem(i + 8, borderBlue);
        }

        // Header
        ItemStack header = createItem(Material.ECHO_SHARD,
            "§b§l📱 Dealer Phone",
            Arrays.asList(
                "",
                "§7Your business contacts!",
                "",
                "§7Click a contact to view",
                "§7their profile and your reputation",
                "",
                "§e★ Better reputation = better deals!"
            ));
        inv.setItem(4, header);

        // Contact cards for each buyer type
        int slot = 11;
        for (NPCManager.NPCType type : NPCManager.NPCType.values()) {
            if (type == NPCManager.NPCType.NONE) continue;

            int rep = repManager != null ? repManager.getReputation(player.getUniqueId(), type.name()) : 0;
            String repDisplay = repManager != null ? repManager.getReputationDisplay(rep) : "§7Unknown";
            String repBonus = repManager != null ? repManager.getReputationBonusText(rep) : "§7N/A";

            Material icon = getBuyerIcon(type);
            String displayName = getBuyerDisplayName(type);
            String colorCode = getBuyerColor(type);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Status: " + repDisplay);
            lore.add("§7Points: §f" + rep);
            lore.add("§7Bonus: " + repBonus);
            lore.add("");
            lore.add(getBuyerDescription(type));
            lore.add("");
            lore.add("§e▶ Click to view full profile");
            lore.add("§8ID: contact_" + type.name());

            ItemStack contact = createItem(icon, colorCode + "§l" + displayName, lore);
            inv.setItem(slot, contact);
            
            slot += 2; // Space between contacts
        }

        // Reputation legend
        inv.setItem(31, createItem(Material.BOOK, 
            "§e§lReputation Levels",
            Arrays.asList(
                "",
                "§c Suspicious §7(-50) §8- Penalties!",
                "§7 Neutral §7(0) §8- Normal prices",
                "§e Friendly §7(50) §8- +5% prices",
                "§a Trusted §7(150) §8- +10% prices",
                "§d VIP §7(300) §8- +15% prices",
                "§6 ★LEGENDARY★ §7(500) §8- +25% prices!",
                "",
                "§7Higher rep = tips & bonuses!"
            )));

        // Close button
        inv.setItem(40, createItem(Material.BARRIER, "§c§l✗ Close Phone",
            Arrays.asList("", "§7Click to close")));
    }

    private void updateBuyerProfile(Inventory inv, Player player, NPCManager.NPCType buyerType, Entity entity) {
        inv.clear();
        ReputationManager repManager = plugin.getReputationManager();
        
        int rep = repManager != null ? repManager.getReputation(player.getUniqueId(), buyerType.name()) : 0;
        String repLevel = repManager != null ? repManager.getReputationLevel(rep) : "NEUTRAL";
        String repDisplay = repManager != null ? repManager.getReputationDisplay(rep) : "§7Unknown";
        String repBonus = repManager != null ? repManager.getReputationBonusText(rep) : "§7N/A";
        double multiplier = repManager != null ? repManager.getReputationMultiplier(rep) : 1.0;

        String buyerColor = getBuyerColor(buyerType);
        String buyerName = getBuyerDisplayName(buyerType);

        // Border - styled for the buyer
        ItemStack border1 = createItem(getBuyerBorderMaterial(buyerType), " ", null);
        ItemStack border2 = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? border1 : border2);
            inv.setItem(45 + i, i % 2 == 0 ? border1 : border2);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border1);
            inv.setItem(i + 8, border1);
        }

        // Profile header with buyer info
        ItemStack profileHead = createItem(getBuyerIcon(buyerType),
            buyerColor + "§l" + buyerName,
            Arrays.asList(
                "",
                "§7" + getBuyerFullDescription(buyerType),
                "",
                getBuyerSpecialty(buyerType)
            ));
        inv.setItem(4, profileHead);

        // ═══════════════════════════════════════
        // REPUTATION CARD (Left side)
        // ═══════════════════════════════════════
        
        // Big reputation display
        ItemStack repCard = createItem(getRepIcon(repLevel),
            "§6§l★ YOUR REPUTATION",
            Arrays.asList(
                "",
                "§7Status: " + repDisplay,
                "§7Points: §f" + rep + " §8/ 500",
                "",
                "§7Price Bonus: " + repBonus,
                "§7Multiplier: §a" + String.format("%.2fx", multiplier),
                "",
                getProgressBar(rep, 500)
            ));
        inv.setItem(20, repCard);

        // Next level info
        String nextLevel = getNextReputationLevel(rep);
        int pointsToNext = getPointsToNextLevel(rep);
        ItemStack nextLevelCard = createItem(Material.EXPERIENCE_BOTTLE,
            "§e§lNext Level: " + nextLevel,
            Arrays.asList(
                "",
                "§7Points needed: §f" + pointsToNext,
                "",
                "§7Earn reputation by:",
                "§7• Successful sales",
                "§7• Higher value deals",
                "§7• Bulk orders"
            ));
        inv.setItem(29, nextLevelCard);

        // ═══════════════════════════════════════
        // BUYER INFO (Right side)
        // ═══════════════════════════════════════
        
        // What they buy
        ItemStack buyInfo = createItem(Material.CHEST,
            buyerColor + "§lWhat They Buy",
            Arrays.asList(
                "",
                "§a✓ §7Packaged Products",
                "§a✓ §7Joints",
                getBuyerPreferences(buyerType),
                "",
                "§7Base price modifier:",
                getBuyerPriceInfo(buyerType)
            ));
        inv.setItem(24, buyInfo);

        // Tips and perks
        ItemStack perksCard = createItem(Material.GOLD_NUGGET,
            "§6§lPerks & Tips",
            getPerksForLevel(repLevel));
        inv.setItem(33, perksCard);

        // ═══════════════════════════════════════
        // ACTION BUTTONS (Bottom)
        // ═══════════════════════════════════════

        // Back to contacts
        inv.setItem(47, createItem(Material.ARROW, "§e§l← Back to Contacts",
            Arrays.asList("", "§7View all your contacts")));
        
        // Quick tip about this buyer
        inv.setItem(49, createItem(Material.PAPER,
            "§e§l💡 Pro Tip",
            Arrays.asList(
                "",
                getBuyerProTip(buyerType)
            )));

        // Close
        inv.setItem(51, createItem(Material.BARRIER, "§c§l✗ Close",
            Arrays.asList("", "§7Close the phone")));
    }

    // ═══════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════

    private String getBuyerDisplayName(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "Market Joe";
            case BLACKMARKET_JOE -> "BlackMarket Joe";
            case VILLAGE_VENDOR -> "Village Vendor";
            case CONFIGURABLE_MOB -> "Custom Buyer";
            default -> "Unknown";
        };
    }

    private String getBuyerColor(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "§a";
            case BLACKMARKET_JOE -> "§5";
            case VILLAGE_VENDOR -> "§e";
            case CONFIGURABLE_MOB -> "§b";
            default -> "§7";
        };
    }

    private Material getBuyerIcon(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> Material.EMERALD;
            case BLACKMARKET_JOE -> Material.ENDER_PEARL;
            case VILLAGE_VENDOR -> Material.WHEAT;
            case CONFIGURABLE_MOB -> Material.PLAYER_HEAD;
            default -> Material.BARRIER;
        };
    }

    private Material getBuyerBorderMaterial(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> Material.LIME_STAINED_GLASS_PANE;
            case BLACKMARKET_JOE -> Material.PURPLE_STAINED_GLASS_PANE;
            case VILLAGE_VENDOR -> Material.YELLOW_STAINED_GLASS_PANE;
            case CONFIGURABLE_MOB -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            default -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }

    private String getBuyerDescription(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "§7Regular market dealer";
            case BLACKMARKET_JOE -> "§5Pays premium for rare stuff";
            case VILLAGE_VENDOR -> "§eLocal buyer, lower prices";
            case CONFIGURABLE_MOB -> "§bCustom configured buyer";
            default -> "§7Unknown buyer";
        };
    }

    private String getBuyerFullDescription(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "The friendly neighborhood dealer. Fair prices for everyone.";
            case BLACKMARKET_JOE -> "Shady but pays premium for exotic and rare products.";
            case VILLAGE_VENDOR -> "Simple folk who pay less but are always willing to buy.";
            case CONFIGURABLE_MOB -> "A mysterious buyer with unique preferences.";
            default -> "Unknown buyer type.";
        };
    }

    private String getBuyerSpecialty(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "§a✦ Specialty: §7Fair trade, all products";
            case BLACKMARKET_JOE -> "§5✦ Specialty: §7Rare strains (+50% bonus!)";
            case VILLAGE_VENDOR -> "§e✦ Specialty: §7Quick sales, no questions";
            case CONFIGURABLE_MOB -> "§b✦ Specialty: §7Varies by configuration";
            default -> "§7✦ Specialty: Unknown";
        };
    }

    private String getBuyerPriceInfo(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "§a100% §7(standard)";
            case BLACKMARKET_JOE -> "§d150% §7(premium!)";
            case VILLAGE_VENDOR -> "§e80% §7(discount)";
            case CONFIGURABLE_MOB -> "§7100% §7(standard)";
            default -> "§7100%";
        };
    }

    private String getBuyerPreferences(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "§a✓ §7Seeds (for sale)";
            case BLACKMARKET_JOE -> "§c✗ §7No seeds - buds only!";
            case VILLAGE_VENDOR -> "§a✓ §7Everything welcome";
            case CONFIGURABLE_MOB -> "§7? §7Varies by config";
            default -> "";
        };
    }

    private String getBuyerProTip(NPCManager.NPCType type) {
        return switch (type) {
            case MARKET_JOE -> "§7Great for selling common strains\n§7and building reputation safely.";
            case BLACKMARKET_JOE -> "§7Sell RARE and LEGENDARY strains\n§7here for maximum profit!";
            case VILLAGE_VENDOR -> "§7Good for quick cash when you\n§7need money fast.";
            case CONFIGURABLE_MOB -> "§7Check what this buyer prefers\n§7in the server config.";
            default -> "§7No tips available.";
        };
    }

    private Material getRepIcon(String level) {
        return switch (level) {
            case "LEGENDARY" -> Material.NETHER_STAR;
            case "VIP" -> Material.DIAMOND;
            case "TRUSTED" -> Material.EMERALD;
            case "FRIENDLY" -> Material.GOLD_INGOT;
            case "NEUTRAL" -> Material.IRON_INGOT;
            case "SUSPICIOUS" -> Material.COAL;
            default -> Material.PAPER;
        };
    }

    private String getNextReputationLevel(int rep) {
        if (rep >= ReputationManager.REPUTATION_LEGENDARY) return "§6★ MAX LEVEL!";
        if (rep >= ReputationManager.REPUTATION_VIP) return "§6LEGENDARY";
        if (rep >= ReputationManager.REPUTATION_TRUSTED) return "§dVIP";
        if (rep >= ReputationManager.REPUTATION_FRIENDLY) return "§aTrusted";
        if (rep > ReputationManager.REPUTATION_SUSPICIOUS) return "§eFriendly";
        return "§7Neutral";
    }

    private int getPointsToNextLevel(int rep) {
        if (rep >= ReputationManager.REPUTATION_LEGENDARY) return 0;
        if (rep >= ReputationManager.REPUTATION_VIP) return ReputationManager.REPUTATION_LEGENDARY - rep;
        if (rep >= ReputationManager.REPUTATION_TRUSTED) return ReputationManager.REPUTATION_VIP - rep;
        if (rep >= ReputationManager.REPUTATION_FRIENDLY) return ReputationManager.REPUTATION_TRUSTED - rep;
        if (rep > ReputationManager.REPUTATION_SUSPICIOUS) return ReputationManager.REPUTATION_FRIENDLY - rep;
        return ReputationManager.REPUTATION_NEUTRAL - rep;
    }

    private String getProgressBar(int current, int max) {
        int percent = Math.min(100, (int) ((current / (double) max) * 100));
        int filled = percent / 5; // 20 segments
        
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                if (percent >= 80) bar.append("§6");
                else if (percent >= 50) bar.append("§a");
                else if (percent >= 25) bar.append("§e");
                else bar.append("§c");
                bar.append("█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8] §f").append(percent).append("%");
        return bar.toString();
    }

    private List<String> getPerksForLevel(String level) {
        List<String> perks = new ArrayList<>();
        perks.add("");
        
        switch (level) {
            case "LEGENDARY" -> {
                perks.add("§6✓ §7+25% sale prices");
                perks.add("§6✓ §7+50% tip chance");
                perks.add("§6✓ §715-25% tip amount");
                perks.add("§6✓ §7Priority bulk orders");
                perks.add("");
                perks.add("§6§l★ MAXIMUM BENEFITS ★");
            }
            case "VIP" -> {
                perks.add("§d✓ §7+15% sale prices");
                perks.add("§d✓ §7+35% tip chance");
                perks.add("§d✓ §710-20% tip amount");
                perks.add("");
                perks.add("§7Next: §6Legendary §7(+10% prices)");
            }
            case "TRUSTED" -> {
                perks.add("§a✓ §7+10% sale prices");
                perks.add("§a✓ §7+20% tip chance");
                perks.add("§a✓ §78-15% tip amount");
                perks.add("");
                perks.add("§7Next: §dVIP §7(+5% prices)");
            }
            case "FRIENDLY" -> {
                perks.add("§e✓ §7+5% sale prices");
                perks.add("§e✓ §7+10% tip chance");
                perks.add("§e✓ §75-10% tip amount");
                perks.add("");
                perks.add("§7Next: §aTrusted §7(+5% prices)");
            }
            case "NEUTRAL" -> {
                perks.add("§7• Standard prices");
                perks.add("§7• 5% tip chance");
                perks.add("§7• 2-5% tip amount");
                perks.add("");
                perks.add("§7Next: §eFriendly §7(+5% prices)");
            }
            case "SUSPICIOUS" -> {
                perks.add("§c✗ §7-15% sale prices!");
                perks.add("§c✗ §7No tips");
                perks.add("§c✗ §7No bulk orders");
                perks.add("");
                perks.add("§cKeep dealing to improve!");
            }
        }
        return perks;
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

    // ═══════════════════════════════════════
    // EVENT HANDLERS
    // ═══════════════════════════════════════

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BuyerProfileGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String title = event.getView().getTitle();
        int slot = event.getRawSlot();

        // Handle contacts list
        if (title.contains("Contacts")) {
            // Close button
            if (slot == 40) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                return;
            }

            // Contact card clicks
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    for (String line : lore) {
                        if (line.startsWith("§8ID: contact_")) {
                            String typeName = line.substring(14);
                            try {
                                NPCManager.NPCType type = NPCManager.NPCType.valueOf(typeName);
                                openBuyerProfile(player, type, null);
                                return;
                            } catch (IllegalArgumentException e) {
                                // Invalid type, ignore
                            }
                        }
                    }
                }
            }
        }
        // Handle profile view
        else if (title.contains("Profile")) {
            // Back to contacts
            if (slot == 47) {
                openContactsList(player);
                return;
            }

            // Close button
            if (slot == 51) {
                player.closeInventory();
                viewingSessions.remove(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
