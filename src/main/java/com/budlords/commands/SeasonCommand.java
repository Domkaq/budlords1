package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.seasons.SeasonManager;
import com.budlords.strain.SeedType.Season;
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
 * Command for viewing and managing seasons in BudLords v2.0.0.
 */
public class SeasonCommand implements CommandExecutor, TabCompleter {

    private final BudLords plugin;

    public SeasonCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        SeasonManager seasonManager = plugin.getSeasonManager();
        if (seasonManager == null) {
            sender.sendMessage("§cSeason system is not enabled!");
            return true;
        }

        if (args.length == 0) {
            // Show current season info
            Season currentSeason = seasonManager.getCurrentSeason();
            
            sender.sendMessage("");
            sender.sendMessage("§8§l═══════════════════════════════════");
            sender.sendMessage("§f§l   ✦ CURRENT SEASON ✦");
            sender.sendMessage("");
            sender.sendMessage("   " + currentSeason.getDisplayName());
            sender.sendMessage("");
            sender.sendMessage("§7   Growth Speed: " + formatMultiplier(currentSeason.getGrowthMultiplier()));
            sender.sendMessage("§7   Quality Bonus: " + formatMultiplier(currentSeason.getQualityMultiplier()));
            sender.sendMessage("§7   Potency Bonus: " + formatMultiplier(currentSeason.getPotencyMultiplier()));
            sender.sendMessage("");
            sender.sendMessage("§7   Time Remaining: §e" + seasonManager.getTimeRemainingFormatted());
            sender.sendMessage("§8§l═══════════════════════════════════");
            sender.sendMessage("");
            
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set" -> {
                if (!sender.hasPermission("budlords.admin")) {
                    sender.sendMessage("§cYou don't have permission to change seasons!");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /season set <spring|summer|autumn|winter>");
                    return true;
                }
                
                try {
                    Season newSeason = Season.valueOf(args[1].toUpperCase());
                    seasonManager.forceSeason(newSeason);
                    sender.sendMessage("§aForced season change to " + newSeason.getDisplayName());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cInvalid season! Use: spring, summer, autumn, winter");
                }
            }
            
            case "info" -> {
                sender.sendMessage("");
                sender.sendMessage("§8§l═══════════════════════════════════");
                sender.sendMessage("§f§l   ✦ SEASON INFORMATION ✦");
                sender.sendMessage("");
                
                for (Season season : Season.values()) {
                    sender.sendMessage("   " + season.getDisplayName());
                    sender.sendMessage("§7     Growth: " + formatMultiplier(season.getGrowthMultiplier()) +
                                      " | Quality: " + formatMultiplier(season.getQualityMultiplier()));
                }
                
                sender.sendMessage("");
                sender.sendMessage("§7   Tip: Certain seed types have seasonal bonuses!");
                sender.sendMessage("§8§l═══════════════════════════════════");
                sender.sendMessage("");
            }
            
            default -> {
                sender.sendMessage("§cUsage: /season [info|set]");
            }
        }

        return true;
    }

    private String formatMultiplier(double mult) {
        if (mult >= 1.0) {
            return "§a+" + String.format("%.0f%%", (mult - 1.0) * 100);
        } else {
            return "§c" + String.format("%.0f%%", (mult - 1.0) * 100);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("info");
            if (sender.hasPermission("budlords.admin")) {
                completions.add("set");
            }
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("set") && sender.hasPermission("budlords.admin")) {
            return Arrays.stream(Season.values())
                .map(s -> s.name().toLowerCase())
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
