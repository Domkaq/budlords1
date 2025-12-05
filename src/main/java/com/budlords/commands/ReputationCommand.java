package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.economy.ReputationManager;
import com.budlords.npc.NPCManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to view reputation with different buyers.
 * Part of BudLords v3.0.0 enhanced selling system.
 */
public class ReputationCommand implements CommandExecutor {

    private final BudLords plugin;

    public ReputationCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        ReputationManager repManager = plugin.getReputationManager();
        if (repManager == null) {
            player.sendMessage("§cReputation system is not available!");
            return true;
        }
        
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("§e§l         YOUR REPUTATION");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("");
        
        // Show reputation with each buyer type
        for (NPCManager.NPCType type : NPCManager.NPCType.values()) {
            int rep = repManager.getReputation(player.getUniqueId(), type.name());
            String display = repManager.getReputationDisplay(rep);
            String bonus = repManager.getReputationBonusText(rep);
            
            String buyerName = switch (type) {
                case MARKET_JOE -> "§a Market Joe";
                case BLACKMARKET_JOE -> "§5 BlackMarket Joe";
                case VILLAGE_VENDOR -> "§e Village Vendor";
            };
            
            player.sendMessage("§7" + buyerName + "§7:");
            player.sendMessage("  §7Status: " + display);
            player.sendMessage("  §7Points: §f" + rep + " §7| Bonus: " + bonus);
            player.sendMessage("");
        }
        
        player.sendMessage("§6§lReputation Levels:");
        player.sendMessage("§c Suspicious §7(-50) | §7Neutral §7(0) | §eFriendly §7(50)");
        player.sendMessage("§aTrusted §7(150) | §d§lVIP §7(300) | §6§l★LEGENDARY★ §7(500)");
        player.sendMessage("");
        player.sendMessage("§7Higher reputation = better prices & tips!");
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("");
        
        return true;
    }
}
