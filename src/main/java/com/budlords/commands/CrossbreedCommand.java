package com.budlords.commands;

import com.budlords.crossbreed.CrossbreedManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the /crossbreed command.
 */
public class CrossbreedCommand implements CommandExecutor, TabCompleter {

    private final CrossbreedManager crossbreedManager;

    public CrossbreedCommand(CrossbreedManager crossbreedManager) {
        this.crossbreedManager = crossbreedManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        crossbreedManager.openCrossbreedGUI(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}
