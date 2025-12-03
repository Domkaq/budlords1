package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.challenges.ChallengeManager;
import com.budlords.crossbreed.CrossbreedManager;
import com.budlords.prestige.PrestigeManager;
import com.budlords.stats.PlayerStats;
import com.budlords.stats.StatsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the /stats command for viewing player statistics.
 */
public class StatsCommand implements CommandExecutor, TabCompleter {

    private final StatsManager statsManager;

    public StatsCommand(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        PlayerStats stats = statsManager.getStats(player);
        
        player.sendMessage("");
        player.sendMessage("§8§l═══════════════════════════════════════");
        player.sendMessage("§e§l        ✦ Your BudLord Statistics ✦");
        player.sendMessage("§8§l═══════════════════════════════════════");
        player.sendMessage("");
        
        // Overall score and title
        player.sendMessage("§7BudLord Score: §a" + stats.calculateBudLordScore());
        player.sendMessage("§7Title: " + stats.getScoreTitle());
        player.sendMessage("§7Prestige: §5" + (stats.getPrestigeLevel() > 0 ? "P" + stats.getPrestigeLevel() : "None"));
        player.sendMessage("");
        
        // Growing stats
        player.sendMessage("§a§l✿ Growing Stats");
        player.sendMessage("§7  Plants Grown: §e" + stats.getTotalPlantsGrown());
        player.sendMessage("§7  Plants Harvested: §e" + stats.getTotalPlantsHarvested());
        player.sendMessage("§7  Perfect Harvests: §a" + stats.getPerfectHarvests());
        player.sendMessage("§7  5★ Buds: §6" + stats.getFiveStarBudsHarvested());
        player.sendMessage("§7  Legendary Buds: §d" + stats.getLegendaryBudsHarvested());
        player.sendMessage("");
        
        // Trading stats
        player.sendMessage("§e§l💰 Trading Stats");
        player.sendMessage("§7  Successful Sales: §a" + stats.getTotalSalesSuccess());
        player.sendMessage("§7  Failed Sales: §c" + stats.getTotalSalesFailed());
        player.sendMessage("§7  Success Rate: §e" + String.format("%.1f%%", stats.getSuccessRate()));
        player.sendMessage("§7  Highest Sale: §6$" + String.format("%,.0f", stats.getHighestSingleSale()));
        player.sendMessage("§7  Total Earned: §a$" + String.format("%,.0f", stats.getTotalMoneyEarned()));
        player.sendMessage("");
        
        // Joint stats
        player.sendMessage("§6§l🚬 Joint Stats");
        player.sendMessage("§7  Joints Rolled: §e" + stats.getJointsRolled());
        player.sendMessage("§7  Perfect Rolls: §a" + stats.getPerfectRolls());
        player.sendMessage("§7  Legendary Joints: §d" + stats.getLegendaryJointsRolled());
        player.sendMessage("");
        
        // Progress stats
        player.sendMessage("§5§l✦ Progress");
        player.sendMessage("§7  Daily Streak: §e" + stats.getCurrentDailyStreak() + " days");
        player.sendMessage("§7  Longest Streak: §e" + stats.getLongestDailyStreak() + " days");
        player.sendMessage("§7  Challenges Completed: §a" + stats.getChallengesCompleted());
        player.sendMessage("§7  Achievements: §6" + stats.getAchievementCount());
        player.sendMessage("");
        
        // Crossbreeding stats
        player.sendMessage("§d§l✿ Crossbreeding");
        player.sendMessage("§7  Strains Created: §e" + stats.getStrainsCreated());
        player.sendMessage("§7  Crossbreeds Attempted: §e" + stats.getCrossbreedsAttempted());
        player.sendMessage("§7  Legendary Strains: §6" + stats.getLegendaryStrainsDiscovered());
        player.sendMessage("");
        player.sendMessage("§8§l═══════════════════════════════════════");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}
