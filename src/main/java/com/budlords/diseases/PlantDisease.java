package com.budlords.diseases;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Defines plant diseases that can affect growing plants.
 * Part of BudLords v2.0.0 Major Update.
 */
public enum PlantDisease {
    
    // === COMMON DISEASES ===
    ROOT_ROT("Root Rot", "§c🍂", Material.BROWN_DYE,
        "Overwatering has caused root decay",
        0.15, -0.20, -0.15, 0.0, 
        Particle.SMOKE_NORMAL, Sound.BLOCK_GRASS_BREAK,
        DiseaseCategory.FUNGAL, 0.08),
    
    LEAF_BLIGHT("Leaf Blight", "§e🍃", Material.DEAD_BUSH,
        "Brown spots spreading across leaves",
        0.12, -0.15, -0.10, -0.05,
        Particle.SMOKE_NORMAL, Sound.BLOCK_GRASS_BREAK,
        DiseaseCategory.BACTERIAL, 0.10),
    
    NUTRIENT_BURN("Nutrient Burn", "§6🔥", Material.ORANGE_DYE,
        "Over-fertilization has damaged the plant",
        0.20, -0.10, 0.0, -0.15,
        Particle.FLAME, Sound.BLOCK_FIRE_AMBIENT,
        DiseaseCategory.ENVIRONMENTAL, 0.06),
    
    // === UNCOMMON DISEASES ===
    POWDERY_MILDEW("Powdery Mildew", "§f🌫", Material.WHITE_DYE,
        "White powdery coating on leaves",
        0.10, -0.25, -0.20, 0.0,
        Particle.CLOUD, Sound.ENTITY_PHANTOM_AMBIENT,
        DiseaseCategory.FUNGAL, 0.07),
    
    SPIDER_MITES("Spider Mites", "§8🕷", Material.COBWEB,
        "Tiny pests webbing the plant",
        0.08, -0.20, -0.25, -0.10,
        Particle.SMOKE_NORMAL, Sound.ENTITY_SPIDER_AMBIENT,
        DiseaseCategory.PEST, 0.05),
    
    APHID_INFESTATION("Aphid Infestation", "§a🐛", Material.SLIME_BALL,
        "Small green insects draining plant sap",
        0.12, -0.15, -0.30, 0.0,
        Particle.SLIME, Sound.ENTITY_SLIME_SQUISH,
        DiseaseCategory.PEST, 0.06),
    
    HEAT_STRESS("Heat Stress", "§c🌡", Material.MAGMA_CREAM,
        "Excessive heat is wilting the plant",
        0.25, -0.15, -0.20, 0.0,
        Particle.FLAME, Sound.BLOCK_FIRE_AMBIENT,
        DiseaseCategory.ENVIRONMENTAL, 0.04),
    
    // === RARE DISEASES ===
    BOTRYTIS("Botrytis (Bud Rot)", "§5🦠", Material.PURPLE_DYE,
        "Gray mold attacking the buds",
        0.05, -0.40, -0.35, -0.20,
        Particle.SMOKE_LARGE, Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT,
        DiseaseCategory.FUNGAL, 0.03),
    
    FUSARIUM("Fusarium Wilt", "§4☠", Material.NETHER_WART,
        "Deadly fungal infection in roots",
        0.03, -0.50, -0.40, -0.30,
        Particle.SMOKE_LARGE, Sound.ENTITY_WITHER_AMBIENT,
        DiseaseCategory.FUNGAL, 0.02),
    
    THRIPS("Thrips Damage", "§e🪲", Material.YELLOW_DYE,
        "Tiny insects leaving silver trails",
        0.06, -0.20, -0.25, -0.15,
        Particle.CRIT, Sound.ENTITY_SILVERFISH_AMBIENT,
        DiseaseCategory.PEST, 0.04),
    
    LIGHT_BURN("Light Burn", "§e☀", Material.GLOWSTONE_DUST,
        "Too much light intensity",
        0.18, -0.10, -0.15, 0.0,
        Particle.END_ROD, Sound.BLOCK_BEACON_AMBIENT,
        DiseaseCategory.ENVIRONMENTAL, 0.03),
    
    // === LEGENDARY DISEASES ===
    ZOMBIE_FUNGUS("Zombie Fungus", "§2🧟", Material.ROTTEN_FLESH,
        "A rare mutation-causing infection",
        0.01, -0.60, -0.50, 0.50, // High mutation chance!
        Particle.ENCHANTMENT_TABLE, Sound.ENTITY_ZOMBIE_AMBIENT,
        DiseaseCategory.MYSTICAL, 0.005),
    
    CRYSTAL_VIRUS("Crystal Virus", "§b💎", Material.PRISMARINE_CRYSTALS,
        "Rare virus that crystallizes plant tissue",
        0.01, -0.30, 0.50, 0.20,  // Increases quality!
        Particle.END_ROD, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
        DiseaseCategory.MYSTICAL, 0.003);

    private final String displayName;
    private final String symbol;
    private final Material iconMaterial;
    private final String description;
    private final double spreadChance;      // Chance to spread to nearby plants
    private final double growthModifier;    // Effect on growth speed
    private final double qualityModifier;   // Effect on final quality
    private final double mutationModifier;  // Effect on mutation chance
    private final Particle particle;
    private final Sound sound;
    private final DiseaseCategory category;
    private final double infectionChance;   // Base chance per growth tick

    PlantDisease(String displayName, String symbol, Material iconMaterial, String description,
                 double spreadChance, double growthModifier, double qualityModifier, 
                 double mutationModifier, Particle particle, Sound sound,
                 DiseaseCategory category, double infectionChance) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.iconMaterial = iconMaterial;
        this.description = description;
        this.spreadChance = spreadChance;
        this.growthModifier = growthModifier;
        this.qualityModifier = qualityModifier;
        this.mutationModifier = mutationModifier;
        this.particle = particle;
        this.sound = sound;
        this.category = category;
        this.infectionChance = infectionChance;
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

    public double getSpreadChance() {
        return spreadChance;
    }

    public double getGrowthModifier() {
        return growthModifier;
    }

    public double getQualityModifier() {
        return qualityModifier;
    }

    public double getMutationModifier() {
        return mutationModifier;
    }

    public Particle getParticle() {
        return particle;
    }

    public Sound getSound() {
        return sound;
    }

    public DiseaseCategory getCategory() {
        return category;
    }

    public double getInfectionChance() {
        return infectionChance;
    }

    /**
     * Gets the colored display name with symbol.
     */
    public String getColoredDisplay() {
        String color = switch (getSeverity()) {
            case MILD -> "§a";
            case MODERATE -> "§e";
            case SEVERE -> "§c";
            case CRITICAL -> "§4§l";
        };
        return color + symbol + " " + displayName;
    }

    /**
     * Gets the severity level based on effects.
     */
    public DiseaseSeverity getSeverity() {
        double totalImpact = Math.abs(growthModifier) + Math.abs(qualityModifier);
        if (totalImpact < 0.3) return DiseaseSeverity.MILD;
        if (totalImpact < 0.5) return DiseaseSeverity.MODERATE;
        if (totalImpact < 0.8) return DiseaseSeverity.SEVERE;
        return DiseaseSeverity.CRITICAL;
    }

    /**
     * Gets the recommended cure for this disease.
     */
    public Cure getRecommendedCure() {
        return switch (category) {
            case FUNGAL -> Cure.FUNGICIDE;
            case BACTERIAL -> Cure.ANTIBACTERIAL_SPRAY;
            case PEST -> Cure.PESTICIDE;
            case ENVIRONMENTAL -> Cure.NUTRIENT_FLUSH;
            case MYSTICAL -> Cure.GOLDEN_ELIXIR;
        };
    }

    /**
     * Disease categories for grouping and treatment.
     */
    public enum DiseaseCategory {
        FUNGAL("§5Fungal", "Caused by fungal infections"),
        BACTERIAL("§3Bacterial", "Caused by bacterial growth"),
        PEST("§8Pest", "Caused by insect infestations"),
        ENVIRONMENTAL("§6Environmental", "Caused by growing conditions"),
        MYSTICAL("§d§lMystical", "Rare supernatural afflictions");

        private final String displayName;
        private final String description;

        DiseaseCategory(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Disease severity levels.
     */
    public enum DiseaseSeverity {
        MILD("§aMild", 1.0),
        MODERATE("§eModerate", 0.8),
        SEVERE("§cSevere", 0.5),
        CRITICAL("§4Critical", 0.2);

        private final String displayName;
        private final double survivalRate;

        DiseaseSeverity(String displayName, double survivalRate) {
            this.displayName = displayName;
            this.survivalRate = survivalRate;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getSurvivalRate() {
            return survivalRate;
        }
    }

    /**
     * Cures for different disease types.
     */
    public enum Cure {
        FUNGICIDE("Fungicide", "§5🧪", Material.POTION, 100, 0.85,
            "Treats fungal infections"),
        
        ANTIBACTERIAL_SPRAY("Antibacterial Spray", "§3💨", Material.SPLASH_POTION, 120, 0.80,
            "Kills bacterial growth"),
        
        PESTICIDE("Pesticide", "§8☠", Material.FERMENTED_SPIDER_EYE, 80, 0.90,
            "Eliminates pest infestations"),
        
        NUTRIENT_FLUSH("Nutrient Flush", "§6💧", Material.WATER_BUCKET, 50, 0.75,
            "Resets nutrient levels"),
        
        NEEM_OIL("Neem Oil", "§a🌿", Material.LIME_DYE, 150, 0.70,
            "Natural treatment for multiple issues"),
        
        GOLDEN_ELIXIR("Golden Elixir", "§6✨", Material.GOLDEN_APPLE, 1000, 0.95,
            "Magical cure for mystical diseases"),
        
        HEALING_SALVE("Healing Salve", "§d💜", Material.MAGENTA_DYE, 200, 0.85,
            "General purpose plant medicine");

        private final String displayName;
        private final String symbol;
        private final Material material;
        private final double baseCost;
        private final double effectiveness;
        private final String description;

        Cure(String displayName, String symbol, Material material, 
             double baseCost, double effectiveness, String description) {
            this.displayName = displayName;
            this.symbol = symbol;
            this.material = material;
            this.baseCost = baseCost;
            this.effectiveness = effectiveness;
            this.description = description;
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

        public double getBaseCost() {
            return baseCost;
        }

        public double getEffectiveness() {
            return effectiveness;
        }

        public String getDescription() {
            return description;
        }

        public String getColoredDisplay() {
            return symbol + " " + displayName;
        }
    }
}
