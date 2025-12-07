package com.budlords.npc;

import com.budlords.BudLords;
import com.budlords.economy.CustomerType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Network effect system where satisfied buyers can refer new buyers.
 * Creates organic growth and rewards for building strong relationships.
 * 
 * Features:
 * - Happy buyers refer friends
 * - Referral bonuses for both parties
 * - Network expansion based on reputation
 * - Special "influencer" buyers with large networks
 */
public class BuyerNetworkEffect {
    
    private final BudLords plugin;
    private final BuyerRegistry registry;
    
    // Referral tracking: new buyer ID -> referrer buyer ID
    private final Map<UUID, UUID> referralLinks;
    
    // Referral bonuses earned: player ID -> total bonus
    private final Map<UUID, Double> referralBonuses;
    
    public BuyerNetworkEffect(BudLords plugin, BuyerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.referralLinks = new HashMap<>();
        this.referralBonuses = new HashMap<>();
    }
    
    /**
     * Checks if a buyer should refer a new buyer based on satisfaction.
     */
    public void checkForReferral(IndividualBuyer buyer, UUID playerId) {
        // Only satisfied/loyal buyers refer others
        if (!buyer.getCurrentMood().equals("loyal") && !buyer.getCurrentMood().equals("satisfied")) {
            return;
        }
        
        // Must have made at least 5 purchases
        if (buyer.getTotalPurchases() < 5) {
            return;
        }
        
        // 10% chance per transaction when conditions are met
        if (ThreadLocalRandom.current().nextDouble() > 0.10) {
            return;
        }
        
        // Generate a new buyer as a referral
        IndividualBuyer newBuyer = generateReferredBuyer(buyer);
        registry.addBuyer(newBuyer);
        referralLinks.put(newBuyer.getId(), buyer.getId());
        
        // Notify player
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage("");
            player.sendMessage("§6§l🌟 NETWORK EXPANSION!");
            player.sendMessage("§e" + buyer.getName() + " §7referred §a" + newBuyer.getName() + "§7!");
            player.sendMessage("§7Your network is growing...");
            player.sendMessage("§7New buyers: §a" + registry.getAllBuyers().size());
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.8f);
        }
    }
    
    /**
     * Generates a new buyer based on the referrer's characteristics.
     */
    private IndividualBuyer generateReferredBuyer(IndividualBuyer referrer) {
        // New buyer has similar personality type (60% chance)
        CustomerType personality;
        if (ThreadLocalRandom.current().nextDouble() < 0.6) {
            personality = referrer.getPersonality();
        } else {
            CustomerType[] types = CustomerType.values();
            personality = types[ThreadLocalRandom.current().nextInt(types.length)];
        }
        
        // Generate a unique name for the new buyer
        String name = generateBuyerName();
        
        // Create new buyer
        IndividualBuyer newBuyer = new IndividualBuyer(java.util.UUID.randomUUID(), name, personality);
        
        // If referrer had favorites, new buyer might be interested in same strains
        if (!referrer.getFavoriteStrains().isEmpty() && ThreadLocalRandom.current().nextBoolean()) {
            String favoriteStrain = referrer.getFavoriteStrains().get(0);
            // This would be tracked separately in a real implementation
        }
        
        return newBuyer;
    }

    /**
     * Generates a random buyer name.
     */
    private String generateBuyerName() {
        String[] firstNames = {"Marcus", "Tony", "Jake", "Derek", "Chris", "Mike", "Steve", "Johnny"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller"};
        String firstName = firstNames[ThreadLocalRandom.current().nextInt(firstNames.length)];
        String lastName = lastNames[ThreadLocalRandom.current().nextInt(lastNames.length)];
        return firstName + " " + lastName;
    }
    
    /**
     * Awards referral bonus when a referred buyer makes their first purchase.
     */
    public double checkReferralBonus(UUID newBuyerId, double saleAmount) {
        if (!referralLinks.containsKey(newBuyerId)) {
            return 0;
        }
        
        UUID referrerId = referralLinks.get(newBuyerId);
        IndividualBuyer referrer = registry.getBuyer(referrerId);
        
        if (referrer == null) {
            return 0;
        }
        
        // Calculate referral bonus (10% of first sale)
        double bonus = saleAmount * 0.10;
        
        // Track bonus
        referralBonuses.merge(referrerId, bonus, Double::sum);
        
        return bonus;
    }
    
    /**
     * Gets total referral bonuses earned.
     */
    public double getTotalReferralBonuses(UUID playerId) {
        return referralBonuses.getOrDefault(playerId, 0.0);
    }
    
    /**
     * Gets network size (total buyers).
     */
    public int getNetworkSize() {
        return registry.getAllBuyers().size();
    }
    
    /**
     * Gets number of referrals made.
     */
    public int getReferralCount() {
        return referralLinks.size();
    }
    
    /**
     * Gets buyers referred by a specific buyer.
     */
    public List<IndividualBuyer> getReferredBuyers(UUID referrerId) {
        List<IndividualBuyer> referred = new ArrayList<>();
        
        for (Map.Entry<UUID, UUID> entry : referralLinks.entrySet()) {
            if (entry.getValue().equals(referrerId)) {
                IndividualBuyer buyer = registry.getBuyer(entry.getKey());
                if (buyer != null) {
                    referred.add(buyer);
                }
            }
        }
        
        return referred;
    }
    
    /**
     * Gets the referrer of a buyer.
     */
    public IndividualBuyer getReferrer(UUID buyerId) {
        UUID referrerId = referralLinks.get(buyerId);
        return referrerId != null ? registry.getBuyer(referrerId) : null;
    }
    
    /**
     * Identifies "influencer" buyers who have referred many others.
     */
    public List<IndividualBuyer> getInfluencers() {
        Map<UUID, Integer> referralCounts = new HashMap<>();
        
        for (UUID referrerId : referralLinks.values()) {
            referralCounts.merge(referrerId, 1, Integer::sum);
        }
        
        return referralCounts.entrySet().stream()
            .filter(e -> e.getValue() >= 3) // 3+ referrals = influencer
            .map(e -> registry.getBuyer(e.getKey()))
            .filter(Objects::nonNull)
            .toList();
    }
    
    /**
     * Calculates network growth rate.
     */
    public double getNetworkGrowthRate() {
        if (registry.getAllBuyers().isEmpty()) return 0;
        
        return (double) referralLinks.size() / registry.getAllBuyers().size();
    }
}
