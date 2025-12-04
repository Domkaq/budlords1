package com.budlords.quality;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a grow lamp with star quality rating.
 * Lamps provide light to pots and improve growth quality.
 */
public class GrowLamp {

    private final StarRating starRating;
    
    public GrowLamp(StarRating starRating) {
        this.starRating = starRating;
    }

    public StarRating getStarRating() {
        return starRating;
    }

    /**
     * Gets the light level bonus provided by this lamp.
     * Higher star ratings provide more light.
     */
    public int getLightLevelBonus() {
        return 8 + starRating.getStars() * 2; // 10-18 light level
    }

    /**
     * Gets the energy consumption multiplier.
     * Higher star lamps are more efficient.
     */
    public double getEfficiencyMultiplier() {
        return switch (starRating) {
            case ONE_STAR -> 1.0;
            case TWO_STAR -> 0.9;
            case THREE_STAR -> 0.8;
            case FOUR_STAR -> 0.7;
            case FIVE_STAR -> 0.6;
            case SIX_STAR -> 0.5;
        };
    }

    /**
     * Gets the quality bonus this lamp provides to growing plants.
     */
    public double getQualityBonus() {
        return starRating.getStars() * 0.05; // 5-25% bonus
    }

    /**
     * Gets the material representing this lamp.
     */
    public Material getLampMaterial() {
        return switch (starRating) {
            case ONE_STAR -> Material.LANTERN;
            case TWO_STAR -> Material.SEA_LANTERN;
            case THREE_STAR -> Material.GLOWSTONE;
            case FOUR_STAR -> Material.SHROOMLIGHT;
            case FIVE_STAR -> Material.END_ROD;
            case SIX_STAR -> Material.BEACON;
        };
    }

    /**
     * Creates a grow lamp item with the given star rating.
     */
    public static ItemStack createLampItem(StarRating rating, int amount) {
        GrowLamp lamp = new GrowLamp(rating);
        ItemStack item = new ItemStack(lamp.getLampMaterial(), amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(rating.getColorCode() + "Grow Lamp " + rating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Quality: " + rating.getDisplay());
            lore.add("");
            lore.add("§7Light Level: §e" + lamp.getLightLevelBonus());
            lore.add("§7Quality Bonus: §a+" + String.format("%.0f%%", lamp.getQualityBonus() * 100));
            lore.add("§7Efficiency: §a" + String.format("%.0f%%", (1 - lamp.getEfficiencyMultiplier()) * 100 + 100));
            lore.add("");
            lore.add("§7Place near pots to boost growth!");
            lore.add("");
            lore.add("§8Rating: " + rating.getStars());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Gets the star rating from a lamp item.
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
     * Checks if an item is a BudLords grow lamp.
     */
    public static boolean isLampItem(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Grow Lamp");
    }
}
