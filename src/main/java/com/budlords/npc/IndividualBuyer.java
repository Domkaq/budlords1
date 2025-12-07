package com.budlords.npc;

import com.budlords.economy.CustomerType;
import com.budlords.strain.Strain;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents an individual buyer NPC with unique personality, preferences, and history.
 * Each buyer has their own name, favorite products, and tracks purchases over time.
 * 
 * This creates a more immersive and realistic trading experience where players
 * build relationships with specific buyers.
 */
public class IndividualBuyer {
    
    private final UUID id;
    private final String name;
    private final CustomerType personality;
    private final String backstory;
    private final List<String> favoriteStrains;
    private final Strain.Rarity favoriteRarity;
    private final Map<String, Integer> purchaseHistory; // strainId -> amount purchased
    private final Map<String, Long> lastPurchaseTime; // strainId -> timestamp
    private int totalPurchases;
    private double totalMoneySpent;
    private long firstMetTimestamp;
    private long lastSeenTimestamp;
    private String currentMood;
    private final List<String> memoryDialogue; // Contextual dialogue based on history
    
    // Special preferences
    private final boolean prefersQuality; // Prefers high star ratings
    private final boolean prefersBulk; // Prefers larger packages
    private final double loyaltyBonus; // Additional price multiplier for repeat business
    private final String specialRequest; // Unique request or comment
    
    /**
     * Creates a new individual buyer with generated personality.
     */
    public IndividualBuyer(UUID id, String name, CustomerType personality) {
        this.id = id;
        this.name = name;
        this.personality = personality;
        this.backstory = generateBackstory(personality);
        this.favoriteStrains = new ArrayList<>();
        this.favoriteRarity = generateFavoriteRarity(personality);
        this.purchaseHistory = new HashMap<>();
        this.lastPurchaseTime = new HashMap<>();
        this.totalPurchases = 0;
        this.totalMoneySpent = 0.0;
        this.firstMetTimestamp = System.currentTimeMillis();
        this.lastSeenTimestamp = System.currentTimeMillis();
        this.currentMood = "neutral";
        this.memoryDialogue = new ArrayList<>();
        this.prefersQuality = generateQualityPreference(personality);
        this.prefersBulk = generateBulkPreference(personality);
        this.loyaltyBonus = 1.0 + (ThreadLocalRandom.current().nextDouble() * 0.15); // 1.0 - 1.15x
        this.specialRequest = generateSpecialRequest(personality);
    }
    
    /**
     * Generates a contextual backstory based on personality type.
     */
    private String generateBackstory(CustomerType type) {
        return switch (type) {
            case CONNOISSEUR -> "A refined individual who appreciates the finer things in life. Has traveled the world sampling exotic varieties.";
            case BULK_BUYER -> "Runs a distribution network and needs consistent supply. Always looking for good deals on volume.";
            case RUSH_CUSTOMER -> "Works a high-stress job and needs quick transactions. Time is money for this client.";
            case SKEPTIC -> "Been burned before by bad product. Takes time to trust but becomes loyal once convinced.";
            case PARTY_ANIMAL -> "Life of every party. Looking for the most fun and exciting experiences.";
            case MEDICAL_USER -> "Uses for therapeutic purposes. Values consistency and quality over everything else.";
            case MYSTERY_BUYER -> "Not much is known about this enigmatic figure. They come and go like shadows.";
            case VIP_CLIENT -> "High-profile individual who values discretion above all. Willing to pay premium for privacy.";
            case COLLECTOR -> "Obsessed with finding rare and unique varieties. Has an extensive personal collection.";
            default -> "Regular customer looking for quality product at fair prices.";
        };
    }
    
    /**
     * Determines which rarity this buyer gravitates toward.
     */
    private Strain.Rarity generateFavoriteRarity(CustomerType type) {
        return switch (type) {
            case CONNOISSEUR, VIP_CLIENT, COLLECTOR -> Strain.Rarity.LEGENDARY;
            case PARTY_ANIMAL, MEDICAL_USER -> Strain.Rarity.RARE;
            case BULK_BUYER, RUSH_CUSTOMER -> Strain.Rarity.COMMON;
            case SKEPTIC -> Strain.Rarity.UNCOMMON;
            case MYSTERY_BUYER -> Strain.Rarity.values()[ThreadLocalRandom.current().nextInt(Strain.Rarity.values().length)];
            default -> Strain.Rarity.UNCOMMON;
        };
    }
    
    /**
     * Determines if buyer prefers high quality (4-5 stars).
     */
    private boolean generateQualityPreference(CustomerType type) {
        return type == CustomerType.CONNOISSEUR || 
               type == CustomerType.VIP_CLIENT || 
               type == CustomerType.MEDICAL_USER ||
               type == CustomerType.COLLECTOR;
    }
    
    /**
     * Determines if buyer prefers larger packages.
     */
    private boolean generateBulkPreference(CustomerType type) {
        return type == CustomerType.BULK_BUYER || 
               type == CustomerType.PARTY_ANIMAL;
    }
    
    /**
     * Generates a unique special request for this buyer.
     */
    private String generateSpecialRequest(CustomerType type) {
        String[] requests = switch (type) {
            case CONNOISSEUR -> new String[]{
                "Can you source something from the Northern regions?",
                "I'm looking for strains with complex terpene profiles.",
                "Do you have anything with a unique aroma?"
            };
            case BULK_BUYER -> new String[]{
                "I need consistent quality for my clients.",
                "Can you handle a standing weekly order?",
                "What kind of bulk discounts can you offer?"
            };
            case COLLECTOR -> new String[]{
                "I'm missing a few strains from my collection...",
                "Do you know of any rare varieties?",
                "I'm particularly interested in legendary strains."
            };
            case VIP_CLIENT -> new String[]{
                "Absolute discretion is required.",
                "I need delivery to a private location.",
                "Money is no object, quality is everything."
            };
            case PARTY_ANIMAL -> new String[]{
                "Got anything that'll make my party unforgettable?",
                "Need enough for the whole crew!",
                "What's your most fun strain?"
            };
            default -> new String[]{
                "Just looking for consistent quality.",
                "Keep me in mind for your best stuff.",
                "Always happy to see what's new."
            };
        };
        return requests[ThreadLocalRandom.current().nextInt(requests.length)];
    }
    
    /**
     * Records a purchase transaction.
     */
    public void recordPurchase(String strainId, int amount, double price) {
        purchaseHistory.put(strainId, purchaseHistory.getOrDefault(strainId, 0) + amount);
        lastPurchaseTime.put(strainId, System.currentTimeMillis());
        totalPurchases++;
        totalMoneySpent += price;
        lastSeenTimestamp = System.currentTimeMillis();
        
        // Update favorites based on purchase history
        updateFavorites();
        
        // Update mood based on transaction
        updateMood();
        
        // Add contextual dialogue
        addMemoryDialogue(strainId, amount);
    }
    
    /**
     * Updates favorite strains based on purchase history.
     */
    private void updateFavorites() {
        // Sort strains by purchase count
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(purchaseHistory.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Keep top 3 as favorites
        favoriteStrains.clear();
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            favoriteStrains.add(sorted.get(i).getKey());
        }
    }
    
    /**
     * Updates mood based on purchase history and recency.
     */
    private void updateMood() {
        long timeSinceLastPurchase = System.currentTimeMillis() - lastSeenTimestamp;
        long daysSince = timeSinceLastPurchase / (1000 * 60 * 60 * 24);
        
        if (totalPurchases > 50) {
            currentMood = "loyal";
        } else if (totalPurchases > 20) {
            currentMood = "satisfied";
        } else if (daysSince > 7) {
            currentMood = "missed_you";
        } else if (totalPurchases < 3) {
            currentMood = "new";
        } else {
            currentMood = "neutral";
        }
    }
    
    /**
     * Adds contextual dialogue based on purchases.
     */
    private void addMemoryDialogue(String strainId, int amount) {
        if (purchaseHistory.getOrDefault(strainId, 0) > 5) {
            memoryDialogue.add("You always bring me that " + strainId + ". I love it!");
        }
        if (totalPurchases == 10) {
            memoryDialogue.add("We've done business 10 times now. You're reliable.");
        }
        if (totalPurchases == 50) {
            memoryDialogue.add("50 transactions! You're my go-to dealer.");
        }
        
        // Keep only last 5 dialogues
        while (memoryDialogue.size() > 5) {
            memoryDialogue.remove(0);
        }
    }
    
    /**
     * Gets a greeting based on current mood and history.
     */
    public String getGreeting() {
        return switch (currentMood) {
            case "loyal" -> "§a" + name + ": \"Ah, my favorite dealer! What do you have today?\"";
            case "satisfied" -> "§e" + name + ": \"Good to see you again. Let's do business.\"";
            case "missed_you" -> "§6" + name + ": \"Long time no see! I was starting to worry.\"";
            case "new" -> "§7" + name + ": \"" + personality.getGreeting() + "\"";
            default -> "§f" + name + ": \"Hey. What've you got?\"";
        };
    }
    
    /**
     * Gets purchase comment based on what was bought.
     */
    public String getPurchaseComment(String strainId) {
        if (favoriteStrains.contains(strainId)) {
            return "§a\"My favorite! Thanks!\"";
        }
        return personality.getRandomComment();
    }
    
    /**
     * Gets the visual head icon for this buyer based on personality.
     */
    public Material getHeadMaterial() {
        return switch (personality) {
            case CONNOISSEUR -> Material.GOLDEN_HELMET;
            case BULK_BUYER -> Material.CHEST;
            case VIP_CLIENT -> Material.DIAMOND;
            case COLLECTOR -> Material.BOOK;
            case PARTY_ANIMAL -> Material.JUKEBOX;
            case MEDICAL_USER -> Material.POTION;
            case MYSTERY_BUYER -> Material.ENDER_PEARL;
            case RUSH_CUSTOMER -> Material.CLOCK;
            case SKEPTIC -> Material.SPYGLASS;
            default -> Material.PLAYER_HEAD;
        };
    }
    
    /**
     * Calculates price multiplier based on relationship and preferences.
     */
    public double calculatePriceMultiplier(String strainId, Strain.Rarity rarity, int starRating, int packageSize) {
        double multiplier = 1.0;
        
        // Loyalty bonus
        multiplier *= loyaltyBonus;
        
        // Favorite strain bonus
        if (favoriteStrains.contains(strainId)) {
            multiplier *= 1.15;
        }
        
        // Rarity preference
        if (rarity == favoriteRarity) {
            multiplier *= 1.10;
        }
        
        // Quality preference
        if (prefersQuality && starRating >= 4) {
            multiplier *= 1.20;
        }
        
        // Bulk preference
        if (prefersBulk && packageSize >= 10) {
            multiplier *= 1.15;
        }
        
        // Mood modifier
        if ("loyal".equals(currentMood)) {
            multiplier *= 1.10;
        }
        
        return multiplier;
    }
    
    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public CustomerType getPersonality() { return personality; }
    public String getBackstory() { return backstory; }
    public List<String> getFavoriteStrains() { return new ArrayList<>(favoriteStrains); }
    public Strain.Rarity getFavoriteRarity() { return favoriteRarity; }
    public Map<String, Integer> getPurchaseHistory() { return new HashMap<>(purchaseHistory); }
    public int getTotalPurchases() { return totalPurchases; }
    public double getTotalMoneySpent() { return totalMoneySpent; }
    public long getFirstMetTimestamp() { return firstMetTimestamp; }
    public long getLastSeenTimestamp() { return lastSeenTimestamp; }
    public String getCurrentMood() { return currentMood; }
    public List<String> getMemoryDialogue() { return new ArrayList<>(memoryDialogue); }
    public boolean isPrefersQuality() { return prefersQuality; }
    public boolean isPrefersBulk() { return prefersBulk; }
    public double getLoyaltyBonus() { return loyaltyBonus; }
    public String getSpecialRequest() { return specialRequest; }
    
    /**
     * Gets a summary of relationship status.
     */
    public String getRelationshipSummary() {
        String level;
        if (totalPurchases >= 50) {
            level = "§6§l★ BEST CUSTOMER ★";
        } else if (totalPurchases >= 20) {
            level = "§d§lLOYAL CLIENT";
        } else if (totalPurchases >= 10) {
            level = "§aREGULAR";
        } else if (totalPurchases >= 5) {
            level = "§eKNOWN";
        } else {
            level = "§7NEW";
        }
        return level;
    }
}
