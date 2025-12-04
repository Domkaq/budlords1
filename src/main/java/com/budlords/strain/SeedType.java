package com.budlords.strain;

import org.bukkit.Material;

/**
 * Defines special seed types with unique growing properties and bonuses.
 * Part of the v2.0.0 Major Update - adds variety and strategy to growing.
 */
public enum SeedType {
    
    // === STANDARD SEEDS ===
    REGULAR("Regular", "§7🌱", Material.WHEAT_SEEDS,
        "Standard seed with balanced properties",
        1.0, 1.0, 1.0, 0.0, SeasonBonus.NONE),
    
    // === CLIMATE-ADAPTED SEEDS ===
    ARCTIC("Arctic", "§b❄", Material.WHEAT_SEEDS,
        "Thrives in cold conditions, frost resistant",
        1.0, 0.9, 1.2, 0.15, SeasonBonus.WINTER),
    
    TROPICAL("Tropical", "§a🌴", Material.WHEAT_SEEDS,
        "Loves heat and humidity, summer grower",
        1.1, 1.1, 1.0, 0.1, SeasonBonus.SUMMER),
    
    DESERT("Desert", "§e🏜", Material.WHEAT_SEEDS,
        "Drought resistant, needs minimal water",
        0.9, 0.85, 1.1, 0.0, SeasonBonus.SUMMER),
    
    RAINFOREST("Rainforest", "§2🌧", Material.WHEAT_SEEDS,
        "Thrives in wet conditions, high yield",
        1.3, 1.0, 1.0, 0.0, SeasonBonus.SPRING),
    
    MOUNTAIN("Mountain", "§f⛰", Material.WHEAT_SEEDS,
        "High altitude adapted, potency boost",
        1.0, 1.15, 0.95, 0.05, SeasonBonus.AUTUMN),
    
    // === SPECIAL GROWTH SEEDS ===
    QUICK_BLOOM("Quick Bloom", "§a⚡", Material.WHEAT_SEEDS,
        "Grows 50% faster but lower yield",
        0.75, 1.0, 1.5, 0.0, SeasonBonus.SPRING),
    
    SLOW_BURN("Slow Burn", "§6🔥", Material.WHEAT_SEEDS,
        "Grows slower but extremely high quality",
        1.2, 1.3, 0.6, 0.1, SeasonBonus.NONE),
    
    MEGA_YIELD("Mega Yield", "§e💰", Material.WHEAT_SEEDS,
        "Produces extra buds per harvest",
        1.5, 0.85, 0.9, 0.0, SeasonBonus.SUMMER),
    
    POTENCY_PLUS("Potency Plus", "§5💪", Material.WHEAT_SEEDS,
        "Enhanced THC production genetics",
        0.9, 1.35, 1.0, 0.08, SeasonBonus.AUTUMN),
    
    // === EXOTIC SEEDS ===
    LUNAR("Lunar", "§f🌙", Material.WHEAT_SEEDS,
        "Grows better at night, moonlight bonus",
        1.1, 1.2, 1.1, 0.12, SeasonBonus.WINTER),
    
    SOLAR("Solar", "§e☀", Material.WHEAT_SEEDS,
        "Maximum daytime growth, sun powered",
        1.15, 1.1, 1.2, 0.1, SeasonBonus.SUMMER),
    
    MYSTIC("Mystic", "§d✨", Material.WHEAT_SEEDS,
        "Magical properties, chance for rare mutations",
        1.0, 1.15, 1.0, 0.25, SeasonBonus.NONE),
    
    HYBRID("Hybrid", "§a🔬", Material.WHEAT_SEEDS,
        "Lab-engineered for balanced excellence",
        1.2, 1.2, 1.1, 0.05, SeasonBonus.NONE),
    
    ANCIENT("Ancient", "§6📜", Material.WHEAT_SEEDS,
        "Heritage genetics, traditional excellence",
        1.0, 1.25, 0.85, 0.15, SeasonBonus.AUTUMN),
    
    // === PREMIUM SEEDS ===
    CRYSTALLINE("Crystalline", "§b💎", Material.WHEAT_SEEDS,
        "Produces crystal-coated premium buds",
        1.1, 1.4, 0.9, 0.18, SeasonBonus.WINTER),
    
    GOLDEN("Golden", "§6🏆", Material.WHEAT_SEEDS,
        "Legendary genetics, competition winner",
        1.25, 1.35, 1.0, 0.2, SeasonBonus.SUMMER),
    
    PHANTOM("Phantom", "§8👻", Material.WHEAT_SEEDS,
        "Rare ghostly strain, nearly invisible growth",
        1.0, 1.3, 1.1, 0.22, SeasonBonus.AUTUMN),
    
    RAINBOW("Rainbow", "§d🌈", Material.WHEAT_SEEDS,
        "Multi-colored buds with unique visual effects",
        1.15, 1.15, 1.05, 0.15, SeasonBonus.SPRING),
    
    VOLCANIC("Volcanic", "§c🌋", Material.WHEAT_SEEDS,
        "Fire-resistant, thrives near lava",
        1.0, 1.25, 0.95, 0.12, SeasonBonus.SUMMER),
    
    // === LEGENDARY SEEDS ===
    CELESTIAL("Celestial", "§f⭐", Material.WHEAT_SEEDS,
        "Cosmic genetics from the stars",
        1.3, 1.4, 1.0, 0.3, SeasonBonus.NONE),
    
    DRAGON_SCALE("Dragon Scale", "§5🐉", Material.WHEAT_SEEDS,
        "Mythical properties, extreme effects",
        1.2, 1.5, 0.8, 0.35, SeasonBonus.AUTUMN),
    
    PHOENIX("Phoenix", "§6🔥", Material.WHEAT_SEEDS,
        "Reborn from ashes, self-healing properties",
        1.15, 1.35, 1.05, 0.28, SeasonBonus.SUMMER),
    
    VOID_WALKER("Void Walker", "§0🕳", Material.WHEAT_SEEDS,
        "Dimension-crossing genetics, rare and powerful",
        1.1, 1.45, 0.9, 0.4, SeasonBonus.WINTER),
    
    TIME_WARP("Time Warp", "§e⏰", Material.WHEAT_SEEDS,
        "Temporal properties, unpredictable growth",
        1.4, 1.3, 1.3, 0.35, SeasonBonus.NONE);

    private final String displayName;
    private final String symbol;
    private final Material material;
    private final String description;
    private final double yieldMultiplier;
    private final double potencyMultiplier;
    private final double growthSpeedMultiplier;
    private final double mutationChance;
    private final SeasonBonus preferredSeason;

    SeedType(String displayName, String symbol, Material material, String description,
             double yieldMultiplier, double potencyMultiplier, double growthSpeedMultiplier,
             double mutationChance, SeasonBonus preferredSeason) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.material = material;
        this.description = description;
        this.yieldMultiplier = yieldMultiplier;
        this.potencyMultiplier = potencyMultiplier;
        this.growthSpeedMultiplier = growthSpeedMultiplier;
        this.mutationChance = mutationChance;
        this.preferredSeason = preferredSeason;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDescription() {
        return description;
    }

    public double getYieldMultiplier() {
        return yieldMultiplier;
    }

    public double getPotencyMultiplier() {
        return potencyMultiplier;
    }

    public double getGrowthSpeedMultiplier() {
        return growthSpeedMultiplier;
    }

    public double getMutationChance() {
        return mutationChance;
    }

    public SeasonBonus getPreferredSeason() {
        return preferredSeason;
    }

    /**
     * Gets the colored display with symbol.
     */
    public String getColoredDisplay() {
        String color = switch (this.getRarity()) {
            case LEGENDARY -> "§6§l";
            case EXOTIC -> "§d";
            case PREMIUM -> "§b";
            case SPECIAL -> "§a";
            default -> "§7";
        };
        return color + symbol + " " + displayName;
    }

    /**
     * Gets the rarity tier of this seed type.
     */
    public SeedRarity getRarity() {
        return switch (this) {
            case REGULAR -> SeedRarity.COMMON;
            case ARCTIC, TROPICAL, DESERT, RAINFOREST, MOUNTAIN -> SeedRarity.UNCOMMON;
            case QUICK_BLOOM, SLOW_BURN, MEGA_YIELD, POTENCY_PLUS -> SeedRarity.SPECIAL;
            case LUNAR, SOLAR, MYSTIC, HYBRID, ANCIENT -> SeedRarity.EXOTIC;
            case CRYSTALLINE, GOLDEN, PHANTOM, RAINBOW, VOLCANIC -> SeedRarity.PREMIUM;
            case CELESTIAL, DRAGON_SCALE, PHOENIX, VOID_WALKER, TIME_WARP -> SeedRarity.LEGENDARY;
        };
    }

    /**
     * Gets the base price for this seed type.
     */
    public double getBasePrice() {
        return switch (getRarity()) {
            case COMMON -> 50;
            case UNCOMMON -> 150;
            case SPECIAL -> 400;
            case EXOTIC -> 1000;
            case PREMIUM -> 2500;
            case LEGENDARY -> 10000;
        };
    }

    /**
     * Checks if this seed type benefits from the current season.
     */
    public boolean benefitsFromSeason(Season season) {
        return preferredSeason != SeasonBonus.NONE && 
               preferredSeason.name().equals(season.name());
    }

    /**
     * Gets the seasonal bonus multiplier.
     */
    public double getSeasonalBonus(Season season) {
        if (benefitsFromSeason(season)) {
            return 1.25; // 25% bonus in preferred season
        }
        return 1.0;
    }

    /**
     * Seed rarity tiers.
     */
    public enum SeedRarity {
        COMMON("§7Common", 1.0),
        UNCOMMON("§aUncommon", 1.2),
        SPECIAL("§eSpecial", 1.5),
        EXOTIC("§dExotic", 2.0),
        PREMIUM("§bPremium", 3.0),
        LEGENDARY("§6§lLegendary", 5.0);

        private final String display;
        private final double priceMultiplier;

        SeedRarity(String display, double priceMultiplier) {
            this.display = display;
            this.priceMultiplier = priceMultiplier;
        }

        public String getDisplay() {
            return display;
        }

        public double getPriceMultiplier() {
            return priceMultiplier;
        }
    }

    /**
     * Season bonus enum for seed types.
     */
    public enum SeasonBonus {
        NONE,
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER
    }

    /**
     * Seasons for the game.
     */
    public enum Season {
        SPRING("§a🌸 Spring", "§a", 1.1, 1.0, 1.0),
        SUMMER("§e☀ Summer", "§e", 1.0, 1.0, 1.15),
        AUTUMN("§6🍂 Autumn", "§6", 1.15, 1.1, 0.95),
        WINTER("§b❄ Winter", "§b", 0.9, 1.15, 0.85);

        private final String displayName;
        private final String color;
        private final double qualityMultiplier;
        private final double potencyMultiplier;
        private final double growthMultiplier;

        Season(String displayName, String color, double qualityMultiplier, 
               double potencyMultiplier, double growthMultiplier) {
            this.displayName = displayName;
            this.color = color;
            this.qualityMultiplier = qualityMultiplier;
            this.potencyMultiplier = potencyMultiplier;
            this.growthMultiplier = growthMultiplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        public double getQualityMultiplier() {
            return qualityMultiplier;
        }

        public double getPotencyMultiplier() {
            return potencyMultiplier;
        }

        public double getGrowthMultiplier() {
            return growthMultiplier;
        }

        /**
         * Gets the next season in cycle.
         */
        public Season next() {
            Season[] seasons = values();
            return seasons[(this.ordinal() + 1) % seasons.length];
        }
    }
}
