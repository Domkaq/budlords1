package com.budlords.commands;

import com.budlords.packaging.PackagingManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PackageCommand implements CommandExecutor, TabCompleter {

    private final PackagingManager packagingManager;

    public PackageCommand(PackagingManager packagingManager) {
        this.packagingManager = packagingManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /package <amount>");
            player.sendMessage("§7Valid amounts: 1, 3, 5, 10 (grams)");
            return true;
        }

        int grams;
        try {
            grams = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount! Use 1, 3, 5, or 10.");
            return true;
        }

        if (grams != 1 && grams != 3 && grams != 5 && grams != 10) {
            player.sendMessage("§cInvalid amount! Use 1, 3, 5, or 10.");
            return true;
        }

        packagingManager.packageBuds(player, grams);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> options = List.of("1", "3", "5", "10");
            List<String> result = new ArrayList<>();
            for (String opt : options) {
                if (opt.startsWith(input)) {
                    result.add(opt);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
}
