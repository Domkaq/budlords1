package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
import com.budlords.stats.PlayerStats;
import com.budlords.stats.StatsManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

/**
 * Daily reward command for BudLords v3.0.0.
 * Players can claim daily rewards that increase with their streak!
 */
public class DailyCommand implements CommandExecutor {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final StatsManager statsManager;
    
    // Base reward amount
    private static final double BASE_REWARD = 100.0;
    // Streak bonus per day (percentage). At 20-day streak, reaches MAX_STREAK_BONUS
    private static final double STREAK_BONUS_PERCENT = 5.0;
    // Maximum streak bonus cap (100% = 2x reward at 20+ day streak)
    private static final double MAX_STREAK_BONUS = 100.0;
    // Cooldown in milliseconds (24 hours)
    private static final long DAILY_COOLDOWN = TimeUnit.HOURS.toMillis(24);
    // Grace period for maintaining streak (32 hours - allows some flexibility)
    private static final long STREAK_GRACE_PERIOD = TimeUnit.HOURS.toMillis(32);

    public DailyCommand(BudLords plugin, EconomyManager economyManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        PlayerStats stats = statsManager.getStats(player);
        if (stats == null) {
            player.sendMessage("§cError: Could not load your stats!");
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastClaim = stats.getLastDailyClaimTime();
        long timeSinceClaim = currentTime - lastClaim;
        
        // Check if player can claim
        if (lastClaim > 0 && timeSinceClaim < DAILY_COOLDOWN) {
            // Cannot claim yet - show time remaining
            long timeRemaining = DAILY_COOLDOWN - timeSinceClaim;
            String timeStr = formatTime(timeRemaining);
            
            player.sendMessage("");
            player.sendMessage("§c§l✗ §cDaily Reward Not Available");
            player.sendMessage("§7You can claim again in: §e" + timeStr);
            player.sendMessage("");
            player.sendMessage("§7Current streak: §e" + stats.getCurrentDailyStreak() + " days");
            player.sendMessage("§7Longest streak: §6" + stats.getLongestDailyStreak() + " days");
            player.sendMessage("");
            return true;
        }
        
        // Calculate streak
        int newStreak;
        if (lastClaim == 0) {
            // First time claiming
            newStreak = 1;
        } else if (timeSinceClaim < STREAK_GRACE_PERIOD) {
            // Within grace period - increase streak
            newStreak = stats.getCurrentDailyStreak() + 1;
        } else {
            // Streak broken - reset to 1
            newStreak = 1;
            if (stats.getCurrentDailyStreak() > 0) {
                player.sendMessage("§c§lStreak Lost! §7Your " + stats.getCurrentDailyStreak() + 
                    "-day streak has been reset.");
            }
        }
        
        // Update stats
        stats.setCurrentDailyStreak(newStreak);
        if (newStreak > stats.getLongestDailyStreak()) {
            stats.setLongestDailyStreak(newStreak);
        }
        stats.setLastDailyClaimTime(currentTime);
        
        // Calculate reward
        double streakBonus = Math.min(newStreak * STREAK_BONUS_PERCENT, MAX_STREAK_BONUS);
        double rewardMultiplier = 1.0 + (streakBonus / 100.0);
        double totalReward = BASE_REWARD * rewardMultiplier;
        
        // Apply prestige bonus if available
        if (plugin.getPrestigeManager() != null && stats.getPrestigeLevel() > 0) {
            double prestigeMultiplier = plugin.getPrestigeManager().getEarningsMultiplier(stats.getPrestigeLevel());
            totalReward *= prestigeMultiplier;
        }
        
        // Give reward
        economyManager.addBalance(player, totalReward);
        
        // Save stats
        statsManager.saveStats();
        
        // Display reward message
        player.sendMessage("");
        player.sendMessage("§a§l✓ §aDaily Reward Claimed!");
        player.sendMessage("");
        player.sendMessage("§7Reward: §a$" + String.format("%,.2f", totalReward));
        player.sendMessage("§7Streak: §e" + newStreak + " days §7(+" + String.format("%.0f%%", streakBonus) + " bonus)");
        if (newStreak == stats.getLongestDailyStreak() && newStreak > 1) {
            player.sendMessage("§6✦ New longest streak!");
        }
        player.sendMessage("");
        player.sendMessage("§7Next claim available in: §e24 hours");
        player.sendMessage("");
        
        // Milestone bonuses
        if (newStreak == 7) {
            giveWeeklyBonus(player);
        } else if (newStreak == 30) {
            giveMonthlyBonus(player);
        } else if (newStreak == 100) {
            giveCenturyBonus(player);
        }
        
        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        player.spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        
        return true;
    }
    
    private void giveWeeklyBonus(Player player) {
        double weeklyBonus = 500.0;
        economyManager.addBalance(player, weeklyBonus);
        
        player.sendMessage("§6§l✦ 7-DAY STREAK BONUS! ✦");
        player.sendMessage("§7Extra reward: §a$" + String.format("%,.2f", weeklyBonus));
        player.sendMessage("");
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        player.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 2, 0), 50, 1, 1, 1, 0.1);
    }
    
    private void giveMonthlyBonus(Player player) {
        double monthlyBonus = 5000.0;
        economyManager.addBalance(player, monthlyBonus);
        
        player.sendMessage("§6§l✦ 30-DAY STREAK BONUS! ✦");
        player.sendMessage("§7Extra reward: §a$" + String.format("%,.2f", monthlyBonus));
        player.sendMessage("§7You're dedicated! Keep it up!");
        player.sendMessage("");
        
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);
        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 100, 2, 2, 2, 0.1);
    }
    
    private void giveCenturyBonus(Player player) {
        double centuryBonus = 50000.0;
        economyManager.addBalance(player, centuryBonus);
        
        player.sendMessage("§6§l✦✦✦ 100-DAY STREAK BONUS! ✦✦✦");
        player.sendMessage("§7Extra reward: §a$" + String.format("%,.2f", centuryBonus));
        player.sendMessage("§6You are a TRUE BudLord!");
        player.sendMessage("");
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.3f, 1.2f);
        player.spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 200, 2, 3, 2, 0.2);
    }
    
    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
