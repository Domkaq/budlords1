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
    
    // Players who have purchased the plant monitoring feature
    private final Set<UUID> plantMonitoringUnlocked;
    
    // Cost for plant monitoring feature
    private static final double PLANT_MONITORING_COST = 20000.0;
    
    // Current page tracking for each player
    private final Map<UUID, String> currentPage;

    public BuyerProfileGUI(BudLords plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.viewingSessions = new ConcurrentHashMap<>();
        this.plantMonitoringUnlocked = ConcurrentHashMap.newKeySet();
        this.currentPage = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Checks if a player has unlocked the plant monitoring feature.
     */
    public boolean hasPlantMonitoring(UUID playerId) {
        return plantMonitoringUnlocked.contains(playerId);
    }
    
    /**
     * Unlocks plant monitoring for a player.
     */
    public void unlockPlantMonitoring(UUID playerId) {
        plantMonitoringUnlocked.add(playerId);
    }

    /**
     * Opens the main phone apps page (home screen).
     */
    @SuppressWarnings("deprecation")
    public void openContactsList(Player player) {
        currentPage.put(player.getUniqueId(), "apps");
        Inventory inv = Bukkit.createInventory(this, 54, "§b§l📱 Dealer Phone");
        updateAppsPage(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.5f);
    }
    
    /**
     * Opens the contacts page.
     */
    @SuppressWarnings("deprecation")
    public void openContactsPage(Player player) {
        currentPage.put(player.getUniqueId(), "contacts");
        Inventory inv = Bukkit.createInventory(this, 45, "§b§l📱 Phone - Contacts");
        updateContactsPage(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }
    
    /**
     * Opens the orders page.
     */
    @SuppressWarnings("deprecation")
    public void openOrdersPage(Player player) {
        currentPage.put(player.getUniqueId(), "orders");
        Inventory inv = Bukkit.createInventory(this, 45, "§b§l📱 Phone - Orders");
        updateOrdersPage(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }
    
    /**
     * Opens the stats page.
     */
    @SuppressWarnings("deprecation")
    public void openStatsPage(Player player) {
        currentPage.put(player.getUniqueId(), "stats");
        Inventory inv = Bukkit.createInventory(this, 45, "§b§l📱 Phone - Stats");
        updateStatsPage(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
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

    /**
     * Updates the main apps page (home screen with app icons).
     */
    private void updateAppsPage(Inventory inv, Player player) {
        inv.clear();

        // Professional phone-style border
        ItemStack borderDark = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        ItemStack borderAccent = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", null);

        // Top border with phone notch style
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, (i == 3 || i == 4 || i == 5) ? borderAccent : borderDark);
        }
        // Bottom border
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, borderDark);
        }
        // Side borders
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, borderDark);
            inv.setItem(i + 8, borderDark);
        }

        // Phone header with status bar style
        ItemStack header = createItem(Material.ECHO_SHARD,
            "§b§l📱 Dealer Phone",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━━",
                "",
                "§7Welcome to your Dealer Phone!",
                "§7Tap an app to get started.",
                "",
                "§8━━━━━━━━━━━━━━━━━━━━━━"
            ));
        inv.setItem(4, header);

        // ===== APPS GRID - Row 1: Contacts, Orders, Stats =====
        inv.setItem(11, createItem(Material.PLAYER_HEAD, 
            "§a§l📞 Contacts",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7View your buyer network",
                "§7and check reputation.",
                "",
                "§e▶ Tap to open",
                "§8ID: app_contacts"
            )));
        
        // NEW: Buyer Registry app
        inv.setItem(12, createItem(Material.WRITABLE_BOOK, 
            "§6§l📋 Buyer Registry",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7View ALL buyers with",
                "§7profiles, stats, favorites!",
                "",
                "§7Total: §e" + plugin.getBuyerRegistry().getAllBuyers().size() + " §7buyers",
                "",
                "§e▶ Tap to open",
                "§8ID: app_buyer_registry"
            )));

        inv.setItem(13, createItem(Material.PAPER, 
            "§6§l📋 Orders",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7View and manage",
                "§7bulk orders.",
                "",
                "§e▶ Tap to open",
                "§8ID: app_orders"
            )));

        inv.setItem(15, createItem(Material.DIAMOND, 
            "§b§l📊 Stats",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7View your dealing",
                "§7statistics and earnings.",
                "",
                "§e▶ Tap to open",
                "§8ID: app_stats"
            )));

        // ===== Row 2: Weather, Plants, Market =====
        com.budlords.weather.WeatherManager weatherManager = plugin.getWeatherManager();
        String weatherDisplay = weatherManager != null ? 
            weatherManager.getCurrentWeather().getColoredDisplay() : "§7N/A";
        inv.setItem(20, createItem(Material.SUNFLOWER, 
            "§e§l☀ Weather",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7Current: " + weatherDisplay,
                "",
                "§7View weather info",
                "§7in Stats page.",
                "",
                "§e▶ Tap to view stats",
                "§8ID: app_weather"
            )));

        boolean hasMonitoring = hasPlantMonitoring(player.getUniqueId());
        int plantCount = 0;
        if (hasMonitoring) {
            for (com.budlords.farming.Plant plant : plugin.getFarmingManager().getAllPlants()) {
                if (plant.getOwnerUuid().equals(player.getUniqueId())) {
                    plantCount++;
                }
            }
        }
        inv.setItem(22, createItem(hasMonitoring ? Material.LIME_DYE : Material.RED_DYE, 
            hasMonitoring ? "§a§l🌿 Plants §7(" + plantCount + ")" : "§c§l🌿 Plants",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                hasMonitoring ? "§7Monitor your plants" : "§cFeature locked!",
                hasMonitoring ? "§7remotely." : "§7Unlock for §e$20,000",
                "",
                hasMonitoring ? "§e▶ Tap to view plants" : "§e▶ Tap to unlock",
                "§8ID: app_plants"
            )));

        inv.setItem(24, createItem(Material.GOLD_INGOT, 
            "§6§l💰 Wallet",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7Balance: §a" + economyManager.formatMoney(economyManager.getBalance(player)),
                "",
                "§7Check balance and",
                "§7send money to players.",
                "",
                "§e▶ Tap to open",
                "§8ID: app_wallet"
            )));

        // ===== Row 3: Skills, Daily, Challenges =====
        inv.setItem(29, createItem(Material.EXPERIENCE_BOTTLE, 
            "§d§l⚡ Skills",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7View and upgrade",
                "§7your dealer skills.",
                "",
                "§e▶ Tap to open",
                "§8ID: app_skills"
            )));

        inv.setItem(31, createItem(Material.CHEST, 
            "§e§l🎁 Daily",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7Claim your daily",
                "§7rewards here!",
                "",
                "§e▶ Tap to claim",
                "§8ID: app_daily"
            )));

        inv.setItem(33, createItem(Material.BOOK, 
            "§c§l🏆 Challenges",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7View active challenges",
                "§7and earn rewards!",
                "",
                "§e▶ Tap to open",
                "§8ID: app_challenges"
            )));

        // ===== Row 4: Collection, Rep Guide =====
        inv.setItem(38, createItem(Material.FILLED_MAP, 
            "§9§l📚 Collection",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7View your strain",
                "§7collection progress.",
                "",
                "§e▶ Tap to open",
                "§8ID: app_collection"
            )));

        inv.setItem(40, createItem(Material.NETHER_STAR, 
            "§e§l★ Rep Guide",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§c Suspicious §8(-50)",
                "§7 Neutral §8(0)",
                "§e Friendly §8(50) +5%",
                "§a Trusted §8(150) +10%",
                "§d VIP §8(300) +15%",
                "§6 ★LEGEND★ §8(500) +25%"
            )));

        // Close button - phone home button style
        inv.setItem(49, createItem(Material.BARRIER, "§c§l✗ Close",
            Arrays.asList("", "§8Tap to close phone")));
    }

    /**
     * Updates the contacts page.
     */
    private void updateContactsPage(Inventory inv, Player player) {
        inv.clear();
        ReputationManager repManager = plugin.getReputationManager();

        // Professional phone-style border
        ItemStack borderDark = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        ItemStack borderAccent = createItem(Material.GREEN_STAINED_GLASS_PANE, " ", null);

        // Top border
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, (i == 3 || i == 4 || i == 5) ? borderAccent : borderDark);
        }
        // Bottom border
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, borderDark);
        }
        // Side borders
        for (int i = 9; i < 36; i += 9) {
            inv.setItem(i, borderDark);
            inv.setItem(i + 8, borderDark);
        }

        // Header
        inv.setItem(4, createItem(Material.PLAYER_HEAD,
            "§a§l📞 Contacts",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━━",
                "",
                "§7Your buyer network.",
                "§7Tap a contact to view profile.",
                "",
                "§8━━━━━━━━━━━━━━━━━━━━━━"
            )));

        // Contact cards - Show fixed NPCs from BuyerRegistry
        int[] contactSlots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
        int slotIdx = 0;
        
        // Get Market Joe and BlackMarket Joe from unified BuyerRegistry
        com.budlords.npc.IndividualBuyer marketJoe = plugin.getBuyerRegistry().getMarketJoe();
        com.budlords.npc.IndividualBuyer blackMarketJoe = plugin.getBuyerRegistry().getBlackMarketJoe();
        
        // Show Market Joe
        if (marketJoe != null && slotIdx < contactSlots.length) {
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7Status: " + marketJoe.getRelationshipSummary());
            lore.add("§7Purchases: §f" + marketJoe.getTotalPurchases());
            lore.add("§7Total Spent: §a$" + String.format("%.2f", marketJoe.getTotalMoneySpent()));
            lore.add("");
            lore.add("§8Your friendly neighborhood dealer");
            lore.add("§8Buys at standard market prices");
            lore.add("");
            lore.add("§e▶ Tap to view full profile");
            lore.add("§8ID: contact_market_joe");

            inv.setItem(contactSlots[slotIdx], createItem(Material.EMERALD, "§a§lMarket Joe", lore));
            slotIdx++;
        }
        
        // Show BlackMarket Joe
        if (blackMarketJoe != null && slotIdx < contactSlots.length) {
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7Status: " + blackMarketJoe.getRelationshipSummary());
            lore.add("§7Purchases: §f" + blackMarketJoe.getTotalPurchases());
            lore.add("§7Total Spent: §a$" + String.format("%.2f", blackMarketJoe.getTotalMoneySpent()));
            lore.add("");
            lore.add("§8Deals in premium product only");
            lore.add("§8Pays +50% for rare goods");
            lore.add("");
            lore.add("§e▶ Tap to view full profile");
            lore.add("§8ID: contact_blackmarket_joe");

            inv.setItem(contactSlots[slotIdx], createItem(Material.NETHER_STAR, "§5§lBlackMarket Joe", lore));
            slotIdx++;
        }
        
        // Show dynamic buyers (up to remaining slots)
        java.util.List<com.budlords.npc.IndividualBuyer> allBuyers = 
            plugin.getBuyerRegistry().getBuyersSortedByRecency();
        for (com.budlords.npc.IndividualBuyer buyer : allBuyers) {
            // Skip fixed NPCs (already shown above) - use Objects.equals for null safety
            if (java.util.Objects.equals(buyer, marketJoe) || java.util.Objects.equals(buyer, blackMarketJoe)) continue;
            if (slotIdx >= contactSlots.length) break;
            
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7Status: " + buyer.getRelationshipSummary());
            lore.add("§7Personality: " + buyer.getPersonality().getDisplayName());
            lore.add("§7Purchases: §f" + buyer.getTotalPurchases());
            lore.add("");
            lore.add("§e▶ Tap to view profile");
            lore.add("§8ID: contact_buyer_" + buyer.getId());
            
            inv.setItem(contactSlots[slotIdx], createItem(buyer.getHeadMaterial(), 
                buyer.getPersonality().getColorCode() + buyer.getName(), lore));
            slotIdx++;
        }

        // Back button
        inv.setItem(38, createItem(Material.ARROW, "§7§l← Back",
            Arrays.asList("", "§8Return to apps", "§8ID: back_to_apps")));

        // Close button
        inv.setItem(40, createItem(Material.BARRIER, "§c§l✗ Close",
            Arrays.asList("", "§8Tap to close phone")));
    }

    /**
     * Updates the orders page.
     */
    private void updateOrdersPage(Inventory inv, Player player) {
        inv.clear();

        // Professional phone-style border
        ItemStack borderDark = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        ItemStack borderAccent = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ", null);

        // Top border
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, (i == 3 || i == 4 || i == 5) ? borderAccent : borderDark);
        }
        // Bottom border
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, borderDark);
        }
        // Side borders
        for (int i = 9; i < 36; i += 9) {
            inv.setItem(i, borderDark);
            inv.setItem(i + 8, borderDark);
        }

        // Header
        inv.setItem(4, createItem(Material.PAPER,
            "§6§l📋 Orders",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━━",
                "",
                "§7Your bulk orders.",
                "§7Complete orders for bonus!",
                "",
                "§8━━━━━━━━━━━━━━━━━━━━━━"
            )));

        // Current order display
        com.budlords.economy.BulkOrderManager orderManager = plugin.getBulkOrderManager();
        com.budlords.economy.BulkOrderManager.BulkOrder activeOrder = 
            orderManager != null ? orderManager.getActiveOrder(player.getUniqueId()) : null;
        
        if (activeOrder != null) {
            inv.setItem(13, createItem(Material.FILLED_MAP,
                "§e§l⚡ Active Order",
                Arrays.asList(
                    "§8━━━━━━━━━━━━━━━━",
                    "",
                    "§7Buyer: §f" + activeOrder.buyerName,
                    "§7Wants: §e" + activeOrder.quantity + "g §f" + activeOrder.strainName,
                    "",
                    "§7Bonus: §a+" + String.format("%.0f%%", (activeOrder.priceMultiplier - 1) * 100),
                    "§7Time: §e" + activeOrder.getTimeRemainingText(),
                    "",
                    "§7§oPackage and sell to complete!",
                    "§7§o(e.g. 4x10g + 1g = 41g)"
                )));
        } else {
            long cooldown = orderManager != null ? orderManager.getTimeUntilRefresh(player.getUniqueId()) : 0;
            
            List<String> orderLore = new ArrayList<>();
            orderLore.add("§8━━━━━━━━━━━━━━━━");
            orderLore.add("");
            orderLore.add("§7No active order.");
            orderLore.add("");
            if (cooldown > 0) {
                orderLore.add("§cNext order in: §e" + (cooldown / 60000) + "m");
            } else {
                orderLore.add("§a▶ Tap to get new order!");
                orderLore.add("");
                orderLore.add("§8ID: new_order");
            }
            
            inv.setItem(13, createItem(
                cooldown > 0 ? Material.CLOCK : Material.LIME_DYE,
                cooldown > 0 ? "§7§lNo Active Order" : "§a§l+ Get New Order",
                orderLore));
        }

        // Order info
        inv.setItem(22, createItem(Material.BOOK, 
            "§e§l? How Orders Work",
            Arrays.asList(
                "",
                "§7Complete buyer orders for",
                "§7bonus prices on your sales!",
                "",
                "§7• Get orders from this menu",
                "§7• Sell the requested strain",
                "§7• Earn bonus multipliers",
                "",
                "§6Higher rep = better orders!"
            )));

        // Back button
        inv.setItem(38, createItem(Material.ARROW, "§7§l← Back",
            Arrays.asList("", "§8Return to apps", "§8ID: back_to_apps")));

        // Close button
        inv.setItem(40, createItem(Material.BARRIER, "§c§l✗ Close",
            Arrays.asList("", "§8Tap to close phone")));
    }

    /**
     * Updates the stats page.
     */
    private void updateStatsPage(Inventory inv, Player player) {
        inv.clear();
        ReputationManager repManager = plugin.getReputationManager();

        // Professional phone-style border
        ItemStack borderDark = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        ItemStack borderAccent = createItem(Material.CYAN_STAINED_GLASS_PANE, " ", null);

        // Top border
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, (i == 3 || i == 4 || i == 5) ? borderAccent : borderDark);
        }
        // Bottom border
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, borderDark);
        }
        // Side borders
        for (int i = 9; i < 36; i += 9) {
            inv.setItem(i, borderDark);
            inv.setItem(i + 8, borderDark);
        }

        // Header
        inv.setItem(4, createItem(Material.DIAMOND,
            "§b§l📊 Statistics",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━━",
                "",
                "§7Your dealing statistics",
                "§7and earnings.",
                "",
                "§8━━━━━━━━━━━━━━━━━━━━━━"
            )));

        // Quick stats display
        int totalRep = 0;
        if (repManager != null) {
            for (NPCManager.NPCType type : NPCManager.NPCType.values()) {
                if (type != NPCManager.NPCType.NONE) {
                    totalRep += repManager.getReputation(player.getUniqueId(), type.name());
                }
            }
        }
        
        com.budlords.stats.PlayerStats stats = plugin.getStatsManager() != null ? 
            plugin.getStatsManager().getStats(player) : null;
        
        inv.setItem(11, createItem(Material.EMERALD,
            "§a§lSales Stats",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7Successful: §a" + (stats != null ? stats.getTotalSalesSuccess() : 0),
                "§7Failed: §c" + (stats != null ? stats.getTotalSalesFailed() : 0),
                "§7Success Rate: §e" + (stats != null ? String.format("%.1f%%", stats.getSuccessRate()) : "0%")
            )));

        inv.setItem(13, createItem(Material.GOLD_INGOT,
            "§6§lEarnings",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7Total: §a" + plugin.getEconomyManager().formatMoney(
                    stats != null ? stats.getTotalMoneyEarned() : 0),
                "§7Best Sale: §e" + plugin.getEconomyManager().formatMoney(
                    stats != null ? stats.getHighestSingleSale() : 0)
            )));

        inv.setItem(15, createItem(Material.NETHER_STAR,
            "§e§lReputation",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━",
                "",
                "§7Total Rep: §f" + totalRep,
                "",
                "§7Build rep by selling",
                "§7to buyers consistently!"
            )));

        // Weather info
        com.budlords.weather.WeatherManager weatherManager = plugin.getWeatherManager();
        if (weatherManager != null) {
            com.budlords.weather.WeatherManager.WeatherType currentWeather = weatherManager.getCurrentWeather();
            double growthMult = weatherManager.getGrowthMultiplier();
            double qualityMult = weatherManager.getQualityMultiplier();
            
            String growthDisplay = growthMult >= 1.0 ? 
                "§a+" + String.format("%.0f%%", (growthMult - 1.0) * 100) :
                "§c" + String.format("%.0f%%", (growthMult - 1.0) * 100);
            String qualityDisplay = qualityMult >= 1.0 ?
                "§a+" + String.format("%.0f%%", (qualityMult - 1.0) * 100) :
                "§c" + String.format("%.0f%%", (qualityMult - 1.0) * 100);
            
            inv.setItem(22, createItem(Material.SUNFLOWER,
                "§e§l☀ Current Weather",
                Arrays.asList(
                    "§8━━━━━━━━━━━━━━━━",
                    "",
                    "§7Current: " + currentWeather.getColoredDisplay(),
                    "",
                    "§7Growth: " + growthDisplay,
                    "§7Quality: " + qualityDisplay
                )));
        }

        // Back button
        inv.setItem(38, createItem(Material.ARROW, "§7§l← Back",
            Arrays.asList("", "§8Return to apps", "§8ID: back_to_apps")));

        // Close button
        inv.setItem(40, createItem(Material.BARRIER, "§c§l✗ Close",
            Arrays.asList("", "§8Tap to close phone")));
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

        // Check for common ID-based actions first
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    // App navigation
                    if (line.equals("§8ID: app_contacts")) {
                        openContactsPage(player);
                        return;
                    }
                    if (line.equals("§8ID: app_buyer_registry")) {
                        player.closeInventory();
                        plugin.getBuyerListGUI().open(player);
                        return;
                    }
                    if (line.equals("§8ID: app_orders")) {
                        openOrdersPage(player);
                        return;
                    }
                    if (line.equals("§8ID: app_stats")) {
                        openStatsPage(player);
                        return;
                    }
                    if (line.equals("§8ID: app_weather")) {
                        // Weather info is shown in stats page
                        openStatsPage(player);
                        return;
                    }
                    if (line.equals("§8ID: app_plants")) {
                        handlePlantMonitoringClick(player, event.getInventory());
                        return;
                    }
                    if (line.equals("§8ID: app_wallet")) {
                        // Show wallet info with balance
                        player.closeInventory();
                        player.sendMessage("");
                        player.sendMessage("§6§l💰 WALLET");
                        player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage("§7Balance: §a" + economyManager.formatMoney(economyManager.getBalance(player)));
                        player.sendMessage("");
                        player.sendMessage("§7Commands:");
                        player.sendMessage("§e/bal §7- Check your balance");
                        player.sendMessage("§e/pay <player> <amount> §7- Send money");
                        player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━");
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                        return;
                    }
                    if (line.equals("§8ID: app_skills")) {
                        // Open skills GUI
                        player.closeInventory();
                        player.performCommand("skills");
                        return;
                    }
                    if (line.equals("§8ID: app_daily")) {
                        // Claim daily rewards
                        player.closeInventory();
                        player.performCommand("daily");
                        return;
                    }
                    if (line.equals("§8ID: app_challenges")) {
                        // Open challenges GUI
                        player.closeInventory();
                        player.performCommand("challenges");
                        return;
                    }
                    if (line.equals("§8ID: app_collection")) {
                        // Open collection GUI
                        player.closeInventory();
                        player.performCommand("collection");
                        return;
                    }
                    
                    // Back button
                    if (line.equals("§8ID: back_to_apps")) {
                        openContactsList(player);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                        return;
                    }
                    
                    // New order request
                    if (line.equals("§8ID: new_order")) {
                        handleNewOrderRequest(player, event.getInventory());
                        return;
                    }
                    
                    // Contact card clicks
                    if (line.startsWith("§8ID: contact_")) {
                        String typeName = line.substring(14);
                        try {
                            NPCManager.NPCType type = NPCManager.NPCType.valueOf(typeName);
                            openBuyerProfile(player, type, null);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                            return;
                        } catch (IllegalArgumentException e) {
                            // Invalid type, ignore
                        }
                    }
                }
            }
        }

        // Handle close button (slot 40 or 49 depending on page)
        if ((slot == 40 || slot == 49) && clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            currentPage.remove(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }

        // Handle plant details view
        if (title.contains("Plant Details")) {
            // Back button
            if (slot == 49) {
                openContactsList(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                return;
            }
        }
        // Handle profile view
        else if (title.contains("Profile")) {
            // Back to contacts
            if (slot == 47) {
                openContactsPage(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                return;
            }

            // Close button
            if (slot == 51) {
                player.closeInventory();
                viewingSessions.remove(player.getUniqueId());
                currentPage.remove(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            }
        }
    }
    
    /**
     * Handles a request for a new bulk order from the phone.
     */
    private void handleNewOrderRequest(Player player, Inventory inv) {
        com.budlords.economy.BulkOrderManager orderManager = plugin.getBulkOrderManager();
        if (orderManager == null) {
            player.sendMessage("§cOrders are not available!");
            return;
        }
        
        long cooldown = orderManager.getTimeUntilRefresh(player.getUniqueId());
        if (cooldown > 0) {
            player.sendMessage("§cYou must wait " + (cooldown / 60000) + " minutes before getting a new order!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        
        // Generate new order
        orderManager.generateOrder(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        
        // Refresh the GUI
        updateOrdersPage(inv, player);
    }
    
    /**
     * Handles clicking on the plant monitoring button.
     */
    private void handlePlantMonitoringClick(Player player, Inventory inv) {
        if (hasPlantMonitoring(player.getUniqueId())) {
            // Open plant details GUI
            openPlantDetailsGUI(player);
        } else {
            // Try to purchase
            double balance = economyManager.getBalance(player);
            if (balance >= PLANT_MONITORING_COST) {
                economyManager.removeBalance(player, PLANT_MONITORING_COST);
                unlockPlantMonitoring(player.getUniqueId());
                
                player.sendMessage("");
                player.sendMessage("§a§l✓ Plant Monitoring Unlocked!");
                player.sendMessage("§7You can now view your plants remotely from the Dealer Phone.");
                player.sendMessage("§7$20,000 has been deducted from your balance.");
                player.sendMessage("");
                
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                
                // Refresh the GUI
                updateAppsPage(inv, player);
            } else {
                player.sendMessage("§cYou need $20,000 to unlock Plant Monitoring!");
                player.sendMessage("§7Current balance: " + economyManager.formatMoney(balance));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            }
        }
    }
    
    /**
     * Opens the plant details GUI showing all the player's plants.
     * 
     * @SuppressWarnings("deprecation") is used because Bukkit.createInventory with string title
     * is deprecated in favor of Adventure API's Component, but we maintain compatibility
     * with Spigot servers that don't support Adventure API.
     */
    @SuppressWarnings("deprecation")
    private void openPlantDetailsGUI(Player player) {
        // Using deprecated string title for Spigot compatibility
        Inventory inv = Bukkit.createInventory(this, 54, "§b§l📱 Plant Details");
        
        // Border
        ItemStack border = createItem(Material.GREEN_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
        
        // Header
        Collection<com.budlords.farming.Plant> allPlants = plugin.getFarmingManager().getAllPlants();
        List<com.budlords.farming.Plant> playerPlants = new ArrayList<>();
        for (com.budlords.farming.Plant plant : allPlants) {
            if (plant.getOwnerUuid().equals(player.getUniqueId())) {
                playerPlants.add(plant);
            }
        }
        
        inv.setItem(4, createItem(Material.OAK_SAPLING,
            "§a§l🌿 Your Plants §7(" + playerPlants.size() + ")",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━━",
                "",
                "§7View detailed information",
                "§7about your growing plants.",
                ""
            )));
        
        // Display plants (max 28 slots: 10-16, 19-25, 28-34, 37-43)
        int[] plantSlots = {10, 11, 12, 13, 14, 15, 16, 
                           19, 20, 21, 22, 23, 24, 25,
                           28, 29, 30, 31, 32, 33, 34,
                           37, 38, 39, 40, 41, 42, 43};
        
        int slotIdx = 0;
        for (com.budlords.farming.Plant plant : playerPlants) {
            if (slotIdx >= plantSlots.length) break;
            
            com.budlords.strain.Strain strain = plugin.getStrainManager().getStrain(plant.getStrainId());
            String strainName = strain != null ? strain.getName() : "Unknown";
            
            // Check for infection
            boolean isInfected = plugin.getDiseaseManager() != null && 
                plugin.getDiseaseManager().isInfected(plant);
            
            List<String> plantLore = new ArrayList<>();
            plantLore.add("§8━━━━━━━━━━━━━━━━");
            plantLore.add("");
            plantLore.add("§7Stage: §e" + plant.getGrowthStageName() + " §7(" + (plant.getGrowthStage() + 1) + "/4)");
            plantLore.add("§7Quality: §e" + plant.getQuality() + "%");
            
            if (plant.hasPot()) {
                plantLore.add("");
                if (plant.getPotRating() != null) {
                    plantLore.add("§7Pot: " + plant.getPotRating().getDisplay());
                }
                if (plant.getSeedRating() != null) {
                    plantLore.add("§7Seed: " + plant.getSeedRating().getDisplay());
                }
                plantLore.add("§7Water: §b" + String.format("%.0f%%", plant.getWaterLevel() * 100));
                plantLore.add("§7Nutrients: §e" + String.format("%.0f%%", plant.getNutrientLevel() * 100));
                if (plant.getLampRating() != null) {
                    plantLore.add("§7Lamp: " + plant.getLampRating().getDisplay());
                }
            }
            
            plantLore.add("");
            plantLore.add("§7Location: §f" + plant.getLocation().getBlockX() + 
                ", " + plant.getLocation().getBlockY() + 
                ", " + plant.getLocation().getBlockZ());
            
            if (isInfected) {
                plantLore.add("");
                plantLore.add("§c⚠ INFECTED! §7Use a cure item");
            }
            
            if (plant.isFullyGrown()) {
                plantLore.add("");
                plantLore.add("§a✓ Ready to harvest!");
            }
            
            Material icon = isInfected ? Material.DEAD_BUSH : 
                (plant.isFullyGrown() ? Material.LIME_DYE : Material.GREEN_DYE);
            String statusPrefix = isInfected ? "§c⚠ " : 
                (plant.isFullyGrown() ? "§a✓ " : "§e");
            
            inv.setItem(plantSlots[slotIdx], createItem(icon,
                statusPrefix + strainName,
                plantLore));
            
            slotIdx++;
        }
        
        // Back button
        inv.setItem(49, createItem(Material.ARROW, "§e§l← Back",
            Arrays.asList("", "§7Return to phone")));
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
