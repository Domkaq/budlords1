package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.achievements.Achievement;
import com.budlords.achievements.AchievementManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for viewing achievements in BudLords v2.0.0.
 */
public class AchievementsCommand implements CommandExecutor, TabCompleter {

    private final BudLords plugin;

    public AchievementsCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        AchievementManager achievementManager = plugin.getAchievementManager();
        if (achievementManager == null) {
            sender.sendMessage("§cAchievement system is not enabled!");
            return true;
        }

        // Sync achievements with stats
        achievementManager.syncWithStats(player);

        // Determine category to view
        Achievement.AchievementCategory category = Achievement.AchievementCategory.FARMING; // Default
        
        if (args.length > 0) {
            try {
                category = Achievement.AchievementCategory.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid category! Use: farming, strains, economy, trading, rolling, challenges, prestige, special, legendary");
                return true;
            }
        }

        achievementManager.openAchievementsGUI(player, category);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Achievement.AchievementCategory.values())
                .map(c -> c.name().toLowerCase())
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
