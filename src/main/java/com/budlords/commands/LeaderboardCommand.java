package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Handles the /leaderboard command for viewing top players.
 */
public class LeaderboardCommand implements CommandExecutor, TabCompleter {

    private final StatsManager statsManager;

    public LeaderboardCommand(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        String type = args.length > 0 ? args[0].toLowerCase() : "score";

        player.sendMessage("");
        player.sendMessage("§8═══════════════════════════════════");
        
        switch (type) {
            case "score" -> showScoreLeaderboard(player);
            case "earnings" -> showEarningsLeaderboard(player);
            case "harvests" -> showHarvestsLeaderboard(player);
            case "prestige" -> showPrestigeLeaderboard(player);
            case "streak" -> showStreakLeaderboard(player);
            default -> {
                player.sendMessage("§e§l      ✦ Leaderboard Categories ✦");
                player.sendMessage("§8═══════════════════════════════════");
                player.sendMessage("");
                player.sendMessage("§e/leaderboard score §7- Top by BudLord Score");
                player.sendMessage("§e/leaderboard earnings §7- Top by Total Earnings");
                player.sendMessage("§e/leaderboard harvests §7- Top by Plants Harvested");
                player.sendMessage("§e/leaderboard prestige §7- Top by Prestige Level");
                player.sendMessage("§e/leaderboard streak §7- Top by Daily Streak");
            }
        }
        
        player.sendMessage("");
        player.sendMessage("§8═══════════════════════════════════");

        return true;
    }

    private void showScoreLeaderboard(Player player) {
        player.sendMessage("§e§l      ✦ Top BudLord Scores ✦");
        player.sendMessage("§8═══════════════════════════════════");
        player.sendMessage("");
        
        List<StatsManager.LeaderboardEntry> top = statsManager.getTopByScore(10);
        displayLeaderboard(player, top, " pts");
    }

    private void showEarningsLeaderboard(Player player) {
        player.sendMessage("§a§l         ✦ Top Earnings ✦");
        player.sendMessage("§8═══════════════════════════════════");
        player.sendMessage("");
        
        List<StatsManager.LeaderboardEntry> top = statsManager.getTopByEarnings(10);
        displayLeaderboard(player, top, "$");
    }

    private void showHarvestsLeaderboard(Player player) {
        player.sendMessage("§a§l        ✦ Top Harvesters ✦");
        player.sendMessage("§8═══════════════════════════════════");
        player.sendMessage("");
        
        List<StatsManager.LeaderboardEntry> top = statsManager.getTopByHarvests(10);
        displayLeaderboard(player, top, " plants");
    }

    private void showPrestigeLeaderboard(Player player) {
        player.sendMessage("§5§l        ✦ Top Prestige ✦");
        player.sendMessage("§8═══════════════════════════════════");
        player.sendMessage("");
        
        List<StatsManager.LeaderboardEntry> top = statsManager.getTopByPrestige(10);
        displayLeaderboard(player, top, " prestige");
    }

    private void showStreakLeaderboard(Player player) {
        player.sendMessage("§6§l         ✦ Top Streaks ✦");
        player.sendMessage("§8═══════════════════════════════════");
        player.sendMessage("");
        
        List<StatsManager.LeaderboardEntry> top = statsManager.getTopByStreak(10);
        displayLeaderboard(player, top, " days");
    }

    private void displayLeaderboard(Player player, List<StatsManager.LeaderboardEntry> entries, String suffix) {
        if (entries.isEmpty()) {
            player.sendMessage("§7No data available yet!");
            return;
        }
        
        int rank = 1;
        for (StatsManager.LeaderboardEntry entry : entries) {
            String rankColor = switch (rank) {
                case 1 -> "§6§l";
                case 2 -> "§f§l";
                case 3 -> "§c§l";
                default -> "§7";
            };
            String medal = switch (rank) {
                case 1 -> "🥇 ";
                case 2 -> "🥈 ";
                case 3 -> "🥉 ";
                default -> "";
            };
            
            String playerName = getPlayerName(entry.playerId());
            String value = suffix.startsWith("$") 
                ? suffix + String.format("%,d", entry.value())
                : String.format("%,d", entry.value()) + suffix;
            
            player.sendMessage(rankColor + "#" + rank + " " + medal + playerName + " §7- §e" + value);
            rank++;
        }
    }

    private String getPlayerName(UUID playerId) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        String name = offlinePlayer.getName();
        return name != null ? name : "Unknown";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("score", "earnings", "harvests", "prestige", "streak");
        }
        return new ArrayList<>();
    }
}
