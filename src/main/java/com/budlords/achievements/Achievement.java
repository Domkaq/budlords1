package com.budlords.achievements;

import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * Defines all achievements in BudLords v2.0.0.
 * Achievements provide goals and unlock special rewards.
 */
public enum Achievement {
    
    // ===== FARMING ACHIEVEMENTS =====
    FIRST_HARVEST("First Harvest", "§a🌱", Material.WHEAT_SEEDS,
        "Harvest your first plant",
        AchievementCategory.FARMING, 1, 50, 10),
    
    GREEN_THUMB("Green Thumb", "§a🌿", Material.GREEN_DYE,
        "Harvest 50 plants",
        AchievementCategory.FARMING, 50, 500, 50),
    
    MASTER_GARDENER("Master Gardener", "§2🌳", Material.OAK_SAPLING,
        "Harvest 500 plants",
        AchievementCategory.FARMING, 500, 5000, 500),
    
    LEGENDARY_GROWER("Legendary Grower", "§6§l🏆", Material.GOLDEN_APPLE,
        "Harvest 5000 plants",
        AchievementCategory.FARMING, 5000, 50000, 5000),
    
    PERFECT_HARVEST("Perfect Harvest", "§e⭐", Material.NETHER_STAR,
        "Get a 5-star bud",
        AchievementCategory.FARMING, 1, 200, 20),
    
    QUALITY_CONTROL("Quality Control", "§e★", Material.GOLD_INGOT,
        "Harvest 10 five-star buds",
        AchievementCategory.FARMING, 10, 1000, 100),
    
    STAR_COLLECTOR("Star Collector", "§6✦", Material.GLOWSTONE_DUST,
        "Harvest 100 five-star buds",
        AchievementCategory.FARMING, 100, 10000, 1000),
    
    // ===== STRAIN ACHIEVEMENTS =====
    STRAIN_DISCOVERER("Strain Discoverer", "§d🔬", Material.BREWING_STAND,
        "Create your first custom strain",
        AchievementCategory.STRAINS, 1, 100, 15),
    
    CROSSBREEDER("Crossbreeder", "§5🧬", Material.POTION,
        "Crossbreed 10 strains",
        AchievementCategory.STRAINS, 10, 1500, 150),
    
    GENETICIST("Geneticist", "§d§l🧪", Material.EXPERIENCE_BOTTLE,
        "Crossbreed 50 strains",
        AchievementCategory.STRAINS, 50, 7500, 750),
    
    STRAIN_MASTER("Strain Master", "§6§l🌟", Material.DRAGON_BREATH,
        "Own all legendary strains",
        AchievementCategory.STRAINS, 4, 25000, 2500),
    
    MUTATION_HUNTER("Mutation Hunter", "§c☢", Material.SPIDER_EYE,
        "Get a mutation during crossbreeding",
        AchievementCategory.STRAINS, 1, 300, 30),
    
    RARE_BREEDER("Rare Breeder", "§9💎", Material.DIAMOND,
        "Get 10 mutations",
        AchievementCategory.STRAINS, 10, 3000, 300),
    
    // ===== ECONOMY ACHIEVEMENTS =====
    FIRST_SALE("First Sale", "§e💰", Material.EMERALD,
        "Complete your first sale",
        AchievementCategory.ECONOMY, 1, 25, 5),
    
    ENTREPRENEUR("Entrepreneur", "§e📈", Material.GOLD_NUGGET,
        "Earn $10,000",
        AchievementCategory.ECONOMY, 10000, 1000, 100),
    
    BUSINESSMAN("Businessman", "§6💼", Material.GOLD_INGOT,
        "Earn $100,000",
        AchievementCategory.ECONOMY, 100000, 10000, 1000),
    
    MILLIONAIRE("Millionaire", "§6§l🤑", Material.GOLD_BLOCK,
        "Earn $1,000,000",
        AchievementCategory.ECONOMY, 1000000, 100000, 10000),
    
    KINGPIN("Kingpin", "§4👑", Material.NETHERITE_INGOT,
        "Reach Kingpin rank",
        AchievementCategory.ECONOMY, 1, 5000, 500),
    
    BUDLORD("BudLord", "§6§l♔", Material.BEACON,
        "Reach BudLord rank",
        AchievementCategory.ECONOMY, 1, 25000, 2500),
    
    // ===== TRADING ACHIEVEMENTS =====
    TRADER("Trader", "§a🤝", Material.EMERALD,
        "Complete 10 successful trades",
        AchievementCategory.TRADING, 10, 200, 20),
    
    DEALER("Dealer", "§e💵", Material.PAPER,
        "Complete 100 trades",
        AchievementCategory.TRADING, 100, 2000, 200),
    
    BLACK_MARKET_MASTER("Black Market Master", "§8🏴", Material.BLACK_WOOL,
        "Complete 50 black market trades",
        AchievementCategory.TRADING, 50, 5000, 500),
    
    LUCKY_STREAK("Lucky Streak", "§a🍀", Material.EMERALD,
        "Complete 10 trades in a row without failure",
        AchievementCategory.TRADING, 10, 1500, 150),
    
    // ===== ROLLING ACHIEVEMENTS =====
    FIRST_ROLL("First Roll", "§e🚬", Material.PAPER,
        "Roll your first joint",
        AchievementCategory.ROLLING, 1, 50, 5),
    
    ROLLER("Roller", "§a💨", Material.PAPER,
        "Roll 50 joints",
        AchievementCategory.ROLLING, 50, 500, 50),
    
    MASTER_ROLLER("Master Roller", "§6🌀", Material.FIREWORK_STAR,
        "Roll 500 joints",
        AchievementCategory.ROLLING, 500, 5000, 500),
    
    PERFECT_ROLL("Perfect Roll", "§d✨", Material.FIREWORK_ROCKET,
        "Get a perfect score on the rolling minigame",
        AchievementCategory.ROLLING, 1, 200, 20),
    
    // ===== CHALLENGE ACHIEVEMENTS =====
    CHALLENGER("Challenger", "§e📋", Material.PAPER,
        "Complete 10 daily challenges",
        AchievementCategory.CHALLENGES, 10, 500, 50),
    
    DAILY_DEVOTEE("Daily Devotee", "§a☀", Material.SUNFLOWER,
        "Complete 100 daily challenges",
        AchievementCategory.CHALLENGES, 100, 5000, 500),
    
    WEEKLY_WARRIOR("Weekly Warrior", "§9📅", Material.CLOCK,
        "Complete 10 weekly challenges",
        AchievementCategory.CHALLENGES, 10, 2000, 200),
    
    CHALLENGE_MASTER("Challenge Master", "§6§l🏅", Material.TOTEM_OF_UNDYING,
        "Complete 500 total challenges",
        AchievementCategory.CHALLENGES, 500, 25000, 2500),
    
    // ===== PRESTIGE ACHIEVEMENTS =====
    FIRST_PRESTIGE("First Prestige", "§5✦", Material.AMETHYST_SHARD,
        "Reach Prestige 1",
        AchievementCategory.PRESTIGE, 1, 10000, 1000),
    
    HIGH_PRESTIGE("High Prestige", "§d✦✦", Material.AMETHYST_CLUSTER,
        "Reach Prestige 5",
        AchievementCategory.PRESTIGE, 5, 50000, 5000),
    
    MAX_PRESTIGE("Max Prestige", "§6§l✦✦✦", Material.AMETHYST_BLOCK,
        "Reach Prestige 10",
        AchievementCategory.PRESTIGE, 10, 250000, 25000),
    
    // ===== SPECIAL ACHIEVEMENTS =====
    NIGHT_OWL("Night Owl", "§8🦉", Material.PHANTOM_MEMBRANE,
        "Harvest during nighttime",
        AchievementCategory.SPECIAL, 10, 300, 30),
    
    STORM_FARMER("Storm Farmer", "§9⛈", Material.LIGHTNING_ROD,
        "Harvest during a thunderstorm",
        AchievementCategory.SPECIAL, 5, 500, 50),
    
    SEASONAL_EXPERT("Seasonal Expert", "§a🍃", Material.AZALEA_LEAVES,
        "Harvest in all 4 seasons",
        AchievementCategory.SPECIAL, 4, 1000, 100),
    
    DISEASE_DOCTOR("Disease Doctor", "§c💉", Material.POTION,
        "Cure 10 plant diseases",
        AchievementCategory.SPECIAL, 10, 800, 80),
    
    SURVIVOR("Survivor", "§4☠", Material.WITHER_ROSE,
        "Survive 5 police raids",
        AchievementCategory.SPECIAL, 5, 2000, 200),
    
    EVENT_MASTER("Event Master", "§e⚡", Material.NETHER_STAR,
        "Experience all random events",
        AchievementCategory.SPECIAL, 6, 1500, 150),
    
    // ===== LEGENDARY ACHIEVEMENTS =====
    COMPLETIONIST("Completionist", "§6§l🎖", Material.NETHERITE_BLOCK,
        "Unlock all other achievements",
        AchievementCategory.LEGENDARY, 1, 500000, 50000),
    
    ULTIMATE_BUDLORD("Ultimate BudLord", "§6§l👑", Material.DRAGON_EGG,
        "Max prestige with $10,000,000 earned",
        AchievementCategory.LEGENDARY, 1, 1000000, 100000);

    private final String displayName;
    private final String symbol;
    private final Material iconMaterial;
    private final String description;
    private final AchievementCategory category;
    private final int requirement;
    private final double rewardMoney;
    private final int rewardXP;

    Achievement(String displayName, String symbol, Material iconMaterial, String description,
                AchievementCategory category, int requirement, double rewardMoney, int rewardXP) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.iconMaterial = iconMaterial;
        this.description = description;
        this.category = category;
        this.requirement = requirement;
        this.rewardMoney = rewardMoney;
        this.rewardXP = rewardXP;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public String getDescription() {
        return description;
    }

    public AchievementCategory getCategory() {
        return category;
    }

    public int getRequirement() {
        return requirement;
    }

    public double getRewardMoney() {
        return rewardMoney;
    }

    public int getRewardXP() {
        return rewardXP;
    }

    /**
     * Gets the colored display with symbol.
     */
    public String getColoredDisplay() {
        return symbol + " " + category.getColor() + displayName;
    }

    /**
     * Gets the unlock sound based on rarity.
     */
    public Sound getUnlockSound() {
        return switch (category) {
            case LEGENDARY -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case PRESTIGE -> Sound.ENTITY_ENDER_DRAGON_DEATH;
            case SPECIAL -> Sound.ENTITY_PLAYER_LEVELUP;
            default -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
    }

    /**
     * Gets the rarity based on rewards.
     */
    public AchievementRarity getRarity() {
        if (rewardMoney >= 50000) return AchievementRarity.LEGENDARY;
        if (rewardMoney >= 10000) return AchievementRarity.EPIC;
        if (rewardMoney >= 2000) return AchievementRarity.RARE;
        if (rewardMoney >= 500) return AchievementRarity.UNCOMMON;
        return AchievementRarity.COMMON;
    }

    /**
     * Achievement categories.
     */
    public enum AchievementCategory {
        FARMING("§a", "Farming", "Growing and harvesting achievements"),
        STRAINS("§d", "Strains", "Strain creation and breeding"),
        ECONOMY("§e", "Economy", "Money and ranks"),
        TRADING("§6", "Trading", "Sales and trades"),
        ROLLING("§b", "Rolling", "Joint rolling"),
        CHALLENGES("§9", "Challenges", "Daily and weekly challenges"),
        PRESTIGE("§5", "Prestige", "Prestige progression"),
        SPECIAL("§c", "Special", "Unique achievements"),
        LEGENDARY("§6§l", "Legendary", "Ultimate achievements");

        private final String color;
        private final String displayName;
        private final String description;

        AchievementCategory(String color, String displayName, String description) {
            this.color = color;
            this.displayName = displayName;
            this.description = description;
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
    }

    /**
     * Achievement rarity levels.
     */
    public enum AchievementRarity {
        COMMON("§7Common"),
        UNCOMMON("§aUncommon"),
        RARE("§9Rare"),
        EPIC("§5Epic"),
        LEGENDARY("§6§lLegendary");

        private final String display;

        AchievementRarity(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
}
