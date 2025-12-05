package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.economy.MarketDemandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to view market status and dynamic pricing information.
 * Part of BudLords v3.0.0 economic improvements.
 */
public class MarketCommand implements CommandExecutor {

    private final BudLords plugin;

    public MarketCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        MarketDemandManager marketManager = plugin.getMarketDemandManager();
        if (marketManager == null) {
            player.sendMessage("§cMarket system is not available!");
            return true;
        }
        
        // Display market status
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("§e§l         MARKET STATUS");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("");
        
        String eventName = marketManager.getCurrentMarketEvent();
        double multiplier = marketManager.getEventMultiplier();
        
        player.sendMessage("§7Current Conditions:");
        switch (eventName) {
            case "BUYER_RUSH" -> {
                player.sendMessage("  §a⬆ §eBUYER RUSH");
                player.sendMessage("  §7High demand is driving prices up!");
                player.sendMessage("  §7Price modifier: §a+" + String.format("%.0f%%", (multiplier - 1) * 100));
            }
            case "POLICE_CRACKDOWN" -> {
                player.sendMessage("  §c⬇ §ePOLICE CRACKDOWN");
                player.sendMessage("  §7Buyers are cautious, prices are down.");
                player.sendMessage("  §7Price modifier: §c" + String.format("%.0f%%", (multiplier - 1) * 100));
            }
            case "FESTIVAL_SEASON" -> {
                player.sendMessage("  §d⬆⬆ §eFESTIVAL SEASON");
                player.sendMessage("  §7Festivals bring massive demand!");
                player.sendMessage("  §7Price modifier: §a+" + String.format("%.0f%%", (multiplier - 1) * 100));
            }
            case "SUPPLY_SHORTAGE" -> {
                player.sendMessage("  §6⬆ §eSUPPLY SHORTAGE");
                player.sendMessage("  §7Low supply means higher prices!");
                player.sendMessage("  §7Price modifier: §a+" + String.format("%.0f%%", (multiplier - 1) * 100));
            }
            case "MARKET_CRASH" -> {
                player.sendMessage("  §c⬇⬇ §eMARKET CRASH");
                player.sendMessage("  §7Oversupply has crashed the market!");
                player.sendMessage("  §7Price modifier: §c" + String.format("%.0f%%", (multiplier - 1) * 100));
            }
            case "PREMIUM_DEMAND" -> {
                player.sendMessage("  §5⬆ §ePREMIUM DEMAND");
                player.sendMessage("  §7Connoisseurs are paying top dollar!");
                player.sendMessage("  §7Price modifier: §a+" + String.format("%.0f%%", (multiplier - 1) * 100));
            }
            default -> {
                player.sendMessage("  §7§oStable Market");
                player.sendMessage("  §7Normal market conditions.");
                player.sendMessage("  §7Price modifier: §f0%");
            }
        }
        
        if (!eventName.equals("NORMAL")) {
            long timeRemaining = marketManager.getEventTimeRemainingMinutes();
            player.sendMessage("");
            player.sendMessage("  §7Time until normal: §e" + timeRemaining + " minutes");
        }
        
        player.sendMessage("");
        player.sendMessage("§6§lTips:");
        player.sendMessage("§7• Market events affect all sale prices");
        player.sendMessage("§7• Rare strains may have individual demand");
        player.sendMessage("§7• Sell during high demand for best profits!");
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("");
        
        return true;
    }
}
