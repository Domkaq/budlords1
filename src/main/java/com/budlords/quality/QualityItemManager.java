package com.budlords.quality;

import com.budlords.BudLords;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages creation and identification of quality-rated items.
 */
public class QualityItemManager {

    private final BudLords plugin;

    public QualityItemManager(BudLords plugin) {
        this.plugin = plugin;
    }

    // ====== POT ITEMS ======

    public ItemStack createPot(StarRating rating, int amount) {
        return GrowingPot.createPotItem(rating, amount);
    }

    public boolean isPotItem(ItemStack item) {
        return GrowingPot.isPotItem(item);
    }

    public StarRating getPotRating(ItemStack item) {
        return GrowingPot.getRatingFromItem(item);
    }

    // ====== LAMP ITEMS ======

    public ItemStack createLamp(StarRating rating, int amount) {
        return GrowLamp.createLampItem(rating, amount);
    }

    public boolean isLampItem(ItemStack item) {
        return GrowLamp.isLampItem(item);
    }

    public StarRating getLampRating(ItemStack item) {
        return GrowLamp.getRatingFromItem(item);
    }

    // ====== FERTILIZER ITEMS ======

    public ItemStack createFertilizer(StarRating rating, int amount) {
        return Fertilizer.createFertilizerItem(rating, amount);
    }

    public boolean isFertilizerItem(ItemStack item) {
        return Fertilizer.isFertilizerItem(item);
    }

    public StarRating getFertilizerRating(ItemStack item) {
        return Fertilizer.getRatingFromItem(item);
    }

    // ====== SCISSORS ITEMS ======

    public ItemStack createScissors(StarRating rating, int amount) {
        return HarvestScissors.createScissorsItem(rating, amount);
    }

    public boolean isScissorsItem(ItemStack item) {
        return HarvestScissors.isScissorsItem(item);
    }

    public StarRating getScissorsRating(ItemStack item) {
        return HarvestScissors.getRatingFromItem(item);
    }

    // ====== WATER CAN ITEMS ======

    public ItemStack createWateringCan(StarRating rating, int amount) {
        ItemStack can = new ItemStack(Material.BUCKET, amount);
        ItemMeta meta = can.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(rating.getColorCode() + "Watering Can " + rating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Quality: " + rating.getDisplay());
            lore.add("");
            lore.add("§7Water Efficiency: §a" + String.format("%.0f%%", (double) ((rating.getStars() * 15) + 70)));
            lore.add("§7Capacity: §e" + (rating.getStars() * 5) + " uses");
            lore.add("");
            lore.add("§7Right-click on water to fill");
            lore.add("§7Right-click on pot to water");
            lore.add("");
            lore.add("§8Rating: " + rating.getStars());
            meta.setLore(lore);
            can.setItemMeta(meta);
        }
        
        return can;
    }

    public boolean isWateringCanItem(ItemStack item) {
        if (item == null || item.getType() != Material.BUCKET) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Watering Can");
    }

    public StarRating getWateringCanRating(ItemStack item) {
        return getRatingFromItemLore(item);
    }

    // ====== UTILITY METHODS ======

    /**
     * Gets a star rating from any item's lore.
     */
    private StarRating getRatingFromItemLore(ItemStack item) {
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
     * Generates a random star rating with weighted probabilities.
     * 1★: 40%, 2★: 30%, 3★: 18%, 4★: 9%, 5★: 3%
     */
    public StarRating generateRandomRating() {
        double rand = ThreadLocalRandom.current().nextDouble();
        if (rand < 0.40) return StarRating.ONE_STAR;
        if (rand < 0.70) return StarRating.TWO_STAR;
        if (rand < 0.88) return StarRating.THREE_STAR;
        if (rand < 0.97) return StarRating.FOUR_STAR;
        return StarRating.FIVE_STAR;
    }

    /**
     * Generates a random star rating with minimum quality.
     */
    public StarRating generateRandomRating(int minStars) {
        StarRating rating = generateRandomRating();
        while (rating.getStars() < minStars) {
            rating = StarRating.fromValue(rating.getStars() + 1);
        }
        return rating;
    }

    /**
     * Gives a player a starter kit of quality items.
     */
    public void giveStarterKit(Player player) {
        player.getInventory().addItem(createPot(StarRating.ONE_STAR, 3));
        player.getInventory().addItem(createFertilizer(StarRating.ONE_STAR, 5));
        player.getInventory().addItem(createScissors(StarRating.ONE_STAR, 1));
        player.sendMessage("§aYou received a starter growing kit!");
    }

    /**
     * Identifies any quality item and returns its type.
     */
    public QualityItemType getItemType(ItemStack item) {
        if (isPotItem(item)) return QualityItemType.POT;
        if (isLampItem(item)) return QualityItemType.LAMP;
        if (isFertilizerItem(item)) return QualityItemType.FERTILIZER;
        if (isScissorsItem(item)) return QualityItemType.SCISSORS;
        if (isWateringCanItem(item)) return QualityItemType.WATERING_CAN;
        return QualityItemType.NONE;
    }

    /**
     * Gets the star rating from any quality item.
     */
    public StarRating getItemRating(ItemStack item) {
        return switch (getItemType(item)) {
            case POT -> getPotRating(item);
            case LAMP -> getLampRating(item);
            case FERTILIZER -> getFertilizerRating(item);
            case SCISSORS -> getScissorsRating(item);
            case WATERING_CAN -> getWateringCanRating(item);
            case NONE -> null;
        };
    }

    public enum QualityItemType {
        NONE,
        POT,
        LAMP,
        FERTILIZER,
        SCISSORS,
        WATERING_CAN
    }
}
