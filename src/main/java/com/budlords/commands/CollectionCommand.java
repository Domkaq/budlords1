package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.collections.CollectionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command for viewing the strain collection book in BudLords v2.0.0.
 */
public class CollectionCommand implements CommandExecutor, TabCompleter {

    private final BudLords plugin;

    public CollectionCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        CollectionManager collectionManager = plugin.getCollectionManager();
        if (collectionManager == null) {
            sender.sendMessage("§cCollection system is not enabled!");
            return true;
        }

        // Optional page argument
        int page = 0;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]) - 1;
                if (page < 0) page = 0;
            } catch (NumberFormatException e) {
                // Ignore, use page 0
            }
        }

        collectionManager.openCollectionGUI(player, page);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> pages = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                pages.add(String.valueOf(i));
            }
            return pages;
        }
        return new ArrayList<>();
    }
}
