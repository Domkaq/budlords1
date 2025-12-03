package com.budlords.quality;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a growing pot with star quality rating.
 * Pots are used to plant seeds instead of farmland.
 */
public class GrowingPot {

    private final UUID id;
    private final StarRating starRating;
    private Location location;
    private UUID ownerUuid;
    private String plantedStrainId;
    private StarRating seedRating;
    private StarRating lampRating;
    private StarRating fertilizerRating;
    
    private double waterLevel;     // 0.0 to 1.0
    private double nutrientLevel;  // 0.0 to 1.0
    private boolean hasLamp;
    private long lastWatered;
    private long lastFertilized;

    public GrowingPot(StarRating starRating) {
        this.id = UUID.randomUUID();
        this.starRating = starRating;
        this.waterLevel = 0.5;
        this.nutrientLevel = 0.3;
        this.hasLamp = false;
        this.lastWatered = System.currentTimeMillis();
        this.lastFertilized = 0;
    }

    public GrowingPot(UUID id, StarRating starRating, Location location, UUID ownerUuid) {
        this.id = id;
        this.starRating = starRating;
        this.location = location;
        this.ownerUuid = ownerUuid;
        this.waterLevel = 0.5;
        this.nutrientLevel = 0.3;
        this.hasLamp = false;
        this.lastWatered = System.currentTimeMillis();
        this.lastFertilized = 0;
    }

    public UUID getId() {
        return id;
    }

    public StarRating getStarRating() {
        return starRating;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getPlantedStrainId() {
        return plantedStrainId;
    }

    public void setPlantedStrainId(String plantedStrainId) {
        this.plantedStrainId = plantedStrainId;
    }

    public boolean hasPlant() {
        return plantedStrainId != null;
    }

    public StarRating getSeedRating() {
        return seedRating;
    }

    public void setSeedRating(StarRating seedRating) {
        this.seedRating = seedRating;
    }

    public StarRating getLampRating() {
        return lampRating;
    }

    public void setLampRating(StarRating lampRating) {
        this.lampRating = lampRating;
        this.hasLamp = lampRating != null;
    }

    public StarRating getFertilizerRating() {
        return fertilizerRating;
    }

    public void setFertilizerRating(StarRating fertilizerRating) {
        this.fertilizerRating = fertilizerRating;
    }

    public double getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel(double waterLevel) {
        this.waterLevel = Math.max(0, Math.min(1.0, waterLevel));
    }

    public void water() {
        this.waterLevel = 1.0;
        this.lastWatered = System.currentTimeMillis();
    }

    public double getNutrientLevel() {
        return nutrientLevel;
    }

    public void setNutrientLevel(double nutrientLevel) {
        this.nutrientLevel = Math.max(0, Math.min(1.0, nutrientLevel));
    }

    public void fertilize(StarRating fertilizerQuality) {
        this.fertilizerRating = fertilizerQuality;
        this.nutrientLevel = Math.min(1.0, this.nutrientLevel + (0.2 * fertilizerQuality.getStars()));
        this.lastFertilized = System.currentTimeMillis();
    }

    public boolean hasLamp() {
        return hasLamp;
    }

    public long getLastWatered() {
        return lastWatered;
    }

    public long getLastFertilized() {
        return lastFertilized;
    }

    /**
     * Calculates the overall care quality based on water, nutrients, and lamp.
     * @return A value between 0.0 and 1.0 representing care quality
     */
    public double getCareQuality() {
        double care = 0;
        care += waterLevel * 0.4;      // 40% weight for water
        care += nutrientLevel * 0.35;  // 35% weight for nutrients
        care += (hasLamp ? 0.25 : 0);  // 25% weight for lamp
        return Math.min(1.0, care);
    }

    /**
     * Calculates the effective growth speed multiplier based on pot quality and care.
     */
    public double getGrowthSpeedMultiplier() {
        double baseMult = starRating.getGrowthSpeedMultiplier();
        double careMult = 0.7 + (getCareQuality() * 0.6); // 0.7 to 1.3 based on care
        double lampMult = hasLamp && lampRating != null ? 
                          (0.9 + (lampRating.getStars() * 0.04)) : 1.0;
        return baseMult * careMult * lampMult;
    }

    /**
     * Gets the material for the pot block.
     * Note: All pots use FLOWER_POT as the base block. The visual distinction
     * comes from the item display name and lore, not the block type.
     * Minecraft's FLOWER_POT is the only valid pot-like block available.
     */
    public Material getPotMaterial() {
        // All star ratings use FLOWER_POT as the block type
        // Visual distinction is provided through item lore and particles
        return Material.FLOWER_POT;
    }

    /**
     * Creates a pot item with the given star rating.
     */
    public static ItemStack createPotItem(StarRating rating, int amount) {
        ItemStack pot = new ItemStack(Material.FLOWER_POT, amount);
        ItemMeta meta = pot.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(rating.getColorCode() + "Growing Pot " + rating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Quality: " + rating.getDisplay());
            lore.add("");
            lore.add("§7Growth Speed: §a+" + String.format("%.0f%%", (rating.getGrowthSpeedMultiplier() - 1) * 100));
            lore.add("§7Quality Bonus: §a+" + String.format("%.0f%%", (rating.getQualityMultiplier() - 1) * 100));
            lore.add("");
            lore.add("§7Right-click to place");
            lore.add("§7Plant seeds in pots to grow!");
            lore.add("");
            lore.add("§8Rating: " + rating.getStars());
            meta.setLore(lore);
            pot.setItemMeta(meta);
        }
        
        return pot;
    }

    /**
     * Gets the star rating from a pot item.
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
     * Checks if an item is a BudLords growing pot.
     */
    public static boolean isPotItem(ItemStack item) {
        if (item == null || item.getType() != Material.FLOWER_POT) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Growing Pot");
    }

    public String getLocationString() {
        if (location == null) return null;
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }
}
