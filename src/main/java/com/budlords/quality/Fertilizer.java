package com.budlords.quality;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents fertilizer with star quality rating.
 * Fertilizers boost nutrient levels and quality of growing plants.
 */
public class Fertilizer {

    private final StarRating starRating;
    
    public Fertilizer(StarRating starRating) {
        this.starRating = starRating;
    }

    public StarRating getStarRating() {
        return starRating;
    }

    /**
     * Gets the nutrient boost provided by this fertilizer.
     */
    public double getNutrientBoost() {
        return switch (starRating) {
            case ONE_STAR -> 0.15;
            case TWO_STAR -> 0.25;
            case THREE_STAR -> 0.35;
            case FOUR_STAR -> 0.50;
            case FIVE_STAR -> 0.70;
            case SIX_STAR -> 1.00;
        };
    }

    /**
     * Gets the quality bonus this fertilizer provides.
     */
    public double getQualityBonus() {
        return starRating.getStars() * 0.04; // 4-20% bonus
    }

    /**
     * Gets the duration multiplier for how long the fertilizer lasts.
     */
    public double getDurationMultiplier() {
        return 0.8 + (starRating.getStars() * 0.2); // 1.0 to 1.8
    }

    /**
     * Gets the material representing this fertilizer.
     */
    public Material getFertilizerMaterial() {
        return Material.BONE_MEAL;
    }

    /**
     * Creates a fertilizer item with the given star rating.
     */
    public static ItemStack createFertilizerItem(StarRating rating, int amount) {
        Fertilizer fert = new Fertilizer(rating);
        ItemStack item = new ItemStack(Material.BONE_MEAL, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(rating.getColorCode() + "Fertilizer " + rating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Quality: " + rating.getDisplay());
            lore.add("");
            lore.add("§7Nutrient Boost: §a+" + String.format("%.0f%%", fert.getNutrientBoost() * 100));
            lore.add("§7Quality Bonus: §a+" + String.format("%.0f%%", fert.getQualityBonus() * 100));
            lore.add("§7Duration: §a" + String.format("%.1fx", fert.getDurationMultiplier()));
            lore.add("");
            lore.add("§7Right-click on a pot to apply!");
            lore.add("");
            lore.add("§8Rating: " + rating.getStars());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Gets the star rating from a fertilizer item.
     */
    public static StarRating getRatingFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8Rating: ")) {
                try {
                    int rating = Integer.parseInt(line.substring(10).trim());
                    return StarRating.fromValue(rating);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Checks if an item is BudLords fertilizer.
     */
    public static boolean isFertilizerItem(ItemStack item) {
        if (item == null || item.getType() != Material.BONE_MEAL) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Fertilizer");
    }
}
