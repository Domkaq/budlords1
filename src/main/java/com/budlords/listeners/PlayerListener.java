package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.data.DataManager;
import com.budlords.items.PhoneItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final BudLords plugin;
    private final DataManager dataManager;

    public PlayerListener(BudLords plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Initialize player data
        plugin.getEconomyManager().initializePlayer(player);
        
        // Initialize stats (ensures player has stats entry)
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().getStats(player);
        }
        
        // Sync achievements with stats on login
        if (plugin.getAchievementManager() != null) {
            // Delay slightly to ensure all systems are initialized
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getAchievementManager().syncWithStats(player);
            }, 20L);
        }
        
        // Send welcome message if first time
        if (!dataManager.getPlayersConfig().contains("players." + player.getUniqueId())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("");
                player.sendMessage("§2§l  Welcome to BudLords!");
                player.sendMessage("§7  Start your weed farming empire!");
                player.sendMessage("");
                player.sendMessage("§7  Use §f/budlords§7 for help.");
                player.sendMessage("");
            }, 40L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getEconomyManager().saveBalances();
            
            // Save stats
            if (plugin.getStatsManager() != null) {
                plugin.getStatsManager().saveStats();
            }
            
            // Save skills
            if (plugin.getSkillManager() != null) {
                plugin.getSkillManager().saveSkills();
            }
            
            // Save achievements
            if (plugin.getAchievementManager() != null) {
                plugin.getAchievementManager().saveAchievements();
            }
        });
    }
    
    /**
     * Handle phone interactions when right-clicking in air.
     * Opens the contacts list when player uses phone without targeting an entity.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click air - block/entity clicks are handled elsewhere
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if holding phone
        if (PhoneItems.isPhone(item)) {
            event.setCancelled(true);
            if (plugin.getBuyerProfileGUI() != null) {
                plugin.getBuyerProfileGUI().openContactsList(player);
            } else {
                player.sendMessage("§cPhone system is not available!");
            }
        }
    }
}
