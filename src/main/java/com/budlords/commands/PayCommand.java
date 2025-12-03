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

public class PayCommand implements CommandExecutor, TabCompleter {

    private final EconomyManager economyManager;

    public PayCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pay <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        if (target.equals(player)) {
            sender.sendMessage("§cYou cannot pay yourself!");
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

        if (!economyManager.hasBalance(player, amount)) {
            sender.sendMessage("§cYou don't have enough money!");
            return true;
        }

        if (economyManager.transfer(player, target, amount)) {
            sender.sendMessage("§aYou paid " + economyManager.formatMoney(amount) + " to " + target.getName() + "!");
            target.sendMessage("§aYou received " + economyManager.formatMoney(amount) + " from " + player.getName() + "!");
        } else {
            sender.sendMessage("§cPayment failed!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (sender instanceof Player player) {
                double balance = economyManager.getBalance(player);
                return List.of(
                    String.valueOf((int)(balance * 0.1)),
                    String.valueOf((int)(balance * 0.25)),
                    String.valueOf((int)(balance * 0.5)),
                    String.valueOf((int)balance)
                );
            }
        }
        return new ArrayList<>();
    }
}
