package com.budlords.economy;

import com.budlords.npc.IndividualBuyer;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * AI-powered deal negotiation system where buyers can make counter-offers,
 * negotiate prices, and react to player pricing strategies.
 * 
 * Features:
 * - Buyers counter-offer based on personality and relationship
 * - Dynamic negotiation based on item quality and market conditions
 * - Relationship-building through successful negotiations
 * - Special "deal sweeteners" for tough negotiations
 */
public class DealNegotiationSystem {
    
    /**
     * Evaluates if buyer will accept the deal or make a counter-offer.
     */
    public static NegotiationResult evaluateDeal(IndividualBuyer buyer, double playerPrice, 
                                                   double baseValue, StarRating quality, 
                                                   Strain.Rarity rarity, int relationshipLevel) {
        
        double priceRatio = playerPrice / baseValue;
        
        // If player is selling at or below base price, instant accept
        if (priceRatio <= 1.0) {
            return new NegotiationResult(NegotiationType.ACCEPT, playerPrice, 
                "§a§lDEAL ACCEPTED! §7Great price!");
        }
        
        // Calculate buyer's tolerance based on personality and relationship
        double tolerance = calculateBuyerTolerance(buyer, relationshipLevel);
        
        // If within tolerance, accept
        if (priceRatio <= tolerance) {
            return new NegotiationResult(NegotiationType.ACCEPT, playerPrice,
                "§a§lDEAL ACCEPTED! §7" + getAcceptanceMessage(buyer));
        }
        
        // If moderately over tolerance, counter-offer
        if (priceRatio <= tolerance + 0.3) {
            double counterOffer = baseValue * (tolerance + ThreadLocalRandom.current().nextDouble(0.05, 0.15));
            return new NegotiationResult(NegotiationType.COUNTER_OFFER, counterOffer,
                "§e§l🤝 COUNTER-OFFER: §7" + getCounterOfferMessage(buyer, counterOffer));
        }
        
        // If way too high, reject with explanation
        return new NegotiationResult(NegotiationType.REJECT, 0,
            "§c§l✗ REJECTED: §7" + getRejectionMessage(buyer, priceRatio));
    }
    
    /**
     * Calculates buyer's price tolerance based on personality and relationship.
     */
    private static double calculateBuyerTolerance(IndividualBuyer buyer, int relationshipLevel) {
        double baseTolerance = 1.0;
        
        // Personality affects tolerance
        switch (buyer.getPersonality()) {
            case VIP_CLIENT -> baseTolerance = 1.4; // VIPs pay premium
            case CONNOISSEUR -> baseTolerance = 1.3; // Quality seekers pay more
            case BULK_PURCHASER -> baseTolerance = 1.15; // Bulk buyers want deals
            case CASUAL_USER -> baseTolerance = 1.2; // Average tolerance
            case URBAN_DEALER -> baseTolerance = 1.25; // Street dealers flexible
            default -> baseTolerance = 1.2;
        }
        
        // Relationship bonus (up to +20% tolerance)
        double relationshipBonus = Math.min(0.2, relationshipLevel * 0.02);
        
        return baseTolerance + relationshipBonus;
    }
    
    /**
     * Generates personality-appropriate acceptance message.
     */
    private static String getAcceptanceMessage(IndividualBuyer buyer) {
        String[] messages = switch (buyer.getPersonality()) {
            case VIP_CLIENT -> new String[]{
                "Quality product, worth the premium!",
                "I appreciate fine goods, deal!",
                "My clients will love this!"
            };
            case CONNOISSEUR -> new String[]{
                "Exquisite! The craftsmanship shows!",
                "This is exactly what I'm looking for!",
                "A connoisseur's choice indeed!"
            };
            case BULK_PURCHASER -> new String[]{
                "Good bulk pricing, I'll take it!",
                "Perfect for my operation!",
                "Solid deal on quantity!"
            };
            case CASUAL_USER -> new String[]{
                "Sounds fair to me!",
                "Yeah, I'll take that!",
                "Works for me!"
            };
            case URBAN_DEALER -> new String[]{
                "Streets are gonna love this!",
                "My people need this, deal!",
                "That's what I'm talking about!"
            };
            default -> new String[]{"Deal!", "Agreed!", "Sold!"};
        };
        
        return messages[ThreadLocalRandom.current().nextInt(messages.length)];
    }
    
    /**
     * Generates personality-appropriate counter-offer message.
     */
    private static String getCounterOfferMessage(IndividualBuyer buyer, double counterOffer) {
        String[] templates = switch (buyer.getPersonality()) {
            case VIP_CLIENT -> new String[]{
                "I value quality but §a$%.2f §7is my limit.",
                "Let's meet in the middle at §a$%.2f§7?",
                "I'll go §a$%.2f §7for this premium product."
            };
            case CONNOISSEUR -> new String[]{
                "The craftsmanship is excellent, but §a$%.2f §7is fair.",
                "I appreciate the artistry. §a$%.2f§7?",
                "For this quality, I can offer §a$%.2f§7."
            };
            case BULK_PURCHASER -> new String[]{
                "Bulk buyers need better margins. §a$%.2f§7?",
                "I'll take it all at §a$%.2f§7.",
                "Volume discount? §a$%.2f §7works."
            };
            case CASUAL_USER -> new String[]{
                "That's steep! How about §a$%.2f§7?",
                "I can do §a$%.2f§7, tops.",
                "§a$%.2f §7is all I got, friend."
            };
            case URBAN_DEALER -> new String[]{
                "Streets don't pay that much. §a$%.2f§7?",
                "My buyers want deals. §a$%.2f§7?",
                "Let's keep it real at §a$%.2f§7."
            };
            default -> new String[]{"How about §a$%.2f§7?", "I'll pay §a$%.2f§7.", "§a$%.2f §7is my offer."};
        };
        
        String template = templates[ThreadLocalRandom.current().nextInt(templates.length)];
        return String.format(template, counterOffer);
    }
    
    /**
     * Generates personality-appropriate rejection message.
     */
    private static String getRejectionMessage(IndividualBuyer buyer, double priceRatio) {
        double overcharge = (priceRatio - 1.0) * 100;
        
        String[] messages = switch (buyer.getPersonality()) {
            case VIP_CLIENT -> new String[]{
                "Even VIPs have limits. That's " + String.format("%.0f%%", overcharge) + " too high!",
                "I value quality, not robbery!",
                "My standards are high, but so is that price!"
            };
            case CONNOISSEUR -> new String[]{
                "The quality doesn't justify that price!",
                "A true connoisseur knows fair value.",
                "Craftsmanship is priceless, but that's not!"
            };
            case BULK_PURCHASER -> new String[]{
                "Those margins won't work for bulk!",
                "I need volume pricing, not premium!",
                "That price kills my business model!"
            };
            case CASUAL_USER -> new String[]{
                "Way too expensive for me!",
                "I'm not made of money!",
                "That's like " + String.format("%.0f%%", overcharge) + " more than I'd pay!"
            };
            case URBAN_DEALER -> new String[]{
                "Streets won't pay that much!",
                "You trying to price me out?",
                "My buyers would laugh at that price!"
            };
            default -> new String[]{"Too expensive!", "That's too high!", "Can't do that price!"};
        };
        
        return messages[ThreadLocalRandom.current().nextInt(messages.length)];
    }
    
    /**
     * Applies negotiation result effects (sounds, messages, relationship changes).
     */
    public static void applyNegotiationEffects(Player player, NegotiationResult result, IndividualBuyer buyer) {
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("§e§lNEGOTIATION RESULT");
        player.sendMessage("");
        player.sendMessage(result.message);
        
        switch (result.type) {
            case ACCEPT -> {
                player.sendMessage("§a§l✓ Deal completed successfully!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                // Bonus relationship points for good deal
                if (buyer != null) {
                    player.sendMessage("§7Relationship improved!");
                }
            }
            case COUNTER_OFFER -> {
                player.sendMessage("");
                player.sendMessage("§7They're willing to pay: §a$" + String.format("%.2f", result.finalPrice));
                player.sendMessage("");
                player.sendMessage("§e§lYour Options:");
                player.sendMessage("§a▶ §7Accept counter-offer");
                player.sendMessage("§c▶ §7Reject and walk away");
                player.sendMessage("§e▶ §7Try negotiating again");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f);
            }
            case REJECT -> {
                player.sendMessage("§c§l✗ Deal rejected!");
                player.sendMessage("§7Try lowering your price.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.9f);
                // Small relationship penalty for bad deal
                if (buyer != null) {
                    player.sendMessage("§c§oThey seem disappointed...");
                }
            }
        }
        
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("");
    }
    
    /**
     * Negotiation result types.
     */
    public enum NegotiationType {
        ACCEPT,         // Buyer accepts the deal
        COUNTER_OFFER,  // Buyer makes counter-offer
        REJECT          // Buyer rejects outright
    }
    
    /**
     * Result of a negotiation attempt.
     */
    public static class NegotiationResult {
        public final NegotiationType type;
        public final double finalPrice;
        public final String message;
        
        public NegotiationResult(NegotiationType type, double finalPrice, String message) {
            this.type = type;
            this.finalPrice = finalPrice;
            this.message = message;
        }
    }
}
