package com.budlords.npc;

import com.budlords.economy.CustomerType;
import com.budlords.npc.IndividualBuyer;
import com.budlords.npc.BuyerRegistry;
import com.budlords.packaging.PackagingManager;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Intelligent buyer matching system that pairs transactions with appropriate buyers
 * based on product characteristics, buyer preferences, and relationship history.
 * 
 * This creates a more immersive and realistic trading experience where:
 * - Quality products go to connoisseurs
 * - Bulk orders go to bulk buyers
 * - Rare strains go to collectors
 * - Regular customers get priority for their favorites
 */
public class BuyerMatcher {
    
    private final BuyerRegistry registry;
    private final PackagingManager packagingManager;
    
    public BuyerMatcher(BuyerRegistry registry, PackagingManager packagingManager) {
        this.registry = registry;
        this.packagingManager = packagingManager;
    }
    
    /**
     * Finds the best matching buyer for a transaction based on products being sold.
     * 
     * Matching algorithm:
     * 1. Analyze products (quality, rarity, quantity)
     * 2. Score each buyer based on preferences
     * 3. Weight by relationship level (loyal customers get priority)
     * 4. Return highest-scoring buyer with some randomness
     */
    public IndividualBuyer findBestMatch(List<ItemStack> items, UUID playerId) {
        if (items == null || items.isEmpty()) {
            return registry.getRandomBuyer();
        }
        
        // Analyze the transaction
        TransactionProfile profile = analyzeTransaction(items);
        
        // Get all buyers and score them
        List<BuyerMatch> matches = new ArrayList<>();
        for (IndividualBuyer buyer : registry.getAllBuyers()) {
            double score = calculateMatchScore(buyer, profile, playerId);
            matches.add(new BuyerMatch(buyer, score));
        }
        
        // Sort by score (highest first)
        matches.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Use weighted random selection from top 5 matches
        // This adds variety while still favoring good matches
        int poolSize = Math.min(5, matches.size());
        List<BuyerMatch> topMatches = matches.subList(0, poolSize);
        
        // Calculate weights (higher scores = higher chance)
        double totalWeight = topMatches.stream().mapToDouble(m -> m.score).sum();
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        
        double cumulative = 0;
        for (BuyerMatch match : topMatches) {
            cumulative += match.score;
            if (random <= cumulative) {
                return match.buyer;
            }
        }
        
        // Fallback (shouldn't reach here)
        return topMatches.get(0).buyer;
    }
    
    /**
     * Analyzes products to create a transaction profile.
     */
    private TransactionProfile analyzeTransaction(List<ItemStack> items) {
        TransactionProfile profile = new TransactionProfile();
        
        for (ItemStack item : items) {
            if (item == null || !packagingManager.isPackagedProduct(item)) continue;
            
            String strainId = packagingManager.getStrainIdFromPackage(item);
            if (strainId != null) {
                profile.strainIds.add(strainId);
            }
            
            StarRating rating = packagingManager.getStarRatingFromPackage(item);
            if (rating != null) {
                profile.totalStars += rating.getStars();
                profile.itemCount++;
            }
            
            int packageSize = packagingManager.getPackageSize(item);
            profile.totalGrams += packageSize * item.getAmount();
            
            // Track rarity (would need access to strain manager)
            // For now, assume set externally
        }
        
        if (profile.itemCount > 0) {
            profile.averageStars = (double) profile.totalStars / profile.itemCount;
        }
        
        return profile;
    }
    
    /**
     * Calculates how well a buyer matches the transaction.
     */
    private double calculateMatchScore(IndividualBuyer buyer, TransactionProfile profile, UUID playerId) {
        double score = 100.0; // Base score
        
        // Quality preference match
        if (buyer.isPrefersQuality()) {
            if (profile.averageStars >= 4.0) {
                score += 50.0; // High quality match!
            } else if (profile.averageStars < 3.0) {
                score -= 30.0; // Quality buyer won't like low quality
            }
        }
        
        // Bulk preference match
        if (buyer.isPrefersBulk()) {
            if (profile.totalGrams >= 20) {
                score += 40.0; // Good bulk order
            } else if (profile.totalGrams < 5) {
                score -= 20.0; // Bulk buyer prefers larger quantities
            }
        }
        
        // Favorite strain bonus (big incentive for repeat business)
        for (String strainId : profile.strainIds) {
            if (buyer.getFavoriteStrains().contains(strainId)) {
                score += 80.0; // Major bonus for favorites!
                break;
            }
        }
        
        // Relationship/history bonus
        int totalPurchases = buyer.getTotalPurchases();
        if (totalPurchases > 50) {
            score += 60.0; // Loyal customer
        } else if (totalPurchases > 20) {
            score += 40.0; // Regular customer
        } else if (totalPurchases > 10) {
            score += 20.0; // Known customer
        } else if (totalPurchases < 3) {
            score += 10.0; // Slight bonus for new customers (diversity)
        }
        
        // Mood bonus (happy buyers are more likely to buy again)
        switch (buyer.getCurrentMood()) {
            case "loyal" -> score += 30.0;
            case "satisfied" -> score += 15.0;
            case "missed_you" -> score += 25.0; // They want to buy!
        }
        
        // Personality-based adjustments
        CustomerType personality = buyer.getPersonality();
        switch (personality) {
            case CONNOISSEUR:
                if (profile.averageStars >= 4.5) score += 30.0;
                break;
            case BULK_BUYER:
                if (profile.totalGrams >= 30) score += 35.0;
                break;
            case COLLECTOR:
                // Collectors want variety (lower score if they already have it)
                boolean hasAllStrains = profile.strainIds.stream()
                    .allMatch(s -> buyer.getPurchaseHistory().containsKey(s));
                if (!hasAllStrains) score += 40.0;
                break;
            case VIP_CLIENT:
                if (profile.averageStars >= 5.0) score += 50.0;
                break;
            case PARTY_ANIMAL:
                if (profile.totalGrams >= 15) score += 25.0;
                break;
            case RUSH_CUSTOMER:
                // Rush customers always in a hurry, slightly higher chance
                score += 15.0;
                break;
        }
        
        // Add some randomness (±20%)
        double randomFactor = 0.8 + (ThreadLocalRandom.current().nextDouble() * 0.4);
        score *= randomFactor;
        
        return Math.max(0, score);
    }
    
    /**
     * Represents the characteristics of a transaction.
     */
    private static class TransactionProfile {
        Set<String> strainIds = new HashSet<>();
        int totalStars = 0;
        int itemCount = 0;
        double averageStars = 0;
        int totalGrams = 0;
        Strain.Rarity highestRarity = Strain.Rarity.COMMON;
    }
    
    /**
     * Buyer with match score.
     */
    private static class BuyerMatch {
        IndividualBuyer buyer;
        double score;
        
        BuyerMatch(IndividualBuyer buyer, double score) {
            this.buyer = buyer;
            this.score = score;
        }
    }
    
    /**
     * Gets recommended buyers for upcoming sales (for UI display).
     */
    public List<IndividualBuyer> getRecommendedBuyers(UUID playerId, int count) {
        List<IndividualBuyer> buyers = registry.getAllBuyers().stream()
            .sorted((a, b) -> {
                // Sort by: loyal customers first, then by last seen (recent first)
                int loyaltyCompare = Integer.compare(b.getTotalPurchases(), a.getTotalPurchases());
                if (loyaltyCompare != 0) return loyaltyCompare;
                return Long.compare(b.getLastSeenTimestamp(), a.getLastSeenTimestamp());
            })
            .limit(count)
            .collect(Collectors.toList());
        
        return buyers;
    }
}
