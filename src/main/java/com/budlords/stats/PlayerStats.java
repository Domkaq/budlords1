package com.budlords.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks detailed statistics for a player.
 * Provides insights into their BudLords journey.
 */
public class PlayerStats {

    private final UUID playerId;
    
    // Growing stats
    private int totalPlantsGrown;
    private int totalPlantsHarvested;
    private int legendaryBudsHarvested;
    private int fiveStarBudsHarvested;
    private int perfectHarvests;
    
    // Trading stats
    private int totalSalesSuccess;
    private int totalSalesFailed;
    private double highestSingleSale;
    private double totalMoneyEarned;
    
    // Joint rolling stats
    private int jointsRolled;
    private int perfectRolls;
    private int legendaryJointsRolled;
    
    // Time stats
    private long totalPlaytimeMinutes;
    private long longestPlaySession;
    private long lastLoginTime;
    
    // Achievement tracking
    private final Map<String, Long> achievementUnlocks; // achievementId -> unlock time
    
    // Prestige stats
    private int prestigeLevel;
    private int totalPrestiges;
    
    // Streak tracking
    private int currentDailyStreak;
    private int longestDailyStreak;
    private long lastDailyClaimTime;
    
    // Challenge stats
    private int challengesCompleted;
    private int dailyChallengesCompleted;
    private int weeklyChallengesCompleted;
    
    // Crossbreeding stats
    private int strainsCreated;
    private int crossbreedsAttempted;
    private int legendaryStrainsDiscovered;

    public PlayerStats(UUID playerId) {
        this.playerId = playerId;
        this.achievementUnlocks = new HashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    // ===== GROWING STATS =====
    
    public int getTotalPlantsGrown() {
        return totalPlantsGrown;
    }

    public void incrementPlantsGrown() {
        this.totalPlantsGrown++;
    }

    public int getTotalPlantsHarvested() {
        return totalPlantsHarvested;
    }

    public void incrementPlantsHarvested() {
        this.totalPlantsHarvested++;
    }

    public int getLegendaryBudsHarvested() {
        return legendaryBudsHarvested;
    }

    public void incrementLegendaryBuds() {
        this.legendaryBudsHarvested++;
    }

    public int getFiveStarBudsHarvested() {
        return fiveStarBudsHarvested;
    }

    public void incrementFiveStarBuds() {
        this.fiveStarBudsHarvested++;
    }

    public int getPerfectHarvests() {
        return perfectHarvests;
    }

    public void incrementPerfectHarvests() {
        this.perfectHarvests++;
    }

    // ===== TRADING STATS =====
    
    public int getTotalSalesSuccess() {
        return totalSalesSuccess;
    }

    public void incrementSuccessfulSales() {
        this.totalSalesSuccess++;
    }

    public int getTotalSalesFailed() {
        return totalSalesFailed;
    }

    public void incrementFailedSales() {
        this.totalSalesFailed++;
    }

    public double getSuccessRate() {
        int total = totalSalesSuccess + totalSalesFailed;
        if (total == 0) return 0;
        return (double) totalSalesSuccess / total * 100;
    }

    public double getHighestSingleSale() {
        return highestSingleSale;
    }

    public void recordSale(double amount) {
        if (amount > highestSingleSale) {
            highestSingleSale = amount;
        }
        totalMoneyEarned += amount;
    }

    public double getTotalMoneyEarned() {
        return totalMoneyEarned;
    }

    // ===== JOINT STATS =====
    
    public int getJointsRolled() {
        return jointsRolled;
    }

    public void incrementJointsRolled() {
        this.jointsRolled++;
    }

    public int getPerfectRolls() {
        return perfectRolls;
    }

    public void incrementPerfectRolls() {
        this.perfectRolls++;
    }

    public int getLegendaryJointsRolled() {
        return legendaryJointsRolled;
    }

    public void incrementLegendaryJoints() {
        this.legendaryJointsRolled++;
    }

    // ===== TIME STATS =====
    
    public long getTotalPlaytimeMinutes() {
        return totalPlaytimeMinutes;
    }

    public void addPlaytime(long minutes) {
        this.totalPlaytimeMinutes += minutes;
    }

    public long getLongestPlaySession() {
        return longestPlaySession;
    }

    public void updatePlaySession(long minutes) {
        if (minutes > longestPlaySession) {
            longestPlaySession = minutes;
        }
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long time) {
        this.lastLoginTime = time;
    }

    // ===== ACHIEVEMENT STATS =====
    
    public boolean hasAchievement(String achievementId) {
        return achievementUnlocks.containsKey(achievementId);
    }

    public void unlockAchievement(String achievementId) {
        if (!hasAchievement(achievementId)) {
            achievementUnlocks.put(achievementId, System.currentTimeMillis());
        }
    }

    public Map<String, Long> getAchievementUnlocks() {
        return new HashMap<>(achievementUnlocks);
    }

    public int getAchievementCount() {
        return achievementUnlocks.size();
    }

    // ===== PRESTIGE STATS =====
    
    public int getPrestigeLevel() {
        return prestigeLevel;
    }

    public void setPrestigeLevel(int level) {
        this.prestigeLevel = level;
    }

    public void incrementPrestige() {
        this.prestigeLevel++;
        this.totalPrestiges++;
    }

    public int getTotalPrestiges() {
        return totalPrestiges;
    }

    // ===== STREAK STATS =====
    
    public int getCurrentDailyStreak() {
        return currentDailyStreak;
    }

    public void incrementDailyStreak() {
        this.currentDailyStreak++;
        if (currentDailyStreak > longestDailyStreak) {
            longestDailyStreak = currentDailyStreak;
        }
    }

    public void resetDailyStreak() {
        this.currentDailyStreak = 0;
    }

    public int getLongestDailyStreak() {
        return longestDailyStreak;
    }

    public long getLastDailyClaimTime() {
        return lastDailyClaimTime;
    }

    public void setLastDailyClaimTime(long time) {
        this.lastDailyClaimTime = time;
    }

    // ===== CHALLENGE STATS =====
    
    public int getChallengesCompleted() {
        return challengesCompleted;
    }

    public void incrementChallengesCompleted() {
        this.challengesCompleted++;
    }

    public int getDailyChallengesCompleted() {
        return dailyChallengesCompleted;
    }

    public void incrementDailyChallenges() {
        this.dailyChallengesCompleted++;
        this.challengesCompleted++;
    }

    public int getWeeklyChallengesCompleted() {
        return weeklyChallengesCompleted;
    }

    public void incrementWeeklyChallenges() {
        this.weeklyChallengesCompleted++;
        this.challengesCompleted++;
    }

    // ===== CROSSBREED STATS =====
    
    public int getStrainsCreated() {
        return strainsCreated;
    }

    public void incrementStrainsCreated() {
        this.strainsCreated++;
    }

    public int getCrossbreedsAttempted() {
        return crossbreedsAttempted;
    }

    public void incrementCrossbreeds() {
        this.crossbreedsAttempted++;
    }

    public int getLegendaryStrainsDiscovered() {
        return legendaryStrainsDiscovered;
    }

    public void incrementLegendaryStrains() {
        this.legendaryStrainsDiscovered++;
    }

    // ===== OVERALL SCORE =====
    
    /**
     * Calculates an overall "BudLord Score" based on all activities.
     */
    public int calculateBudLordScore() {
        int score = 0;
        
        // Growing contribution (max ~2000)
        score += totalPlantsHarvested * 5;
        score += legendaryBudsHarvested * 50;
        score += fiveStarBudsHarvested * 25;
        score += perfectHarvests * 100;
        
        // Trading contribution (max ~2000)
        score += totalSalesSuccess * 10;
        score += (int) (highestSingleSale / 100);
        
        // Crafting contribution (max ~1500)
        score += jointsRolled * 15;
        score += perfectRolls * 75;
        score += legendaryJointsRolled * 100;
        
        // Achievement contribution (max ~2500)
        score += achievementUnlocks.size() * 100;
        
        // Prestige contribution (exponential bonus)
        score += prestigeLevel * 500;
        
        // Streak contribution
        score += currentDailyStreak * 25;
        score += longestDailyStreak * 10;
        
        // Challenge contribution
        score += challengesCompleted * 50;
        
        // Crossbreed contribution
        score += strainsCreated * 75;
        score += legendaryStrainsDiscovered * 200;
        
        return score;
    }

    /**
     * Gets a title based on the player's BudLord score.
     */
    public String getScoreTitle() {
        int score = calculateBudLordScore();
        if (score >= 50000) return "§6§l✦ Legendary BudLord ✦";
        if (score >= 25000) return "§5§l✦ Master Cultivator ✦";
        if (score >= 10000) return "§9§l✦ Expert Grower ✦";
        if (score >= 5000) return "§a§l✦ Skilled Farmer ✦";
        if (score >= 2000) return "§e§l✦ Apprentice ✦";
        if (score >= 500) return "§7§l✦ Beginner ✦";
        return "§8§l✦ Newcomer ✦";
    }
}
