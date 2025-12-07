package com.budlords.npc;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Competitive leaderboard system for buyer relationships.
 * Tracks and ranks players based on their buyer network success.
 * 
 * Features:
 * - Multiple leaderboard categories
 * - Weekly and all-time rankings
 * - Achievement milestones
 * - Competition rewards
 */
public class BuyerLeaderboard {
    
    private final BuyerRegistry registry;
    
    public enum LeaderboardType {
        TOTAL_REVENUE("Total Revenue", "Most money earned from buyers"),
        TOTAL_SALES("Total Sales", "Most transactions completed"),
        UNIQUE_BUYERS("Buyer Network", "Most unique buyers served"),
        LEGENDARY_BUYERS("VIP Network", "Most VIP/Legendary relationships"),
        REQUEST_COMPLETIONS("Quest Master", "Most requests fulfilled"),
        AVERAGE_LOYALTY("Loyalty King", "Highest average buyer loyalty");
        
        private final String displayName;
        private final String description;
        
        LeaderboardType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public BuyerLeaderboard(BuyerRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * Gets top players for a specific leaderboard category.
     */
    public List<LeaderboardEntry> getTopPlayers(LeaderboardType type, int limit) {
        // For now, return mock data
        // In a real implementation, this would query player statistics
        return new ArrayList<>();
    }
    
    /**
     * Gets a player's rank in a specific category.
     */
    public int getPlayerRank(UUID playerId, LeaderboardType type) {
        List<LeaderboardEntry> entries = getTopPlayers(type, 100);
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).playerId.equals(playerId)) {
                return i + 1;
            }
        }
        return -1; // Not ranked
    }
    
    /**
     * Gets all leaderboard positions for a player.
     */
    public Map<LeaderboardType, Integer> getPlayerRankings(UUID playerId) {
        Map<LeaderboardType, Integer> rankings = new HashMap<>();
        for (LeaderboardType type : LeaderboardType.values()) {
            int rank = getPlayerRank(playerId, type);
            if (rank > 0) {
                rankings.put(type, rank);
            }
        }
        return rankings;
    }
    
    /**
     * Calculates player's buyer network score.
     */
    public double calculateNetworkScore(UUID playerId) {
        double score = 0;
        
        // Points for unique buyers
        score += registry.getAllBuyers().size() * 10;
        
        // Points for VIP relationships
        long vipCount = registry.getAllBuyers().stream()
            .filter(b -> b.getTotalPurchases() >= 50)
            .count();
        score += vipCount * 50;
        
        // Points for total transactions
        int totalPurchases = registry.getAllBuyers().stream()
            .mapToInt(IndividualBuyer::getTotalPurchases)
            .sum();
        score += totalPurchases * 5;
        
        // Points for total revenue
        double totalRevenue = registry.getAllBuyers().stream()
            .mapToDouble(IndividualBuyer::getTotalMoneySpent)
            .sum();
        score += totalRevenue / 100; // 1 point per $100
        
        return score;
    }
    
    /**
     * Gets player's tier based on network score.
     */
    public NetworkTier getNetworkTier(UUID playerId) {
        double score = calculateNetworkScore(playerId);
        
        if (score >= 10000) return NetworkTier.KINGPIN;
        if (score >= 5000) return NetworkTier.MOGUL;
        if (score >= 2500) return NetworkTier.TYCOON;
        if (score >= 1000) return NetworkTier.ENTREPRENEUR;
        if (score >= 500) return NetworkTier.DEALER;
        if (score >= 100) return NetworkTier.HUSTLER;
        return NetworkTier.ROOKIE;
    }
    
    public enum NetworkTier {
        ROOKIE("§7Rookie", "Just getting started"),
        HUSTLER("§aHustler", "Building connections"),
        DEALER("§2Dealer", "Established network"),
        ENTREPRENEUR("§eEntrepreneur", "Growing empire"),
        TYCOON("§6Tycoon", "Major player"),
        MOGUL("§5Mogul", "Industry leader"),
        KINGPIN("§4§lKingpin", "Legendary status");
        
        private final String display;
        private final String description;
        
        NetworkTier(String display, String description) {
            this.display = display;
            this.description = description;
        }
        
        public String getDisplay() { return display; }
        public String getDescription() { return description; }
        
        public NetworkTier getNext() {
            int nextOrdinal = this.ordinal() + 1;
            NetworkTier[] values = NetworkTier.values();
            return nextOrdinal < values.length ? values[nextOrdinal] : this;
        }
    }
    
    /**
     * Represents a leaderboard entry.
     */
    public static class LeaderboardEntry {
        public final UUID playerId;
        public final String playerName;
        public final double score;
        public final int rank;
        
        public LeaderboardEntry(UUID playerId, String playerName, double score, int rank) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.score = score;
            this.rank = rank;
        }
    }
}
