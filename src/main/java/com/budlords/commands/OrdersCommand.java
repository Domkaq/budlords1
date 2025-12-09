package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.economy.BulkOrderManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to view and manage bulk orders.
 * Part of BudLords v3.0.0 enhanced selling system.
 * 
 * Regular players should use the Dealer Phone GUI to access orders.
 * This command is only accessible to operators.
 */
public class OrdersCommand implements CommandExecutor {

    private final BudLords plugin;

    public OrdersCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        // Open the bulk orders GUI
        com.budlords.gui.BulkOrdersGUI gui = plugin.getBulkOrdersGUI();
        if (gui == null) {
            player.sendMessage("§cBulk orders system is not available!");
            return true;
        }
        
        gui.open(player);
        return true;
    }
}
