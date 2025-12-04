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

    /**
     * Creates a watering can with the given star rating.
     * The can starts empty and must be filled at a water source.
     */
    public ItemStack createWateringCan(StarRating rating, int amount) {
        return createWateringCan(rating, amount, 0); // Start empty
    }
    
    /**
     * Creates a watering can with the given star rating and water level.
     * @param rating Star rating of the can
     * @param amount Number of cans to create
     * @param waterLevel Current water level (0 to max capacity)
     */
    public ItemStack createWateringCan(StarRating rating, int amount, int waterLevel) {
        ItemStack can = new ItemStack(Material.BUCKET, amount);
        ItemMeta meta = can.getItemMeta();
        
        int maxCapacity = rating.getStars() * 5;
        int currentWater = Math.min(waterLevel, maxCapacity);
        
        if (meta != null) {
            String statusColor = currentWater > 0 ? "§b" : "§7";
            meta.setDisplayName(statusColor + "Watering Can " + rating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Quality: " + rating.getDisplay());
            lore.add("");
            lore.add("§7Water: " + createWaterBar(currentWater, maxCapacity));
            lore.add("§7Capacity: §e" + currentWater + "/" + maxCapacity);
            lore.add("§7Efficiency: §a" + String.format("%.0f%%", (double) ((rating.getStars() * 15) + 70)));
            lore.add("");
            if (currentWater == 0) {
                lore.add("§cEmpty! §7Right-click water to fill");
            } else {
                lore.add("§7Right-click on pot to water");
            }
            lore.add("");
            lore.add("§8Rating: " + rating.getStars());
            lore.add("§8Water: " + currentWater);
            meta.setLore(lore);
            can.setItemMeta(meta);
        }
        
        return can;
    }
    
    /**
     * Creates a visual water level bar.
     */
    private String createWaterBar(int current, int max) {
        StringBuilder bar = new StringBuilder("§8[");
        int filled = max > 0 ? (current * 10 / max) : 0;
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§b█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }
    
    /**
     * Gets the current water level of a watering can.
     */
    public int getWateringCanWater(ItemStack item) {
        if (!isWateringCanItem(item)) return 0;
        if (!item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return 0;
        
        List<String> lore = meta.getLore();
        if (lore == null) return 0;
        
        for (String line : lore) {
            if (line.startsWith("§8Water: ")) {
                try {
                    return Integer.parseInt(line.substring(9).trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
    
    /**
     * Gets the maximum capacity of a watering can based on its rating.
     */
    public int getWateringCanMaxCapacity(ItemStack item) {
        StarRating rating = getWateringCanRating(item);
        return rating != null ? rating.getStars() * 5 : 5;
    }
    
    /**
     * Updates the water level of a watering can and returns the updated item.
     */
    public ItemStack setWateringCanWater(ItemStack item, int newWaterLevel) {
        StarRating rating = getWateringCanRating(item);
        if (rating == null) rating = StarRating.ONE_STAR;
        
        return createWateringCan(rating, 1, newWaterLevel);
    }
    
    /**
     * Fills a watering can to maximum capacity.
     */
    public ItemStack fillWateringCan(ItemStack item) {
        StarRating rating = getWateringCanRating(item);
        if (rating == null) rating = StarRating.ONE_STAR;
        
        int maxCapacity = rating.getStars() * 5;
        return createWateringCan(rating, 1, maxCapacity);
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
