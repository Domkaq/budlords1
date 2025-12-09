package com.budlords.commands;

import com.budlords.BudLords;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open sale analytics/history GUI.
 */
public class SalesCommand implements CommandExecutor {
    
    private final BudLords plugin;
    
    public SalesCommand(BudLords plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        if (plugin.getSaleAnalyticsGUI() == null) {
            player.sendMessage("§cSale analytics system is not available!");
            return true;
        }
        
        plugin.getSaleAnalyticsGUI().open(player);
        return true;
    }
}
