package com.budlords.economy;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player sale history for analytics and features like quick-repeat,
 * favorite buyers, streaks, and recommendations.
 */
public class SaleHistory {
    
    private final Map<UUID, PlayerSaleData> playerData;
    
    public SaleHistory() {
        this.playerData = new ConcurrentHashMap<>();
    }
    
    /**
     * Records a successful sale.
     */
    public void recordSale(UUID playerId, String buyerName, double amount, int itemCount, String strainId) {
        PlayerSaleData data = playerData.computeIfAbsent(playerId, k -> new PlayerSaleData());
        data.recordSale(buyerName, amount, itemCount, strainId);
    }
    
    /**
     * Gets player sale data.
     */
    public PlayerSaleData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, k -> new PlayerSaleData());
    }
    
    /**
     * Player-specific sale data.
     */
    public static class PlayerSaleData {
        private final List<SaleRecord> recentSales;
        private final Map<String, Integer> buyerSaleCount;
        private final Map<String, Double> buyerTotalRevenue;
        private final Set<String> favoriteBuyers;
        private final Map<String, SalePreset> savedPresets;
        private int currentStreak;
        private long lastSaleTime;
        private SaleRecord lastSale;
        
        private static final int MAX_RECENT_SALES = 50;
        private static final long STREAK_TIMEOUT_MS = 600000; // 10 minutes
        
        public PlayerSaleData() {
            this.recentSales = new ArrayList<>();
            this.buyerSaleCount = new HashMap<>();
            this.buyerTotalRevenue = new HashMap<>();
            this.favoriteBuyers = new HashSet<>();
            this.savedPresets = new HashMap<>();
            this.currentStreak = 0;
            this.lastSaleTime = 0;
        }
        
        public void recordSale(String buyerName, double amount, int itemCount, String strainId) {
            SaleRecord record = new SaleRecord(buyerName, amount, itemCount, strainId, System.currentTimeMillis());
            
            // Add to recent sales (keep only last 50)
            recentSales.add(0, record);
            if (recentSales.size() > MAX_RECENT_SALES) {
                recentSales.remove(recentSales.size() - 1);
            }
            
            // Update buyer stats
            buyerSaleCount.merge(buyerName, 1, Integer::sum);
            buyerTotalRevenue.merge(buyerName, amount, Double::sum);
            
            // Update streak
            long timeSinceLastSale = System.currentTimeMillis() - lastSaleTime;
            if (timeSinceLastSale <= STREAK_TIMEOUT_MS) {
                currentStreak++;
            } else {
                currentStreak = 1;
            }
            
            lastSaleTime = System.currentTimeMillis();
            lastSale = record;
        }
        
        public List<SaleRecord> getRecentSales(int count) {
            return recentSales.subList(0, Math.min(count, recentSales.size()));
        }
        
        public String getMostFrequentBuyer() {
            return buyerSaleCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        public String getHighestRevenueBuyer() {
            return buyerTotalRevenue.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        public double getTotalRevenue() {
            return buyerTotalRevenue.values().stream().mapToDouble(Double::doubleValue).sum();
        }
        
        public int getTotalSales() {
            return recentSales.size();
        }
        
        public int getCurrentStreak() {
            // Check if streak expired
            if (System.currentTimeMillis() - lastSaleTime > STREAK_TIMEOUT_MS) {
                currentStreak = 0;
            }
            return currentStreak;
        }
        
        public SaleRecord getLastSale() {
            return lastSale;
        }
        
        public void addFavoriteBuyer(String buyerName) {
            favoriteBuyers.add(buyerName);
        }
        
        public void removeFavoriteBuyer(String buyerName) {
            favoriteBuyers.remove(buyerName);
        }
        
        public boolean isFavoriteBuyer(String buyerName) {
            return favoriteBuyers.contains(buyerName);
        }
        
        public Set<String> getFavoriteBuyers() {
            return new HashSet<>(favoriteBuyers);
        }
        
        public void savePreset(String name, SalePreset preset) {
            savedPresets.put(name, preset);
        }
        
        public SalePreset getPreset(String name) {
            return savedPresets.get(name);
        }
        
        public Map<String, SalePreset> getAllPresets() {
            return new HashMap<>(savedPresets);
        }
        
        public void deletePreset(String name) {
            savedPresets.remove(name);
        }
    }
    
    /**
     * Record of a single sale.
     */
    public static class SaleRecord {
        private final String buyerName;
        private final double amount;
        private final int itemCount;
        private final String strainId;
        private final long timestamp;
        
        public SaleRecord(String buyerName, double amount, int itemCount, String strainId, long timestamp) {
            this.buyerName = buyerName;
            this.amount = amount;
            this.itemCount = itemCount;
            this.strainId = strainId;
            this.timestamp = timestamp;
        }
        
        public String getBuyerName() { return buyerName; }
        public double getAmount() { return amount; }
        public int getItemCount() { return itemCount; }
        public String getStrainId() { return strainId; }
        public long getTimestamp() { return timestamp; }
        
        public String getTimeAgo() {
            long diff = System.currentTimeMillis() - timestamp;
            long minutes = diff / 60000;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (days > 0) return days + "d ago";
            if (hours > 0) return hours + "h ago";
            if (minutes > 0) return minutes + "m ago";
            return "Just now";
        }
    }
    
    /**
     * Saved pricing preset.
     */
    public static class SalePreset {
        private final String name;
        private final double priceMultiplier;
        private final String description;
        
        public SalePreset(String name, double priceMultiplier, String description) {
            this.name = name;
            this.priceMultiplier = priceMultiplier;
            this.description = description;
        }
        
        public String getName() { return name; }
        public double getPriceMultiplier() { return priceMultiplier; }
        public String getDescription() { return description; }
    }
}
