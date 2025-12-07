package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.quality.SeedBag;
import com.budlords.quality.SeedBagManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all seed bag interactions.
 * - Opening seed bags (right-click)
 * - Validating items placed in seed bags (bounds checking)
 * - Closing and saving seed bags
 */
public class SeedBagListener implements Listener {
    
    private final BudLords plugin;
    private final SeedBagManager seedBagManager;
    
    public SeedBagListener(BudLords plugin, SeedBagManager seedBagManager) {
        this.plugin = plugin;
        this.seedBagManager = seedBagManager;
    }
    
    /**
     * Handle right-clicking with a seed bag to open it.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSeedBagUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!SeedBag.isSeedBagItem(item)) return;
        
        // Don't open if clicking on a block that would open something else
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            switch (event.getClickedBlock().getType()) {
                case CHEST, BARREL, FURNACE, CRAFTING_TABLE, ENCHANTING_TABLE, 
                     BREWING_STAND, ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> {
                    return; // Let normal block interaction happen
                }
            }
        }
        
        event.setCancelled(true);
        
        // Get the slot of the item
        int slot = player.getInventory().getHeldItemSlot();
        
        seedBagManager.openSeedBag(player, item, slot);
    }
    
    /**
     * Validate items being placed into seed bag inventories.
     * This is the core bounds checking - only seeds are allowed.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Inventory topInv = event.getView().getTopInventory();
        
        // Check if the top inventory is a seed bag
        if (!seedBagManager.isSeedBagInventory(topInv)) return;
        
        // Get the clicked inventory
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;
        
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        
        // If clicking in the seed bag inventory
        if (clickedInv.equals(topInv)) {
            // Placing an item
            if (cursor != null && !cursor.getType().isAir()) {
                if (!seedBagManager.validateItem(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage("§c✗ Only seeds can be placed in seed bags!");
                    return;
                }
            }
            
            // Shift-clicking from player inventory to seed bag
            if (event.isShiftClick() && current != null && !current.getType().isAir()) {
                // This is handled by the shift-click from bottom inventory case
                return;
            }
        }
        
        // If shift-clicking from player inventory into seed bag
        if (event.isShiftClick() && !clickedInv.equals(topInv)) {
            if (current != null && !current.getType().isAir()) {
                if (!seedBagManager.validateItem(current)) {
                    event.setCancelled(true);
                    player.sendMessage("§c✗ Only seeds can be placed in seed bags!");
                    return;
                }
            }
        }
    }
    
    /**
     * Validate items being dragged into seed bag inventories.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Inventory topInv = event.getView().getTopInventory();
        
        // Check if the top inventory is a seed bag
        if (!seedBagManager.isSeedBagInventory(topInv)) return;
        
        // Check if any dragged slot is in the seed bag
        boolean draggingIntoSeedBag = false;
        int topSize = topInv.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                draggingIntoSeedBag = true;
                break;
            }
        }
        
        if (draggingIntoSeedBag) {
            ItemStack item = event.getOldCursor();
            if (!seedBagManager.validateItem(item)) {
                event.setCancelled(true);
                player.sendMessage("§c✗ Only seeds can be placed in seed bags!");
            }
        }
    }
    
    /**
     * Handle closing seed bag inventories.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        Inventory inv = event.getInventory();
        
        if (!seedBagManager.isSeedBagInventory(inv)) return;
        
        // Validate all items and remove any non-seeds
        seedBagManager.removeInvalidItems(player, inv);
        
        // Close and save the bag
        seedBagManager.closeSeedBag(player);
    }
}
