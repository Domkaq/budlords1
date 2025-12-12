package com.budlords.skills;

import org.bukkit.Material;

/**
 * Defines all skills in the skill tree for BudLords v2.0.0.
 * Skills provide permanent passive bonuses as players progress.
 */
public enum Skill {
    
    // ===== FARMING TREE =====
    // Tier 1 - Basic Farming Skills
    QUICK_HARVEST("Quick Harvest", "§a", Material.GOLDEN_HOE,
        "15% faster harvest (15% fewer minigame rounds)", SkillTree.FARMING, 1, 0, 1,
        new SkillBonus(BonusType.MINIGAME_SPEED, 0.15)),
    
    HARVEST_BONUS("Harvest Bonus", "§e", Material.GOLD_INGOT,
        "+5% sale price from all harvests", SkillTree.FARMING, 1, 0, 2,
        new SkillBonus(BonusType.PRICE_BONUS, 0.05)),
    
    GREEN_FINGERS("Green Fingers", "§a", Material.GREEN_DYE,
        "Plants grow 5% faster", SkillTree.FARMING, 1, 0, 3,
        new SkillBonus(BonusType.GROWTH_SPEED, 0.05)),
    
    // Tier 2 - Advanced Farming
    RAPID_HARVEST("Rapid Harvest", "§e", Material.IRON_HOE,
        "35% faster harvest (35% fewer minigame rounds)", SkillTree.FARMING, 2, 50, 1,
        new SkillBonus(BonusType.MINIGAME_SPEED, 0.35)),
    
    INSTANT_HARVEST("Instant Harvest", "§6", Material.DIAMOND_HOE,
        "Skip harvest minigame completely!", SkillTree.FARMING, 2, 75, 2,
        new SkillBonus(BonusType.SKIP_MINIGAME, 1)),
    
    ACCELERATED_GROWTH("Accelerated Growth", "§2", Material.BONE_MEAL,
        "Plants grow 10% faster", SkillTree.FARMING, 2, 50, 3,
        new SkillBonus(BonusType.GROWTH_SPEED, 0.10)),
    
    // Tier 3 - Master Farming
    WATER_EFFICIENCY("Water Efficiency", "§b", Material.WATER_BUCKET,
        "Plants need 25% less water", SkillTree.FARMING, 3, 200, 1,
        new SkillBonus(BonusType.WATER_EFFICIENCY, 0.25)),
    
    MASTER_FARMER("Master Farmer", "§6", Material.NETHERITE_HOE,
        "+1 bonus bud per harvest", SkillTree.FARMING, 3, 500, 2,
        new SkillBonus(BonusType.YIELD_BONUS, 1)),
    
    NATURE_BLESSING("Nature's Blessing", "§2", Material.EMERALD,
        "5% chance for instant growth stage", SkillTree.FARMING, 3, 500, 3,
        new SkillBonus(BonusType.INSTANT_GROWTH_CHANCE, 0.05)),
    
    // ===== QUALITY TREE =====
    // Tier 1
    QUALITY_FOCUS("Quality Focus", "§e", Material.GOLD_NUGGET,
        "+5% quality bonus", SkillTree.QUALITY, 1, 0, 1,
        new SkillBonus(BonusType.QUALITY_BONUS, 0.05)),
    
    STAR_SEEKER("Star Seeker", "§e", Material.GLOWSTONE_DUST,
        "5% higher chance for better star rating", SkillTree.QUALITY, 1, 0, 2,
        new SkillBonus(BonusType.STAR_RATING_CHANCE, 0.05)),
    
    NUTRIENT_BOOST("Nutrient Boost", "§a", Material.COMPOSTER,
        "Fertilizers 10% more effective", SkillTree.QUALITY, 1, 0, 3,
        new SkillBonus(BonusType.FERTILIZER_EFFICIENCY, 0.10)),
    
    // Tier 2
    PREMIUM_QUALITY("Premium Quality", "§6", Material.GOLD_INGOT,
        "+10% quality bonus", SkillTree.QUALITY, 2, 100, 1,
        new SkillBonus(BonusType.QUALITY_BONUS, 0.10)),
    
    CONSTELLATION("Constellation", "§e", Material.NETHER_STAR,
        "10% higher chance for 5-star", SkillTree.QUALITY, 2, 100, 2,
        new SkillBonus(BonusType.STAR_RATING_CHANCE, 0.10)),
    
    SUPER_NUTRIENTS("Super Nutrients", "§2", Material.SLIME_BALL,
        "Fertilizers 20% more effective", SkillTree.QUALITY, 2, 100, 3,
        new SkillBonus(BonusType.FERTILIZER_EFFICIENCY, 0.20)),
    
    // Tier 3
    LEGENDARY_QUALITY("Legendary Quality", "§6§l", Material.GOLD_BLOCK,
        "+20% quality bonus, can exceed max", SkillTree.QUALITY, 3, 500, 1,
        new SkillBonus(BonusType.QUALITY_BONUS, 0.20)),
    
    STAR_MASTER("Star Master", "§6§l", Material.BEACON,
        "Guaranteed minimum 3-star", SkillTree.QUALITY, 3, 500, 2,
        new SkillBonus(BonusType.MIN_STAR_RATING, 3)),
    
    // ===== TRADING TREE =====
    // Tier 1
    SMOOTH_TALKER("Smooth Talker", "§b", Material.PAPER,
        "+5% trade success chance", SkillTree.TRADING, 1, 0, 1,
        new SkillBonus(BonusType.TRADE_SUCCESS, 0.05)),
    
    HAGGLER("Haggler", "§e", Material.EMERALD,
        "+5% sale prices", SkillTree.TRADING, 1, 0, 2,
        new SkillBonus(BonusType.PRICE_BONUS, 0.05)),
    
    QUICK_RECOVERY("Quick Recovery", "§9", Material.CLOCK,
        "15% faster cooldown after failed trade", SkillTree.TRADING, 1, 0, 3,
        new SkillBonus(BonusType.COOLDOWN_REDUCTION, 0.15)),
    
    // Tier 2
    NEGOTIATOR("Negotiator", "§6", Material.NAME_TAG,
        "+10% trade success chance", SkillTree.TRADING, 2, 100, 1,
        new SkillBonus(BonusType.TRADE_SUCCESS, 0.10)),
    
    PREMIUM_PRICES("Premium Prices", "§e", Material.GOLD_INGOT,
        "+10% sale prices", SkillTree.TRADING, 2, 100, 2,
        new SkillBonus(BonusType.PRICE_BONUS, 0.10)),
    
    RESILIENCE("Resilience", "§5", Material.OBSIDIAN,
        "30% faster cooldown after failed trade", SkillTree.TRADING, 2, 100, 3,
        new SkillBonus(BonusType.COOLDOWN_REDUCTION, 0.30)),
    
    // Tier 3
    MASTER_DEALER("Master Dealer", "§6§l", Material.DIAMOND,
        "+15% trade success, +15% prices", SkillTree.TRADING, 3, 500, 1,
        new SkillBonus(BonusType.TRADE_SUCCESS, 0.15), new SkillBonus(BonusType.PRICE_BONUS, 0.15)),
    
    BLACK_MARKET_VIP("Black Market VIP", "§8", Material.COAL_BLOCK,
        "25% bonus from black market trades", SkillTree.TRADING, 3, 500, 2,
        new SkillBonus(BonusType.BLACK_MARKET_BONUS, 0.25)),
    
    // ===== GENETICS TREE =====
    // Tier 1
    BASIC_GENETICS("Basic Genetics", "§d", Material.WHEAT_SEEDS,
        "5% better crossbreed outcomes", SkillTree.GENETICS, 1, 0, 1,
        new SkillBonus(BonusType.CROSSBREED_QUALITY, 0.05)),
    
    MUTATION_CHANCE("Mutation Affinity", "§c", Material.SPIDER_EYE,
        "+3% mutation chance", SkillTree.GENETICS, 1, 0, 2,
        new SkillBonus(BonusType.MUTATION_CHANCE, 0.03)),
    
    TRAIT_TRANSFER("Trait Transfer", "§5", Material.EXPERIENCE_BOTTLE,
        "10% better trait inheritance", SkillTree.GENETICS, 1, 0, 3,
        new SkillBonus(BonusType.TRAIT_INHERITANCE, 0.10)),
    
    // Tier 2
    ADVANCED_GENETICS("Advanced Genetics", "§d", Material.GOLDEN_CARROT,
        "10% better crossbreed outcomes", SkillTree.GENETICS, 2, 100, 1,
        new SkillBonus(BonusType.CROSSBREED_QUALITY, 0.10)),
    
    MUTAGEN("Mutagen Expert", "§c", Material.FERMENTED_SPIDER_EYE,
        "+7% mutation chance", SkillTree.GENETICS, 2, 100, 2,
        new SkillBonus(BonusType.MUTATION_CHANCE, 0.07)),
    
    PERFECT_TRANSFER("Perfect Transfer", "§5", Material.ENCHANTED_BOOK,
        "25% better trait inheritance", SkillTree.GENETICS, 2, 100, 3,
        new SkillBonus(BonusType.TRAIT_INHERITANCE, 0.25)),
    
    // Tier 3
    MASTER_GENETICIST("Master Geneticist", "§d§l", Material.DRAGON_BREATH,
        "20% better crossbreed + legendary strain chance", SkillTree.GENETICS, 3, 500, 1,
        new SkillBonus(BonusType.CROSSBREED_QUALITY, 0.20), new SkillBonus(BonusType.LEGENDARY_STRAIN_CHANCE, 0.05)),
    
    EVOLUTION("Evolution Master", "§c§l", Material.END_CRYSTAL,
        "+15% mutation chance, mutations are stronger", SkillTree.GENETICS, 3, 500, 2,
        new SkillBonus(BonusType.MUTATION_CHANCE, 0.15), new SkillBonus(BonusType.MUTATION_STRENGTH, 0.25)),
    
    // ===== EFFECTS TREE =====
    // Tier 1
    EFFECT_DURATION("Effect Duration", "§d", Material.POTION,
        "+15% effect duration", SkillTree.EFFECTS, 1, 0, 1,
        new SkillBonus(BonusType.EFFECT_DURATION, 0.15)),
    
    EFFECT_POTENCY("Effect Potency", "§5", Material.BLAZE_POWDER,
        "+10% effect strength", SkillTree.EFFECTS, 1, 0, 2,
        new SkillBonus(BonusType.EFFECT_STRENGTH, 0.10)),
    
    // Tier 2
    EXTENDED_EFFECTS("Extended Effects", "§d", Material.LINGERING_POTION,
        "+30% effect duration", SkillTree.EFFECTS, 2, 100, 1,
        new SkillBonus(BonusType.EFFECT_DURATION, 0.30)),
    
    POWERFUL_EFFECTS("Powerful Effects", "§5", Material.DRAGON_BREATH,
        "+20% effect strength", SkillTree.EFFECTS, 2, 100, 2,
        new SkillBonus(BonusType.EFFECT_STRENGTH, 0.20)),
    
    // Tier 3
    EFFECT_MASTER("Effect Master", "§6§l", Material.BEACON,
        "+50% duration, +30% strength, bonus effect slot", SkillTree.EFFECTS, 3, 500, 1,
        new SkillBonus(BonusType.EFFECT_DURATION, 0.50), new SkillBonus(BonusType.EFFECT_STRENGTH, 0.30),
        new SkillBonus(BonusType.BONUS_EFFECT_SLOT, 1));

    private final String displayName;
    private final String color;
    private final Material iconMaterial;
    private final String description;
    private final SkillTree tree;
    private final int tier;
    private final int requiredXP;
    private final int position;
    private final SkillBonus[] bonuses;

    Skill(String displayName, String color, Material iconMaterial, String description,
          SkillTree tree, int tier, int requiredXP, int position, SkillBonus... bonuses) {
        this.displayName = displayName;
        this.color = color;
        this.iconMaterial = iconMaterial;
        this.description = description;
        this.tree = tree;
        this.tier = tier;
        this.requiredXP = requiredXP;
        this.position = position;
        this.bonuses = bonuses;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public String getDescription() {
        return description;
    }

    public SkillTree getTree() {
        return tree;
    }

    public int getTier() {
        return tier;
    }

    public int getRequiredXP() {
        return requiredXP;
    }

    public int getPosition() {
        return position;
    }

    public SkillBonus[] getBonuses() {
        return bonuses;
    }

    /**
     * Gets the colored display name.
     */
    public String getColoredName() {
        return color + displayName;
    }

    /**
     * Gets the skill point cost.
     */
    public int getSkillPointCost() {
        return tier;
    }

    /**
     * Skill trees for organization.
     */
    public enum SkillTree {
        FARMING("§a", "Farming", "Improve your growing abilities", Material.DIAMOND_HOE),
        QUALITY("§e", "Quality", "Enhance product quality", Material.GOLD_INGOT),
        TRADING("§6", "Trading", "Master the art of dealing", Material.EMERALD),
        GENETICS("§d", "Genetics", "Unlock crossbreeding power", Material.EXPERIENCE_BOTTLE),
        EFFECTS("§5", "Effects", "Enhance strain effects", Material.POTION);

        private final String color;
        private final String displayName;
        private final String description;
        private final Material icon;

        SkillTree(String color, String displayName, String description, Material icon) {
            this.color = color;
            this.displayName = displayName;
            this.description = description;
            this.icon = icon;
        }

        public String getColor() {
            return color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public Material getIcon() {
            return icon;
        }
    }

    /**
     * Types of skill bonuses.
     */
    public enum BonusType {
        MINIGAME_SPEED,         // Reduces minigame round count
        SKIP_MINIGAME,          // Skips harvest minigame
        HARVEST_SPEED,
        GROWTH_SPEED,
        WATER_EFFICIENCY,
        YIELD_BONUS,
        INSTANT_GROWTH_CHANCE,
        DISEASE_RESISTANCE,
        QUALITY_BONUS,
        STAR_RATING_CHANCE,
        FERTILIZER_EFFICIENCY,
        MIN_STAR_RATING,
        TRADE_SUCCESS,
        PRICE_BONUS,
        COOLDOWN_REDUCTION,
        BLACK_MARKET_BONUS,
        CROSSBREED_QUALITY,
        MUTATION_CHANCE,
        TRAIT_INHERITANCE,
        LEGENDARY_STRAIN_CHANCE,
        MUTATION_STRENGTH,
        EFFECT_DURATION,
        EFFECT_STRENGTH,
        BONUS_EFFECT_SLOT
    }

    /**
     * Represents a bonus from a skill.
     */
    public static class SkillBonus {
        private final BonusType type;
        private final double value;

        public SkillBonus(BonusType type, double value) {
            this.type = type;
            this.value = value;
        }

        public BonusType getType() {
            return type;
        }

        public double getValue() {
            return value;
        }
    }
}
