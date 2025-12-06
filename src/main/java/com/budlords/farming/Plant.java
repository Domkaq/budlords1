package com.budlords.farming;

import com.budlords.strain.Strain;
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

    public Plant(String strainId, Location location, UUID ownerUuid) {
        this.id = UUID.randomUUID();
        this.strainId = strainId;
        this.location = location;
        this.ownerUuid = ownerUuid;
        this.plantedTime = System.currentTimeMillis();
        this.growthStage = 0;
        this.quality = 50;
        this.lastGrowthUpdate = System.currentTimeMillis();
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
