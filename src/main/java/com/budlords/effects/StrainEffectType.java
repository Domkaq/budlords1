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
        "Warping visual effects", EffectCategory.LEGENDARY),
    
    // === NEW v2.0.1 EFFECTS ===
    
    // MORE VISUAL EFFECTS
    NEON_GLOW("Neon Glow", "§e💡", "You glow with neon light!", Material.GLOWSTONE,
        "Bright colorful glowing aura", EffectCategory.VISUAL),
    
    CRYSTAL_SHIMMER("Crystal Shimmer", "§b💎", "Sparkling crystal particles!", Material.AMETHYST_SHARD,
        "Shimmering crystal particles", EffectCategory.VISUAL),
    
    SMOKE_SCREEN("Smoke Screen", "§8💨", "Mysterious smoke follows you!", Material.CAMPFIRE,
        "Dense smoke particle trail", EffectCategory.VISUAL),
    
    ELECTRIC_SURGE("Electric Surge", "§e⚡", "Electricity crackles around you!", Material.LIGHTNING_ROD,
        "Electric spark particles", EffectCategory.DRAMATIC),
    
    // MORE MOVEMENT EFFECTS
    FEATHER_FALL("Feather Fall", "§f🪶", "Light as a feather!", Material.FEATHER,
        "Slow falling and reduced fall damage", EffectCategory.MOVEMENT),
    
    BLINK_STEP("Blink Step", "§5✦", "Teleport short distances!", Material.CHORUS_FRUIT,
        "Random short-range teleportation hints", EffectCategory.MOVEMENT),
    
    WATER_WALK("Water Walk", "§b🌊", "Walk on water!", Material.LILY_PAD,
        "Ability to walk on water", EffectCategory.MOVEMENT),
    
    // MORE PERCEPTION EFFECTS
    PREDATOR_SENSE("Predator Sense", "§c👁", "Sense all nearby entities!", Material.SPIDER_EYE,
        "Entities glow through walls", EffectCategory.PERCEPTION),
    
    SONIC_HEARING("Sonic Hearing", "§a👂", "Hear everything around you!", Material.BELL,
        "Enhanced hearing range", EffectCategory.PERCEPTION),
    
    X_RAY_VISION("X-Ray Vision", "§b✕", "See through walls!", Material.SPYGLASS,
        "Outline of ores and entities", EffectCategory.PERCEPTION),
    
    // MORE GAMEPLAY EFFECTS
    DOUBLE_HARVEST("Double Harvest", "§a🌿", "Double plant drops!", Material.GOLDEN_HOE,
        "Chance for double plant harvests", EffectCategory.GAMEPLAY),
    
    MONEY_MAGNET("Money Magnet", "§6💵", "Attract bonus coins!", Material.GOLD_NUGGET,
        "Bonus coins from sales", EffectCategory.GAMEPLAY),
    
    SEED_FINDER("Seed Finder", "§2🔍", "Find better seeds!", Material.COMPASS,
        "Chance to find bonus seeds when harvesting", EffectCategory.GAMEPLAY),
    
    XP_BOOST("XP Boost", "§a✨", "Gain extra experience!", Material.EXPERIENCE_BOTTLE,
        "Bonus XP from all activities", EffectCategory.GAMEPLAY),
    
    // MORE COMBAT EFFECTS
    POISON_TOUCH("Poison Touch", "§2☠", "Your attacks poison enemies!", Material.SPIDER_EYE,
        "Attacks apply poison effect", EffectCategory.COMBAT),
    
    FIRE_PUNCH("Fire Punch", "§c🔥", "Your fists burn with fire!", Material.BLAZE_POWDER,
        "Melee attacks set targets on fire", EffectCategory.COMBAT),
    
    ICE_ARMOR("Ice Armor", "§b❄", "Frozen protection!", Material.BLUE_ICE,
        "Ice particles and cold damage resistance", EffectCategory.COMBAT),
    
    RAGE_MODE("Rage Mode", "§4💢", "Unleash your inner rage!", Material.REDSTONE,
        "Damage increases when low health", EffectCategory.COMBAT),
    
    // MORE FUN EFFECTS
    RAINBOW_TRAIL("Rainbow Trail", "§d🌈", "Leave a rainbow behind!", Material.PRISMARINE_SHARD,
        "Colorful rainbow particle trail", EffectCategory.FUN),
    
    FIREWORK_EXPLOSION("Firework Explosion", "§e🎆", "Periodic firework effects!", Material.FIREWORK_ROCKET,
        "Random firework particle bursts", EffectCategory.FUN),
    
    SLIME_BOUNCE("Slime Bounce", "§a🟢", "Bounce like slime!", Material.SLIME_BALL,
        "Bouncy movement and slime particles", EffectCategory.FUN),
    
    SNOWMAN("Snowman", "§f☃", "Leave snow wherever you go!", Material.SNOWBALL,
        "Snow trail and snowfall around you", EffectCategory.FUN),
    
    // MORE NATURE EFFECTS
    PLANT_GROWTH("Plant Growth", "§2🌱", "Plants grow faster near you!", Material.WHEAT_SEEDS,
        "Accelerate nearby plant growth", EffectCategory.NATURE),
    
    ANIMAL_FRIEND("Animal Friend", "§a🐾", "Animals are friendly to you!", Material.WHEAT,
        "Animals don't flee and may follow", EffectCategory.NATURE),
    
    RAIN_DANCER("Rain Dancer", "§9🌧", "Make it rain around you!", Material.WATER_BUCKET,
        "Rain particles follow you", EffectCategory.NATURE),
    
    EARTHQUAKE("Earthquake", "§6🌍", "The ground shakes!", Material.BROWN_CONCRETE,
        "Ground shaking visual effect", EffectCategory.NATURE),
    
    // MORE MYSTICAL EFFECTS
    SOUL_SIGHT("Soul Sight", "§5👻", "See the spirits around you!", Material.SOUL_LANTERN,
        "Ghost particles and spirit visuals", EffectCategory.MYSTICAL),
    
    FORTUNE_TELLER("Fortune Teller", "§d🔮", "Glimpse the future!", Material.LECTERN,
        "Random fortune hints and luck boost", EffectCategory.MYSTICAL),
    
    ELEMENTAL_CHAOS("Elemental Chaos", "§e⚡", "All elements swirl around you!", Material.NETHER_STAR,
        "Random elemental particle effects", EffectCategory.MYSTICAL),
    
    DIMENSIONAL_RIFT("Dimensional Rift", "§0🌀", "A rift follows you!", Material.END_PORTAL_FRAME,
        "Portal-like visual distortions", EffectCategory.MYSTICAL),
    
    // MORE LEGENDARY EFFECTS
    TIME_FREEZE("Time Freeze", "§b⏸", "Freeze time around you!", Material.CLOCK,
        "Time-stopping visual effects", EffectCategory.LEGENDARY),
    
    INFINITY_POWER("Infinity Power", "§6∞", "Unlimited cosmic energy!", Material.NETHER_STAR,
        "Infinite power aura and particles", EffectCategory.LEGENDARY),
    
    GODMODE_AURA("Godmode Aura", "§f✦", "Divine protection surrounds you!", Material.TOTEM_OF_UNDYING,
        "Golden divine aura and invincibility hints", EffectCategory.LEGENDARY),
    
    UNIVERSE_CONTROL("Universe Control", "§d🌌", "Control the very fabric of reality!", Material.END_CRYSTAL,
        "Cosmic particles and reality distortion", EffectCategory.LEGENDARY),
    
    // ===== V3.0.0 NEW EFFECTS - 25+ ADDITIONAL EFFECTS =====
    
    // NEW TRANSFORMATION EFFECTS
    WEREWOLF_FORM("Werewolf Form", "§4🐺", "Transform under the full moon!", Material.BONE,
        "Wolf-like strength at night", EffectCategory.TRANSFORMATION),
    
    ELEMENTAL_FORM("Elemental Form", "§e🔶", "Become pure elemental energy!", Material.MAGMA_CREAM,
        "Random elemental transformations", EffectCategory.TRANSFORMATION),
    
    QUANTUM_STATE("Quantum State", "§b◊", "Exist in multiple states!", Material.CHORUS_FRUIT,
        "Flickering visibility and random teleports", EffectCategory.TRANSFORMATION),
    
    CRYSTALLINE_BODY("Crystalline Body", "§b💎", "Your body turns to crystal!", Material.DIAMOND,
        "Crystal armor and reflection particles", EffectCategory.TRANSFORMATION),
    
    SMOKE_FORM("Smoke Form", "§8💨", "Become living smoke!", Material.GUNPOWDER,
        "Ethereal smoke particles and phasing hints", EffectCategory.TRANSFORMATION),
    
    // NEW VISUAL EFFECTS
    PLASMA_AURA("Plasma Aura", "§d⚡", "Crackling plasma surrounds you!", Material.END_ROD,
        "Electric plasma particles all around", EffectCategory.VISUAL),
    
    VOID_EYES("Void Eyes", "§0👁", "Your eyes become dark voids!", Material.ENDER_EYE,
        "Dark particles emanating from head", EffectCategory.VISUAL),
    
    GOLDEN_GLOW("Golden Glow", "§6✨", "Radiate golden light!", Material.GOLD_BLOCK,
        "Warm golden particle aura", EffectCategory.VISUAL),
    
    BLOOD_TRAIL("Blood Trail", "§4💧", "Leave a crimson path!", Material.REDSTONE,
        "Red particle trail when walking", EffectCategory.VISUAL),
    
    MIRROR_IMAGE("Mirror Image", "§f👤", "Create illusory copies!", Material.GLASS,
        "Ghost-like duplicate particles nearby", EffectCategory.VISUAL),
    
    // NEW MOVEMENT EFFECTS
    GRAVITY_WELL("Gravity Well", "§8⬇", "Control local gravity!", Material.OBSIDIAN,
        "Pull particles toward you", EffectCategory.MOVEMENT),
    
    PHASE_SHIFT("Phase Shift", "§5✧", "Phase through matter!", Material.PHANTOM_MEMBRANE,
        "Ghostly pass-through particles", EffectCategory.MOVEMENT),
    
    SONIC_BOOM("Sonic Boom", "§f💥", "Move faster than sound!", Material.FIREWORK_ROCKET,
        "Explosive speed burst particles", EffectCategory.MOVEMENT),
    
    MAGNETIC_PULL("Magnetic Pull", "§8🧲", "Attract nearby items!", Material.IRON_INGOT,
        "Items drift slowly toward you", EffectCategory.MOVEMENT),
    
    TELEKINETIC_LIFT("Telekinetic Lift", "§d⬆", "Float objects around you!", Material.SHULKER_SHELL,
        "Floating particle effects", EffectCategory.MOVEMENT),
    
    // NEW PERCEPTION EFFECTS
    DEATH_SENSE("Death Sense", "§4☠", "Sense death nearby!", Material.WITHER_ROSE,
        "See when entities are low health", EffectCategory.PERCEPTION),
    
    TREASURE_HUNTER("Treasure Hunter", "§6🗝", "Find hidden treasures!", Material.RAW_GOLD,
        "Sparkles near valuable blocks", EffectCategory.PERCEPTION),
    
    DANGER_SENSE("Danger Sense", "§c⚠", "Feel danger approaching!", Material.TNT,
        "Warning particles when hostiles nearby", EffectCategory.PERCEPTION),
    
    AURA_READING("Aura Reading", "§d👁", "See players' auras!", Material.SPECTRAL_ARROW,
        "Colored aura around entities", EffectCategory.PERCEPTION),
    
    // NEW GAMEPLAY EFFECTS
    LUCKY_DROPS("Lucky Drops", "§a🍀", "Everything drops more!", Material.RABBIT_FOOT,
        "Increased drop rates from all sources", EffectCategory.GAMEPLAY),
    
    MERCHANT_BLESSING("Merchant Blessing", "§6💰", "Better trade prices!", Material.EMERALD,
        "NPCs offer better deals", EffectCategory.GAMEPLAY),
    
    HARVEST_MOON("Harvest Moon", "§e🌙", "Moonlit farming bonus!", Material.WHEAT,
        "Plants grow faster at night", EffectCategory.GAMEPLAY),
    
    SEED_MULTIPLIER("Seed Multiplier", "§a🌱", "Seeds multiply!", Material.OAK_SAPLING,
        "Chance to get extra seeds", EffectCategory.GAMEPLAY),
    
    QUALITY_BOOST("Quality Boost", "§b⭐", "Everything is better quality!", Material.NETHER_STAR,
        "Bonus quality to all crafted/grown items", EffectCategory.GAMEPLAY),
    
    // NEW COMBAT EFFECTS
    LIGHTNING_FIST("Lightning Fist", "§e⚡", "Punch with lightning!", Material.LIGHTNING_ROD,
        "Melee attacks cause lightning particles", EffectCategory.COMBAT),
    
    SHADOW_STRIKE("Shadow Strike", "§8⚔", "Strike from the shadows!", Material.IRON_SWORD,
        "Bonus damage from stealth", EffectCategory.COMBAT),
    
    LIFE_DRAIN("Life Drain", "§4❤", "Drain life from enemies!", Material.WITHER_SKELETON_SKULL,
        "Steal health on hit", EffectCategory.COMBAT),
    
    SHIELD_BASH("Shield Bash", "§f🛡", "Your shields hit back!", Material.SHIELD,
        "Reflect damage to attackers", EffectCategory.COMBAT),
    
    CRITICAL_FURY("Critical Fury", "§c💢", "Increased critical chance!", Material.NETHERITE_SWORD,
        "Higher critical hit rate", EffectCategory.COMBAT),
    
    // NEW FUN EFFECTS
    PARTY_MODE("Party Mode", "§d🎊", "Turn everything into a party!", Material.CAKE,
        "Confetti, music, and celebrations", EffectCategory.FUN),
    
    LAUGH_TRACK("Laugh Track", "§e😂", "Everything is hilarious!", Material.NOTE_BLOCK,
        "Random laugh sound effects", EffectCategory.FUN),
    
    PET_MAGNET("Pet Magnet", "§a🐕", "Animals follow you!", Material.LEAD,
        "Passive animals are attracted to you", EffectCategory.FUN),
    
    SPARKLE_STEP("Sparkle Step", "§f✨", "Every step sparkles!", Material.GLOWSTONE_DUST,
        "Sparkling particles under feet", EffectCategory.FUN),
    
    GHOST_FRIEND("Ghost Friend", "§f👻", "A friendly ghost follows you!", Material.SOUL_LANTERN,
        "Ghostly particle companion", EffectCategory.FUN),
    
    // NEW NATURE EFFECTS
    PHOTOSYNTHESIS("Photosynthesis", "§a☀", "Heal in sunlight!", Material.SUNFLOWER,
        "Regenerate health in daylight", EffectCategory.NATURE),
    
    ROOT_CONNECTION("Root Connection", "§6🌳", "Connect with all plants!", Material.OAK_SAPLING,
        "Nature particles when near plants", EffectCategory.NATURE),
    
    STORM_RIDER("Storm Rider", "§9⛈", "Ride the storm!", Material.WIND_CHARGE,
        "Boost during rainy weather", EffectCategory.NATURE),
    
    VOLCANIC_HEAT("Volcanic Heat", "§c🌋", "Radiate volcanic energy!", Material.MAGMA_BLOCK,
        "Fire resistance and heat particles", EffectCategory.NATURE),
    
    ARCTIC_CHILL("Arctic Chill", "§b❄", "Freeze everything around!", Material.BLUE_ICE,
        "Slow nearby mobs with cold", EffectCategory.NATURE),
    
    // NEW MYSTICAL EFFECTS
    ORACLE_VISION("Oracle Vision", "§5🔮", "See glimpses of the future!", Material.END_CRYSTAL,
        "Predictive particles and luck boost", EffectCategory.MYSTICAL),
    
    ANCESTOR_SPIRITS("Ancestor Spirits", "§f👻", "Call upon ancient spirits!", Material.SOUL_TORCH,
        "Spiritual particles surround you", EffectCategory.MYSTICAL),
    
    CHAKRA_ALIGNMENT("Chakra Alignment", "§d🕉", "Align your chakras!", Material.AMETHYST_CLUSTER,
        "Rainbow energy points along body", EffectCategory.MYSTICAL),
    
    KARMA_BALANCE("Karma Balance", "§e☯", "Balance your karma!", Material.ENCHANTED_BOOK,
        "Good luck after bad events", EffectCategory.MYSTICAL),
    
    ETHER_SIGHT("Ether Sight", "§b✦", "See the ethereal plane!", Material.ENDER_EYE,
        "See invisible entities and particles", EffectCategory.MYSTICAL),
    
    // NEW LEGENDARY EFFECTS
    TITAN_FORM("Titan Form", "§4§l👹", "Become a titan of power!", Material.NETHERITE_BLOCK,
        "Massive strength and size illusion", EffectCategory.LEGENDARY),
    
    SINGULARITY("Singularity", "§0§l◉", "Create a singularity!", Material.END_PORTAL_FRAME,
        "Black hole particle effects", EffectCategory.LEGENDARY),
    
    COSMIC_REBIRTH("Cosmic Rebirth", "§d§l✦", "Be reborn from the cosmos!", Material.NETHER_STAR,
        "Death triggers cosmic respawn effect", EffectCategory.LEGENDARY),
    
    MULTIVERSE_ECHO("Multiverse Echo", "§5§l◇", "Echo across realities!", Material.END_CRYSTAL,
        "Multiple phantom copies of yourself", EffectCategory.LEGENDARY),
    
    ENTROPY_MASTER("Entropy Master", "§8§l♠", "Control chaos itself!", Material.DRAGON_EGG,
        "Random beneficial effects stack", EffectCategory.LEGENDARY);
    
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
            // NEW v2.0.1 EFFECTS
            case NEON_GLOW -> Particle.END_ROD;
            case CRYSTAL_SHIMMER -> Particle.END_ROD;
            case SMOKE_SCREEN -> Particle.SMOKE_LARGE;
            case ELECTRIC_SURGE -> Particle.FIREWORKS_SPARK;
            case FEATHER_FALL -> Particle.END_ROD;
            case BLINK_STEP -> Particle.PORTAL;
            case WATER_WALK -> Particle.DRIP_WATER;
            case PREDATOR_SENSE -> Particle.VILLAGER_ANGRY;
            case SONIC_HEARING -> Particle.NOTE;
            case X_RAY_VISION -> Particle.ENCHANTMENT_TABLE;
            case DOUBLE_HARVEST -> Particle.COMPOSTER;
            case MONEY_MAGNET -> Particle.FALLING_HONEY;
            case SEED_FINDER -> Particle.COMPOSTER;
            case XP_BOOST -> Particle.ENCHANTMENT_TABLE;
            case POISON_TOUCH -> Particle.SPELL_MOB;
            case FIRE_PUNCH -> Particle.FLAME;
            case ICE_ARMOR -> Particle.SNOWFLAKE;
            case RAGE_MODE -> Particle.DAMAGE_INDICATOR;
            case RAINBOW_TRAIL -> Particle.SPELL_MOB;
            case FIREWORK_EXPLOSION -> Particle.FIREWORKS_SPARK;
            case SLIME_BOUNCE -> Particle.VILLAGER_HAPPY;
            case SNOWMAN -> Particle.SNOWFLAKE;
            case PLANT_GROWTH -> Particle.COMPOSTER;
            case ANIMAL_FRIEND -> Particle.HEART;
            case RAIN_DANCER -> Particle.DRIP_WATER;
            case EARTHQUAKE -> Particle.BLOCK_CRACK;
            case SOUL_SIGHT -> Particle.SOUL;
            case FORTUNE_TELLER -> Particle.ENCHANTMENT_TABLE;
            case ELEMENTAL_CHAOS -> Particle.SPELL_MOB;
            case DIMENSIONAL_RIFT -> Particle.PORTAL;
            case TIME_FREEZE -> Particle.END_ROD;
            case INFINITY_POWER -> Particle.END_ROD;
            case GODMODE_AURA -> Particle.TOTEM;
            case UNIVERSE_CONTROL -> Particle.REVERSE_PORTAL;
            // V3.0.0 NEW EFFECTS
            case WEREWOLF_FORM -> Particle.SMOKE_LARGE;
            case ELEMENTAL_FORM -> Particle.FLAME;
            case QUANTUM_STATE -> Particle.PORTAL;
            case CRYSTALLINE_BODY -> Particle.END_ROD;
            case SMOKE_FORM -> Particle.SMOKE_LARGE;
            case PLASMA_AURA -> Particle.FIREWORKS_SPARK;
            case VOID_EYES -> Particle.PORTAL;
            case GOLDEN_GLOW -> Particle.FALLING_HONEY;
            case BLOOD_TRAIL -> Particle.DAMAGE_INDICATOR;
            case MIRROR_IMAGE -> Particle.END_ROD;
            case GRAVITY_WELL -> Particle.PORTAL;
            case PHASE_SHIFT -> Particle.SOUL;
            case SONIC_BOOM -> Particle.CLOUD;
            case MAGNETIC_PULL -> Particle.CRIT_MAGIC;
            case TELEKINETIC_LIFT -> Particle.END_ROD;
            case DEATH_SENSE -> Particle.DAMAGE_INDICATOR;
            case TREASURE_HUNTER -> Particle.FALLING_HONEY;
            case DANGER_SENSE -> Particle.VILLAGER_ANGRY;
            case AURA_READING -> Particle.SPELL_MOB;
            case LUCKY_DROPS -> Particle.VILLAGER_HAPPY;
            case MERCHANT_BLESSING -> Particle.FALLING_HONEY;
            case HARVEST_MOON -> Particle.END_ROD;
            case SEED_MULTIPLIER -> Particle.COMPOSTER;
            case QUALITY_BOOST -> Particle.END_ROD;
            case LIGHTNING_FIST -> Particle.FIREWORKS_SPARK;
            case SHADOW_STRIKE -> Particle.SMOKE_LARGE;
            case LIFE_DRAIN -> Particle.DAMAGE_INDICATOR;
            case SHIELD_BASH -> Particle.CRIT_MAGIC;
            case CRITICAL_FURY -> Particle.CRIT;
            case PARTY_MODE -> Particle.FIREWORKS_SPARK;
            case LAUGH_TRACK -> Particle.NOTE;
            case PET_MAGNET -> Particle.HEART;
            case SPARKLE_STEP -> Particle.FIREWORKS_SPARK;
            case GHOST_FRIEND -> Particle.SOUL;
            case PHOTOSYNTHESIS -> Particle.VILLAGER_HAPPY;
            case ROOT_CONNECTION -> Particle.COMPOSTER;
            case STORM_RIDER -> Particle.CLOUD;
            case VOLCANIC_HEAT -> Particle.FLAME;
            case ARCTIC_CHILL -> Particle.SNOWFLAKE;
            case ORACLE_VISION -> Particle.ENCHANTMENT_TABLE;
            case ANCESTOR_SPIRITS -> Particle.SOUL;
            case CHAKRA_ALIGNMENT -> Particle.END_ROD;
            case KARMA_BALANCE -> Particle.VILLAGER_HAPPY;
            case ETHER_SIGHT -> Particle.END_ROD;
            case TITAN_FORM -> Particle.DAMAGE_INDICATOR;
            case SINGULARITY -> Particle.PORTAL;
            case COSMIC_REBIRTH -> Particle.TOTEM;
            case MULTIVERSE_ECHO -> Particle.REVERSE_PORTAL;
            case ENTROPY_MASTER -> Particle.SPELL_MOB;
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
