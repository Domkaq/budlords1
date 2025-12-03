package com.budlords.packaging;

import org.bukkit.Location;
import org.bukkit.entity.Item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks dropped bud items for the drag-and-drop packaging system.
 * When buds are dropped, they are tracked so packs can be dropped on them.
 */
public class DroppedBudTracker {

    // Maps dropped item entity UUID to bud info
    private final Map<UUID, DroppedBudInfo> trackedBuds;
    
    // Cleanup interval - remove old entries after 60 seconds
    private static final long EXPIRY_MS = 60_000;

    public DroppedBudTracker() {
        this.trackedBuds = new ConcurrentHashMap<>();
    }

    /**
     * Tracks a dropped bud item.
     */
    public void trackBud(Item itemEntity, String strainId, int amount, 
                          com.budlords.quality.StarRating rating, UUID dropperId) {
        DroppedBudInfo info = new DroppedBudInfo(
            itemEntity.getUniqueId(),
            strainId,
            amount,
            rating,
            dropperId,
            itemEntity.getLocation(),
            System.currentTimeMillis()
        );
        trackedBuds.put(itemEntity.getUniqueId(), info);
        cleanup(); // Clean old entries
    }

    /**
     * Gets info about a tracked bud item.
     */
    public DroppedBudInfo getTrackedBud(UUID entityId) {
        DroppedBudInfo info = trackedBuds.get(entityId);
        if (info != null && isExpired(info)) {
            trackedBuds.remove(entityId);
            return null;
        }
        return info;
    }

    /**
     * Removes a tracked bud (after it's been packaged or picked up).
     */
    public void untrackBud(UUID entityId) {
        trackedBuds.remove(entityId);
    }

    /**
     * Finds nearby tracked buds at a location.
     */
    public DroppedBudInfo findNearbyBud(Location location, double radius) {
        cleanup();
        for (DroppedBudInfo info : trackedBuds.values()) {
            if (info.getLocation() != null && 
                info.getLocation().getWorld() != null &&
                location.getWorld() != null &&
                info.getLocation().getWorld().equals(location.getWorld()) &&
                info.getLocation().distanceSquared(location) <= radius * radius) {
                return info;
            }
        }
        return null;
    }

    /**
     * Cleans up expired entries.
     */
    public void cleanup() {
        trackedBuds.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private boolean isExpired(DroppedBudInfo info) {
        return System.currentTimeMillis() - info.getDroppedTime() > EXPIRY_MS;
    }

    /**
     * Information about a dropped bud item.
     */
    public static class DroppedBudInfo {
        private final UUID entityId;
        private final String strainId;
        private final int amount;
        private final com.budlords.quality.StarRating rating;
        private final UUID dropperId;
        private Location location;
        private final long droppedTime;

        public DroppedBudInfo(UUID entityId, String strainId, int amount, 
                              com.budlords.quality.StarRating rating, UUID dropperId,
                              Location location, long droppedTime) {
            this.entityId = entityId;
            this.strainId = strainId;
            this.amount = amount;
            this.rating = rating;
            this.dropperId = dropperId;
            this.location = location;
            this.droppedTime = droppedTime;
        }

        public UUID getEntityId() {
            return entityId;
        }

        public String getStrainId() {
            return strainId;
        }

        public int getAmount() {
            return amount;
        }

        public com.budlords.quality.StarRating getRating() {
            return rating;
        }

        public UUID getDropperId() {
            return dropperId;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public long getDroppedTime() {
            return droppedTime;
        }
    }
}
