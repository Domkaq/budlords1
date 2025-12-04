package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.challenges.ChallengeManager;
import com.budlords.crossbreed.CrossbreedManager;
import com.budlords.prestige.PrestigeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles click events for the new GUI systems:
 * - Prestige Menu
 * - Challenge Menu
 * - Crossbreeding Lab
 */
public class GUIListener implements Listener {

    private final BudLords plugin;

    public GUIListener(BudLords plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() == null) return;
        
        // Handle Prestige GUI
        if (event.getInventory().getHolder() instanceof PrestigeManager) {
            event.setCancelled(true);
            plugin.getPrestigeManager().handlePrestigeClick(player, event.getRawSlot());
            return;
        }
        
        // Handle Challenge GUI
        if (event.getInventory().getHolder() instanceof ChallengeManager) {
            event.setCancelled(true);
            plugin.getChallengeManager().handleChallengeClick(player, event.getRawSlot());
            return;
        }
        
        // Handle Crossbreed GUI
        if (event.getInventory().getHolder() instanceof CrossbreedManager) {
            int slot = event.getRawSlot();
            
            // Check if it's a seed drop on parent slots
            if ((slot == 20 || slot == 22) && event.getCursor() != null) {
                ItemStack cursor = event.getCursor();
                if (plugin.getStrainManager().isSeedItem(cursor)) {
                    event.setCancelled(true);
                    plugin.getCrossbreedManager().handleSeedDrop(player, cursor, slot);
                    return;
                }
            }
            
            event.setCancelled(true);
            plugin.getCrossbreedManager().handleCrossbreedClick(player, slot);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Any cleanup needed when closing GUIs can go here
    }
}
