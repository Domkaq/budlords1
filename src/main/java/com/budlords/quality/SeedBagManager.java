package com.budlords.quality;

import com.budlords.BudLords;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages seed bag inventories and interactions.
 * Handles opening, closing, and validating seed bag contents.
 */
public class SeedBagManager {
    
    private final BudLords plugin;
    
    // Track open seed bag inventories: player UUID -> seed bag inventory
    private final Map<UUID, Inventory> openBags;
    
    // Track which item was used to open the bag: player UUID -> item slot
    private final Map<UUID, Integer> bagSlots;
    
    public SeedBagManager(BudLords plugin) {
        this.plugin = plugin;
        this.openBags = new HashMap<>();
        this.bagSlots = new HashMap<>();
    }
    
    /**
     * Opens a seed bag inventory for a player.
     */
    public void openSeedBag(Player player, ItemStack seedBagItem, int slot) {
        if (!SeedBag.isSeedBagItem(seedBagItem)) {
            player.sendMessage("§cInvalid seed bag!");
            return;
        }
        
        StarRating rating = SeedBag.getRatingFromItem(seedBagItem);
        if (rating == null) rating = StarRating.ONE_STAR;
        
        int capacity = SeedBag.getCapacity(rating);
        
        // Create inventory with appropriate size
        String title = rating.getColor() + "✦ Seed Bag " + rating.getDisplay();
        Inventory inv = Bukkit.createInventory(null, capacity, title);
        
        // TODO: Load saved seeds from NBT/PDC in future
        // For now, just open empty inventory
        
        openBags.put(player.getUniqueId(), inv);
        bagSlots.put(player.getUniqueId(), slot);
        
        player.openInventory(inv);
        player.sendMessage("§aOpened seed bag! §7Only seeds can be stored here.");
    }
    
    /**
     * Checks if a player has an open seed bag.
     */
    public boolean hasOpenBag(Player player) {
        return openBags.containsKey(player.getUniqueId());
    }
    
    /**
     * Gets the open seed bag inventory for a player.
     */
    public Inventory getOpenBag(Player player) {
        return openBags.get(player.getUniqueId());
    }
    
    /**
     * Closes a seed bag and saves its contents.
     */
    public void closeSeedBag(Player player) {
        Inventory inv = openBags.remove(player.getUniqueId());
        Integer slot = bagSlots.remove(player.getUniqueId());
        
        if (inv == null || slot == null) return;
        
        // Count seeds in inventory
        int seedCount = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && !item.getType().equals(Material.AIR)) {
                seedCount += item.getAmount();
            }
        }
        
        // TODO: Save seeds to NBT/PDC in future
        // For now, just notify player
        if (seedCount > 0) {
            player.sendMessage("§7Seed bag closed with §a" + seedCount + " seeds§7.");
        } else {
            player.sendMessage("§7Seed bag closed (empty).");
        }
    }
    
    /**
     * Validates that an item can be placed in a seed bag.
     * This enforces the bounds checking - only seeds allowed.
     */
    public boolean validateItem(ItemStack item) {
        if (item == null || item.getType().equals(Material.AIR)) {
            return true; // Allow removing items
        }
        return SeedBag.canStoreItem(item);
    }
    
    /**
     * Removes all items that are not seeds from an inventory.
     * Returns removed items to the player.
     */
    public void removeInvalidItems(Player player, Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().equals(Material.AIR)) {
                if (!SeedBag.canStoreItem(item)) {
                    // Remove invalid item and return to player
                    inventory.setItem(i, null);
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                    
                    // Drop items that don't fit
                    for (ItemStack dropped : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                    }
                    
                    player.sendMessage("§c✗ " + item.getType().name() + " §cis not a seed and was removed!");
                }
            }
        }
    }
    
    /**
     * Checks if an inventory is a seed bag inventory.
     */
    public boolean isSeedBagInventory(Inventory inventory) {
        String title = inventory.getTitle();
        return title != null && title.contains("Seed Bag");
    }
    
    /**
     * Cleans up on plugin disable.
     */
    public void shutdown() {
        // Close all open bags
        for (UUID playerId : openBags.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        openBags.clear();
        bagSlots.clear();
    }
}
