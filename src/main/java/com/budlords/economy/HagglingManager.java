package com.budlords.economy;

import com.budlords.BudLords;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Haggling system for BudLords v3.0.0.
 * Players can try to negotiate for better prices!
 */
public class HagglingManager {

    private final BudLords plugin;
    
    // Cooldowns for haggling attempts: playerUUID -> next haggle time
    private final Map<UUID, Long> haggleCooldowns;
    
    // Player haggle skill levels (improves with use)
    private final Map<UUID, Integer> haggleSkill;
    
    // Cooldown time in milliseconds (2 minutes)
    private static final long HAGGLE_COOLDOWN_MS = 120000L;
    
    // Max haggle skill level
    private static final int MAX_HAGGLE_SKILL = 100;
    
    // Haggle outcomes
    public enum HaggleResult {
        CRITICAL_SUCCESS,  // +20-30% price
        SUCCESS,           // +10-15% price
        MINOR_SUCCESS,     // +5-8% price
        NEUTRAL,           // No change
        MINOR_FAIL,        // -3-5% price
        FAIL,              // -8-12% price
        CRITICAL_FAIL      // -15-20% price, buyer may leave
    }

    public HagglingManager(BudLords plugin) {
        this.plugin = plugin;
        this.haggleCooldowns = new ConcurrentHashMap<>();
        this.haggleSkill = new ConcurrentHashMap<>();
        loadSkills();
    }
    
    private void loadSkills() {
        var config = plugin.getDataManager().getPlayersConfig();
        var skillSection = config.getConfigurationSection("haggle-skill");
        if (skillSection != null) {
            for (String uuid : skillSection.getKeys(false)) {
                try {
                    haggleSkill.put(UUID.fromString(uuid), skillSection.getInt(uuid, 0));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
    
    public void saveSkills() {
        var config = plugin.getDataManager().getPlayersConfig();
        for (Map.Entry<UUID, Integer> entry : haggleSkill.entrySet()) {
            config.set("haggle-skill." + entry.getKey().toString(), entry.getValue());
        }
        plugin.getDataManager().savePlayers();
    }
    
    /**
     * Gets the player's haggle skill level.
     */
    public int getHaggleSkill(UUID playerId) {
        return haggleSkill.getOrDefault(playerId, 0);
    }
    
    /**
     * Adds to the player's haggle skill.
     */
    public void addHaggleSkill(UUID playerId, int amount) {
        int current = getHaggleSkill(playerId);
        int newSkill = Math.min(current + amount, MAX_HAGGLE_SKILL);
        haggleSkill.put(playerId, newSkill);
        
        // Notify on skill up milestones
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            if (newSkill % 10 == 0 && newSkill > current) {
                player.sendMessage("§e§lHaggling Skill Up! §7Level " + newSkill);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
            }
        }
    }
    
    /**
     * Checks if a player can haggle.
     */
    public boolean canHaggle(UUID playerId) {
        Long cooldown = haggleCooldowns.get(playerId);
        if (cooldown == null) return true;
        return System.currentTimeMillis() >= cooldown;
    }
    
    /**
     * Gets remaining cooldown in seconds.
     */
    public long getHaggleCooldown(UUID playerId) {
        Long cooldown = haggleCooldowns.get(playerId);
        if (cooldown == null) return 0;
        long remaining = (cooldown - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    /**
     * Sets haggle cooldown for a player.
     */
    private void setCooldown(UUID playerId) {
        haggleCooldowns.put(playerId, System.currentTimeMillis() + HAGGLE_COOLDOWN_MS);
    }
    
    /**
     * Attempts to haggle for better prices.
     * @return The result of the haggle attempt
     */
    public HaggleResult attemptHaggle(Player player, int buyerReputation) {
        UUID playerId = player.getUniqueId();
        
        if (!canHaggle(playerId)) {
            return null; // On cooldown
        }
        
        setCooldown(playerId);
        
        int skill = getHaggleSkill(playerId);
        double roll = ThreadLocalRandom.current().nextDouble() * 100;
        
        // Skill affects success rates
        // Higher skill = better chance of good outcomes, lower chance of bad
        double critSuccessChance = 2 + (skill * 0.08); // 2-10%
        double successChance = critSuccessChance + 10 + (skill * 0.15); // 12-25%
        double minorSuccessChance = successChance + 15 + (skill * 0.10); // 27-40%
        double neutralChance = minorSuccessChance + 25 - (skill * 0.05); // 52-45%
        double minorFailChance = neutralChance + 15 - (skill * 0.08); // 67-52%
        double failChance = minorFailChance + 10 - (skill * 0.05); // 77-62%
        // Rest is critical fail
        
        // Reputation also affects outcomes
        if (buyerReputation >= ReputationManager.REPUTATION_TRUSTED) {
            // Trusted players get bonus
            critSuccessChance += 3;
            successChance += 5;
            minorSuccessChance += 5;
        } else if (buyerReputation <= ReputationManager.REPUTATION_SUSPICIOUS) {
            // Suspicious players get penalty
            critSuccessChance -= 2;
            successChance -= 3;
        }
        
        HaggleResult result;
        
        if (roll < critSuccessChance) {
            result = HaggleResult.CRITICAL_SUCCESS;
            addHaggleSkill(playerId, 3);
        } else if (roll < successChance) {
            result = HaggleResult.SUCCESS;
            addHaggleSkill(playerId, 2);
        } else if (roll < minorSuccessChance) {
            result = HaggleResult.MINOR_SUCCESS;
            addHaggleSkill(playerId, 1);
        } else if (roll < neutralChance) {
            result = HaggleResult.NEUTRAL;
            addHaggleSkill(playerId, 1);
        } else if (roll < minorFailChance) {
            result = HaggleResult.MINOR_FAIL;
            addHaggleSkill(playerId, 1);
        } else if (roll < failChance) {
            result = HaggleResult.FAIL;
        } else {
            result = HaggleResult.CRITICAL_FAIL;
        }
        
        return result;
    }
    
    /**
     * Gets the price multiplier for a haggle result.
     */
    public double getHaggleMultiplier(HaggleResult result) {
        return switch (result) {
            case CRITICAL_SUCCESS -> 1.20 + ThreadLocalRandom.current().nextDouble() * 0.10;
            case SUCCESS -> 1.10 + ThreadLocalRandom.current().nextDouble() * 0.05;
            case MINOR_SUCCESS -> 1.05 + ThreadLocalRandom.current().nextDouble() * 0.03;
            case NEUTRAL -> 1.0;
            case MINOR_FAIL -> 0.95 + ThreadLocalRandom.current().nextDouble() * 0.02;
            case FAIL -> 0.88 + ThreadLocalRandom.current().nextDouble() * 0.04;
            case CRITICAL_FAIL -> 0.80 + ThreadLocalRandom.current().nextDouble() * 0.05;
        };
    }
    
    /**
     * Gets the display message for a haggle result.
     */
    public String getHaggleMessage(HaggleResult result) {
        return switch (result) {
            case CRITICAL_SUCCESS -> "§a§l★ CRITICAL SUCCESS! §a\"Deal! I'll even throw in extra!\"";
            case SUCCESS -> "§a§lSUCCESS! §e\"Alright, you drive a hard bargain.\"";
            case MINOR_SUCCESS -> "§eMINOR SUCCESS §7\"Fine, a little extra.\"";
            case NEUTRAL -> "§7NEUTRAL §7\"Take it or leave it.\"";
            case MINOR_FAIL -> "§cMINOR FAIL §7\"Actually, I was being generous...\"";
            case FAIL -> "§c§lFAIL! §c\"You're pushing your luck...\"";
            case CRITICAL_FAIL -> "§4§l✗ CRITICAL FAIL! §c\"Now you've annoyed me!\"";
        };
    }
    
    /**
     * Gets buyer response messages for haggle outcomes.
     */
    public String[] getBuyerHaggleResponses(HaggleResult result) {
        return switch (result) {
            case CRITICAL_SUCCESS -> new String[] {
                "§a\"You know what? I like you. Deal!\"",
                "§a\"Impressive negotiation! You've earned this!\"",
                "§a\"Ha! You got me. Here's extra for your skills!\""
            };
            case SUCCESS -> new String[] {
                "§e\"Alright, alright. You win this one.\"",
                "§e\"Fine, I can do a bit better.\"",
                "§e\"You drive a hard bargain!\""
            };
            case MINOR_SUCCESS -> new String[] {
                "§7\"I suppose I can add a little more.\"",
                "§7\"Fair enough, a small bonus.\"",
                "§7\"You're getting slightly better at this.\""
            };
            case NEUTRAL -> new String[] {
                "§7\"My price is fair. Take it or leave it.\"",
                "§7\"I'm not budging.\"",
                "§7\"That's my final offer.\""
            };
            case MINOR_FAIL -> new String[] {
                "§c\"Actually... let me reconsider that price.\"",
                "§c\"Hmm, on second thought...\"",
                "§c\"I was being too generous.\""
            };
            case FAIL -> new String[] {
                "§c\"Now you've annoyed me. Price goes down.\"",
                "§c\"Bad move. Lower offer now.\"",
                "§c\"Don't push your luck!\""
            };
            case CRITICAL_FAIL -> new String[] {
                "§4\"That's it! Drastically reduced price now!\"",
                "§4\"You've insulted me! This is what you get!\"",
                "§4\"One more word and I walk!\""
            };
        };
    }
    
    public void shutdown() {
        saveSkills();
    }
}
