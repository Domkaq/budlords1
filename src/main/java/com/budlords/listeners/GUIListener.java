package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.challenges.ChallengeManager;
import com.budlords.crossbreed.CrossbreedManager;
import com.budlords.prestige.PrestigeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
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
            
            // Allow clicking in player inventory to pick up seeds
            if (slot >= 45) {
                // Player clicked in their own inventory
                // Check if they're clicking a seed to then drop on parent slots
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && plugin.getStrainManager().isSeedItem(clicked)) {
                    // Allow picking up seeds from player inventory
                    return; // Don't cancel this - let them pick up the seed
                }
            }
            
            // Check if it's a seed on cursor being placed on parent slots
            if ((slot == 20 || slot == 22) && event.getCursor() != null && !event.getCursor().getType().isAir()) {
                ItemStack cursor = event.getCursor();
                if (plugin.getStrainManager().isSeedItem(cursor)) {
                    event.setCancelled(true);
                    plugin.getCrossbreedManager().handleSeedDrop(player, cursor, slot);
                    return;
                }
            }
            
            // Check if clicking parent slot with a seed in hand (current item)
            if ((slot == 20 || slot == 22) && event.getClick() == ClickType.LEFT) {
                ItemStack currentItem = event.getCurrentItem();
                // Check if there's a seed in the player's cursor or if they clicked holding shift with a seed
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (currentItem == null || currentItem.getType().isAir()) {
                    // Empty slot - check if player has seed in cursor
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && plugin.getStrainManager().isSeedItem(cursor)) {
                        event.setCancelled(true);
                        plugin.getCrossbreedManager().handleSeedDrop(player, cursor, slot);
                        return;
                    }
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
