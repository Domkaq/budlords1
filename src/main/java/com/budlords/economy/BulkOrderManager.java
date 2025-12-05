package com.budlords.economy;

import com.budlords.BudLords;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bulk Order system for BudLords v3.0.0.
 * Buyers can place special bulk orders for specific strains with bonus payouts!
 */
public class BulkOrderManager {

    private final BudLords plugin;
    
    // Active bulk orders: playerUUID -> BulkOrder
    private final Map<UUID, BulkOrder> activeOrders;
    
    // Order refresh cooldown (30 minutes)
    private static final long ORDER_REFRESH_MS = 1800000L;
    
    // Last order generation time per player
    private final Map<UUID, Long> lastOrderTime;

    public BulkOrderManager(BudLords plugin) {
        this.plugin = plugin;
        this.activeOrders = new ConcurrentHashMap<>();
        this.lastOrderTime = new ConcurrentHashMap<>();
    }
    
    /**
     * Represents a bulk order request.
     */
    public static class BulkOrder {
        public final String strainId;
        public final String strainName;
        public final int quantity;
        public final double priceMultiplier;  // Bonus multiplier for completing
        public final long expiryTime;
        public final String buyerName;
        public final OrderTier tier;
        
        public BulkOrder(String strainId, String strainName, int quantity, 
                        double priceMultiplier, long expiryTime, String buyerName, OrderTier tier) {
            this.strainId = strainId;
            this.strainName = strainName;
            this.quantity = quantity;
            this.priceMultiplier = priceMultiplier;
            this.expiryTime = expiryTime;
            this.buyerName = buyerName;
            this.tier = tier;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        
        public long getTimeRemaining() {
            return Math.max(0, expiryTime - System.currentTimeMillis());
        }
        
        public String getTimeRemainingText() {
            long remaining = getTimeRemaining();
            long minutes = remaining / 60000;
            long seconds = (remaining % 60000) / 1000;
            return minutes + "m " + seconds + "s";
        }
    }
    
    /**
     * Order tiers with different bonuses and requirements.
     */
    public enum OrderTier {
        SMALL("§7Small Order", 5, 10, 1.15, 1.25),      // 5-10 items, 15-25% bonus
        MEDIUM("§aMedium Order", 15, 25, 1.25, 1.40),    // 15-25 items, 25-40% bonus
        LARGE("§eLarge Order", 30, 50, 1.40, 1.60),      // 30-50 items, 40-60% bonus
        MASSIVE("§6§lMassive Order", 75, 100, 1.60, 2.00), // 75-100 items, 60-100% bonus
        LEGENDARY("§d§lLegendary Order", 150, 200, 2.00, 3.00); // 150-200 items, 100-200% bonus
        
        public final String displayName;
        public final int minQuantity;
        public final int maxQuantity;
        public final double minBonus;
        public final double maxBonus;
        
        OrderTier(String displayName, int minQuantity, int maxQuantity, 
                 double minBonus, double maxBonus) {
            this.displayName = displayName;
            this.minQuantity = minQuantity;
            this.maxQuantity = maxQuantity;
            this.minBonus = minBonus;
            this.maxBonus = maxBonus;
        }
        
        public int getRandomQuantity() {
            return ThreadLocalRandom.current().nextInt(minQuantity, maxQuantity + 1);
        }
        
        public double getRandomBonus() {
            return minBonus + ThreadLocalRandom.current().nextDouble() * (maxBonus - minBonus);
        }
    }
    
    /**
     * Generates a new bulk order for a player.
     */
    public BulkOrder generateOrder(UUID playerId) {
        // Check cooldown
        Long lastTime = lastOrderTime.get(playerId);
        if (lastTime != null && System.currentTimeMillis() - lastTime < ORDER_REFRESH_MS) {
            return activeOrders.get(playerId);
        }
        
        // Get available strains
        var strains = plugin.getStrainManager().getAllStrains();
        if (strains.isEmpty()) return null;
        
        // Pick a random strain
        var strainList = new java.util.ArrayList<>(strains);
        var strain = strainList.get(ThreadLocalRandom.current().nextInt(strainList.size()));
        
        // Determine tier based on player stats
        OrderTier tier = determineOrderTier(playerId);
        
        // Generate order details
        int quantity = tier.getRandomQuantity();
        double bonus = tier.getRandomBonus();
        
        // Order expires in 1-2 hours
        long expiryTime = System.currentTimeMillis() + 
            (3600000L + ThreadLocalRandom.current().nextLong(3600000L));
        
        // Random buyer name
        String buyerName = getRandomBuyerName();
        
        BulkOrder order = new BulkOrder(
            strain.getId(), 
            strain.getName(), 
            quantity, 
            bonus, 
            expiryTime, 
            buyerName,
            tier
        );
        
        activeOrders.put(playerId, order);
        lastOrderTime.put(playerId, System.currentTimeMillis());
        
        // Notify player
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage("");
            player.sendMessage("§6§l📋 NEW BULK ORDER AVAILABLE!");
            player.sendMessage("§7Buyer: §f" + buyerName);
            player.sendMessage("§7Wants: §e" + quantity + "x §f" + strain.getName());
            player.sendMessage("§7Bonus: §a+" + String.format("%.0f%%", (bonus - 1) * 100) + " §7price!");
            player.sendMessage("§7Tier: " + tier.displayName);
            player.sendMessage("§7Expires in: §e" + order.getTimeRemainingText());
            player.sendMessage("§7Use §e/orders §7to view details!");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
        }
        
        return order;
    }
    
    /**
     * Gets the active order for a player.
     */
    public BulkOrder getActiveOrder(UUID playerId) {
        BulkOrder order = activeOrders.get(playerId);
        if (order != null && order.isExpired()) {
            activeOrders.remove(playerId);
            return null;
        }
        return order;
    }
    
    /**
     * Determines the order tier based on player progression.
     */
    private OrderTier determineOrderTier(UUID playerId) {
        int prestigeLevel = 0;
        int successfulSales = 0;
        
        if (plugin.getStatsManager() != null) {
            var stats = plugin.getStatsManager().getStats(playerId);
            if (stats != null) {
                prestigeLevel = stats.getPrestigeLevel();
                successfulSales = stats.getTotalSalesSuccess();
            }
        }
        
        // Higher prestige and more sales = chance for better orders
        double roll = ThreadLocalRandom.current().nextDouble() * 100;
        roll -= prestigeLevel * 5; // Prestige helps
        roll -= successfulSales / 10.0; // Experience helps
        
        if (roll < 5) return OrderTier.LEGENDARY;
        if (roll < 15) return OrderTier.MASSIVE;
        if (roll < 35) return OrderTier.LARGE;
        if (roll < 60) return OrderTier.MEDIUM;
        return OrderTier.SMALL;
    }
    
    /**
     * Checks if a sale fulfills a bulk order.
     * @return Bonus multiplier if fulfilled, 1.0 otherwise
     */
    public double checkOrderFulfillment(UUID playerId, String strainId, int quantity) {
        BulkOrder order = getActiveOrder(playerId);
        if (order == null) return 1.0;
        
        if (order.strainId.equals(strainId) && quantity >= order.quantity) {
            // Order fulfilled!
            activeOrders.remove(playerId);
            
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("");
                player.sendMessage("§a§l✓ BULK ORDER COMPLETED!");
                player.sendMessage("§7" + order.buyerName + " is very pleased!");
                player.sendMessage("§7Bonus applied: §a+" + String.format("%.0f%%", (order.priceMultiplier - 1) * 100));
                player.sendMessage("");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
            }
            
            return order.priceMultiplier;
        }
        
        return 1.0;
    }
    
    /**
     * Gets time until next order refresh.
     */
    public long getTimeUntilRefresh(UUID playerId) {
        Long lastTime = lastOrderTime.get(playerId);
        if (lastTime == null) return 0;
        
        long elapsed = System.currentTimeMillis() - lastTime;
        return Math.max(0, ORDER_REFRESH_MS - elapsed);
    }
    
    /**
     * Gets a random buyer name for orders.
     */
    private String getRandomBuyerName() {
        String[] names = {
            "Big Tony", "Smoke Stevens", "Mary Jane", "The Professor",
            "Silent Mike", "Lucky Luke", "The Chemist", "Shady Steve",
            "Green Goddess", "Mr. Haze", "The Collector", "Dank Danny",
            "Crystal Claire", "Hash Harry", "Budmaster Supreme", "The Baron",
            "Kush King", "Vapor Vince", "The Pharmacist", "Chronic Chris"
        };
        return names[ThreadLocalRandom.current().nextInt(names.length)];
    }
    
    public void shutdown() {
        // Orders are temporary, no need to save
    }
}
