package com.budlords.farming;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.skills.Skill;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages formation bonuses for plants.
 * When same-strain plants are arranged in specific patterns,
 * they can boost each other's star rating and provide special effects.
 * 
 * FARMING XP REQUIREMENTS (Difficulty: HARD):
 * - Basic formations (Line, Corner, L): Unlocked at 0 XP, but only 5% base chance
 * - Medium formations (T, Cross, Square): Requires 500+ XP to unlock
 * - Advanced formations (Diamond, Star, Spiral): Requires 1500+ XP to unlock
 * - Master formations (Pentagon, Hexagon, Octagon): Requires 3000+ XP to unlock
 * - Legendary formations (Yin-Yang, Infinity, Sacred Geometry): Requires 5000+ XP
 * - Mythic formations (Dragon, Phoenix, Celestial): Requires 10000+ XP
 */
public class FormationManager {

    private final BudLords plugin;
    private final FarmingManager farmingManager;
    
    // Formation bonus configuration constants - HARD difficulty
    private static final double BASE_SUCCESS_CHANCE = 0.05;       // Only 5% base chance (was 10%)
    private static final double MAX_SUCCESS_CHANCE = 0.40;        // 40% cap (was 50%)
    private static final double XP_SUCCESS_DIVISOR = 500.0;       // Need 500 XP per increment (was 100)
    private static final double XP_SUCCESS_BONUS = 0.03;          // +3% per increment (was 5%)
    private static final int MAX_STAR_BOOST = 6;                  // Up to +6 stars for mythic formations
    private static final int XP_PER_STAR_BOOST = 1000;            // 1000 XP per star boost (was 200)
    
    // XP thresholds for unlocking formation tiers (HARD requirements)
    private static final int XP_TIER_BASIC = 0;           // Line, Corner, L-Shape
    private static final int XP_TIER_MEDIUM = 500;        // T-Shape, Cross, Square
    private static final int XP_TIER_ADVANCED = 1500;     // Diamond, Star, Spiral
    private static final int XP_TIER_MASTER = 3000;       // Pentagon, Hexagon, Octagon
    private static final int XP_TIER_LEGENDARY = 5000;    // Yin-Yang, Infinity, Sacred Geometry
    private static final int XP_TIER_MYTHIC = 10000;      // Dragon, Phoenix, Celestial

    // ==================== BASIC FORMATIONS (0 XP) ====================
    
    // L-shape pattern variants (3 plants)
    private static final int[][][] L_FORMATIONS = {
        {{1, 0}, {0, 1}}, {{-1, 0}, {0, 1}}, {{-1, 0}, {0, -1}}, {{1, 0}, {0, -1}},
        {{0, 1}, {1, 0}}, {{0, 1}, {-1, 0}}, {{0, -1}, {1, 0}}, {{0, -1}, {-1, 0}}
    };
    
    // Line pattern (3 plants in a row)
    private static final int[][][] LINE_FORMATIONS = {
        {{1, 0}, {2, 0}}, {{-1, 0}, {-2, 0}}, {{0, 1}, {0, 2}}, {{0, -1}, {0, -2}}
    };
    
    // Corner pattern (3 plants forming a corner)
    private static final int[][][] CORNER_FORMATIONS = {
        {{1, 0}, {1, 1}}, {{-1, 0}, {-1, 1}}, {{1, 0}, {1, -1}}, {{-1, 0}, {-1, -1}},
        {{0, 1}, {1, 1}}, {{0, -1}, {1, -1}}, {{0, 1}, {-1, 1}}, {{0, -1}, {-1, -1}}
    };
    
    // ==================== MEDIUM FORMATIONS (500 XP) ====================
    
    // T-shape pattern (4 plants)
    private static final int[][][] T_FORMATIONS = {
        {{0, 1}, {1, 0}, {-1, 0}}, {{0, -1}, {1, 0}, {-1, 0}},
        {{1, 0}, {0, 1}, {0, -1}}, {{-1, 0}, {0, 1}, {0, -1}}
    };
    
    // Cross/Plus pattern (5 plants in + shape)
    private static final int[][][] CROSS_FORMATIONS = {
        {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}
    };
    
    // Square pattern (4 plants forming a 2x2 square)
    private static final int[][][] SQUARE_FORMATIONS = {
        {{1, 0}, {0, 1}, {1, 1}}, {{-1, 0}, {0, 1}, {-1, 1}},
        {{1, 0}, {0, -1}, {1, -1}}, {{-1, 0}, {0, -1}, {-1, -1}}
    };
    
    // ==================== ADVANCED FORMATIONS (1500 XP) ====================
    
    // Diamond pattern (5 plants in diamond shape)
    private static final int[][][] DIAMOND_FORMATIONS = {
        {{0, 1}, {0, -1}, {1, 0}, {-1, 0}}, // Standard diamond
        {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}} // Rotated diamond
    };
    
    // Star pattern (6 plants in triangular star)
    private static final int[][][] STAR_FORMATIONS = {
        {{2, 0}, {-2, 0}, {1, 1}, {-1, 1}, {1, -1}}, // 5-point partial star
        {{0, 2}, {0, -2}, {1, 1}, {-1, 1}, {-1, -1}}
    };
    
    // Spiral pattern (5 plants in spiral shape)
    private static final int[][][] SPIRAL_FORMATIONS = {
        {{1, 0}, {1, 1}, {0, 1}, {-1, 1}}, // Clockwise spiral start
        {{0, 1}, {-1, 1}, {-1, 0}, {-1, -1}}, // Counter-clockwise
        {{1, 0}, {2, 0}, {2, 1}, {2, 2}} // Extended spiral
    };
    
    // Arrow pattern (5 plants pointing direction)
    private static final int[][][] ARROW_FORMATIONS = {
        {{1, 0}, {2, 0}, {1, 1}, {1, -1}}, // Arrow pointing right
        {{-1, 0}, {-2, 0}, {-1, 1}, {-1, -1}}, // Arrow pointing left
        {{0, 1}, {0, 2}, {1, 1}, {-1, 1}}, // Arrow pointing up
        {{0, -1}, {0, -2}, {1, -1}, {-1, -1}} // Arrow pointing down
    };
    
    // ==================== MASTER FORMATIONS (3000 XP) ====================
    
    // Pentagon pattern (5 plants in pentagon shape)
    private static final int[][][] PENTAGON_FORMATIONS = {
        {{0, 2}, {2, 1}, {1, -1}, {-1, -1}, {-2, 1}}, // Regular pentagon
        {{2, 0}, {1, 2}, {-1, 2}, {-2, 0}} // Partial pentagon
    };
    
    // Hexagon pattern (6 plants in hexagon)
    private static final int[][][] HEXAGON_FORMATIONS = {
        {{1, 0}, {2, 1}, {1, 2}, {-1, 2}, {-2, 1}}, // Hexagon
        {{0, 1}, {1, 1}, {2, 0}, {1, -1}, {0, -1}} // Compact hexagon
    };
    
    // Octagon pattern (8 plants in octagon)
    private static final int[][][] OCTAGON_FORMATIONS = {
        {{1, 0}, {2, 1}, {2, 2}, {1, 3}, {0, 3}, {-1, 2}, {-1, 1}}, // Large octagon
        {{1, 0}, {1, 1}, {0, 2}, {-1, 1}, {-1, 0}, {0, -1}, {1, -1}} // Compact octagon
    };
    
    // Heart pattern (special romantic formation)
    private static final int[][][] HEART_FORMATIONS = {
        {{-1, 1}, {1, 1}, {-2, 0}, {2, 0}, {-1, -1}, {1, -1}, {0, -2}}, // Heart shape
        {{0, 1}, {1, 1}, {-1, 1}, {1, 0}, {-1, 0}, {0, -1}} // Small heart
    };
    
    // ==================== LEGENDARY FORMATIONS (5000 XP) ====================
    
    // Yin-Yang pattern (curved balance formation)
    private static final int[][][] YIN_YANG_FORMATIONS = {
        {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}}, // Full yin-yang
        {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {2, 0}, {-2, 0}, {0, 2}} // Balance pattern
    };
    
    // Infinity pattern (figure 8 shape)
    private static final int[][][] INFINITY_FORMATIONS = {
        {{1, 0}, {2, 1}, {2, -1}, {-1, 0}, {-2, 1}, {-2, -1}, {3, 0}, {-3, 0}}, // Wide infinity
        {{1, 1}, {2, 0}, {1, -1}, {-1, 1}, {-2, 0}, {-1, -1}} // Compact infinity
    };
    
    // Sacred Geometry (Flower of Life pattern)
    private static final int[][][] SACRED_GEOMETRY_FORMATIONS = {
        {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {2, 0}, {-2, 0}}, // Flower core
        {{1, 0}, {0, 1}, {-1, 1}, {-1, 0}, {0, -1}, {1, -1}} // Seed of life
    };
    
    // Crown pattern (royal formation)
    private static final int[][][] CROWN_FORMATIONS = {
        {{-2, 0}, {-1, 1}, {0, 2}, {1, 1}, {2, 0}, {-1, 0}, {1, 0}}, // Crown shape
        {{0, 1}, {-1, 2}, {1, 2}, {-2, 1}, {2, 1}} // Crown peaks
    };
    
    // ==================== MYTHIC FORMATIONS (10000 XP) ====================
    
    // Dragon pattern (serpentine shape)
    private static final int[][][] DRAGON_FORMATIONS = {
        {{1, 0}, {2, 0}, {3, 1}, {4, 1}, {5, 0}, {5, -1}, {4, -2}, {3, -2}, {2, -1}}, // Dragon body
        {{1, 0}, {2, 1}, {3, 1}, {4, 0}, {4, -1}, {3, -2}, {2, -2}, {1, -1}, {0, -1}} // Curved dragon
    };
    
    // Phoenix pattern (rising bird shape)
    private static final int[][][] PHOENIX_FORMATIONS = {
        {{0, 1}, {0, 2}, {-1, 2}, {1, 2}, {-2, 1}, {2, 1}, {-1, 0}, {1, 0}, {0, -1}, {0, -2}}, // Rising phoenix
        {{0, 1}, {1, 2}, {-1, 2}, {2, 1}, {-2, 1}, {1, 0}, {-1, 0}, {0, -1}} // Spread wings
    };
    
    // Celestial pattern (cosmic constellation)
    private static final int[][][] CELESTIAL_FORMATIONS = {
        {{0, 2}, {2, 2}, {3, 0}, {2, -2}, {0, -2}, {-2, -2}, {-3, 0}, {-2, 2}, {0, 0}}, // Nine-point star
        {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}} // Compass rose
    };
    
    // Ancient Rune pattern (mystical symbols)
    private static final int[][][] ANCIENT_RUNE_FORMATIONS = {
        {{0, 1}, {0, 2}, {1, 0}, {-1, 0}, {1, 2}, {-1, 2}, {2, 1}, {-2, 1}}, // Rune of power
        {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {0, 2}, {0, -2}, {2, 0}, {-2, 0}, {0, 0}} // Elder rune
    };
    
    // ==================== SECRET 666 FORMATION (HIDDEN) ====================
    // DEMON FORMATION - Unlocks demonic bonus for plants
    // Grants +1 star to center pot and plants, enables Blood Moon transformation
    // Pattern: Inverted quotation marks / devilish shape
    //     P
    //   P
    // [C] ← Center (demon pot)
    //   P
    //     P
    private static final int[][][] DEMON_666_FORMATIONS = {
        {{1, 2}, {1, 1}, {-1, -1}, {-1, -2}}, // Primary 666 pattern
        {{-1, 2}, {-1, 1}, {1, -1}, {1, -2}}, // Mirrored 666 pattern
        {{2, 1}, {1, 1}, {-1, -1}, {-2, -1}}, // Rotated 90°
        {{2, -1}, {1, -1}, {-1, 1}, {-2, 1}}  // Rotated 270°
    };

    public FormationManager(BudLords plugin, FarmingManager farmingManager) {
        this.plugin = plugin;
        this.farmingManager = farmingManager;
    }

    /**
     * Checks if a plant is part of a formation and calculates potential star boost.
     * @param plant The plant being harvested
     * @param ownerUuid The owner's UUID (for skill checks)
     * @return The number of bonus stars (0 if no formation or boost failed)
     */
    public int calculateFormationBonus(Plant plant, UUID ownerUuid) {
        Location plantLoc = plant.getLocation();
        String strainId = plant.getStrainId();
        
        // Get player's farming XP first to determine available formations
        int farmingXP = 0;
        if (plugin.getSkillManager() != null) {
            farmingXP = plugin.getSkillManager().getTreeXP(ownerUuid, Skill.SkillTree.FARMING);
        }
        
        // Find formation type this plant is part of
        FormationType formation = detectFormation(plantLoc, strainId, farmingXP);
        
        if (formation == FormationType.NONE) {
            return 0;
        }
        
        // Calculate success chance based on formation tier and XP
        double baseSuccessChance = BASE_SUCCESS_CHANCE;
        int maxStarBoost = 1;
        
        // XP-based bonuses with HARD scaling
        double xpBonus = (farmingXP / XP_SUCCESS_DIVISOR) * XP_SUCCESS_BONUS;
        baseSuccessChance = Math.min(MAX_SUCCESS_CHANCE, BASE_SUCCESS_CHANCE + xpBonus);
        maxStarBoost = Math.min(MAX_STAR_BOOST, 1 + farmingXP / XP_PER_STAR_BOOST);
        
        // Formation tier multipliers (higher tiers = better bonuses)
        double formationMultiplier = getFormationMultiplier(formation);
        double successChance = baseSuccessChance * formationMultiplier;
        
        // Roll for success
        if (ThreadLocalRandom.current().nextDouble() > successChance) {
            return 0; // Formation bonus didn't trigger
        }
        
        // Determine bonus stars based on formation tier
        int bonusStars = formation.getBaseStarBonus();
        
        // Chance for additional stars based on XP
        for (int i = bonusStars + 1; i <= maxStarBoost; i++) {
            double additionalChance = successChance / (i * 3); // Even harder scaling
            if (ThreadLocalRandom.current().nextDouble() < additionalChance) {
                bonusStars = i;
            }
        }
        
        return bonusStars;
    }
    
    /**
     * Gets the formation effect result including special bonuses.
     * Called at harvest time to apply formation-specific effects to the player.
     */
    public FormationEffect getFormationEffect(Plant plant, UUID ownerUuid) {
        Location plantLoc = plant.getLocation();
        String strainId = plant.getStrainId();
        
        int farmingXP = 0;
        if (plugin.getSkillManager() != null) {
            farmingXP = plugin.getSkillManager().getTreeXP(ownerUuid, Skill.SkillTree.FARMING);
        }
        
        FormationType formation = detectFormation(plantLoc, strainId, farmingXP);
        
        if (formation == FormationType.NONE) {
            return null;
        }
        
        // Calculate effect chance based on XP (harder requirements)
        double effectChance = Math.min(0.3, 0.05 + (farmingXP / 2000.0) * 0.05);
        
        if (ThreadLocalRandom.current().nextDouble() > effectChance) {
            return null; // No special effect triggered
        }
        
        return new FormationEffect(formation, farmingXP);
    }
    
    /**
     * Applies formation effects to a player at harvest time.
     */
    public void applyFormationEffects(Player player, FormationEffect effect) {
        if (effect == null) return;
        
        World world = player.getWorld();
        Location loc = player.getLocation();
        
        // Apply the formation-specific effect
        switch (effect.getFormation()) {
            // Basic formations - Simple buffs
            case LINE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0));
                player.sendMessage("§a⟿ Line Formation: §7+Speed boost!");
            }
            case CORNER -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 200, 0));
                player.sendMessage("§b⌐ Corner Formation: §7+Mining speed!");
            }
            case L_SHAPE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 300, 0));
                player.sendMessage("§a⌐ L-Shape Formation: §7+Lucky harvests!");
            }
            
            // Medium formations - Better buffs
            case T_SHAPE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
                player.sendMessage("§6⊤ T-Shape Formation: §7+Speed II boost!");
            }
            case CROSS -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100, 0));
                world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 20, 1, 1, 1, 0.1);
                player.sendMessage("§e✚ Cross Formation: §7+Health regen & saturation!");
            }
            case SQUARE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 300, 0));
                player.sendMessage("§9□ Square Formation: §7+Damage resistance!");
            }
            
            // Advanced formations - Strong buffs
            case DIAMOND -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 600, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 300, 0));
                world.spawnParticle(Particle.END_ROD, loc, 30, 1, 1, 1, 0.1);
                player.sendMessage("§b◆ Diamond Formation: §7+Luck II & Village Hero!");
            }
            case STAR -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
                world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 50, 1, 2, 1, 0.1);
                player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.2f);
                player.sendMessage("§e★ Star Formation: §7+Night vision & Glowing aura!");
            }
            case SPIRAL -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 400, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 200, 0));
                world.spawnParticle(Particle.BUBBLE_COLUMN_UP, loc, 40, 1, 1, 1, 0.1);
                player.sendMessage("§3@ Spiral Formation: §7+Aquatic powers!");
            }
            case ARROW -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 400, 1));
                world.spawnParticle(Particle.CRIT, loc, 30, 1, 1, 1, 0.2);
                player.sendMessage("§c➤ Arrow Formation: §7+Speed III & Jump Boost!");
            }
            
            // Master formations - Powerful buffs
            case PENTAGON -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 400, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 400, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0));
                world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 50, 1, 2, 1, 0.5);
                player.sendMessage("§5⬠ Pentagon Formation: §7+Strength & Protection!");
            }
            case HEXAGON -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 500, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 500, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 500, 1));
                world.spawnParticle(Particle.SOUL, loc, 40, 1, 1, 1, 0.1);
                player.sendMessage("§d⬡ Hexagon Formation: §7+Speed, Haste & Luck II!");
            }
            case OCTAGON -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 600, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0));
                world.spawnParticle(Particle.HEART, loc, 20, 1, 1, 1, 0.1);
                player.sendMessage("§c⯃ Octagon Formation: §7+Massive health boost!");
            }
            case HEART -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 3));
                world.spawnParticle(Particle.HEART, loc, 50, 2, 2, 2, 0.2);
                player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
                player.sendMessage("§c♥ Heart Formation: §7+Regeneration II & Absorption IV!");
            }
            
            // Legendary formations - Elite buffs
            case YIN_YANG -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 600, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 600, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 1));
                world.spawnParticle(Particle.DRAGON_BREATH, loc, 30, 1, 1, 1, 0.1);
                world.spawnParticle(Particle.END_ROD, loc, 30, 1, 1, 1, 0.1);
                player.sendMessage("§f☯ Yin-Yang Formation: §7+Perfect balance of power!");
            }
            case INFINITY -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 800, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 800, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 400, 1));
                world.spawnParticle(Particle.TOTEM, loc, 50, 2, 2, 2, 0.3);
                player.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
                player.sendMessage("§a∞ Infinity Formation: §7+Endless energy!");
            }
            case SACRED_GEOMETRY -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 1000, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 600, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1000, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 600, 0));
                world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 100, 3, 3, 3, 1.0);
                player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                player.sendMessage("§e✡ Sacred Geometry Formation: §7+Divine enlightenment!");
            }
            case CROWN -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 800, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 800, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 400, 0));
                world.spawnParticle(Particle.SPELL_WITCH, loc, 50, 1, 2, 1, 0.1);
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                player.sendMessage("§6♛ Crown Formation: §7+Royal blessing!");
            }
            
            // Mythic formations - God-tier buffs
            case DRAGON -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1000, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1000, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 600, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 2));
                world.spawnParticle(Particle.DRAGON_BREATH, loc, 100, 3, 3, 3, 0.2);
                world.spawnParticle(Particle.FLAME, loc, 50, 2, 2, 2, 0.1);
                player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                player.sendMessage("§4🐉 Dragon Formation: §7+Ancient dragon's might!");
            }
            case PHOENIX -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 4));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 800, 0));
                world.spawnParticle(Particle.FLAME, loc, 80, 2, 3, 2, 0.15);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 40, 2, 3, 2, 0.1);
                player.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.5f);
                player.sendMessage("§6🔥 Phoenix Formation: §7+Rebirth flames!");
            }
            case CELESTIAL -> {
                // Ultimate buff - all positive effects!
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 800, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 800, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 800, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 800, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 1200, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1200, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 800, 4));
                world.spawnParticle(Particle.END_ROD, loc, 150, 4, 4, 4, 0.3);
                world.spawnParticle(Particle.TOTEM, loc, 100, 3, 3, 3, 0.5);
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
                player.sendMessage("§d✦ Celestial Formation: §7+Cosmic power unleashed!");
            }
            case ANCIENT_RUNE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 1000, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 1000, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 1000, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 600, 0));
                world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 150, 4, 4, 4, 2.0);
                world.spawnParticle(Particle.PORTAL, loc, 80, 3, 3, 3, 0.5);
                player.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.7f);
                player.sendMessage("§5ᚱ Ancient Rune Formation: §7+Mystical knowledge awakened!");
            }
            default -> {}
        }
    }
    
    /**
     * Gets the formation multiplier for success chance.
     */
    private double getFormationMultiplier(FormationType formation) {
        return switch (formation) {
            // Basic tier
            case LINE -> 1.0;
            case CORNER -> 1.1;
            case L_SHAPE -> 1.2;
            // Medium tier
            case T_SHAPE -> 1.3;
            case CROSS -> 1.4;
            case SQUARE -> 1.35;
            // Advanced tier
            case DIAMOND -> 1.5;
            case STAR -> 1.6;
            case SPIRAL -> 1.55;
            case ARROW -> 1.45;
            // Master tier
            case PENTAGON -> 1.7;
            case HEXAGON -> 1.8;
            case OCTAGON -> 1.9;
            case HEART -> 1.75;
            // Legendary tier
            case YIN_YANG -> 2.0;
            case INFINITY -> 2.1;
            case SACRED_GEOMETRY -> 2.2;
            case CROWN -> 2.0;
            // Mythic tier
            case DRAGON -> 2.5;
            case PHOENIX -> 2.6;
            case CELESTIAL -> 3.0;
            case ANCIENT_RUNE -> 2.7;
            default -> 0;
        };
    }
    
    /**
     * Detects what formation pattern (if any) a plant is part of.
     * Only checks formations that the player has unlocked based on XP.
     */
    public FormationType detectFormation(Location plantLoc, String strainId, int farmingXP) {
        // Check formations from highest tier to lowest (mythic first)
        
        // SECRET: Check for 666 Demon Formation (no XP required, always available but hidden)
        if (matchesFormation(plantLoc, strainId, DEMON_666_FORMATIONS)) {
            return FormationType.DEMON_666;
        }
        
        // Mythic tier (10000+ XP required)
        if (farmingXP >= XP_TIER_MYTHIC) {
            if (matchesFormation(plantLoc, strainId, CELESTIAL_FORMATIONS)) return FormationType.CELESTIAL;
            if (matchesFormation(plantLoc, strainId, PHOENIX_FORMATIONS)) return FormationType.PHOENIX;
            if (matchesFormation(plantLoc, strainId, DRAGON_FORMATIONS)) return FormationType.DRAGON;
            if (matchesFormation(plantLoc, strainId, ANCIENT_RUNE_FORMATIONS)) return FormationType.ANCIENT_RUNE;
        }
        
        // Legendary tier (5000+ XP required)
        if (farmingXP >= XP_TIER_LEGENDARY) {
            if (matchesFormation(plantLoc, strainId, SACRED_GEOMETRY_FORMATIONS)) return FormationType.SACRED_GEOMETRY;
            if (matchesFormation(plantLoc, strainId, INFINITY_FORMATIONS)) return FormationType.INFINITY;
            if (matchesFormation(plantLoc, strainId, YIN_YANG_FORMATIONS)) return FormationType.YIN_YANG;
            if (matchesFormation(plantLoc, strainId, CROWN_FORMATIONS)) return FormationType.CROWN;
        }
        
        // Master tier (3000+ XP required)
        if (farmingXP >= XP_TIER_MASTER) {
            if (matchesFormation(plantLoc, strainId, OCTAGON_FORMATIONS)) return FormationType.OCTAGON;
            if (matchesFormation(plantLoc, strainId, HEXAGON_FORMATIONS)) return FormationType.HEXAGON;
            if (matchesFormation(plantLoc, strainId, PENTAGON_FORMATIONS)) return FormationType.PENTAGON;
            if (matchesFormation(plantLoc, strainId, HEART_FORMATIONS)) return FormationType.HEART;
        }
        
        // Advanced tier (1500+ XP required)
        if (farmingXP >= XP_TIER_ADVANCED) {
            if (matchesFormation(plantLoc, strainId, STAR_FORMATIONS)) return FormationType.STAR;
            if (matchesFormation(plantLoc, strainId, DIAMOND_FORMATIONS)) return FormationType.DIAMOND;
            if (matchesFormation(plantLoc, strainId, SPIRAL_FORMATIONS)) return FormationType.SPIRAL;
            if (matchesFormation(plantLoc, strainId, ARROW_FORMATIONS)) return FormationType.ARROW;
        }
        
        // Medium tier (500+ XP required)
        if (farmingXP >= XP_TIER_MEDIUM) {
            if (matchesFormation(plantLoc, strainId, CROSS_FORMATIONS)) return FormationType.CROSS;
            if (matchesFormation(plantLoc, strainId, T_FORMATIONS)) return FormationType.T_SHAPE;
            if (matchesFormation(plantLoc, strainId, SQUARE_FORMATIONS)) return FormationType.SQUARE;
        }
        
        // Basic tier (always available)
        if (matchesFormation(plantLoc, strainId, L_FORMATIONS)) return FormationType.L_SHAPE;
        if (matchesFormation(plantLoc, strainId, CORNER_FORMATIONS)) return FormationType.CORNER;
        if (matchesFormation(plantLoc, strainId, LINE_FORMATIONS)) return FormationType.LINE;
        
        return FormationType.NONE;
    }
    
    /**
     * Legacy method for backward compatibility.
     */
    public FormationType detectFormation(Location plantLoc, String strainId) {
        return detectFormation(plantLoc, strainId, 0);
    }
    
    /**
     * Checks if a plant matches any variant of a formation pattern.
     */
    private boolean matchesFormation(Location plantLoc, String strainId, int[][][] patterns) {
        for (int[][] pattern : patterns) {
            if (matchesSinglePattern(plantLoc, strainId, pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if plants exist at the relative positions with the same strain.
     */
    private boolean matchesSinglePattern(Location plantLoc, String strainId, int[][] pattern) {
        for (int[] offset : pattern) {
            Location checkLoc = plantLoc.clone().add(offset[0], 0, offset[1]);
            Plant neighborPlant = farmingManager.getPlantAt(checkLoc);
            
            if (neighborPlant == null || !neighborPlant.getStrainId().equals(strainId)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets all plants that are part of the same formation as a given plant.
     */
    public List<Plant> getFormationPlants(Location plantLoc, String strainId) {
        List<Plant> formationPlants = new ArrayList<>();
        
        Plant centerPlant = farmingManager.getPlantAt(plantLoc);
        if (centerPlant != null) {
            formationPlants.add(centerPlant);
        }
        
        // Check all pattern sets
        int[][][][] allPatterns = {
            L_FORMATIONS, LINE_FORMATIONS, CORNER_FORMATIONS,
            T_FORMATIONS, CROSS_FORMATIONS, SQUARE_FORMATIONS,
            DIAMOND_FORMATIONS, STAR_FORMATIONS, SPIRAL_FORMATIONS, ARROW_FORMATIONS,
            PENTAGON_FORMATIONS, HEXAGON_FORMATIONS, OCTAGON_FORMATIONS, HEART_FORMATIONS,
            YIN_YANG_FORMATIONS, INFINITY_FORMATIONS, SACRED_GEOMETRY_FORMATIONS, CROWN_FORMATIONS,
            DRAGON_FORMATIONS, PHOENIX_FORMATIONS, CELESTIAL_FORMATIONS, ANCIENT_RUNE_FORMATIONS
        };
        
        for (int[][][] patternSet : allPatterns) {
            for (int[][] pattern : patternSet) {
                if (matchesSinglePattern(plantLoc, strainId, pattern)) {
                    for (int[] offset : pattern) {
                        Location neighborLoc = plantLoc.clone().add(offset[0], 0, offset[1]);
                        Plant neighborPlant = farmingManager.getPlantAt(neighborLoc);
                        if (neighborPlant != null && !formationPlants.contains(neighborPlant)) {
                            formationPlants.add(neighborPlant);
                        }
                    }
                }
            }
        }
        
        return formationPlants;
    }
    
    /**
     * Gets a display string for a formation type with XP requirement.
     */
    public static String getFormationDisplay(FormationType type) {
        return switch (type) {
            // Basic
            case LINE -> "§7━ Line Formation";
            case CORNER -> "§7⌐ Corner Formation";
            case L_SHAPE -> "§a⌐ L-Shape Formation";
            // Medium
            case T_SHAPE -> "§e⊤ T-Shape Formation";
            case CROSS -> "§e✚ Cross Formation";
            case SQUARE -> "§e□ Square Formation";
            // Advanced
            case DIAMOND -> "§b◆ Diamond Formation";
            case STAR -> "§b★ Star Formation";
            case SPIRAL -> "§b@ Spiral Formation";
            case ARROW -> "§b➤ Arrow Formation";
            // Master
            case PENTAGON -> "§5⬠ Pentagon Formation";
            case HEXAGON -> "§5⬡ Hexagon Formation";
            case OCTAGON -> "§5⯃ Octagon Formation";
            case HEART -> "§c♥ Heart Formation";
            // Legendary
            case YIN_YANG -> "§f☯ Yin-Yang Formation";
            case INFINITY -> "§a∞ Infinity Formation";
            case SACRED_GEOMETRY -> "§e✡ Sacred Geometry Formation";
            case CROWN -> "§6♛ Crown Formation";
            // Mythic
            case DRAGON -> "§4🐉 Dragon Formation";
            case PHOENIX -> "§6🔥 Phoenix Formation";
            case CELESTIAL -> "§d✦ Celestial Formation";
            case ANCIENT_RUNE -> "§5ᚱ Ancient Rune Formation";
            case NONE -> "§7No Formation";
        };
    }
    
    /**
     * Gets required XP for a formation tier.
     */
    public static int getRequiredXP(FormationType type) {
        return switch (type) {
            case LINE, CORNER, L_SHAPE -> XP_TIER_BASIC;
            case T_SHAPE, CROSS, SQUARE -> XP_TIER_MEDIUM;
            case DIAMOND, STAR, SPIRAL, ARROW -> XP_TIER_ADVANCED;
            case PENTAGON, HEXAGON, OCTAGON, HEART -> XP_TIER_MASTER;
            case YIN_YANG, INFINITY, SACRED_GEOMETRY, CROWN -> XP_TIER_LEGENDARY;
            case DRAGON, PHOENIX, CELESTIAL, ANCIENT_RUNE -> XP_TIER_MYTHIC;
            default -> 0;
        };
    }
    
    /**
     * Enum for formation types with base star bonuses.
     */
    public enum FormationType {
        NONE(0, 0),
        // Basic (0 XP)
        LINE(1, 1),
        CORNER(1, 1),
        L_SHAPE(1, 1),
        // Medium (500 XP)
        T_SHAPE(2, 1),
        CROSS(2, 2),
        SQUARE(2, 1),
        // Advanced (1500 XP)
        DIAMOND(3, 2),
        STAR(3, 2),
        SPIRAL(3, 2),
        ARROW(3, 2),
        // Master (3000 XP)
        PENTAGON(4, 2),
        HEXAGON(4, 3),
        OCTAGON(4, 3),
        HEART(4, 2),
        // Legendary (5000 XP)
        YIN_YANG(5, 3),
        INFINITY(5, 3),
        SACRED_GEOMETRY(5, 4),
        CROWN(5, 3),
        // Mythic (10000 XP)
        DRAGON(6, 4),
        PHOENIX(6, 4),
        CELESTIAL(6, 5),
        ANCIENT_RUNE(6, 4),
        // Secret Formation (HIDDEN - No XP requirement but extremely rare)
        DEMON_666(7, 1); // Tier 7, +1 star (grants demon bonus)
        
        private final int tier;
        private final int baseStarBonus;
        
        FormationType(int tier, int baseStarBonus) {
            this.tier = tier;
            this.baseStarBonus = baseStarBonus;
        }
        
        public int getTier() {
            return tier;
        }
        
        public int getBaseStarBonus() {
            return baseStarBonus;
        }
        
        public int getBonusWeight() {
            return tier;
        }
    }
    
    /**
     * Class to hold formation effect data.
     */
    public static class FormationEffect {
        private final FormationType formation;
        private final int farmingXP;
        
        public FormationEffect(FormationType formation, int farmingXP) {
            this.formation = formation;
            this.farmingXP = farmingXP;
        }
        
        public FormationType getFormation() {
            return formation;
        }
        
        public int getFarmingXP() {
            return farmingXP;
        }
    }
}
