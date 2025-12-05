package com.budlords.economy;

import com.budlords.BudLords;
import com.budlords.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages buyer reputation and relationships for BudLords v3.0.0.
 * Better reputation = better prices, tips, and special offers!
 */
public class ReputationManager {

    private final BudLords plugin;
    
    // Player reputation with different buyer types
    // Key: "playerUUID:buyerType", Value: reputation points
    private final Map<String, Integer> buyerReputation;
    
    // Reputation levels and thresholds
    public static final int REPUTATION_SUSPICIOUS = -50;
    public static final int REPUTATION_NEUTRAL = 0;
    public static final int REPUTATION_FRIENDLY = 50;
    public static final int REPUTATION_TRUSTED = 150;
    public static final int REPUTATION_VIP = 300;
    public static final int REPUTATION_LEGENDARY = 500;
    
    // Reputation effects
    public static final double SUSPICIOUS_PENALTY = 0.85;   // -15% prices
    public static final double NEUTRAL_BONUS = 1.0;          // No change
    public static final double FRIENDLY_BONUS = 1.05;        // +5% prices
    public static final double TRUSTED_BONUS = 1.10;         // +10% prices
    public static final double VIP_BONUS = 1.15;             // +15% prices
    public static final double LEGENDARY_BONUS = 1.25;       // +25% prices
    
    // Tip chances by reputation level
    public static final double TIP_CHANCE_NEUTRAL = 0.05;    // 5% chance
    public static final double TIP_CHANCE_FRIENDLY = 0.10;   // 10% chance
    public static final double TIP_CHANCE_TRUSTED = 0.20;    // 20% chance
    public static final double TIP_CHANCE_VIP = 0.35;        // 35% chance
    public static final double TIP_CHANCE_LEGENDARY = 0.50;  // 50% chance

    public ReputationManager(BudLords plugin) {
        this.plugin = plugin;
        this.buyerReputation = new ConcurrentHashMap<>();
        loadReputation();
    }
    
    private void loadReputation() {
        // Load reputation from player config
        var config = plugin.getDataManager().getPlayersConfig();
        var repSection = config.getConfigurationSection("reputation");
        if (repSection != null) {
            for (String key : repSection.getKeys(false)) {
                buyerReputation.put(key, repSection.getInt(key, 0));
            }
        }
    }
    
    public void saveReputation() {
        var config = plugin.getDataManager().getPlayersConfig();
        for (Map.Entry<String, Integer> entry : buyerReputation.entrySet()) {
            config.set("reputation." + entry.getKey(), entry.getValue());
        }
        plugin.getDataManager().savePlayers();
    }
    
    /**
     * Gets the reputation key for a player and buyer type.
     */
    private String getRepKey(UUID playerId, String buyerType) {
        return playerId.toString() + ":" + buyerType;
    }
    
    /**
     * Gets the reputation points for a player with a specific buyer type.
     */
    public int getReputation(UUID playerId, String buyerType) {
        return buyerReputation.getOrDefault(getRepKey(playerId, buyerType), 0);
    }
    
    /**
     * Adds reputation points for a player with a specific buyer type.
     */
    public void addReputation(UUID playerId, String buyerType, int amount) {
        String key = getRepKey(playerId, buyerType);
        int current = buyerReputation.getOrDefault(key, 0);
        int newRep = current + amount;
        buyerReputation.put(key, newRep);
        
        // Check for level up
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            String oldLevel = getReputationLevel(current);
            String newLevel = getReputationLevel(newRep);
            
            if (!oldLevel.equals(newLevel) && newRep > current) {
                player.sendMessage("");
                player.sendMessage("§6§l⬆ REPUTATION UP! §e" + buyerType);
                player.sendMessage("§7You are now: " + getReputationDisplay(newRep));
                player.sendMessage("§7New bonus: " + getReputationBonusText(newRep));
                player.sendMessage("");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
            }
        }
    }
    
    /**
     * Gets the reputation level name.
     */
    public String getReputationLevel(int rep) {
        if (rep >= REPUTATION_LEGENDARY) return "LEGENDARY";
        if (rep >= REPUTATION_VIP) return "VIP";
        if (rep >= REPUTATION_TRUSTED) return "TRUSTED";
        if (rep >= REPUTATION_FRIENDLY) return "FRIENDLY";
        if (rep > REPUTATION_SUSPICIOUS) return "NEUTRAL";
        return "SUSPICIOUS";
    }
    
    /**
     * Gets the colored reputation display.
     */
    public String getReputationDisplay(int rep) {
        if (rep >= REPUTATION_LEGENDARY) return "§6§l★ LEGENDARY ★";
        if (rep >= REPUTATION_VIP) return "§d§lVIP Status";
        if (rep >= REPUTATION_TRUSTED) return "§aTrusted Dealer";
        if (rep >= REPUTATION_FRIENDLY) return "§eFriendly";
        if (rep > REPUTATION_SUSPICIOUS) return "§7Neutral";
        return "§cSuspicious";
    }
    
    /**
     * Gets the reputation bonus text.
     */
    public String getReputationBonusText(int rep) {
        double bonus = getReputationMultiplier(rep);
        if (bonus >= 1.0) {
            return "§a+" + String.format("%.0f%%", (bonus - 1.0) * 100) + " prices";
        } else {
            return "§c" + String.format("%.0f%%", (bonus - 1.0) * 100) + " prices";
        }
    }
    
    /**
     * Gets the price multiplier based on reputation.
     */
    public double getReputationMultiplier(int rep) {
        if (rep >= REPUTATION_LEGENDARY) return LEGENDARY_BONUS;
        if (rep >= REPUTATION_VIP) return VIP_BONUS;
        if (rep >= REPUTATION_TRUSTED) return TRUSTED_BONUS;
        if (rep >= REPUTATION_FRIENDLY) return FRIENDLY_BONUS;
        if (rep > REPUTATION_SUSPICIOUS) return NEUTRAL_BONUS;
        return SUSPICIOUS_PENALTY;
    }
    
    /**
     * Gets the price multiplier for a specific player and buyer.
     */
    public double getReputationMultiplier(UUID playerId, String buyerType) {
        return getReputationMultiplier(getReputation(playerId, buyerType));
    }
    
    /**
     * Calculates and applies a tip based on reputation and sale value.
     * @return The tip amount (0 if no tip)
     */
    public double calculateTip(UUID playerId, String buyerType, double saleValue) {
        int rep = getReputation(playerId, buyerType);
        double tipChance;
        double tipPercent;
        
        if (rep >= REPUTATION_LEGENDARY) {
            tipChance = TIP_CHANCE_LEGENDARY;
            tipPercent = 0.15 + ThreadLocalRandom.current().nextDouble() * 0.10; // 15-25%
        } else if (rep >= REPUTATION_VIP) {
            tipChance = TIP_CHANCE_VIP;
            tipPercent = 0.10 + ThreadLocalRandom.current().nextDouble() * 0.10; // 10-20%
        } else if (rep >= REPUTATION_TRUSTED) {
            tipChance = TIP_CHANCE_TRUSTED;
            tipPercent = 0.08 + ThreadLocalRandom.current().nextDouble() * 0.07; // 8-15%
        } else if (rep >= REPUTATION_FRIENDLY) {
            tipChance = TIP_CHANCE_FRIENDLY;
            tipPercent = 0.05 + ThreadLocalRandom.current().nextDouble() * 0.05; // 5-10%
        } else if (rep > REPUTATION_SUSPICIOUS) {
            tipChance = TIP_CHANCE_NEUTRAL;
            tipPercent = 0.02 + ThreadLocalRandom.current().nextDouble() * 0.03; // 2-5%
        } else {
            return 0; // No tips for suspicious dealers
        }
        
        if (ThreadLocalRandom.current().nextDouble() < tipChance) {
            return saleValue * tipPercent;
        }
        
        return 0;
    }
    
    /**
     * Gets the reputation gain for a sale.
     */
    public int calculateReputationGain(double saleValue, boolean successfulDeal) {
        if (!successfulDeal) return -5; // Lose rep for failed deals
        
        // Base 1 rep per sale + bonus based on value
        int baseRep = 1;
        int valueBonus = (int) (saleValue / 500); // +1 rep per $500
        
        return Math.min(baseRep + valueBonus, 10); // Cap at 10 per sale
    }
    
    /**
     * Gets a random buyer comment based on reputation.
     */
    public String getReputationComment(int rep) {
        String[] comments;
        
        if (rep >= REPUTATION_LEGENDARY) {
            comments = new String[] {
                "§6\"You're a legend in this business!\"",
                "§6\"I tell everyone about your product!\"",
                "§6\"Best dealer I've ever worked with!\"",
                "§6\"Always a pleasure doing business!\"",
                "§6\"You're my #1 supplier!\""
            };
        } else if (rep >= REPUTATION_VIP) {
            comments = new String[] {
                "§d\"VIP treatment for my favorite dealer!\"",
                "§d\"Here's a little extra for you!\"",
                "§d\"Quality product, as always!\"",
                "§d\"You never disappoint!\""
            };
        } else if (rep >= REPUTATION_TRUSTED) {
            comments = new String[] {
                "§a\"I trust your product quality!\"",
                "§a\"Good doing business with you!\"",
                "§a\"You're becoming one of my regulars!\"",
                "§a\"Keep up the good work!\""
            };
        } else if (rep >= REPUTATION_FRIENDLY) {
            comments = new String[] {
                "§e\"Nice product!\"",
                "§e\"Thanks for the deal!\"",
                "§e\"See you next time!\"",
                "§e\"Good stuff!\""
            };
        } else if (rep > REPUTATION_SUSPICIOUS) {
            comments = new String[] {
                "§7\"This looks fine.\"",
                "§7\"Deal.\"",
                "§7\"Alright.\"",
                "§7\"We'll see how this goes.\""
            };
        } else {
            comments = new String[] {
                "§c\"I'm watching you...\"",
                "§c\"Don't try anything funny.\"",
                "§c\"I'll take it... this time.\"",
                "§c\"Better be worth my money.\""
            };
        }
        
        return comments[ThreadLocalRandom.current().nextInt(comments.length)];
    }
    
    public void shutdown() {
        saveReputation();
    }
}
