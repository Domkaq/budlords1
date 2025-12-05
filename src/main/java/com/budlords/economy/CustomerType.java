package com.budlords.economy;

import org.bukkit.entity.EntityType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Customer types with different preferences and behaviors for BudLords v3.0.0.
 * Different customers pay differently for different products!
 */
public enum CustomerType {
    
    // Regular customers - balanced prices
    CASUAL_USER("Casual User", "§7", 1.0, 1.0, 
        "Just looking for something chill.",
        new String[] {"This'll do.", "Thanks man.", "Nice stuff."}),
    
    // Prefers high quality - pays more for 4-5 star
    CONNOISSEUR("Connoisseur", "§6", 0.8, 1.5,
        "I only accept the finest product.",
        new String[] {"Exquisite!", "Marvelous quality!", "This is art!"}),
    
    // Bulk buyer - wants quantity discounts
    BULK_BUYER("Bulk Buyer", "§e", 1.2, 0.9,
        "I need a lot. Discount for volume?",
        new String[] {"Good deal.", "I'll take more next time.", "Keep it coming."}),
    
    // Impatient - pays more but won't wait
    RUSH_CUSTOMER("Rush Customer", "§c", 1.3, 1.3,
        "Quick quick! I'm in a hurry!",
        new String[] {"Finally!", "That took forever!", "Gotta go!"}),
    
    // Suspicious - tests quality, may reject
    SKEPTIC("Skeptic", "§8", 0.9, 1.1,
        "Hmm... Is this legit?",
        new String[] {"I suppose it's okay.", "Better than expected.", "Acceptable."}),
    
    // Party buyer - loves fun strains
    PARTY_ANIMAL("Party Animal", "§d", 1.0, 1.2,
        "Is this gonna be fun? I need FUN!",
        new String[] {"WOOO!", "Party time!", "This is gonna be epic!"}),
    
    // Medical user - prefers calming strains
    MEDICAL_USER("Medical User", "§a", 1.1, 1.0,
        "I need this for... therapeutic purposes.",
        new String[] {"This helps a lot.", "Thank you.", "Much appreciated."}),
    
    // Mysterious stranger - random behavior
    MYSTERY_BUYER("???", "§5", 0.5, 2.0,
        "...",
        new String[] {"...", "*nods*", "Interesting."}),
    
    // Celebrity - pays premium for discretion
    VIP_CLIENT("VIP Client", "§6§l", 1.5, 1.5,
        "Keep this between us, yeah?",
        new String[] {"Excellent service.", "Very discrete.", "You'll hear from my people."}),
    
    // Collector - wants rare strains
    COLLECTOR("Collector", "§b", 0.7, 2.5,
        "I'm looking for something... special.",
        new String[] {"A fine addition!", "Rare indeed!", "Perfect for my collection!"});
    
    private final String displayName;
    private final String colorCode;
    private final double commonMultiplier;    // Multiplier for common/uncommon strains
    private final double rareMultiplier;      // Multiplier for rare/legendary strains
    private final String greeting;
    private final String[] purchaseComments;
    
    CustomerType(String displayName, String colorCode, double commonMultiplier, 
                double rareMultiplier, String greeting, String[] purchaseComments) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.commonMultiplier = commonMultiplier;
        this.rareMultiplier = rareMultiplier;
        this.greeting = greeting;
        this.purchaseComments = purchaseComments;
    }
    
    public String getDisplayName() {
        return colorCode + displayName;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    public double getCommonMultiplier() {
        return commonMultiplier;
    }
    
    public double getRareMultiplier() {
        return rareMultiplier;
    }
    
    public String getGreeting() {
        return colorCode + "\"" + greeting + "\"";
    }
    
    public String getRandomComment() {
        return colorCode + "\"" + purchaseComments[ThreadLocalRandom.current().nextInt(purchaseComments.length)] + "\"";
    }
    
    /**
     * Gets a random customer type with weighted probabilities.
     */
    public static CustomerType getRandomCustomer() {
        double roll = ThreadLocalRandom.current().nextDouble() * 100;
        
        if (roll < 25) return CASUAL_USER;      // 25%
        if (roll < 40) return BULK_BUYER;       // 15%
        if (roll < 52) return CONNOISSEUR;      // 12%
        if (roll < 62) return PARTY_ANIMAL;     // 10%
        if (roll < 72) return MEDICAL_USER;     // 10%
        if (roll < 80) return RUSH_CUSTOMER;    // 8%
        if (roll < 87) return SKEPTIC;          // 7%
        if (roll < 93) return COLLECTOR;        // 6%
        if (roll < 98) return VIP_CLIENT;       // 5%
        return MYSTERY_BUYER;                    // 2%
    }
    
    /**
     * Gets the price multiplier for a specific rarity.
     */
    public double getMultiplierForRarity(com.budlords.strain.Strain.Rarity rarity) {
        return switch (rarity) {
            case COMMON, UNCOMMON -> commonMultiplier;
            case RARE, LEGENDARY -> rareMultiplier;
        };
    }
    
    /**
     * Gets an appropriate entity type to represent this customer.
     */
    public EntityType getEntityType() {
        return switch (this) {
            case CASUAL_USER -> EntityType.VILLAGER;
            case CONNOISSEUR -> EntityType.WANDERING_TRADER;
            case BULK_BUYER -> EntityType.PILLAGER;
            case RUSH_CUSTOMER -> EntityType.VILLAGER;
            case SKEPTIC -> EntityType.WITCH;
            case PARTY_ANIMAL -> EntityType.VILLAGER;
            case MEDICAL_USER -> EntityType.VILLAGER;
            case MYSTERY_BUYER -> EntityType.ENDERMAN;
            case VIP_CLIENT -> EntityType.VILLAGER;
            case COLLECTOR -> EntityType.WANDERING_TRADER;
        };
    }
    
    /**
     * Gets rejection chance for this customer type (0.0 - 1.0).
     * Higher quality/reputation reduces this.
     */
    public double getBaseRejectionChance() {
        return switch (this) {
            case CASUAL_USER -> 0.02;
            case CONNOISSEUR -> 0.15;  // Picky about quality
            case BULK_BUYER -> 0.05;
            case RUSH_CUSTOMER -> 0.03;
            case SKEPTIC -> 0.20;      // Very suspicious
            case PARTY_ANIMAL -> 0.05;
            case MEDICAL_USER -> 0.08;
            case MYSTERY_BUYER -> 0.10;
            case VIP_CLIENT -> 0.03;
            case COLLECTOR -> 0.25;    // Only wants specific things
        };
    }
}
