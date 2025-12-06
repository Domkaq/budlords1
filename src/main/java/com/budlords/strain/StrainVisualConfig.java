package com.budlords.strain;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom visual appearance settings for strains.
 * Allows admins to customize how each strain looks when growing.
 * 
 * This includes:
 * - Leaf colors (primary, secondary, accent)
 * - Bud appearance (material, player head for special strains)
 * - Stem material
 * - Special particle effects
 * - Glow effects
 * - Custom animation styles
 * - Unique visual themes (werewolf, crystal, fire, etc.)
 */
public class StrainVisualConfig {

    // ===== COLORS =====
    private Color leafColorPrimary;      // Main leaf color
    private Color leafColorSecondary;    // Secondary leaf accents
    private Color leafColorAccent;       // Highlight/glow color
    private Color budColor;              // Bud tint color
    private Color stemColor;             // Stem tint
    
    // ===== MATERIALS =====
    private Material leafMaterial;       // What material to use for leaves
    private Material budMaterial;        // What material to use for buds
    private Material stemMaterial;       // Stem material (bamboo, stick, etc.)
    private Material potMaterial;        // Pot material override
    
    // ===== SPECIAL BUD TYPES =====
    private BudType budType;             // Type of bud visual
    private String customHeadTexture;    // Base64 texture for custom player heads
    private String customHeadOwner;      // Player name for head (if using player head)
    
    // ===== PARTICLES =====
    private Particle ambientParticle;    // Particles around the plant
    private Particle harvestParticle;    // Particles when harvesting
    private int particleIntensity;       // How many particles (1-10)
    private boolean glowing;             // Whether plant glows at night
    private Color glowColor;             // Glow color
    
    // ===== SPECIAL THEMES =====
    private VisualTheme visualTheme;     // Pre-built visual themes
    
    // ===== ANIMATION =====
    private AnimationStyle animationStyle;  // How the plant moves
    private double animationSpeed;          // Animation speed multiplier
    
    // ===== SIZE MODIFIERS =====
    private double leafScale;           // Leaf size multiplier (0.5-2.0)
    private double budScale;            // Bud size multiplier (0.5-2.0)
    private double heightScale;         // Overall height multiplier (0.5-2.0)
    
    /**
     * Types of bud visuals available.
     */
    public enum BudType {
        NORMAL("Normal", Material.GREEN_WOOL, "Standard marijuana buds"),
        CRYSTAL("Crystal", Material.PRISMARINE, "Crystalline, frosty buds"),
        FIRE("Fire", Material.MAGMA_BLOCK, "Fiery orange/red buds"),
        ICE("Ice", Material.PACKED_ICE, "Frozen, icy blue buds"),
        GOLD("Gold", Material.GOLD_BLOCK, "Luxurious golden buds"),
        DIAMOND("Diamond", Material.DIAMOND_BLOCK, "Premium diamond-like buds"),
        NETHER("Nether", Material.CRIMSON_FUNGUS, "Hellish crimson buds"),
        END("End", Material.CHORUS_FLOWER, "Otherworldly purple buds"),
        SLIME("Slime", Material.SLIME_BLOCK, "Gooey slime buds"),
        HONEY("Honey", Material.HONEYCOMB_BLOCK, "Sweet honey buds"),
        SKULL("Skull", Material.SKELETON_SKULL, "Spooky skull buds"),
        ZOMBIE("Zombie", Material.ZOMBIE_HEAD, "Undead zombie buds"),
        CREEPER("Creeper", Material.CREEPER_HEAD, "Explosive creeper buds"),
        WITHER("Wither", Material.WITHER_SKELETON_SKULL, "Dark wither buds"),
        DRAGON("Dragon", Material.DRAGON_HEAD, "Legendary dragon buds"),
        PIGLIN("Piglin", Material.PIGLIN_HEAD, "Piglin trophy buds"),
        PLAYER_HEAD("Player Head", Material.PLAYER_HEAD, "Custom player head buds"),
        AMETHYST("Amethyst", Material.AMETHYST_CLUSTER, "Sparkling amethyst buds"),
        COPPER("Oxidized", Material.OXIDIZED_COPPER, "Oxidized copper buds"),
        SCULK("Sculk", Material.SCULK, "Deep dark sculk buds"),
        MOSS("Mossy", Material.MOSS_BLOCK, "Natural mossy buds"),
        MUSHROOM("Mushroom", Material.RED_MUSHROOM_BLOCK, "Fungal mushroom buds"),
        CORAL("Coral", Material.BRAIN_CORAL_BLOCK, "Vibrant coral buds"),
        ANCIENT("Ancient", Material.ANCIENT_DEBRIS, "Rare ancient buds"),
        BEACON("Beacon", Material.BEACON, "Glowing beacon buds"),
        CONDUIT("Conduit", Material.CONDUIT, "Ocean power buds"),
        HEART("Heart", Material.PINK_GLAZED_TERRACOTTA, "Love heart buds"),
        EMERALD("Emerald", Material.EMERALD_BLOCK, "Merchant's emerald buds"),
        VOLCANO("Volcano", Material.MAGMA_BLOCK, "Volcanic lava buds"),
        VOID("Void", Material.OBSIDIAN, "Dark void dimension buds"),
        STAR("Star", Material.NETHER_STAR, "Celestial star buds"),
        MIRROR("Mirror", Material.GLASS, "Reflective mirror buds");
        
        private final String displayName;
        private final Material defaultMaterial;
        private final String description;
        
        BudType(String displayName, Material defaultMaterial, String description) {
            this.displayName = displayName;
            this.defaultMaterial = defaultMaterial;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public Material getDefaultMaterial() { return defaultMaterial; }
        public String getDescription() { return description; }
    }
    
    /**
     * Pre-built visual themes for quick selection.
     */
    public enum VisualTheme {
        CLASSIC("Classic", "Traditional green marijuana plant", 
            Color.fromRGB(34, 139, 34), Material.OAK_LEAVES, BudType.NORMAL, Particle.COMPOSTER),
        PURPLE_HAZE("Purple Haze", "Deep purple mystical plant",
            Color.fromRGB(128, 0, 128), Material.PURPLE_WOOL, BudType.CRYSTAL, Particle.PORTAL),
        GOLDEN_LEAF("Golden Leaf", "Luxurious golden plant",
            Color.fromRGB(255, 215, 0), Material.HORN_CORAL_FAN, BudType.GOLD, Particle.WAX_ON),
        FROST_BITE("Frost Bite", "Frozen icy plant",
            Color.fromRGB(173, 216, 230), Material.POWDER_SNOW, BudType.ICE, Particle.SNOWFLAKE),
        FIRE_OG("Fire OG", "Blazing hot plant",
            Color.fromRGB(255, 69, 0), Material.FIRE_CORAL_FAN, BudType.FIRE, Particle.FLAME),
        ALIEN("Alien", "Otherworldly extraterrestrial plant",
            Color.fromRGB(0, 255, 127), Material.WARPED_ROOTS, BudType.END, Particle.REVERSE_PORTAL),
        ZOMBIE_KUSH("Zombie Kush", "Undead horror plant",
            Color.fromRGB(85, 107, 47), Material.DEAD_BUSH, BudType.ZOMBIE, Particle.DAMAGE_INDICATOR),
        WEREWOLF("Werewolf", "Lycanthropic beast plant",
            Color.fromRGB(139, 90, 43), Material.BROWN_WOOL, BudType.SKULL, Particle.CRIT),
        CRYSTAL_CLEAR("Crystal Clear", "Pure crystalline plant",
            Color.fromRGB(255, 255, 255), Material.GLASS, BudType.AMETHYST, Particle.END_ROD),
        NETHER_WARP("Nether Warp", "Hellish nether plant",
            Color.fromRGB(139, 0, 0), Material.CRIMSON_ROOTS, BudType.NETHER, Particle.CRIMSON_SPORE),
        VOID_WALKER("Void Walker", "End dimension plant",
            Color.fromRGB(75, 0, 130), Material.CHORUS_PLANT, BudType.END, Particle.PORTAL),
        DRAGON_BREATH("Dragon Breath", "Legendary dragon plant",
            Color.fromRGB(148, 0, 211), Material.END_ROD, BudType.DRAGON, Particle.DRAGON_BREATH),
        HONEY_BEE("Honey Bee", "Sweet honeyed plant",
            Color.fromRGB(255, 191, 0), Material.HONEYCOMB, BudType.HONEY, Particle.WAX_ON),
        OCEAN_DEEP("Ocean Deep", "Underwater aquatic plant",
            Color.fromRGB(0, 105, 148), Material.SEAGRASS, BudType.CORAL, Particle.BUBBLE_COLUMN_UP),
        SKULK_TERROR("Skulk Terror", "Deep dark horror plant",
            Color.fromRGB(0, 48, 73), Material.SCULK_VEIN, BudType.SCULK, Particle.SCULK_CHARGE_POP),
        CHERRY_BLOSSOM("Cherry Blossom", "Beautiful pink plant",
            Color.fromRGB(255, 192, 203), Material.CHERRY_LEAVES, BudType.HEART, Particle.CHERRY_LEAVES),
        RAINBOW("Rainbow", "Colorful rainbow plant",
            Color.fromRGB(255, 0, 0), Material.RED_WOOL, BudType.BEACON, Particle.TOTEM),
        ANCIENT_RELIC("Ancient Relic", "Ancient powerful plant",
            Color.fromRGB(101, 67, 33), Material.ANCIENT_DEBRIS, BudType.ANCIENT, Particle.SOUL),
        SLIME_TIME("Slime Time", "Bouncy slime plant",
            Color.fromRGB(124, 252, 0), Material.SLIME_BLOCK, BudType.SLIME, Particle.SLIME),
        CREEPER_BOOM("Creeper Boom", "Explosive creeper plant",
            Color.fromRGB(0, 128, 0), Material.TNT, BudType.CREEPER, Particle.EXPLOSION_LARGE);
        
        private final String displayName;
        private final String description;
        private final Color primaryColor;
        private final Material leafMaterial;
        private final BudType budType;
        private final Particle ambientParticle;
        
        VisualTheme(String displayName, String description, Color primaryColor, 
                    Material leafMaterial, BudType budType, Particle ambientParticle) {
            this.displayName = displayName;
            this.description = description;
            this.primaryColor = primaryColor;
            this.leafMaterial = leafMaterial;
            this.budType = budType;
            this.ambientParticle = ambientParticle;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public Color getPrimaryColor() { return primaryColor; }
        public Material getLeafMaterial() { return leafMaterial; }
        public BudType getBudType() { return budType; }
        public Particle getAmbientParticle() { return ambientParticle; }
    }
    
    /**
     * Animation styles for plant movement.
     */
    public enum AnimationStyle {
        GENTLE_SWAY("Gentle Sway", "Soft swaying in the breeze"),
        AGGRESSIVE("Aggressive", "Violent shaking movement"),
        PULSE("Pulse", "Rhythmic pulsing motion"),
        SPIN("Spin", "Slow rotating motion"),
        BOUNCE("Bounce", "Bouncy up and down motion"),
        FROZEN("Frozen", "No movement at all"),
        WAVE("Wave", "Wave-like flowing motion"),
        SHAKE("Shake", "Quick nervous shaking"),
        FLOAT("Float", "Ethereal floating motion"),
        HEARTBEAT("Heartbeat", "Pulsing like a heart");
        
        private final String displayName;
        private final String description;
        
        AnimationStyle(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Creates a default visual config.
     */
    public StrainVisualConfig() {
        // Default classic look
        this.leafColorPrimary = Color.fromRGB(34, 139, 34);
        this.leafColorSecondary = Color.fromRGB(50, 205, 50);
        this.leafColorAccent = Color.fromRGB(144, 238, 144);
        this.budColor = Color.fromRGB(0, 100, 0);
        this.stemColor = Color.fromRGB(139, 90, 43);
        
        this.leafMaterial = Material.OAK_LEAVES;
        this.budMaterial = Material.GREEN_WOOL;
        this.stemMaterial = Material.BAMBOO;
        this.potMaterial = Material.FLOWER_POT;
        
        this.budType = BudType.NORMAL;
        this.customHeadTexture = null;
        this.customHeadOwner = null;
        
        this.ambientParticle = Particle.COMPOSTER;
        this.harvestParticle = Particle.VILLAGER_HAPPY;
        this.particleIntensity = 3;
        this.glowing = false;
        this.glowColor = Color.GREEN;
        
        this.visualTheme = VisualTheme.CLASSIC;
        
        this.animationStyle = AnimationStyle.GENTLE_SWAY;
        this.animationSpeed = 1.0;
        
        this.leafScale = 1.0;
        this.budScale = 1.0;
        this.heightScale = 1.0;
    }
    
    /**
     * Creates a visual config from a pre-built theme.
     */
    public static StrainVisualConfig fromTheme(VisualTheme theme) {
        StrainVisualConfig config = new StrainVisualConfig();
        config.applyTheme(theme);
        return config;
    }
    
    /**
     * Applies a visual theme to this config.
     */
    public void applyTheme(VisualTheme theme) {
        this.visualTheme = theme;
        this.leafColorPrimary = theme.getPrimaryColor();
        this.leafMaterial = theme.getLeafMaterial();
        this.budType = theme.getBudType();
        this.budMaterial = theme.getBudType().getDefaultMaterial();
        this.ambientParticle = theme.getAmbientParticle();
        
        // Set glowing for special themes
        switch (theme) {
            case DRAGON_BREATH, RAINBOW, VOID_WALKER, SKULK_TERROR, CRYSTAL_CLEAR -> {
                this.glowing = true;
                this.glowColor = theme.getPrimaryColor();
            }
            default -> this.glowing = false;
        }
        
        // Set animation based on theme
        this.animationStyle = switch (theme) {
            case ZOMBIE_KUSH, WEREWOLF -> AnimationStyle.AGGRESSIVE;
            case FROST_BITE -> AnimationStyle.FROZEN;
            case SLIME_TIME -> AnimationStyle.BOUNCE;
            case OCEAN_DEEP -> AnimationStyle.WAVE;
            case CREEPER_BOOM -> AnimationStyle.SHAKE;
            case VOID_WALKER, DRAGON_BREATH -> AnimationStyle.FLOAT;
            case ANCIENT_RELIC -> AnimationStyle.PULSE;
            default -> AnimationStyle.GENTLE_SWAY;
        };
    }
    
    /**
     * Serializes this config to a string for storage.
     */
    public String serialize() {
        List<String> parts = new ArrayList<>();
        
        parts.add("theme:" + visualTheme.name());
        parts.add("budType:" + budType.name());
        parts.add("animation:" + animationStyle.name());
        parts.add("glowing:" + glowing);
        parts.add("particleIntensity:" + particleIntensity);
        parts.add("leafScale:" + leafScale);
        parts.add("budScale:" + budScale);
        parts.add("heightScale:" + heightScale);
        parts.add("animSpeed:" + animationSpeed);
        
        if (customHeadOwner != null) {
            parts.add("headOwner:" + customHeadOwner);
        }
        if (customHeadTexture != null) {
            parts.add("headTexture:" + customHeadTexture);
        }
        if (leafColorPrimary != null) {
            parts.add("leafColor:" + leafColorPrimary.asRGB());
        }
        if (budColor != null) {
            parts.add("budColor:" + budColor.asRGB());
        }
        if (leafMaterial != null) {
            parts.add("leafMat:" + leafMaterial.name());
        }
        if (budMaterial != null) {
            parts.add("budMat:" + budMaterial.name());
        }
        if (ambientParticle != null) {
            parts.add("particle:" + ambientParticle.name());
        }
        
        return String.join(";", parts);
    }
    
    /**
     * Deserializes a config from a string.
     */
    public static StrainVisualConfig deserialize(String data) {
        StrainVisualConfig config = new StrainVisualConfig();
        
        if (data == null || data.isEmpty()) {
            return config;
        }
        
        String[] parts = data.split(";");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            
            String key = kv[0];
            String value = kv[1];
            
            try {
                switch (key) {
                    case "theme" -> config.visualTheme = VisualTheme.valueOf(value);
                    case "budType" -> config.budType = BudType.valueOf(value);
                    case "animation" -> config.animationStyle = AnimationStyle.valueOf(value);
                    case "glowing" -> config.glowing = Boolean.parseBoolean(value);
                    case "particleIntensity" -> config.particleIntensity = Integer.parseInt(value);
                    case "leafScale" -> config.leafScale = Double.parseDouble(value);
                    case "budScale" -> config.budScale = Double.parseDouble(value);
                    case "heightScale" -> config.heightScale = Double.parseDouble(value);
                    case "animSpeed" -> config.animationSpeed = Double.parseDouble(value);
                    case "headOwner" -> config.customHeadOwner = value;
                    case "headTexture" -> config.customHeadTexture = value;
                    case "leafColor" -> config.leafColorPrimary = Color.fromRGB(Integer.parseInt(value));
                    case "budColor" -> config.budColor = Color.fromRGB(Integer.parseInt(value));
                    case "leafMat" -> config.leafMaterial = Material.valueOf(value);
                    case "budMat" -> config.budMaterial = Material.valueOf(value);
                    case "particle" -> config.ambientParticle = Particle.valueOf(value);
                }
            } catch (Exception e) {
                // Skip invalid values
            }
        }
        
        return config;
    }
    
    // ===== GETTERS & SETTERS =====
    
    public Color getLeafColorPrimary() { return leafColorPrimary; }
    public void setLeafColorPrimary(Color color) { this.leafColorPrimary = color; }
    
    public Color getLeafColorSecondary() { return leafColorSecondary; }
    public void setLeafColorSecondary(Color color) { this.leafColorSecondary = color; }
    
    public Color getLeafColorAccent() { return leafColorAccent; }
    public void setLeafColorAccent(Color color) { this.leafColorAccent = color; }
    
    public Color getBudColor() { return budColor; }
    public void setBudColor(Color color) { this.budColor = color; }
    
    public Color getStemColor() { return stemColor; }
    public void setStemColor(Color color) { this.stemColor = color; }
    
    public Material getLeafMaterial() { return leafMaterial; }
    public void setLeafMaterial(Material material) { this.leafMaterial = material; }
    
    public Material getBudMaterial() { return budMaterial; }
    public void setBudMaterial(Material material) { this.budMaterial = material; }
    
    public Material getStemMaterial() { return stemMaterial; }
    public void setStemMaterial(Material material) { this.stemMaterial = material; }
    
    public Material getPotMaterial() { return potMaterial; }
    public void setPotMaterial(Material material) { this.potMaterial = material; }
    
    public BudType getBudType() { return budType; }
    public void setBudType(BudType type) { 
        this.budType = type;
        this.budMaterial = type.getDefaultMaterial();
    }
    
    public String getCustomHeadTexture() { return customHeadTexture; }
    public void setCustomHeadTexture(String texture) { this.customHeadTexture = texture; }
    
    public String getCustomHeadOwner() { return customHeadOwner; }
    public void setCustomHeadOwner(String owner) { this.customHeadOwner = owner; }
    
    public Particle getAmbientParticle() { return ambientParticle; }
    public void setAmbientParticle(Particle particle) { this.ambientParticle = particle; }
    
    public Particle getHarvestParticle() { return harvestParticle; }
    public void setHarvestParticle(Particle particle) { this.harvestParticle = particle; }
    
    public int getParticleIntensity() { return particleIntensity; }
    public void setParticleIntensity(int intensity) { this.particleIntensity = Math.max(1, Math.min(10, intensity)); }
    
    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    
    public Color getGlowColor() { return glowColor; }
    public void setGlowColor(Color color) { this.glowColor = color; }
    
    public VisualTheme getVisualTheme() { return visualTheme; }
    public void setVisualTheme(VisualTheme theme) { applyTheme(theme); }
    
    public AnimationStyle getAnimationStyle() { return animationStyle; }
    public void setAnimationStyle(AnimationStyle style) { this.animationStyle = style; }
    
    public double getAnimationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(double speed) { this.animationSpeed = Math.max(0.1, Math.min(3.0, speed)); }
    
    public double getLeafScale() { return leafScale; }
    public void setLeafScale(double scale) { this.leafScale = Math.max(0.5, Math.min(2.0, scale)); }
    
    public double getBudScale() { return budScale; }
    public void setBudScale(double scale) { this.budScale = Math.max(0.5, Math.min(2.0, scale)); }
    
    public double getHeightScale() { return heightScale; }
    public void setHeightScale(double scale) { this.heightScale = Math.max(0.5, Math.min(2.0, scale)); }
}
