package com.budlords.quality;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Represents a grow lamp that has been placed in the world as a block.
 * Lamps can be placed several blocks above plants and affect all pots within their range.
 */
public class PlacedLamp {

    private final UUID id;
    private final Location location;
    private final StarRating starRating;
    private final UUID ownerUuid;
    private final long placedTime;

    public PlacedLamp(UUID id, Location location, StarRating starRating, UUID ownerUuid) {
        this.id = id;
        this.location = location;
        this.starRating = starRating;
        this.ownerUuid = ownerUuid;
        this.placedTime = System.currentTimeMillis();
    }
    
    public PlacedLamp(UUID id, Location location, StarRating starRating, UUID ownerUuid, long placedTime) {
        this.id = id;
        this.location = location;
        this.starRating = starRating;
        this.ownerUuid = ownerUuid;
        this.placedTime = placedTime;
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public StarRating getStarRating() {
        return starRating;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public long getPlacedTime() {
        return placedTime;
    }

    /**
     * Gets the effective range of this lamp in blocks.
     * Higher star ratings provide more range (1-6 blocks).
     */
    public int getRange() {
        return starRating.getStars() + 1; // 2-7 blocks range
    }

    /**
     * Gets the light level bonus provided by this lamp.
     */
    public int getLightLevelBonus() {
        return 8 + starRating.getStars() * 2; // 10-18 light level
    }

    /**
     * Gets the quality bonus provided to plants within range.
     */
    public double getQualityBonus() {
        return starRating.getStars() * 0.05; // 5-30% bonus
    }

    /**
     * Checks if a location is within this lamp's effect range.
     * The lamp affects all blocks below it within its horizontal range.
     * @param targetLocation The location to check
     * @return true if the location is within range
     */
    public boolean isLocationInRange(Location targetLocation) {
        if (!location.getWorld().equals(targetLocation.getWorld())) {
            return false;
        }
        
        // Target must be below or at the same level as the lamp
        if (targetLocation.getBlockY() > location.getBlockY()) {
            return false;
        }
        
        // Check horizontal distance
        double horizontalDistance = Math.sqrt(
            Math.pow(location.getBlockX() - targetLocation.getBlockX(), 2) +
            Math.pow(location.getBlockZ() - targetLocation.getBlockZ(), 2)
        );
        
        return horizontalDistance <= getRange();
    }

    /**
     * Gets the location key for this lamp (used for tracking in maps).
     */
    public String getLocationKey() {
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }
}
