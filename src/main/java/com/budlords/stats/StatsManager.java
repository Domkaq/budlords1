package com.budlords.stats;

import com.budlords.BudLords;
import com.budlords.data.DataManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages player statistics, leaderboards, and stat persistence.
 */
public class StatsManager {

    private final BudLords plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private File statsFile;
    private FileConfiguration statsConfig;

    public StatsManager(BudLords plugin) {
        this.plugin = plugin;
        this.playerStats = new ConcurrentHashMap<>();
        loadStats();
    }

    private void loadStats() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create stats.yml");
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        
        ConfigurationSection playersSection = statsConfig.getConfigurationSection("players");
        if (playersSection == null) return;
        
        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                PlayerStats stats = loadPlayerStats(uuid, playersSection.getConfigurationSection(uuidStr));
                playerStats.put(uuid, stats);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load stats for: " + uuidStr);
            }
        }
        
        plugin.getLogger().info("Loaded stats for " + playerStats.size() + " players");
    }

    private PlayerStats loadPlayerStats(UUID uuid, ConfigurationSection section) {
        PlayerStats stats = new PlayerStats(uuid);
        
        if (section == null) return stats;
        
        // Load growing stats
        ConfigurationSection growing = section.getConfigurationSection("growing");
        if (growing != null) {
            for (int i = 0; i < growing.getInt("plants-grown", 0); i++) stats.incrementPlantsGrown();
            for (int i = 0; i < growing.getInt("plants-harvested", 0); i++) stats.incrementPlantsHarvested();
            for (int i = 0; i < growing.getInt("legendary-buds", 0); i++) stats.incrementLegendaryBuds();
            for (int i = 0; i < growing.getInt("five-star-buds", 0); i++) stats.incrementFiveStarBuds();
            for (int i = 0; i < growing.getInt("perfect-harvests", 0); i++) stats.incrementPerfectHarvests();
        }
        
        // Load trading stats
        ConfigurationSection trading = section.getConfigurationSection("trading");
        if (trading != null) {
            for (int i = 0; i < trading.getInt("successful-sales", 0); i++) stats.incrementSuccessfulSales();
            for (int i = 0; i < trading.getInt("failed-sales", 0); i++) stats.incrementFailedSales();
            stats.recordSale(trading.getDouble("highest-sale", 0));
        }
        
        // Load joint stats
        ConfigurationSection joints = section.getConfigurationSection("joints");
        if (joints != null) {
            for (int i = 0; i < joints.getInt("total-rolled", 0); i++) stats.incrementJointsRolled();
            for (int i = 0; i < joints.getInt("perfect-rolls", 0); i++) stats.incrementPerfectRolls();
            for (int i = 0; i < joints.getInt("legendary-joints", 0); i++) stats.incrementLegendaryJoints();
        }
        
        // Load time stats
        stats.addPlaytime(section.getLong("playtime-minutes", 0));
        stats.updatePlaySession(section.getLong("longest-session", 0));
        stats.setLastLoginTime(section.getLong("last-login", 0));
        
        // Load prestige
        stats.setPrestigeLevel(section.getInt("prestige-level", 0));
        
        // Load streaks
        for (int i = 0; i < section.getInt("daily-streak", 0); i++) stats.incrementDailyStreak();
        stats.setLastDailyClaimTime(section.getLong("last-daily-claim", 0));
        
        // Load achievements
        ConfigurationSection achievements = section.getConfigurationSection("achievements");
        if (achievements != null) {
            for (String achievementId : achievements.getKeys(false)) {
                stats.unlockAchievement(achievementId);
            }
        }
        
        // Load challenges
        for (int i = 0; i < section.getInt("daily-challenges-completed", 0); i++) {
            stats.incrementDailyChallenges();
        }
        for (int i = 0; i < section.getInt("weekly-challenges-completed", 0); i++) {
            stats.incrementWeeklyChallenges();
        }
        
        // Load crossbreed stats
        ConfigurationSection crossbreed = section.getConfigurationSection("crossbreed");
        if (crossbreed != null) {
            for (int i = 0; i < crossbreed.getInt("strains-created", 0); i++) stats.incrementStrainsCreated();
            for (int i = 0; i < crossbreed.getInt("crossbreeds-attempted", 0); i++) stats.incrementCrossbreeds();
            for (int i = 0; i < crossbreed.getInt("legendary-strains", 0); i++) stats.incrementLegendaryStrains();
        }
        
        return stats;
    }

    public void saveStats() {
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            savePlayerStats(entry.getKey(), entry.getValue());
        }
        
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save stats.yml");
        }
    }

    private void savePlayerStats(UUID uuid, PlayerStats stats) {
        String path = "players." + uuid.toString();
        
        // Growing stats
        statsConfig.set(path + ".growing.plants-grown", stats.getTotalPlantsGrown());
        statsConfig.set(path + ".growing.plants-harvested", stats.getTotalPlantsHarvested());
        statsConfig.set(path + ".growing.legendary-buds", stats.getLegendaryBudsHarvested());
        statsConfig.set(path + ".growing.five-star-buds", stats.getFiveStarBudsHarvested());
        statsConfig.set(path + ".growing.perfect-harvests", stats.getPerfectHarvests());
        
        // Trading stats
        statsConfig.set(path + ".trading.successful-sales", stats.getTotalSalesSuccess());
        statsConfig.set(path + ".trading.failed-sales", stats.getTotalSalesFailed());
        statsConfig.set(path + ".trading.highest-sale", stats.getHighestSingleSale());
        statsConfig.set(path + ".trading.total-earned", stats.getTotalMoneyEarned());
        
        // Joint stats
        statsConfig.set(path + ".joints.total-rolled", stats.getJointsRolled());
        statsConfig.set(path + ".joints.perfect-rolls", stats.getPerfectRolls());
        statsConfig.set(path + ".joints.legendary-joints", stats.getLegendaryJointsRolled());
        
        // Time stats
        statsConfig.set(path + ".playtime-minutes", stats.getTotalPlaytimeMinutes());
        statsConfig.set(path + ".longest-session", stats.getLongestPlaySession());
        statsConfig.set(path + ".last-login", stats.getLastLoginTime());
        
        // Prestige
        statsConfig.set(path + ".prestige-level", stats.getPrestigeLevel());
        statsConfig.set(path + ".total-prestiges", stats.getTotalPrestiges());
        
        // Streaks
        statsConfig.set(path + ".daily-streak", stats.getCurrentDailyStreak());
        statsConfig.set(path + ".longest-streak", stats.getLongestDailyStreak());
        statsConfig.set(path + ".last-daily-claim", stats.getLastDailyClaimTime());
        
        // Achievements
        for (Map.Entry<String, Long> achievement : stats.getAchievementUnlocks().entrySet()) {
            statsConfig.set(path + ".achievements." + achievement.getKey(), achievement.getValue());
        }
        
        // Challenges
        statsConfig.set(path + ".daily-challenges-completed", stats.getDailyChallengesCompleted());
        statsConfig.set(path + ".weekly-challenges-completed", stats.getWeeklyChallengesCompleted());
        
        // Crossbreed
        statsConfig.set(path + ".crossbreed.strains-created", stats.getStrainsCreated());
        statsConfig.set(path + ".crossbreed.crossbreeds-attempted", stats.getCrossbreedsAttempted());
        statsConfig.set(path + ".crossbreed.legendary-strains", stats.getLegendaryStrainsDiscovered());
    }

    public PlayerStats getStats(Player player) {
        return getStats(player.getUniqueId());
    }

    public PlayerStats getStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, PlayerStats::new);
    }

    // ===== LEADERBOARD METHODS =====

    /**
     * Gets the top players by BudLord Score.
     */
    public List<LeaderboardEntry> getTopByScore(int limit) {
        return playerStats.values().stream()
            .sorted((a, b) -> Integer.compare(b.calculateBudLordScore(), a.calculateBudLordScore()))
            .limit(limit)
            .map(stats -> new LeaderboardEntry(stats.getPlayerId(), stats.calculateBudLordScore()))
            .collect(Collectors.toList());
    }

    /**
     * Gets the top players by total money earned.
     */
    public List<LeaderboardEntry> getTopByEarnings(int limit) {
        return playerStats.values().stream()
            .sorted((a, b) -> Double.compare(b.getTotalMoneyEarned(), a.getTotalMoneyEarned()))
            .limit(limit)
            .map(stats -> new LeaderboardEntry(stats.getPlayerId(), (int) stats.getTotalMoneyEarned()))
            .collect(Collectors.toList());
    }

    /**
     * Gets the top players by plants harvested.
     */
    public List<LeaderboardEntry> getTopByHarvests(int limit) {
        return playerStats.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTotalPlantsHarvested(), a.getTotalPlantsHarvested()))
            .limit(limit)
            .map(stats -> new LeaderboardEntry(stats.getPlayerId(), stats.getTotalPlantsHarvested()))
            .collect(Collectors.toList());
    }

    /**
     * Gets the top players by prestige level.
     */
    public List<LeaderboardEntry> getTopByPrestige(int limit) {
        return playerStats.values().stream()
            .sorted((a, b) -> Integer.compare(b.getPrestigeLevel(), a.getPrestigeLevel()))
            .limit(limit)
            .map(stats -> new LeaderboardEntry(stats.getPlayerId(), stats.getPrestigeLevel()))
            .collect(Collectors.toList());
    }

    /**
     * Gets the top players by daily streak.
     */
    public List<LeaderboardEntry> getTopByStreak(int limit) {
        return playerStats.values().stream()
            .sorted((a, b) -> Integer.compare(b.getCurrentDailyStreak(), a.getCurrentDailyStreak()))
            .limit(limit)
            .map(stats -> new LeaderboardEntry(stats.getPlayerId(), stats.getCurrentDailyStreak()))
            .collect(Collectors.toList());
    }

    /**
     * Represents a leaderboard entry.
     */
    public record LeaderboardEntry(UUID playerId, int value) {
    }
}
