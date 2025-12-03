package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.gui.StrainCreatorGUI;
import com.budlords.strain.StrainManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StrainCreatorCommand implements CommandExecutor, TabCompleter {

    private final BudLords plugin;
    private final StrainCreatorGUI gui;

    public StrainCreatorCommand(BudLords plugin, StrainManager strainManager) {
        this.plugin = plugin;
        this.gui = new StrainCreatorGUI(plugin, strainManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!player.hasPermission("budlords.admin")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        gui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
