package com.budlords.quality;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents harvest scissors with star quality rating.
 * Better scissors improve harvest drops and quality.
 */
public class HarvestScissors {

    private final StarRating starRating;
    
    public HarvestScissors(StarRating starRating) {
        this.starRating = starRating;
    }

    public StarRating getStarRating() {
        return starRating;
    }

    /**
     * Gets the yield bonus from these scissors.
     */
    public double getYieldBonus() {
        return switch (starRating) {
            case ONE_STAR -> 0.0;
            case TWO_STAR -> 0.10;
            case THREE_STAR -> 0.20;
            case FOUR_STAR -> 0.35;
            case FIVE_STAR -> 0.50;
            case SIX_STAR -> 0.75;
        };
    }

    /**
     * Gets the quality upgrade chance.
     * Higher star scissors have a chance to upgrade the final bud quality.
     */
    public double getQualityUpgradeChance() {
        return switch (starRating) {
            case ONE_STAR -> 0.0;
            case TWO_STAR -> 0.05;
            case THREE_STAR -> 0.10;
            case FOUR_STAR -> 0.20;
            case FIVE_STAR -> 0.35;
            case SIX_STAR -> 0.50;
        };
    }

    /**
     * Gets the rare drop bonus chance.
     * Better scissors can yield bonus rare items.
     */
    public double getRareDropChance() {
        return switch (starRating) {
            case ONE_STAR -> 0.01;
            case TWO_STAR -> 0.03;
            case THREE_STAR -> 0.05;
            case FOUR_STAR -> 0.10;
            case FIVE_STAR -> 0.20;
            case SIX_STAR -> 0.35;
        };
    }

    /**
     * Gets the durability of the scissors in uses.
     */
    public int getDurability() {
        return switch (starRating) {
            case ONE_STAR -> 50;
            case TWO_STAR -> 100;
            case THREE_STAR -> 200;
            case FOUR_STAR -> 350;
            case FIVE_STAR -> 500;
            case SIX_STAR -> 1000;
        };
    }

    /**
     * Calculates the final yield based on base yield and scissors quality.
     */
    public int calculateFinalYield(int baseYield) {
        double bonus = baseYield * getYieldBonus();
        // Add random element for bonus
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            bonus += 1; // Chance for extra bud
        }
        return baseYield + (int) Math.round(bonus);
    }

    /**
     * Checks if the scissors trigger a quality upgrade.
     */
    public boolean triggersQualityUpgrade() {
        return ThreadLocalRandom.current().nextDouble() < getQualityUpgradeChance();
    }

    /**
     * Checks if the scissors trigger a rare drop.
     */
    public boolean triggersRareDrop() {
        return ThreadLocalRandom.current().nextDouble() < getRareDropChance();
    }

    /**
     * Creates a scissors item with the given star rating.
     */
    public static ItemStack createScissorsItem(StarRating rating, int amount) {
        HarvestScissors scissors = new HarvestScissors(rating);
        ItemStack item = new ItemStack(Material.SHEARS, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(rating.getColorCode() + "Harvest Scissors " + rating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Quality: " + rating.getDisplay());
            lore.add("");
            lore.add("§7Yield Bonus: §a+" + String.format("%.0f%%", scissors.getYieldBonus() * 100));
            lore.add("§7Quality Upgrade: §a" + String.format("%.0f%% chance", scissors.getQualityUpgradeChance() * 100));
            lore.add("§7Rare Drop: §a" + String.format("%.0f%% chance", scissors.getRareDropChance() * 100));
            lore.add("§7Durability: §e" + scissors.getDurability() + " uses");
            lore.add("");
            lore.add("§7Use to harvest mature plants!");
            lore.add("");
            lore.add("§8Rating: " + rating.getStars());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Gets the star rating from scissors item.
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
     * Checks if an item is BudLords harvest scissors.
     */
    public static boolean isScissorsItem(ItemStack item) {
        if (item == null || item.getType() != Material.SHEARS) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Harvest Scissors");
    }
}
