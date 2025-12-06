package com.budlords.farming;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.skills.Skill;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages formation bonuses for plants.
 * When same-strain plants are arranged in specific patterns (like L-shape),
 * they can boost each other's star rating based on the player's farming skill.
 */
public class FormationManager {

    private final BudLords plugin;
    private final FarmingManager farmingManager;
    
    // Formation bonus configuration constants
    private static final double BASE_SUCCESS_CHANCE = 0.1;        // 10% base chance
    private static final double MAX_SUCCESS_CHANCE = 0.5;         // 50% cap
    private static final double XP_SUCCESS_DIVISOR = 100.0;       // XP per chance increment
    private static final double XP_SUCCESS_BONUS = 0.05;          // +5% per increment
    private static final int MAX_STAR_BOOST = 3;                  // Maximum bonus stars
    private static final int XP_PER_STAR_BOOST = 200;             // XP needed for +1 max boost

    // Formation patterns - relative positions from a center plant
    // L-shape pattern variants
    private static final int[][][] L_FORMATIONS = {
        // Standard L (facing right-down)
        {{1, 0}, {0, 1}},
        // L rotated 90° (facing left-down)
        {{-1, 0}, {0, 1}},
        // L rotated 180° (facing left-up)
        {{-1, 0}, {0, -1}},
        // L rotated 270° (facing right-up)
        {{1, 0}, {0, -1}},
        // Mirrored L variants
        {{0, 1}, {1, 0}},
        {{0, 1}, {-1, 0}},
        {{0, -1}, {1, 0}},
        {{0, -1}, {-1, 0}}
    };
    
    // T-shape pattern variants (4 plants needed)
    private static final int[][][] T_FORMATIONS = {
        {{0, 1}, {1, 0}, {-1, 0}}, // T facing down
        {{0, -1}, {1, 0}, {-1, 0}}, // T facing up
        {{1, 0}, {0, 1}, {0, -1}}, // T facing right
        {{-1, 0}, {0, 1}, {0, -1}}  // T facing left
    };
    
    // Line pattern (3 plants in a row)
    private static final int[][][] LINE_FORMATIONS = {
        {{1, 0}, {2, 0}},  // Horizontal line
        {{-1, 0}, {-2, 0}},
        {{0, 1}, {0, 2}},  // Vertical line
        {{0, -1}, {0, -2}}
    };
    
    // Square corner pattern (3 plants forming a corner)
    private static final int[][][] CORNER_FORMATIONS = {
        {{1, 0}, {1, 1}},
        {{-1, 0}, {-1, 1}},
        {{1, 0}, {1, -1}},
        {{-1, 0}, {-1, -1}},
        {{0, 1}, {1, 1}},
        {{0, -1}, {1, -1}},
        {{0, 1}, {-1, 1}},
        {{0, -1}, {-1, -1}}
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
        
        // Find formation type this plant is part of
        FormationType formation = detectFormation(plantLoc, strainId);
        
        if (formation == FormationType.NONE) {
            return 0;
        }
        
        // Get player's farming skill level for success chance calculation
        int farmingXP = 0;
        int maxStarBoost = 1; // Default max boost
        double baseSuccessChance = BASE_SUCCESS_CHANCE;
        
        if (plugin.getSkillManager() != null) {
            farmingXP = plugin.getSkillManager().getTreeXP(ownerUuid, Skill.SkillTree.FARMING);
            
            // Higher farming XP = better chance and higher max boost
            baseSuccessChance = Math.min(MAX_SUCCESS_CHANCE, 
                BASE_SUCCESS_CHANCE + (farmingXP / XP_SUCCESS_DIVISOR) * XP_SUCCESS_BONUS);
            maxStarBoost = Math.min(MAX_STAR_BOOST, 1 + farmingXP / XP_PER_STAR_BOOST);
        }
        
        // Better formations have higher success chance
        double formationMultiplier = switch (formation) {
            case L_SHAPE -> 1.0;      // Standard L = base chance
            case T_SHAPE -> 1.3;      // T = 30% bonus
            case LINE -> 1.1;         // Line = 10% bonus
            case CORNER -> 1.2;       // Corner = 20% bonus
            default -> 0;
        };
        
        double successChance = baseSuccessChance * formationMultiplier;
        
        // Roll for success
        if (ThreadLocalRandom.current().nextDouble() > successChance) {
            return 0; // Formation bonus didn't trigger
        }
        
        // Determine how many bonus stars to give (1 to maxStarBoost)
        // Higher formations have better chances of giving more stars
        int bonusStars = 1;
        for (int i = 2; i <= maxStarBoost; i++) {
            // Each additional star has progressively lower chance
            double additionalChance = successChance / (i * 2);
            if (ThreadLocalRandom.current().nextDouble() < additionalChance) {
                bonusStars = i;
            }
        }
        
        return bonusStars;
    }
    
    /**
     * Detects what formation pattern (if any) a plant is part of.
     * @param plantLoc The location of the plant
     * @param strainId The strain ID to match
     * @return The detected formation type
     */
    public FormationType detectFormation(Location plantLoc, String strainId) {
        // Check T-shape first (more valuable)
        if (matchesFormation(plantLoc, strainId, T_FORMATIONS)) {
            return FormationType.T_SHAPE;
        }
        
        // Check L-shape
        if (matchesFormation(plantLoc, strainId, L_FORMATIONS)) {
            return FormationType.L_SHAPE;
        }
        
        // Check corner
        if (matchesFormation(plantLoc, strainId, CORNER_FORMATIONS)) {
            return FormationType.CORNER;
        }
        
        // Check line
        if (matchesFormation(plantLoc, strainId, LINE_FORMATIONS)) {
            return FormationType.LINE;
        }
        
        return FormationType.NONE;
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
     * @param plantLoc The location of the center plant
     * @param strainId The strain to match
     * @return List of plants in the formation (including the center plant)
     */
    public List<Plant> getFormationPlants(Location plantLoc, String strainId) {
        List<Plant> formationPlants = new ArrayList<>();
        
        // Add the center plant
        Plant centerPlant = farmingManager.getPlantAt(plantLoc);
        if (centerPlant != null) {
            formationPlants.add(centerPlant);
        }
        
        // Check all patterns to find matching neighbors
        int[][][][] allPatterns = {L_FORMATIONS, T_FORMATIONS, LINE_FORMATIONS, CORNER_FORMATIONS};
        
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
     * Gets a display string for a formation type.
     */
    public static String getFormationDisplay(FormationType type) {
        return switch (type) {
            case L_SHAPE -> "§a⌐ L-Shape Formation";
            case T_SHAPE -> "§6⊤ T-Shape Formation";
            case LINE -> "§e━ Line Formation";
            case CORNER -> "§b⌐ Corner Formation";
            case NONE -> "§7No Formation";
        };
    }
    
    /**
     * Enum for formation types.
     */
    public enum FormationType {
        NONE(0),
        LINE(1),
        CORNER(1),
        L_SHAPE(2),
        T_SHAPE(3);
        
        private final int bonusWeight;
        
        FormationType(int bonusWeight) {
            this.bonusWeight = bonusWeight;
        }
        
        public int getBonusWeight() {
            return bonusWeight;
        }
    }
}
