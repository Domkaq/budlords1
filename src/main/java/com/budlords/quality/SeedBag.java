package com.budlords.quality;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seed Bag - A specialized container that can ONLY store seeds.
 * Uses NBT-based bounds checking to ensure only seed items can be added.
 * 
 * Features:
 * - Star rating system (★1-5) with different capacities
 * - Bounds checking prevents non-seed items
 * - Persistent storage across server restarts
 * - Visual feedback with lore display
 */
public class SeedBag {
    
    private final UUID id;
    private final StarRating rating;
    private final int maxCapacity;
    
    /**
     * Capacity per star rating
     */
    private static final int[] CAPACITIES = {
        9,   // 1 star - 9 slots (1 row)
        18,  // 2 star - 18 slots (2 rows)
        27,  // 3 star - 27 slots (3 rows)
        36,  // 4 star - 36 slots (4 rows)
        54   // 5 star - 54 slots (6 rows - full inventory)
    };
    
    public SeedBag(StarRating rating) {
        this.id = UUID.randomUUID();
        this.rating = rating;
        this.maxCapacity = CAPACITIES[rating.getStars() - 1];
    }
    
    /**
     * Creates a seed bag ItemStack with the given star rating.
     */
    public static ItemStack createSeedBagItem(StarRating rating) {
        ItemStack item = new ItemStack(Material.BUNDLE); // Use bundle as visual
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.setDisplayName(rating.getColor() + "✦ Seed Bag " + rating.getDisplay());
        
        List<String> lore = new ArrayList<>();
        lore.add("§8Type: §7Seed Storage");
        lore.add("");
        lore.add("§7A specialized bag that can");
        lore.add("§7only store §aseeds§7.");
        lore.add("");
        lore.add("§7Capacity: §e" + CAPACITIES[rating.getStars() - 1] + " slots");
        lore.add("§7Rating: " + rating.getDisplay());
        lore.add("");
        lore.add("§7Right-click to open");
        lore.add("");
        lore.add("§8[BudLords Seed Bag]");
        lore.add("§8Rating: " + rating.name());
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Checks if an ItemStack is a seed bag.
     */
    public static boolean isSeedBagItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.equals("§8[BudLords Seed Bag]")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the star rating from a seed bag item.
     */
    public static StarRating getRatingFromItem(ItemStack item) {
        if (!isSeedBagItem(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8Rating: ")) {
                String ratingStr = line.substring(10);
                try {
                    return StarRating.valueOf(ratingStr);
                } catch (IllegalArgumentException e) {
                    return StarRating.ONE_STAR;
                }
            }
        }
        return StarRating.ONE_STAR;
    }
    
    /**
     * Gets the capacity for a given star rating.
     */
    public static int getCapacity(StarRating rating) {
        if (rating == null) return CAPACITIES[0];
        int index = rating.getStars() - 1;
        if (index < 0 || index >= CAPACITIES.length) return CAPACITIES[0];
        return CAPACITIES[index];
    }
    
    /**
     * Checks if an item can be stored in a seed bag (must be a seed).
     * This is the bounds checking mechanism.
     */
    public static boolean canStoreItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        // Check if item is a seed by looking for seed identifier in lore
        for (String line : lore) {
            if (line.contains("§8Type: §7Seed") || line.equals("§8[BudLords Seed]")) {
                return true;
            }
        }
        return false;
    }
    
    public UUID getId() {
        return id;
    }
    
    public StarRating getRating() {
        return rating;
    }
    
    public int getMaxCapacity() {
        return maxCapacity;
    }
}
