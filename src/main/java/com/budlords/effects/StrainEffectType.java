package com.budlords.effects;

import org.bukkit.Material;
import org.bukkit.Particle;

/**
 * Defines all available strain effect types.
 * Each strain can have one or more effects that trigger when consumed.
 */
public enum StrainEffectType {
    
    // ===== VISUAL TRANSFORMATION EFFECTS =====
    GHOST_RIDER("Ghost Rider", "§6🔥", "Your head burns with hellfire!", Material.MAGMA_BLOCK,
        "Flaming head like Ghost Rider", EffectCategory.TRANSFORMATION),
    
    RAINBOW_AURA("Rainbow Aura", "§d🌈", "Surrounded by rainbow particles!", Material.PRISMARINE_SHARD,
        "Colorful rainbow aura around you", EffectCategory.VISUAL),
    
    SHADOW_WALKER("Shadow Walker", "§8👤", "Become one with the shadows!", Material.BLACK_CONCRETE,
        "Dark smoky particles follow you", EffectCategory.TRANSFORMATION),
    
    ANGEL_WINGS("Angel Wings", "§f✦", "Feathery particles form wings!", Material.FEATHER,
        "White feather particles form wing shapes", EffectCategory.VISUAL),
    
    DEMON_HORNS("Demon Horns", "§4👿", "Dark red particles form horns!", Material.NETHER_WART,
        "Devilish red particle horns", EffectCategory.VISUAL),
    
    SPARKLING_EYES("Sparkling Eyes", "§e✨", "Your eyes sparkle magically!", Material.GLOW_INK_SAC,
        "Sparkle particles from eye level", EffectCategory.VISUAL),
    
    FROST_AURA("Frost Aura", "§b❄", "Cold mist surrounds you!", Material.PACKED_ICE,
        "Icy snow particles around you", EffectCategory.VISUAL),
    
    FIRE_TRAIL("Fire Trail", "§c🔥", "Leave flames in your path!", Material.FIRE_CHARGE,
        "Fire particles trail behind you", EffectCategory.MOVEMENT),
    
    LIGHTNING_STRIKE("Lightning Strike", "§e⚡", "Occasional lightning effects!", Material.LIGHTNING_ROD,
        "Random lightning visual effects", EffectCategory.DRAMATIC),
    
    GALAXY_PORTAL("Galaxy Portal", "§5🌌", "A cosmic aura!", Material.OBSIDIAN,
        "Purple cosmic portal particles", EffectCategory.VISUAL),
    
    // ===== MOVEMENT EFFECTS =====
    BUNNY_HOP("Bunny Hop", "§a🐰", "Hop hop hop!", Material.CARROT,
        "Enhanced jumping ability", EffectCategory.MOVEMENT),
    
    SPEED_DEMON("Speed Demon", "§c💨", "Can't stop, won't stop!", Material.SUGAR,
        "Extreme speed boost", EffectCategory.MOVEMENT),
    
    SLOW_MO("Slow Motion", "§7🐌", "Everything feels so slooow...", Material.SOUL_SAND,
        "Time seems to slow down", EffectCategory.MOVEMENT),
    
    MOON_GRAVITY("Moon Gravity", "§f🌙", "Float like you're on the moon!", Material.END_STONE,
        "Reduced gravity effect", EffectCategory.MOVEMENT),
    
    DOLPHIN_SWIM("Dolphin Swimmer", "§3🐬", "Swim like a dolphin!", Material.PRISMARINE_SHARD,
        "Enhanced swimming ability", EffectCategory.MOVEMENT),
    
    ROCKET_BOOST("Rocket Boost", "§c🚀", "Blast off!", Material.FIREWORK_ROCKET,
        "Occasional upward boost", EffectCategory.MOVEMENT),
    
    // ===== PERCEPTION EFFECTS =====
    THIRD_EYE("Third Eye", "§5👁", "See beyond the visible!", Material.ENDER_EYE,
        "Night vision and glowing entities", EffectCategory.PERCEPTION),
    
    MATRIX_VISION("Matrix Vision", "§a📟", "See the code...", Material.LIME_STAINED_GLASS,
        "Green tinted vision effect", EffectCategory.PERCEPTION),
    
    DRUNK_VISION("Drunk Vision", "§e🍺", "Wobbly world!", Material.HONEY_BOTTLE,
        "Nausea and confusion effect", EffectCategory.PERCEPTION),
    
    EAGLE_SIGHT("Eagle Sight", "§6🦅", "See everything clearly!", Material.SPYGLASS,
        "Enhanced zoom-like vision", EffectCategory.PERCEPTION),
    
    THERMAL_VISION("Thermal Vision", "§c🔴", "See heat signatures!", Material.BLAZE_POWDER,
        "Entities glow red/orange", EffectCategory.PERCEPTION),
    
    // ===== GAMEPLAY EFFECTS =====
    LUCKY_CHARM("Lucky Charm", "§a🍀", "Fortune favors you!", Material.EMERALD,
        "Increased luck stat", EffectCategory.GAMEPLAY),
    
    MIDAS_TOUCH("Midas Touch", "§6💰", "Everything turns to gold!", Material.GOLD_INGOT,
        "Bonus money from sales", EffectCategory.GAMEPLAY),
    
    GREEN_THUMB("Green Thumb", "§2🌱", "Plants love you!", Material.BONE_MEAL,
        "Faster plant growth nearby", EffectCategory.GAMEPLAY),
    
    IRON_LUNGS("Iron Lungs", "§7🫁", "Breathe deep!", Material.IRON_INGOT,
        "Extended effect duration", EffectCategory.GAMEPLAY),
    
    COTTON_MOUTH("Cotton Mouth", "§e🥤", "So thirsty!", Material.BUCKET,
        "Hunger effect but with saturation", EffectCategory.GAMEPLAY),
    
    MUNCHIES("Munchies", "§e🍔", "Need food NOW!", Material.COOKED_BEEF,
        "Increased hunger with regen", EffectCategory.GAMEPLAY),
    
    // ===== COMBAT EFFECTS =====
    BERSERKER("Berserker", "§c⚔", "RAGE MODE!", Material.NETHERITE_AXE,
        "Strength boost but lower defense", EffectCategory.COMBAT),
    
    TANK_MODE("Tank Mode", "§8🛡", "Become unstoppable!", Material.NETHERITE_CHESTPLATE,
        "High resistance but slower", EffectCategory.COMBAT),
    
    NINJA_MODE("Ninja Mode", "§0🥷", "Swift and silent!", Material.LEATHER_BOOTS,
        "Invisibility with speed", EffectCategory.COMBAT),
    
    VAMPIRE("Vampire", "§4🧛", "Drain life from enemies!", Material.REDSTONE,
        "Lifesteal effect", EffectCategory.COMBAT),
    
    THORNS("Thorns", "§2🌵", "Touch me and hurt!", Material.CACTUS,
        "Damage reflection", EffectCategory.COMBAT),
    
    // ===== SOCIAL/FUN EFFECTS =====
    DISCO_FEVER("Disco Fever", "§d💃", "Get down!", Material.JUKEBOX,
        "Random colorful particles and music", EffectCategory.FUN),
    
    CHIPMUNK_VOICE("Chipmunk Voice", "§a🐿", "High pitched sounds!", Material.NOTE_BLOCK,
        "Higher pitched player sounds", EffectCategory.FUN),
    
    BASS_DROP("Bass Drop", "§5🔊", "Feel the bass!", Material.NOTE_BLOCK,
        "Deep bass sounds and vibration", EffectCategory.FUN),
    
    CONFETTI("Confetti", "§e🎉", "Party time!", Material.FIREWORK_STAR,
        "Colorful confetti particles", EffectCategory.FUN),
    
    BUBBLE_AURA("Bubble Aura", "§b🫧", "Surrounded by bubbles!", Material.PUFFERFISH_BUCKET,
        "Floating bubble particles", EffectCategory.FUN),
    
    HEART_TRAIL("Heart Trail", "§c❤", "Spreading love!", Material.PINK_DYE,
        "Heart particles follow you", EffectCategory.FUN),
    
    MUSIC_NOTES("Music Notes", "§d♪", "Musical aura!", Material.NOTE_BLOCK,
        "Floating music note particles", EffectCategory.FUN),
    
    PIXEL_GLITCH("Pixel Glitch", "§f⬜", "Reality glitching!", Material.BARRIER,
        "Glitchy block particles", EffectCategory.FUN),
    
    // ===== NATURE EFFECTS =====
    FLOWER_POWER("Flower Power", "§d🌸", "Flowers bloom around you!", Material.POPPY,
        "Flower particles trail", EffectCategory.NATURE),
    
    EARTH_BOUND("Earth Bound", "§6🌍", "Connected to nature!", Material.GRASS_BLOCK,
        "Dirt and grass particles", EffectCategory.NATURE),
    
    WIND_WALKER("Wind Walker", "§f💨", "One with the wind!", Material.ELYTRA,
        "Wind swirl particles", EffectCategory.NATURE),
    
    STORM_CALLER("Storm Caller", "§8⛈", "Control the weather!", Material.LIGHTNING_ROD,
        "Storm cloud particles overhead", EffectCategory.NATURE),
    
    AURORA_BOREALIS("Aurora Borealis", "§b🌌", "Northern lights surround you!", Material.LIGHT_BLUE_DYE,
        "Colorful aurora particles", EffectCategory.NATURE),
    
    // ===== MYSTICAL EFFECTS =====
    ASTRAL_PROJECTION("Astral Projection", "§d👻", "Out of body experience!", Material.SOUL_LANTERN,
        "Ghostly transparent appearance", EffectCategory.MYSTICAL),
    
    TIME_WARP("Time Warp", "§e⏰", "Time distortion!", Material.CLOCK,
        "Time-related visual effects", EffectCategory.MYSTICAL),
    
    DREAM_STATE("Dream State", "§d💭", "Lucid dreaming...", Material.WHITE_BED,
        "Dreamy floaty particles", EffectCategory.MYSTICAL),
    
    SPIRIT_ANIMAL("Spirit Animal", "§a🐺", "Your spirit animal appears!", Material.BONE,
        "Random animal particle shapes", EffectCategory.MYSTICAL),
    
    MEDITATION("Meditation", "§e🧘", "Inner peace!", Material.AMETHYST_SHARD,
        "Calm particles with regeneration", EffectCategory.MYSTICAL),
    
    ENLIGHTENMENT("Enlightenment", "§f✨", "Achieve nirvana!", Material.BEACON,
        "Golden light rays from above", EffectCategory.MYSTICAL),
    
    // ===== LEGENDARY SPECIAL EFFECTS =====
    PHOENIX_REBIRTH("Phoenix Rebirth", "§6🔥", "Rise from the ashes!", Material.BLAZE_ROD,
        "Fire wings and auto-revive effect", EffectCategory.LEGENDARY),
    
    DRAGON_BREATH("Dragon Breath", "§5🐉", "Breathe purple fire!", Material.DRAGON_BREATH,
        "Purple dragon breath particles", EffectCategory.LEGENDARY),
    
    VOID_WALKER("Void Walker", "§0🕳", "Touch the void!", Material.END_PORTAL_FRAME,
        "Dark void particles and teleport hints", EffectCategory.LEGENDARY),
    
    CELESTIAL_BEING("Celestial Being", "§f⭐", "Become one with the stars!", Material.NETHER_STAR,
        "Star particles and divine aura", EffectCategory.LEGENDARY),
    
    REALITY_BENDER("Reality Bender", "§d🌀", "Bend reality itself!", Material.ENDER_PEARL,
        "Warping visual effects", EffectCategory.LEGENDARY);
    
    private final String displayName;
    private final String symbol;
    private final String activationMessage;
    private final Material iconMaterial;
    private final String description;
    private final EffectCategory category;
    
    StrainEffectType(String displayName, String symbol, String activationMessage, 
                     Material iconMaterial, String description, EffectCategory category) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.activationMessage = activationMessage;
        this.iconMaterial = iconMaterial;
        this.description = description;
        this.category = category;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getActivationMessage() {
        return activationMessage;
    }
    
    public Material getIconMaterial() {
        return iconMaterial;
    }
    
    public String getDescription() {
        return description;
    }
    
    public EffectCategory getCategory() {
        return category;
    }
    
    public String getColoredName() {
        return getCategoryColor() + displayName;
    }
    
    public String getCategoryColor() {
        return switch (category) {
            case TRANSFORMATION -> "§6";
            case VISUAL -> "§d";
            case MOVEMENT -> "§a";
            case PERCEPTION -> "§5";
            case GAMEPLAY -> "§e";
            case COMBAT -> "§c";
            case FUN -> "§b";
            case NATURE -> "§2";
            case MYSTICAL -> "§d";
            case LEGENDARY -> "§6§l";
            case DRAMATIC -> "§c";
        };
    }
    
    /**
     * Gets the default particle type for this effect.
     */
    public Particle getDefaultParticle() {
        return switch (this) {
            case GHOST_RIDER -> Particle.FLAME;
            case RAINBOW_AURA -> Particle.SPELL_MOB;
            case SHADOW_WALKER -> Particle.SMOKE_LARGE;
            case ANGEL_WINGS -> Particle.END_ROD;
            case DEMON_HORNS -> Particle.FLAME;
            case SPARKLING_EYES -> Particle.FIREWORKS_SPARK;
            case FROST_AURA -> Particle.SNOWFLAKE;
            case FIRE_TRAIL -> Particle.FLAME;
            case LIGHTNING_STRIKE -> Particle.FIREWORKS_SPARK;
            case GALAXY_PORTAL -> Particle.PORTAL;
            case BUNNY_HOP -> Particle.COMPOSTER;
            case SPEED_DEMON -> Particle.CLOUD;
            case SLOW_MO -> Particle.SUSPENDED_DEPTH;
            case MOON_GRAVITY -> Particle.END_ROD;
            case DOLPHIN_SWIM -> Particle.BUBBLE_COLUMN_UP;
            case ROCKET_BOOST -> Particle.FLAME;
            case THIRD_EYE -> Particle.ENCHANTMENT_TABLE;
            case MATRIX_VISION -> Particle.VILLAGER_HAPPY;
            case DRUNK_VISION -> Particle.SPELL_MOB;
            case EAGLE_SIGHT -> Particle.END_ROD;
            case THERMAL_VISION -> Particle.DRIP_LAVA;
            case LUCKY_CHARM -> Particle.VILLAGER_HAPPY;
            case MIDAS_TOUCH -> Particle.FALLING_HONEY;
            case GREEN_THUMB -> Particle.COMPOSTER;
            case IRON_LUNGS -> Particle.SMOKE_NORMAL;
            case COTTON_MOUTH -> Particle.DRIP_WATER;
            case MUNCHIES -> Particle.VILLAGER_HAPPY;
            case BERSERKER -> Particle.DAMAGE_INDICATOR;
            case TANK_MODE -> Particle.CRIT_MAGIC;
            case NINJA_MODE -> Particle.SMOKE_NORMAL;
            case VAMPIRE -> Particle.DAMAGE_INDICATOR;
            case THORNS -> Particle.CRIT;
            case DISCO_FEVER -> Particle.SPELL_MOB;
            case CHIPMUNK_VOICE -> Particle.NOTE;
            case BASS_DROP -> Particle.NOTE;
            case CONFETTI -> Particle.FIREWORKS_SPARK;
            case BUBBLE_AURA -> Particle.BUBBLE_POP;
            case HEART_TRAIL -> Particle.HEART;
            case MUSIC_NOTES -> Particle.NOTE;
            case PIXEL_GLITCH -> Particle.ENCHANTMENT_TABLE;
            case FLOWER_POWER -> Particle.COMPOSTER;
            case EARTH_BOUND -> Particle.BLOCK_CRACK;
            case WIND_WALKER -> Particle.CLOUD;
            case STORM_CALLER -> Particle.CLOUD;
            case AURORA_BOREALIS -> Particle.SPELL_MOB;
            case ASTRAL_PROJECTION -> Particle.SOUL;
            case TIME_WARP -> Particle.PORTAL;
            case DREAM_STATE -> Particle.ENCHANTMENT_TABLE;
            case SPIRIT_ANIMAL -> Particle.SOUL;
            case MEDITATION -> Particle.END_ROD;
            case ENLIGHTENMENT -> Particle.END_ROD;
            case PHOENIX_REBIRTH -> Particle.FLAME;
            case DRAGON_BREATH -> Particle.DRAGON_BREATH;
            case VOID_WALKER -> Particle.PORTAL;
            case CELESTIAL_BEING -> Particle.END_ROD;
            case REALITY_BENDER -> Particle.REVERSE_PORTAL;
        };
    }
    
    /**
     * Checks if this is a legendary effect (rare and powerful).
     */
    public boolean isLegendary() {
        return category == EffectCategory.LEGENDARY;
    }
    
    /**
     * Gets the rarity weight for random effect generation.
     * Lower values = more rare.
     */
    public int getRarityWeight() {
        return switch (category) {
            case LEGENDARY -> 1;
            case MYSTICAL -> 3;
            case TRANSFORMATION -> 5;
            case COMBAT -> 7;
            case PERCEPTION -> 8;
            case MOVEMENT -> 10;
            case GAMEPLAY -> 10;
            case NATURE -> 12;
            case VISUAL -> 15;
            case FUN -> 15;
            case DRAMATIC -> 8;
        };
    }
    
    /**
     * Effect categories for organization and balancing.
     */
    public enum EffectCategory {
        TRANSFORMATION("§6Transformation"),
        VISUAL("§dVisual"),
        MOVEMENT("§aMovement"),
        PERCEPTION("§5Perception"),
        GAMEPLAY("§eGameplay"),
        COMBAT("§cCombat"),
        FUN("§bFun"),
        NATURE("§2Nature"),
        MYSTICAL("§dMystical"),
        LEGENDARY("§6§lLegendary"),
        DRAMATIC("§cDramatic");
        
        private final String displayName;
        
        EffectCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
