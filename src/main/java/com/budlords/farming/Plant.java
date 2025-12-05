package com.budlords.farming;

import com.budlords.quality.StarRating;
import org.bukkit.Location;

import java.util.UUID;

public class Plant {

    private final UUID id;
    private final String strainId;
    private final Location location;
    private final UUID ownerUuid;
    private final long plantedTime;
    
    private int growthStage; // 0 = seed, 1 = small, 2 = mid, 3 = full
    private int quality;
    private long lastGrowthUpdate;
    
    // Star quality ratings for pot-based growing
    private StarRating potRating;
    private StarRating seedRating;
    private StarRating lampRating;
    private StarRating fertilizerRating;
    private double waterLevel;
    private double nutrientLevel;
    private UUID potId;
    
    // Cooldown tracking for watering quality bonus
    private long lastWateringBonusTime;
    private static final long WATERING_BONUS_COOLDOWN_MS = 60000L; // 60 seconds cooldown for quality bonus

    public Plant(String strainId, Location location, UUID ownerUuid) {
        this.id = UUID.randomUUID();
        this.strainId = strainId;
        this.location = location;
        this.ownerUuid = ownerUuid;
        this.plantedTime = System.currentTimeMillis();
        this.growthStage = 0;
        this.quality = 50;
        this.lastGrowthUpdate = System.currentTimeMillis();
        this.waterLevel = 0.5;
        this.nutrientLevel = 0.3;
        this.lastWateringBonusTime = 0;
    }

    public Plant(UUID id, String strainId, Location location, UUID ownerUuid, 
                 long plantedTime, int growthStage, int quality, long lastGrowthUpdate) {
        this.id = id;
        this.strainId = strainId;
        this.location = location;
        this.ownerUuid = ownerUuid;
        this.plantedTime = plantedTime;
        this.growthStage = growthStage;
        this.quality = quality;
        this.lastGrowthUpdate = lastGrowthUpdate;
        this.waterLevel = 0.5;
        this.nutrientLevel = 0.3;
        this.lastWateringBonusTime = 0;
    }
    
    /**
     * Full constructor with star ratings for pot-based growing.
     */
    public Plant(UUID id, String strainId, Location location, UUID ownerUuid, 
                 long plantedTime, int growthStage, int quality, long lastGrowthUpdate,
                 StarRating potRating, StarRating seedRating, StarRating lampRating,
                 StarRating fertilizerRating, double waterLevel, double nutrientLevel, UUID potId) {
        this.id = id;
        this.strainId = strainId;
        this.location = location;
        this.ownerUuid = ownerUuid;
        this.plantedTime = plantedTime;
        this.growthStage = growthStage;
        this.quality = quality;
        this.lastGrowthUpdate = lastGrowthUpdate;
        this.potRating = potRating;
        this.seedRating = seedRating;
        this.lampRating = lampRating;
        this.fertilizerRating = fertilizerRating;
        this.waterLevel = waterLevel;
        this.nutrientLevel = nutrientLevel;
        this.potId = potId;
        this.lastWateringBonusTime = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getStrainId() {
        return strainId;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public long getPlantedTime() {
        return plantedTime;
    }

    public int getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(int growthStage) {
        this.growthStage = Math.max(0, Math.min(3, growthStage));
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = Math.max(0, Math.min(100, quality));
    }

    public void addQuality(int amount) {
        setQuality(this.quality + amount);
    }

    public long getLastGrowthUpdate() {
        return lastGrowthUpdate;
    }

    public void setLastGrowthUpdate(long lastGrowthUpdate) {
        this.lastGrowthUpdate = lastGrowthUpdate;
    }

    public boolean isFullyGrown() {
        return growthStage >= 3;
    }

    public void grow() {
        if (growthStage < 3) {
            growthStage++;
            lastGrowthUpdate = System.currentTimeMillis();
        }
    }

    public String getGrowthStageName() {
        return switch (growthStage) {
            case 0 -> "Seed";
            case 1 -> "Sprout";
            case 2 -> "Growing";
            case 3 -> "Mature";
            default -> "Unknown";
        };
    }

    public String getLocationString() {
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }

    // Star rating getters and setters
    
    public StarRating getPotRating() {
        return potRating;
    }

    public void setPotRating(StarRating potRating) {
        this.potRating = potRating;
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
    }
    
    /**
     * Waters the plant using a specific quality watering can.
     * Higher quality watering cans provide additional quality bonuses, but only
     * if the watering bonus cooldown has passed (prevents spam for free quality).
     * @param wateringCanRating The star rating of the watering can used
     * @return true if quality bonus was applied, false if still on cooldown
     */
    public boolean water(StarRating wateringCanRating) {
        this.waterLevel = 1.0;
        
        // Higher star watering cans give quality bonus when watering
        // But only if the cooldown has passed to prevent spam
        if (wateringCanRating != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWateringBonusTime >= WATERING_BONUS_COOLDOWN_MS) {
                // Add quality bonus based on watering can quality
                // 1-star: +1 quality, 5-star: +5 quality
                int qualityBonus = wateringCanRating.getStars();
                addQuality(qualityBonus);
                lastWateringBonusTime = currentTime;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the remaining cooldown time for watering quality bonus in seconds.
     * @return remaining cooldown in seconds, or 0 if no cooldown active
     */
    public long getWateringBonusCooldownRemaining() {
        long elapsed = System.currentTimeMillis() - lastWateringBonusTime;
        if (elapsed >= WATERING_BONUS_COOLDOWN_MS) {
            return 0;
        }
        return (WATERING_BONUS_COOLDOWN_MS - elapsed) / 1000;
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
    }

    public UUID getPotId() {
        return potId;
    }

    public void setPotId(UUID potId) {
        this.potId = potId;
    }

    public boolean hasPot() {
        return potId != null || potRating != null;
    }

    /**
     * Calculates the overall care quality based on water, nutrients, and lamp.
     * @return A value between 0.0 and 1.0 representing care quality
     */
    public double getCareQuality() {
        double care = 0;
        care += waterLevel * 0.4;      // 40% weight for water
        care += nutrientLevel * 0.35;  // 35% weight for nutrients
        care += (lampRating != null ? 0.25 : 0);  // 25% weight for lamp
        return Math.min(1.0, care);
    }

    /**
     * Calculates the effective growth speed multiplier based on all factors.
     */
    public double getGrowthSpeedMultiplier() {
        double baseMult = potRating != null ? potRating.getGrowthSpeedMultiplier() : 1.0;
        double careMult = 0.7 + (getCareQuality() * 0.6); // 0.7 to 1.3 based on care
        double lampMult = lampRating != null ? (0.9 + (lampRating.getStars() * 0.04)) : 1.0;
        return baseMult * careMult * lampMult;
    }

    /**
     * Calculates the final star rating for the harvested bud.
     */
    public StarRating calculateFinalBudRating(StarRating scissorsRating) {
        return StarRating.calculateBudRating(
            potRating, 
            seedRating, 
            lampRating, 
            fertilizerRating, 
            scissorsRating, 
            getCareQuality()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Plant plant = (Plant) obj;
        return id.equals(plant.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
