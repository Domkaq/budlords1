package com.budlords.commands;

import com.budlords.economy.EconomyManager;
import com.budlords.progression.RankManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final EconomyManager economyManager;
    private final RankManager rankManager;

    public BalanceCommand(EconomyManager economyManager, RankManager rankManager) {
        this.economyManager = economyManager;
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        double balance = economyManager.getBalance(player);
        double totalEarnings = economyManager.getTotalEarnings(player);
        String rankDisplay = rankManager.getRankDisplayName(player);
        RankManager.Rank nextRank = rankManager.getNextRank(player);
        double progress = rankManager.getProgressToNextRank(player);

        player.sendMessage("§8§m                                          ");
        player.sendMessage("§6§l  BudLords §7- §fBalance");
        player.sendMessage("");
        player.sendMessage("§7  Balance: §a" + economyManager.formatMoney(balance));
        player.sendMessage("§7  Total Earnings: §e" + economyManager.formatMoney(totalEarnings));
        player.sendMessage("§7  Rank: " + rankDisplay);
        
        if (nextRank != null) {
            int progressBars = (int) (progress * 10);
            StringBuilder progressBar = new StringBuilder("§7[");
            for (int i = 0; i < 10; i++) {
                if (i < progressBars) {
                    progressBar.append("§a█");
                } else {
                    progressBar.append("§8█");
                }
            }
            progressBar.append("§7]");
            player.sendMessage("§7  Next Rank: §f" + nextRank.name() + " " + progressBar);
            player.sendMessage("§7  Required: §e" + economyManager.formatMoney(nextRank.requiredEarnings()));
        } else {
            player.sendMessage("§7  §d§lMAX RANK ACHIEVED!");
        }
        
        player.sendMessage("§8§m                                          ");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
