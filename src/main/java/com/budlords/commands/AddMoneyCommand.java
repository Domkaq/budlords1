package com.budlords.commands;

import com.budlords.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddMoneyCommand implements CommandExecutor, TabCompleter {

    private final EconomyManager economyManager;

    public AddMoneyCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("budlords.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /addmoney <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                sender.sendMessage("§cAmount must be positive!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount!");
            return true;
        }

        economyManager.addBalance(target, amount);
        sender.sendMessage("§aAdded " + economyManager.formatMoney(amount) + " to " + target.getName() + "'s balance.");
        target.sendMessage("§aYou received " + economyManager.formatMoney(amount) + " from an admin!");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return List.of("100", "500", "1000", "5000", "10000");
        }
        return new ArrayList<>();
    }
}
