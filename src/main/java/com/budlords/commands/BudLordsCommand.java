package com.budlords.commands;

import com.budlords.BudLords;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class BudLordsCommand implements CommandExecutor, TabCompleter {

    private final BudLords plugin;

    public BudLordsCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("budlords.admin")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            
            plugin.reloadConfig();
            plugin.getDataManager().reloadAll();
            sender.sendMessage("§aBudLords configuration reloaded!");
            return true;
        }

        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§2§l  BudLords §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("");
        sender.sendMessage("§7  A weed farming economy plugin");
        sender.sendMessage("");
        sender.sendMessage("§e  Commands:");
        sender.sendMessage("§7  /bal §8- §fCheck your balance");
        sender.sendMessage("§7  /pay <player> <amount> §8- §fPay someone");
        sender.sendMessage("§7  /package <amount> §8- §fPackage buds for sale");
        sender.sendMessage("");
        if (sender.hasPermission("budlords.admin")) {
            sender.sendMessage("§c  Admin Commands:");
            sender.sendMessage("§7  /addmoney <player> <amount> §8- §fAdd money");
            sender.sendMessage("§7  /straincreator §8- §fCreate new strains");
            sender.sendMessage("§7  /spawnmarket §8- §fSpawn Market Joe");
            sender.sendMessage("§7  /spawnblackmarket §8- §fSpawn BlackMarket Joe");
            sender.sendMessage("§7  /budlords reload §8- §fReload config");
            sender.sendMessage("");
        }
        sender.sendMessage("§8§m                                          ");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("budlords.admin")) {
            String input = args[0].toLowerCase();
            List<String> options = new ArrayList<>();
            if ("reload".startsWith(input)) {
                options.add("reload");
            }
            return options;
        }
        return new ArrayList<>();
    }
}
