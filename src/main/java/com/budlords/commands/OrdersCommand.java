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
        
        // Display current order
        BulkOrderManager.BulkOrder order = orderManager.getActiveOrder(player.getUniqueId());
        
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("§e§l         BULK ORDERS §7(Admin View)");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("");
        
        if (order == null) {
            player.sendMessage("§7No active bulk order.");
            player.sendMessage("§7Use §e/orders new §7to get a new order!");
            
            long cooldown = orderManager.getTimeUntilRefresh(player.getUniqueId());
            if (cooldown > 0) {
                player.sendMessage("§7Next order available in: §e" + (cooldown / 60000) + " minutes");
            }
        } else {
            player.sendMessage("§7Buyer: §f" + order.buyerName);
            player.sendMessage("§7Tier: " + order.tier.displayName);
            player.sendMessage("");
            player.sendMessage("§7Wants: §e" + order.quantity + "x §f" + order.strainName);
            player.sendMessage("§7Bonus: §a+" + String.format("%.0f%%", (order.priceMultiplier - 1) * 100) + " §7price!");
            player.sendMessage("");
            player.sendMessage("§7Time remaining: §e" + order.getTimeRemainingText());
            player.sendMessage("");
            player.sendMessage("§7§oSell the requested items to any buyer to complete!");
        }
        
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("");
        
        return true;
    }
}
