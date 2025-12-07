package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.npc.BuyerRegistry;
import com.budlords.npc.BuyerRequestManager;
import com.budlords.npc.IndividualBuyer;
import com.budlords.npc.BuyerRequest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced analytics dashboard for the buyer system.
 * Provides insights, trends, and recommendations for optimal trading.
 * 
 * Features:
 * - Active buyer requests with urgency indicators
 * - Top buyers by value and purchases
 * - Recommended buyers for next sale
 * - Market trends and insights
 * - Achievement progress
 */
public class BuyerAnalyticsGUI implements InventoryHolder, Listener {
    
    private final BudLords plugin;
    private final BuyerRegistry buyerRegistry;
    private final BuyerRequestManager requestManager;
    
    public BuyerAnalyticsGUI(BudLords plugin, BuyerRegistry buyerRegistry, BuyerRequestManager requestManager) {
        this.plugin = plugin;
        this.buyerRegistry = buyerRegistry;
        this.requestManager = requestManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the analytics dashboard.
     */
    @SuppressWarnings("deprecation")
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, 54, "§6§l📊 Buyer Analytics");
        updateInventory(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.3f);
    }
    
    private void updateInventory(Inventory inv, Player player) {
        inv.clear();
        
        // Border
        ItemStack border = createItem(Material.YELLOW_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        
        // Header
        ItemStack header = createItem(Material.DIAMOND,
            "§6§l📊 Advanced Analytics",
            Arrays.asList(
                "",
                "§7Your comprehensive trading dashboard",
                "§7Make data-driven decisions!",
                ""
            ));
        inv.setItem(4, header);
        
        // Active Requests Section
        List<BuyerRequest> urgentRequests = requestManager.getUrgentRequests();
        List<BuyerRequest> highValueRequests = requestManager.getHighValueRequests();
        
        ItemStack requestsItem = createItem(Material.PAPER,
            "§e§l📋 Active Requests (" + requestManager.getActiveRequests().size() + ")",
            Arrays.asList(
                "",
                "§7Urgent: §c" + urgentRequests.size(),
                "§7High Value: §6" + highValueRequests.size(),
                "",
                "§aClick to view all requests",
                ""
            ));
        inv.setItem(10, requestsItem);
        
        // Show top 3 urgent requests
        int slot = 19;
        for (int i = 0; i < Math.min(3, urgentRequests.size()); i++) {
            BuyerRequest request = urgentRequests.get(i);
            inv.setItem(slot++, createRequestCard(request));
        }
        
        // Top Buyers by Value
        List<IndividualBuyer> topByValue = buyerRegistry.getBuyersSortedByValue().stream()
            .limit(3)
            .collect(Collectors.toList());
        
        ItemStack topBuyersItem = createItem(Material.GOLD_INGOT,
            "§6§l💰 Top Buyers by Value",
            Arrays.asList(
                "",
                "§7Your most valuable customers",
                "",
                "§aClick to view details",
                ""
            ));
        inv.setItem(12, topBuyersItem);
        
        // Show top 3 buyers
        slot = 21;
        for (int i = 0; i < topByValue.size(); i++) {
            IndividualBuyer buyer = topByValue.get(i);
            inv.setItem(slot++, createBuyerCard(buyer, i + 1));
        }
        
        // Recommendations
        List<IndividualBuyer> recommended = plugin.getBuyerMatcher().getRecommendedBuyers(player.getUniqueId(), 3);
        
        ItemStack recsItem = createItem(Material.COMPASS,
            "§b§l⭐ Recommended Buyers",
            Arrays.asList(
                "",
                "§7Based on your inventory",
                "§7and buyer preferences",
                "",
                "§aSell to these for best results!",
                ""
            ));
        inv.setItem(14, recsItem);
        
        // Show recommendations
        slot = 23;
        for (IndividualBuyer buyer : recommended) {
            inv.setItem(slot++, createRecommendationCard(buyer));
        }
        
        // Market Insights
        Map<String, Object> stats = buyerRegistry.getStatistics();
        Map<String, Object> requestStats = requestManager.getStatistics();
        
        ItemStack insightsItem = createItem(Material.SPYGLASS,
            "§d§l🔍 Market Insights",
            Arrays.asList(
                "",
                "§7Total Buyers: §e" + stats.get("total_buyers"),
                "§7Total Transactions: §e" + stats.get("total_purchases"),
                "§7Total Revenue: §a$" + String.format("%.2f", (double) stats.get("total_money")),
                "",
                "§7Active Requests: §e" + requestStats.get("total_active"),
                "§7Potential Bonuses: §a$" + String.format("%.2f", (double) requestStats.get("total_potential_bonuses")),
                ""
            ));
        inv.setItem(31, insightsItem);
        
        // Achievements Progress
        ItemStack achievementsItem = createItem(Material.NETHER_STAR,
            "§5§l🏆 Achievements",
            Arrays.asList(
                "",
                "§7Track your milestones:",
                "",
                getMilestoneProgress(buyerRegistry),
                "",
                "§aClick to view all achievements",
                ""
            ));
        inv.setItem(40, achievementsItem);
        
        // Back/Close buttons
        ItemStack back = createItem(Material.ARROW,
            "§e◄ Back to Registry",
            Arrays.asList("§7Return to buyer list"));
        inv.setItem(45, back);
        
        ItemStack close = createItem(Material.BARRIER,
            "§c✕ Close",
            Arrays.asList("§7Exit analytics"));
        inv.setItem(49, close);
    }
    
    private ItemStack createRequestCard(BuyerRequest request) {
        return createItem(Material.WRITABLE_BOOK,
            request.getUrgencyDisplay() + " " + request.getDisplayName(),
            Arrays.asList(
                "",
                request.getRequestMessage(),
                "",
                "§7Bonus: §a+$" + String.format("%.2f", request.getBonusPayment()),
                "§7Time Left: §e" + request.getHoursRemaining() + "h",
                "",
                "§8ID: " + request.getRequestId().toString().substring(0, 8)
            ));
    }
    
    private ItemStack createBuyerCard(IndividualBuyer buyer, int rank) {
        String rankDisplay = switch (rank) {
            case 1 -> "§6§l🥇 #1";
            case 2 -> "§7§l🥈 #2";
            case 3 -> "§c§l🥉 #3";
            default -> "§e#" + rank;
        };
        
        return createItem(buyer.getHeadMaterial(),
            rankDisplay + " " + buyer.getName(),
            Arrays.asList(
                "",
                "§7Total Spent: §a$" + String.format("%.2f", buyer.getTotalMoneySpent()),
                "§7Purchases: §e" + buyer.getTotalPurchases(),
                "§7Loyalty: §6+" + String.format("%.0f%%", (buyer.getLoyaltyBonus() - 1.0) * 100),
                "",
                "§8Buyer ID: " + buyer.getId().toString().substring(0, 8)
            ));
    }
    
    private ItemStack createRecommendationCard(IndividualBuyer buyer) {
        return createItem(Material.LIME_DYE,
            "§a⭐ " + buyer.getName(),
            Arrays.asList(
                "",
                "§7Personality: " + buyer.getPersonality().getDisplayName(),
                "§7Mood: " + buyer.getCurrentMood(),
                "§7Status: " + buyer.getRelationshipSummary(),
                "",
                "§aRecommended for your next sale!",
                "",
                "§8Buyer ID: " + buyer.getId().toString().substring(0, 8)
            ));
    }
    
    private String getMilestoneProgress(BuyerRegistry registry) {
        int totalBuyers = registry.getAllBuyers().size();
        long totalPurchases = registry.getAllBuyers().stream()
            .mapToInt(IndividualBuyer::getTotalPurchases)
            .sum();
        
        StringBuilder progress = new StringBuilder();
        
        if (totalPurchases >= 100) {
            progress.append("§a✓ §7100+ Total Transactions");
        } else {
            progress.append("§7⏳ ").append(totalPurchases).append("/100 Transactions");
        }
        
        return progress.toString();
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
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BuyerAnalyticsGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        
        // Back button
        if (clicked.getType() == Material.ARROW) {
            player.closeInventory();
            plugin.getBuyerListGUI().open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        // Close button
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            return;
        }
        
        // Handle clicks on requests, buyers, etc.
        ItemMeta meta = clicked.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                if (line.startsWith("§8Buyer ID: ") || line.startsWith("§8ID: ")) {
                    // Could open detailed view
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    break;
                }
            }
        }
    }
    
    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, 54, "§6§l📊 Buyer Analytics");
    }
}
