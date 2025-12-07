package com.budlords.npc;

import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;

import java.util.UUID;

/**
 * Represents a specific purchase request from an individual buyer.
 * Buyers can request specific strains, qualities, or quantities.
 * Fulfilling requests provides bonus rewards and strengthens relationships.
 * 
 * This creates a quest-like system within the trading mechanics:
 * - Buyers post requests for products they want
 * - Players can view active requests
 * - Fulfilling requests gives extra money and reputation
 * - Failed/expired requests may hurt relationships
 */
public class BuyerRequest {
    
    private final UUID requestId;
    private final UUID buyerId;
    private final String buyerName;
    private final RequestType type;
    private final String strainId;
    private final Strain.Rarity requestedRarity;
    private final StarRating minimumQuality;
    private final int quantity;
    private final double bonusPayment;
    private final long expirationTime;
    private final String requestMessage;
    private boolean fulfilled;
    private boolean expired;
    
    public enum RequestType {
        SPECIFIC_STRAIN,    // "I need 10g of Purple Haze"
        ANY_HIGH_QUALITY,   // "I need any 5★ product"
        BULK_ORDER,         // "I need 50g of anything"
        RARE_STRAIN,        // "I need any Legendary strain"
        COLLECTOR_QUEST     // "I need one of each rarity"
    }
    
    public BuyerRequest(UUID buyerId, String buyerName, RequestType type, 
                       String strainId, Strain.Rarity rarity, StarRating quality,
                       int quantity, double bonusPayment, long durationHours) {
        this.requestId = UUID.randomUUID();
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.type = type;
        this.strainId = strainId;
        this.requestedRarity = rarity;
        this.minimumQuality = quality;
        this.quantity = quantity;
        this.bonusPayment = bonusPayment;
        this.expirationTime = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000);
        this.requestMessage = generateRequestMessage();
        this.fulfilled = false;
        this.expired = false;
    }
    
    private String generateRequestMessage() {
        return switch (type) {
            case SPECIFIC_STRAIN -> "§eI'm looking for §a" + quantity + "g §eof §a" + strainId + "§e. Can you help?";
            case ANY_HIGH_QUALITY -> "§eI need §a" + quantity + "g §eof §6" + minimumQuality.getDisplay() + " §equality product.";
            case BULK_ORDER -> "§eI need a bulk order: §a" + quantity + "g §eof anything you have.";
            case RARE_STRAIN -> "§eI'm searching for §6" + requestedRarity + " §equality strains. §a" + quantity + "g §eneeded.";
            case COLLECTOR_QUEST -> "§eI need one of each rarity tier for my collection!";
        };
    }
    
    /**
     * Checks if the given item matches this request.
     */
    public boolean matches(String itemStrainId, Strain.Rarity itemRarity, 
                          StarRating itemQuality, int itemQuantity) {
        if (fulfilled || expired) return false;
        
        return switch (type) {
            case SPECIFIC_STRAIN -> strainId.equals(itemStrainId) && itemQuantity >= quantity;
            case ANY_HIGH_QUALITY -> itemQuality.getStars() >= minimumQuality.getStars() && itemQuantity >= quantity;
            case BULK_ORDER -> itemQuantity >= quantity;
            case RARE_STRAIN -> itemRarity == requestedRarity && itemQuantity >= quantity;
            case COLLECTOR_QUEST -> true; // Special handling needed
        };
    }
    
    /**
     * Marks this request as fulfilled.
     */
    public void fulfill() {
        this.fulfilled = true;
    }
    
    /**
     * Checks if this request has expired.
     */
    public boolean checkExpiration() {
        if (!fulfilled && System.currentTimeMillis() > expirationTime) {
            this.expired = true;
            return true;
        }
        return false;
    }
    
    /**
     * Gets the time remaining in hours.
     */
    public long getHoursRemaining() {
        long remaining = expirationTime - System.currentTimeMillis();
        return Math.max(0, remaining / (60 * 60 * 1000));
    }
    
    /**
     * Gets the urgency level based on time remaining.
     */
    public String getUrgencyDisplay() {
        long hours = getHoursRemaining();
        if (hours <= 1) {
            return "§c§l🔥 URGENT!";
        } else if (hours <= 6) {
            return "§6⚠ High Priority";
        } else if (hours <= 24) {
            return "§e⏰ Time Sensitive";
        } else {
            return "§a✓ Available";
        }
    }
    
    // Getters
    public UUID getRequestId() { return requestId; }
    public UUID getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public RequestType getType() { return type; }
    public String getStrainId() { return strainId; }
    public Strain.Rarity getRequestedRarity() { return requestedRarity; }
    public StarRating getMinimumQuality() { return minimumQuality; }
    public int getQuantity() { return quantity; }
    public double getBonusPayment() { return bonusPayment; }
    public long getExpirationTime() { return expirationTime; }
    public String getRequestMessage() { return requestMessage; }
    public boolean isFulfilled() { return fulfilled; }
    public boolean isExpired() { return expired; }
    
    /**
     * Gets the display name for GUI.
     */
    public String getDisplayName() {
        String typeDisplay = switch (type) {
            case SPECIFIC_STRAIN -> "§aSpecific Order";
            case ANY_HIGH_QUALITY -> "§6Quality Request";
            case BULK_ORDER -> "§eBulk Order";
            case RARE_STRAIN -> "§9Rare Collection";
            case COLLECTOR_QUEST -> "§5Collector Quest";
        };
        return typeDisplay + " §7- " + buyerName;
    }
}
