package com.budlords.farming;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import com.budlords.strain.StrainVisualConfig;
import com.budlords.strain.StrainVisualConfig.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages realistic 3D plant visualizations using armor stands.
 * Creates a multi-layered visual representation of marijuana plants
 * that grows and develops through different stages.
 * 
 * Now supports custom visual themes from StrainVisualConfig:
 * - Custom bud types (skulls, heads, special blocks)
 * - Custom leaf materials and colors
 * - Animation styles (sway, shake, pulse, etc.)
 * - Particle effects
 * - Glow effects for special strains
 * 
 * Stage 0 (Seed): Small seed in pot
 * Stage 1 (Sprout): Small green sprout emerging
 * Stage 2 (Vegetative): Medium plant with leaves spreading
 * Stage 3 (Flowering/Mature): Full plant with buds, ready to harvest
 */
public class PlantVisualizationManager {

    private final BudLords plugin;
    private final StrainManager strainManager;
    
    // Track armor stands for each plant (plant location -> list of armor stand UUIDs)
    private final Map<String, List<UUID>> plantArmorStands;
    
    // Track visual configs for animation purposes
    private final Map<String, StrainVisualConfig> plantVisualConfigs;
    
    // Animation task for swaying
    private BukkitTask animationTask;
    // Particle task for ambient effects
    private BukkitTask particleTask;
    
    // Custom model data IDs (these would correspond to resource pack custom models)
    // For now we'll use vanilla items creatively
    private static final int MODEL_SEED = 1;
    private static final int MODEL_SPROUT = 2;
    private static final int MODEL_LEAF_SMALL = 3;
    private static final int MODEL_LEAF_LARGE = 4;
    private static final int MODEL_BUD = 5;
    
    // Visual detail constants
    private static final double POSITION_RANDOMIZATION_RANGE = 0.08;
    private static final int SOIL_PARTICLE_COUNT = 3;
    private static final double SOIL_PARTICLE_SPREAD = 0.04;
    private static final int MIN_NODE_LEAVES = 2;
    private static final int MAX_NODE_LEAVES = 4;
    
    // Level of Detail (LOD) system - dynamically adjust detail based on plant count
    private enum DetailLevel {
        HIGH,      // <10 plants - full detail
        MEDIUM,    // 10-20 plants - reduced detail
        LOW        // 20+ plants - minimal detail for performance
    }
    
    /**
     * Determines the appropriate level of detail based on plant count.
     */
    private DetailLevel getDetailLevel(int plantCount) {
        if (plantCount < 10) return DetailLevel.HIGH;
        if (plantCount < 20) return DetailLevel.MEDIUM;
        return DetailLevel.LOW;
    }
    
    // Y offset to position plants inside the pot rather than floating above
    // Plant location is 1 block above pot, so we offset down to appear inside pot
    // Small armor stands render helmets ~0.5 blocks above spawn point, so we need
    // to go deeper to get the helmet inside the pot
    // 
    // Math: For an item to render at pot level:
    // - Plant location = pot + 1
    // - Helmet renders at = armor_stand_Y + 0.5
    // - To render at pot level: (pot + 1) + offset + 0.5 = pot
    // - Therefore: offset = -1.5
    //
    // To render INSIDE the pot (0.2-0.3 blocks down from top):
    // - offset = -1.5 - 0.2 = -1.7 (to -1.8)
    private static final double POT_Y_OFFSET = -1.50;
    
    // Specific Y offsets for early growth stages to ensure proper positioning
    // Stage 0 (Seed): Deep in the soil, near bottom of pot
    private static final double SEED_Y_OFFSET = -1.75;
    // Stage 1 (Sprout): Emerging from soil, still mostly inside pot  
    private static final double SPROUT_Y_OFFSET = -1.65;
    // Stage 2 (Vegetative): Growing from pot base, bottom leaves at pot rim
    private static final double VEG_Y_OFFSET = -1.55;

    public PlantVisualizationManager(BudLords plugin, StrainManager strainManager) {
        this.plugin = plugin;
        this.strainManager = strainManager;
        this.plantArmorStands = new ConcurrentHashMap<>();
        this.plantVisualConfigs = new ConcurrentHashMap<>();
        startAnimationTask();
        startParticleTask();
    }

    /**
     * Creates or updates the visual representation of a plant.
     */
    public void updatePlantVisual(Plant plant) {
        Location loc = plant.getLocation();
        String locKey = getLocationKey(loc);
        
        // Remove old visualization
        removeVisualization(locKey);
        
        // Get visual config from strain (if available)
        StrainVisualConfig visualConfig = getStrainVisualConfig(plant);
        plantVisualConfigs.put(locKey, visualConfig);
        
        // Calculate expected plant count (current count + 1 for this plant if it's new)
        // This ensures LOD is calculated correctly even during plant creation
        int expectedPlantCount = plantArmorStands.size() + 1;
        
        // Create new visualization based on growth stage
        List<UUID> armorStandIds = new ArrayList<>();
        
        switch (plant.getGrowthStage()) {
            case 0 -> armorStandIds.addAll(createSeedVisual(plant, loc, visualConfig, expectedPlantCount));
            case 1 -> armorStandIds.addAll(createSproutVisual(plant, loc, visualConfig, expectedPlantCount));
            case 2 -> armorStandIds.addAll(createVegetativeVisual(plant, loc, visualConfig, expectedPlantCount));
            case 3 -> armorStandIds.addAll(createFloweringVisual(plant, loc, visualConfig, expectedPlantCount));
        }
        
        plantArmorStands.put(locKey, armorStandIds);
        
        // NEW: Play professional growth stage transition effects
        playGrowthStageEffects(loc, plant.getGrowthStage());
        
        // Note: The flower pot block is already placed at the block BELOW the plant location
        // by the FarmingListener when the player initially places the pot.
        // We don't need to set it here - the armor stands provide the visual.
    }
    
    /**
     * NEW: Plays professional sound and particle effects when plant grows to new stage
     * Enhanced feedback for player engagement
     */
    private void playGrowthStageEffects(Location loc, int growthStage) {
        if (loc.getWorld() == null) return;
        
        World world = loc.getWorld();
        Location effectLoc = loc.clone().add(0.5, 0.5, 0.5);
        
        switch (growthStage) {
            case 0 -> { // Seed planted
                world.spawnParticle(Particle.BLOCK_CRACK, effectLoc, 8, 0.2, 0.1, 0.2, 
                    Material.DIRT.createBlockData());
                world.playSound(effectLoc, Sound.ITEM_HOE_TILL, 0.6f, 1.2f);
            }
            case 1 -> { // Sprout emerging
                world.spawnParticle(Particle.VILLAGER_HAPPY, effectLoc, 10, 0.3, 0.3, 0.3, 0);
                world.spawnParticle(Particle.COMPOSTER, effectLoc, 5, 0.2, 0.2, 0.2, 0);
                world.playSound(effectLoc, Sound.BLOCK_GRASS_BREAK, 0.5f, 1.5f);
                world.playSound(effectLoc, Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.4f, 1.3f);
            }
            case 2 -> { // Vegetative growth
                world.spawnParticle(Particle.VILLAGER_HAPPY, effectLoc.clone().add(0, 0.5, 0), 15, 0.4, 0.4, 0.4, 0);
                world.spawnParticle(Particle.BLOCK_CRACK, effectLoc, 5, 0.2, 0.1, 0.2, 
                    Material.OAK_LEAVES.createBlockData());
                world.playSound(effectLoc, Sound.BLOCK_AZALEA_LEAVES_PLACE, 0.7f, 1.1f);
                world.playSound(effectLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, 1.8f);
            }
            case 3 -> { // Flowering/Mature - PREMIUM CELEBRATION!
                // Big celebration for mature plant!
                world.spawnParticle(Particle.FIREWORKS_SPARK, effectLoc.clone().add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.05);
                world.spawnParticle(Particle.VILLAGER_HAPPY, effectLoc.clone().add(0, 0.8, 0), 25, 0.6, 0.6, 0.6, 0);
                world.spawnParticle(Particle.END_ROD, effectLoc.clone().add(0, 1.2, 0), 10, 0.3, 0.5, 0.3, 0.02);
                
                // Celebration sounds
                world.playSound(effectLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                world.playSound(effectLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 2.0f);
                world.playSound(effectLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.8f);
                
                // OPTIMIZED: Notify only nearby players using getNearbyEntities for better performance
                for (org.bukkit.entity.Entity entity : world.getNearbyEntities(effectLoc, 16, 16, 16)) {
                    if (entity instanceof Player player) {
                        player.sendMessage("§a§l✓ §aA plant has reached §6§lFULL MATURITY §anearby!");
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.8f);
                    }
                }
            }
        }
    }
    
    /**
     * Gets the visual config for a plant from its strain.
     */
    private StrainVisualConfig getStrainVisualConfig(Plant plant) {
        if (strainManager != null) {
            Strain strain = strainManager.getStrain(plant.getStrainId());
            if (strain != null && strain.getVisualConfig() != null) {
                return strain.getVisualConfig();
            }
        }
        // Return default config based on rarity
        StrainVisualConfig defaultConfig = new StrainVisualConfig();
        if (strainManager != null) {
            Strain strain = strainManager.getStrain(plant.getStrainId());
            if (strain != null) {
                switch (strain.getRarity()) {
                    case LEGENDARY -> defaultConfig.applyTheme(VisualTheme.DRAGON_BREATH);
                    case RARE -> defaultConfig.applyTheme(VisualTheme.PURPLE_HAZE);
                    case UNCOMMON -> defaultConfig.applyTheme(VisualTheme.GOLDEN_LEAF);
                    default -> defaultConfig.applyTheme(VisualTheme.CLASSIC);
                }
            }
        }
        return defaultConfig;
    }

    /**
     * Creates the seed stage visual (Stage 0).
     * A realistic cannabis seed sitting deep in the soil with proper depth.
     * Enhanced with additional detail elements for a more professional look.
     * Seeds appear firmly planted in the pot, not floating above it.
     * 
     * FIXED: Corrected Y offset calculation to prevent floating
     * IMPROVED: Added soil coverage and multiple seed detail levels
     * 
     * @param plantCount The expected total plant count (used for LOD calculation)
     */
    private List<UUID> createSeedVisual(Plant plant, Location loc, StrainVisualConfig config, int plantCount) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        DetailLevel lod = getDetailLevel(plantCount);
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        boolean glowing = config != null && config.isGlowing();
        
        // Base location - centered on the pot block with slight randomization
        double randomX = (Math.random() - 0.5) * POSITION_RANDOMIZATION_RANGE;
        double randomZ = (Math.random() - 0.5) * POSITION_RANDOMIZATION_RANGE;
        Location baseLoc = loc.clone().add(0.5 + randomX, SEED_Y_OFFSET, 0.5 + randomZ);
        
        // Soil layer representation (in MEDIUM and HIGH detail)
        if (lod != DetailLevel.LOW) {
            ArmorStand soil = createBaseArmorStand(world, baseLoc.clone().add(0, -0.06, 0));
            soil.setHelmet(new ItemStack(Material.DIRT));
            soil.setSmall(true);
            soil.setHeadPose(new EulerAngle(Math.toRadians(85), 0, 0));
            ids.add(soil.getUniqueId());
        }
        
        // Additional soil detail for HIGH mode
        if (lod == DetailLevel.HIGH) {
            // Small peat moss/soil particles around seed
            for (int i = 0; i < SOIL_PARTICLE_COUNT; i++) {
                double angle = (Math.PI * 2 / SOIL_PARTICLE_COUNT) * i;
                double offsetX = Math.cos(angle) * SOIL_PARTICLE_SPREAD;
                double offsetZ = Math.sin(angle) * SOIL_PARTICLE_SPREAD;
                ArmorStand particle = createBaseArmorStand(world, baseLoc.clone().add(offsetX, -0.03, offsetZ));
                particle.setHelmet(new ItemStack(Material.BROWN_CONCRETE_POWDER));
                particle.setSmall(true);
                particle.setHeadPose(new EulerAngle(Math.toRadians(90), angle, 0));
                ids.add(particle.getUniqueId());
            }
        }
        
        // Main seed - positioned deeper for more realistic appearance
        ArmorStand seed = createBaseArmorStand(world, baseLoc);
        seed.setHelmet(new ItemStack(Material.COCOA_BEANS));
        seed.setSmall(true);
        if (glowing) seed.setGlowing(true);
        
        // Natural tilt for organic look
        double randomAngle = Math.random() * Math.PI * 2;
        seed.setHeadPose(new EulerAngle(Math.toRadians(25), randomAngle, Math.toRadians(8)));
        ids.add(seed.getUniqueId());
        
        return ids;
    }

    /**
     * Creates the sprout stage visual (Stage 1).
     * A realistic cannabis seedling with cotyledons (seed leaves) and first true leaves emerging.
     * Shows the characteristic double-round seed leaves with the first serrated cannabis leaves above.
     * The sprout is anchored firmly in the soil with visible root system and proper stem structure.
     * 
     * FIXED: Corrected Y offset to prevent floating appearance
     * IMPROVED: Enhanced stem detail, better leaf positioning, added root representation
     * 
     * @param plantCount The expected total plant count (used for LOD calculation)
     */
    private List<UUID> createSproutVisual(Plant plant, Location loc, StrainVisualConfig config, int plantCount) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        DetailLevel lod = getDetailLevel(plantCount);
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        double leafScale = config != null ? config.getLeafScale() : 1.0;
        Material leafMaterial = config != null && config.getLeafMaterial() != null ? 
            config.getLeafMaterial() : Material.FERN;
        boolean glowing = config != null && config.isGlowing();
        
        // Add random rotation offset for visual variety
        double randomRotation = Math.random() * Math.PI * 2;
        
        Location baseLoc = loc.clone().add(0.5, SPROUT_Y_OFFSET, 0.5);
        
        // Root/soil anchor (only in MEDIUM and HIGH detail)
        if (lod != DetailLevel.LOW) {
            ArmorStand rootBase = createBaseArmorStand(world, baseLoc.clone().add(0, -0.05, 0));
            rootBase.setHelmet(new ItemStack(Material.BROWN_MUSHROOM_BLOCK));
            rootBase.setSmall(true);
            rootBase.setHeadPose(new EulerAngle(Math.toRadians(90), 0, 0));
            ids.add(rootBase.getUniqueId());
        }
        
        // Lower stem base - thicker, more visible, anchored in soil
        ArmorStand stemBase = createBaseArmorStand(world, baseLoc.clone().add(0, 0.02, 0));
        stemBase.setHelmet(new ItemStack(Material.BAMBOO));
        stemBase.setSmall(true);
        stemBase.setHeadPose(new EulerAngle(0, 0, 0)); // Straight up
        ids.add(stemBase.getUniqueId());
        
        // Middle stem segment - transition zone
        ArmorStand stemMiddle = createBaseArmorStand(world, baseLoc.clone().add(0, 0.12 * heightScale, 0));
        stemMiddle.setHelmet(new ItemStack(Material.STICK));
        stemMiddle.setSmall(true);
        ids.add(stemMiddle.getUniqueId());
        
        // Upper stem segment - young green stem
        ArmorStand stem = createBaseArmorStand(world, baseLoc.clone().add(0, 0.20 * heightScale, 0));
        stem.setHelmet(new ItemStack(Material.END_ROD));
        stem.setSmall(true);
        if (glowing) stem.setGlowing(true);
        ids.add(stem.getUniqueId());
        
        // Cotyledons (seed leaves) - rounded, opposite pairs
        for (int i = 0; i < 2; i++) {
            double angle = randomRotation + (Math.PI * i);
            double offsetX = Math.cos(angle) * 0.11 * leafScale;
            double offsetZ = Math.sin(angle) * 0.11 * leafScale;
            
            ArmorStand coty = createBaseArmorStand(world, baseLoc.clone().add(offsetX, 0.25 * heightScale, offsetZ));
            coty.setHelmet(new ItemStack(Material.LILY_PAD));
            coty.setSmall(true);
            coty.setHeadPose(new EulerAngle(Math.toRadians(50), angle + Math.toRadians(90), Math.toRadians(12)));
            ids.add(coty.getUniqueId());
        }
        
        // First true leaves - characteristic serrated cannabis leaves
        if (lod != DetailLevel.LOW) {
            for (int i = 0; i < 2; i++) {
                double angle = randomRotation + Math.PI * i + Math.toRadians(90); // Perpendicular to cotyledons
                double offsetX = Math.cos(angle) * 0.09 * leafScale;
                double offsetZ = Math.sin(angle) * 0.09 * leafScale;
                
                ArmorStand trueLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, 0.36 * heightScale, offsetZ));
                trueLeaf.setHelmet(new ItemStack(leafMaterial));
                trueLeaf.setSmall(true);
                trueLeaf.setHeadPose(new EulerAngle(Math.toRadians(38), angle + Math.toRadians(90), Math.toRadians(6)));
                ids.add(trueLeaf.getUniqueId());
            }
        }
        
        // Second set of true leaves - only in HIGH LOD
        if (lod == DetailLevel.HIGH) {
            for (int i = 0; i < 2; i++) {
                double angle = randomRotation + Math.PI * i + Math.toRadians(45);
                double offsetX = Math.cos(angle) * 0.06 * leafScale;
                double offsetZ = Math.sin(angle) * 0.06 * leafScale;
                
                ArmorStand youngLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, 0.45 * heightScale, offsetZ));
                youngLeaf.setHelmet(new ItemStack(Material.OAK_LEAVES));
                youngLeaf.setSmall(true);
                youngLeaf.setHeadPose(new EulerAngle(Math.toRadians(25), angle + Math.toRadians(90), Math.toRadians(3)));
                ids.add(youngLeaf.getUniqueId());
            }
        }
        
        // Growing tip/apical meristem - only show in HIGH LOD
        if (lod == DetailLevel.HIGH) {
            ArmorStand growingTip = createBaseArmorStand(world, baseLoc.clone().add(0, 0.52 * heightScale, 0));
            growingTip.setHelmet(new ItemStack(Material.SWEET_BERRIES));
            growingTip.setSmall(true);
            if (glowing) growingTip.setGlowing(true);
            ids.add(growingTip.getUniqueId());
        }
        
        return ids;
    }

    /**
     * Creates the vegetative stage visual (Stage 2).
     * A detailed cannabis plant in vegetative growth with multiple node levels,
     * realistic fan leaves with proper finger arrangement, and a structured stem system.
     * This stage shows rapid vegetative growth with lush green foliage anchored in the pot.
     * 
     * FIXED: Corrected Y offset to ensure plant grows from pot base, not floating
     * IMPROVED: Enhanced root representation, better stem structure, more detailed nodes
     * OPTIMIZED: Balanced detail levels - 8-18 armor stands based on LOD
     * - LOW: 8 stands (root + stem + basic leaves)
     * - MEDIUM: 13 stands (root + stem + leaves + some details)
     * - HIGH: 18 stands (root + stem + leaves + fingers + internodes)
     * 
     * @param plantCount The expected total plant count (used for LOD calculation)
     */
    private List<UUID> createVegetativeVisual(Plant plant, Location loc, StrainVisualConfig config, int plantCount) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        DetailLevel lod = getDetailLevel(plantCount);
        
        Location baseLoc = loc.clone().add(0.5, VEG_Y_OFFSET, 0.5);
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        double leafScale = config != null ? config.getLeafScale() : 1.0;
        Material leafMaterial = config != null && config.getLeafMaterial() != null ? 
            config.getLeafMaterial() : Material.FERN;
        boolean glowing = config != null && config.isGlowing();
        
        // Random rotation for visual variety
        double randomRotation = Math.random() * Math.PI * 2;
        
        // ===== ROOT SYSTEM / POT BASE (only in MEDIUM and HIGH) =====
        if (lod != DetailLevel.LOW) {
            ArmorStand rootMass = createBaseArmorStand(world, baseLoc.clone().add(0, -0.04, 0));
            rootMass.setHelmet(new ItemStack(Material.BROWN_MUSHROOM_BLOCK));
            rootMass.setSmall(true);
            rootMass.setHeadPose(new EulerAngle(Math.toRadians(85), 0, 0));
            ids.add(rootMass.getUniqueId());
        }
        
        // ===== MAIN STEM (3-4 segments based on LOD) =====
        // Lower stem - thick, woody base
        ArmorStand stemBase = createBaseArmorStand(world, baseLoc.clone().add(0, 0.02, 0));
        stemBase.setHelmet(new ItemStack(Material.DARK_OAK_LOG));
        stemBase.setSmall(true);
        ids.add(stemBase.getUniqueId());
        
        ArmorStand stemLower = createBaseArmorStand(world, baseLoc.clone().add(0, 0.12 * heightScale, 0));
        stemLower.setHelmet(new ItemStack(Material.BAMBOO));
        stemLower.setSmall(true);
        if (glowing) stemLower.setGlowing(true);
        ids.add(stemLower.getUniqueId());
        
        if (lod != DetailLevel.LOW) {
            ArmorStand stemMiddle = createBaseArmorStand(world, baseLoc.clone().add(0, 0.32 * heightScale, 0));
            stemMiddle.setHelmet(new ItemStack(Material.STICK));
            stemMiddle.setSmall(true);
            ids.add(stemMiddle.getUniqueId());
        }
        
        ArmorStand stemUpper = createBaseArmorStand(world, baseLoc.clone().add(0, 0.52 * heightScale, 0));
        stemUpper.setHelmet(new ItemStack(Material.END_ROD));
        stemUpper.setSmall(true);
        ids.add(stemUpper.getUniqueId());
        
        // ===== NODE 1 - BOTTOM FAN LEAVES =====
        double node1Height = 0.22 * heightScale;
        // Always show at least minimum leaves for structure
        int node1Leaves = (lod == DetailLevel.LOW) ? MIN_NODE_LEAVES : MAX_NODE_LEAVES;
        for (int i = 0; i < node1Leaves; i++) {
            double angle = randomRotation + (Math.PI * 2 / node1Leaves) * i;
            double offsetX = Math.cos(angle) * 0.18 * leafScale;
            double offsetZ = Math.sin(angle) * 0.18 * leafScale;
            
            ArmorStand fanLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, node1Height, offsetZ));
            fanLeaf.setHelmet(new ItemStack(Material.JUNGLE_LEAVES));
            fanLeaf.setSmall(true);
            fanLeaf.setHeadPose(new EulerAngle(Math.toRadians(55), angle + Math.toRadians(90), Math.toRadians(12)));
            ids.add(fanLeaf.getUniqueId());
            
            // Add characteristic 5-finger detail in HIGH LOD mode
            if (lod == DetailLevel.HIGH && i < 2) {
                for (int j = -1; j <= 1; j += 2) {
                    double fingerAngle = angle + Math.toRadians(j * 20);
                    double fingerDist = 0.13 * leafScale;
                    double fingerX = Math.cos(fingerAngle) * fingerDist;
                    double fingerZ = Math.sin(fingerAngle) * fingerDist;
                    
                    ArmorStand finger = createBaseArmorStand(world, baseLoc.clone().add(fingerX, node1Height + 0.02, fingerZ));
                    finger.setHelmet(new ItemStack(Material.OAK_LEAVES));
                    finger.setSmall(true);
                    finger.setHeadPose(new EulerAngle(Math.toRadians(48), fingerAngle + Math.toRadians(90), Math.toRadians(j * 8)));
                    ids.add(finger.getUniqueId());
                }
            }
        }
        
        // ===== NODE 2 - MIDDLE INTERNODAL LEAVES (only in MEDIUM and HIGH) =====
        if (lod != DetailLevel.LOW) {
            double node2Height = 0.42 * heightScale;
            int node2Leaves = (lod == DetailLevel.MEDIUM) ? MIN_NODE_LEAVES : MAX_NODE_LEAVES;
            for (int i = 0; i < node2Leaves; i++) {
                double angle = randomRotation + (Math.PI * 2 / node2Leaves) * i + Math.toRadians(45);
                double offsetX = Math.cos(angle) * 0.14 * leafScale;
                double offsetZ = Math.sin(angle) * 0.14 * leafScale;
                
                ArmorStand fanLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, node2Height, offsetZ));
                fanLeaf.setHelmet(new ItemStack(leafMaterial));
                fanLeaf.setSmall(true);
                fanLeaf.setHeadPose(new EulerAngle(Math.toRadians(45), angle + Math.toRadians(90), Math.toRadians(10)));
                ids.add(fanLeaf.getUniqueId());
            }
        }
        
        // ===== NODE 3 - UPPER DEVELOPING LEAVES (only in HIGH) =====
        if (lod == DetailLevel.HIGH) {
            double node3Height = 0.62 * heightScale;
            for (int i = 0; i < 4; i++) {
                double angle = randomRotation + (Math.PI / 2) * i;
                double offsetX = Math.cos(angle) * 0.10 * leafScale;
                double offsetZ = Math.sin(angle) * 0.10 * leafScale;
                
                ArmorStand youngLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, node3Height, offsetZ));
                youngLeaf.setHelmet(new ItemStack(Material.AZALEA_LEAVES));
                youngLeaf.setSmall(true);
                youngLeaf.setHeadPose(new EulerAngle(Math.toRadians(32), angle + Math.toRadians(90), Math.toRadians(5)));
                if (glowing) youngLeaf.setGlowing(true);
                ids.add(youngLeaf.getUniqueId());
            }
        }
        
        // ===== APICAL MERISTEM / GROWING TIP =====
        ArmorStand growingTip = createBaseArmorStand(world, baseLoc.clone().add(0, 0.72 * heightScale, 0));
        growingTip.setHelmet(new ItemStack(Material.SWEET_BERRIES));
        growingTip.setSmall(true);
        if (glowing) growingTip.setGlowing(true);
        ids.add(growingTip.getUniqueId());
        
        return ids;
    }

    /**
     * Creates the flowering/mature stage visual (Stage 3).
     * PROFESSIONAL PREMIUM QUALITY: Stunning, highly detailed cannabis plant with:
     * - Multiple impressive cola structures (main + side colas)
     * - Dense, beautiful bud formations with visible calyxes
     * - Lush sugar leaves interspersed with buds
     * - Full canopy structure with gorgeous fan leaves
     * - Enhanced trichome representation on high-quality plants
     * - Realistic proportions with extra visual polish
     * 
     * ENHANCED: More impressive visuals with 10-35 armor stands based on LOD.
     * - LOW: 10 stands (stem + enhanced buds + leaves)
     * - MEDIUM: 20 stands (stem + full buds + leaves + some details)
     * - HIGH: 35 stands (stem + premium buds + full details + trichomes)
     * 
     * @param plantCount The expected total plant count (used for LOD calculation)
     */
    private List<UUID> createFloweringVisual(Plant plant, Location loc, StrainVisualConfig config, int plantCount) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        DetailLevel lod = getDetailLevel(plantCount);
        
        Location baseLoc = loc.clone().add(0.5, POT_Y_OFFSET, 0.5);
        StarRating rating = plant.calculateFinalBudRating(null);
        int qualityLevel = rating != null ? rating.getStars() : 2;
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        double leafScale = config != null ? config.getLeafScale() : 1.0;
        double budScale = config != null ? config.getBudScale() : 1.0;
        
        Material leafMaterial = config != null && config.getLeafMaterial() != null ? 
            config.getLeafMaterial() : Material.FERN;
        BudType budType = config != null ? config.getBudType() : BudType.NORMAL;
        boolean glowing = config != null && config.isGlowing();
        
        // Random rotation for visual variety
        double randomRotation = Math.random() * Math.PI * 2;
        
        // ===== MAIN STEM (2-3 segments based on LOD) =====
        ArmorStand stemBase = createBaseArmorStand(world, baseLoc.clone().add(0, 0.08 * heightScale, 0));
        stemBase.setHelmet(new ItemStack(Material.BAMBOO));
        stemBase.setSmall(true);
        if (glowing) stemBase.setGlowing(true);
        ids.add(stemBase.getUniqueId());
        
        if (lod != DetailLevel.LOW) {
            ArmorStand stemMiddle = createBaseArmorStand(world, baseLoc.clone().add(0, 0.32 * heightScale, 0));
            stemMiddle.setHelmet(new ItemStack(Material.STICK));
            stemMiddle.setSmall(true);
            ids.add(stemMiddle.getUniqueId());
        }
        
        ArmorStand stemUpper = createBaseArmorStand(world, baseLoc.clone().add(0, 0.52 * heightScale, 0));
        stemUpper.setHelmet(new ItemStack(Material.END_ROD));
        stemUpper.setSmall(true);
        ids.add(stemUpper.getUniqueId());
        
        // ===== BOTTOM FAN LEAVES (ENHANCED - more lush and impressive) =====
        double fanLeafLevel = 0.18 * heightScale;
        int leafCount = (lod == DetailLevel.LOW) ? 3 : 6; // Increased from 2:4 to 3:6 for fuller look
        for (int i = 0; i < leafCount; i++) {
            double angle = randomRotation + (Math.PI * 2 / leafCount) * i;
            double offsetX = Math.cos(angle) * 0.22 * leafScale;
            double offsetZ = Math.sin(angle) * 0.22 * leafScale;
            
            ArmorStand fanLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, fanLeafLevel, offsetZ));
            fanLeaf.setHelmet(new ItemStack(Material.JUNGLE_LEAVES));
            fanLeaf.setSmall(true);
            fanLeaf.setHeadPose(new EulerAngle(Math.toRadians(58), angle + Math.toRadians(90), Math.toRadians(10)));
            if (glowing && qualityLevel >= 4) fanLeaf.setGlowing(true); // High quality plants glow
            ids.add(fanLeaf.getUniqueId());
            
            // PROFESSIONAL: Add more finger details for richer, fuller appearance
            if (lod != DetailLevel.LOW && i < 3) { // More leaves get fingers
                for (int f = -1; f <= 1; f += 2) {
                    double fingerAngle = angle + Math.toRadians(f * 16);
                    double fingerX = Math.cos(fingerAngle) * 0.13 * leafScale;
                    double fingerZ = Math.sin(fingerAngle) * 0.13 * leafScale;
                    
                    ArmorStand finger = createBaseArmorStand(world, baseLoc.clone().add(fingerX, fanLeafLevel + 0.02, fingerZ));
                    finger.setHelmet(new ItemStack(Material.OAK_LEAVES));
                    finger.setSmall(true);
                    finger.setHeadPose(new EulerAngle(Math.toRadians(52), fingerAngle + Math.toRadians(90), Math.toRadians(f * 6)));
                    ids.add(finger.getUniqueId());
                }
            }
        }
        
        // ===== BRANCH BUDS (ENHANCED - more impressive bud count) =====
        int branchBudCount;
        if (lod == DetailLevel.LOW) {
            branchBudCount = 3; // Increased from 2
        } else if (lod == DetailLevel.MEDIUM) {
            branchBudCount = 5; // Increased from 3
        } else {
            branchBudCount = Math.min(qualityLevel + 2, 6); // More buds for impressive look
        }
        
        for (int i = 0; i < branchBudCount; i++) {
            double angle = randomRotation + (Math.PI * 2 / branchBudCount) * i;
            double radius = 0.14 * budScale;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double height = (0.52 + (i % 2) * 0.06) * heightScale;
            
            // Skip branch stems in LOW mode for performance
            if (lod != DetailLevel.LOW) {
                ArmorStand branchStem = createBaseArmorStand(world, baseLoc.clone().add(offsetX * 0.5, height - 0.08, offsetZ * 0.5));
                branchStem.setHelmet(new ItemStack(Material.STICK));
                branchStem.setSmall(true);
                branchStem.setHeadPose(new EulerAngle(Math.toRadians(45), angle, 0));
                ids.add(branchStem.getUniqueId());
            }
            
            ArmorStand sideBud = createBaseArmorStand(world, baseLoc.clone().add(offsetX, height, offsetZ));
            sideBud.setHelmet(createCustomBudItem(rating, budType, config, false));
            sideBud.setSmall(true);
            if (glowing) sideBud.setGlowing(true);
            sideBud.setHeadPose(new EulerAngle(Math.toRadians(22), angle, Math.toRadians(12)));
            ids.add(sideBud.getUniqueId());
        }
        
        // ===== MAIN COLA (PROFESSIONAL - more impressive and detailed) =====
        double colaBase = 0.68 * heightScale;
        
        // ENHANCED: More sugar leaves for fuller, more premium appearance
        int sugarLeafCount = switch (lod) {
            case LOW -> 2;      // Even LOW gets some detail
            case MEDIUM -> 4;   // Increased from 2
            case HIGH -> 6;     // Increased from 4 for lush look
        };
        
        for (int i = 0; i < sugarLeafCount; i++) {
            double angle = randomRotation + (Math.PI * 2 / sugarLeafCount) * i;
            ArmorStand colaSugarLeaf = createBaseArmorStand(world, baseLoc.clone().add(
                Math.cos(angle) * 0.06, colaBase, Math.sin(angle) * 0.06));
            colaSugarLeaf.setHelmet(new ItemStack(Material.BIRCH_LEAVES));
            colaSugarLeaf.setSmall(true);
            colaSugarLeaf.setHeadPose(new EulerAngle(Math.toRadians(32), angle + Math.toRadians(90), 0));
            if (glowing && qualityLevel >= 3) colaSugarLeaf.setGlowing(true);
            ids.add(colaSugarLeaf.getUniqueId());
        }
        
        // PROFESSIONAL: More cola segments for impressive, dense bud structure
        int colaSegments = switch (lod) {
            case LOW -> 2;      // Increased from 1
            case MEDIUM -> 3;   // Increased from 2
            case HIGH -> 5;     // Increased from 3 for stunning appearance
        };
        
        for (int i = 0; i < colaSegments; i++) {
            double yOffset = colaBase + (0.04 + i * 0.06) * heightScale;
            ArmorStand colaSegment = createBaseArmorStand(world, baseLoc.clone().add(0, yOffset, 0));
            colaSegment.setHelmet(createCustomBudItem(rating, budType, config, true));
            colaSegment.setSmall(true);
            if (glowing) colaSegment.setGlowing(true);
            ids.add(colaSegment.getUniqueId());
        }
        
        // ===== PISTILS (ENHANCED - visible on 3+ star plants in MEDIUM and HIGH) =====
        if (lod != DetailLevel.LOW && qualityLevel >= 3) {
            int pistilCount = (lod == DetailLevel.MEDIUM) ? 4 : 6; // More pistils in HIGH mode
            for (int i = 0; i < pistilCount; i++) {
                double angle = randomRotation + (Math.PI * 2 / pistilCount) * i;
                ArmorStand pistil = createBaseArmorStand(world, baseLoc.clone().add(
                    Math.cos(angle) * 0.035, colaBase + 0.16 * heightScale, Math.sin(angle) * 0.035));
                pistil.setHelmet(new ItemStack(Material.ORANGE_WOOL));
                pistil.setSmall(true);
                pistil.setHeadPose(new EulerAngle(Math.toRadians(18), angle, Math.toRadians(25)));
                ids.add(pistil.getUniqueId());
            }
        }
        
        // ===== TRICHOMES (PROFESSIONAL - visible on 4+ star plants for premium look) =====
        if (lod != DetailLevel.LOW && qualityLevel >= 4) {
            // Multiple trichome layers for ultra-premium appearance
            int trichomeCount = (lod == DetailLevel.HIGH && qualityLevel >= 5) ? 3 : 2;
            for (int t = 0; t < trichomeCount; t++) {
                ArmorStand trichomes = createBaseArmorStand(world, baseLoc.clone().add(0, colaBase + (0.12 + t * 0.04) * heightScale, 0));
                trichomes.setHelmet(new ItemStack(Material.WHITE_STAINED_GLASS));
                trichomes.setSmall(true);
                trichomes.setGlowing(true);
                ids.add(trichomes.getUniqueId());
            }
        }
        
        return ids;
    }

    /**
     * Creates a base armor stand with proper settings.
     */
    private ArmorStand createBaseArmorStand(World world, Location loc) {
        ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setInvulnerable(true);
        stand.setCanPickupItems(false);
        stand.setPersistent(true);
        stand.setCustomNameVisible(false);
        stand.setMarker(true); // Makes it non-collidable
        
        return stand;
    }

    /**
     * Creates a colored leaf item using the config's leaf material.
     */
    private ItemStack createColoredLeaf(Color color, boolean small, StrainVisualConfig config) {
        // Use config leaf material if available, otherwise default
        Material mat;
        if (config != null && config.getLeafMaterial() != null) {
            mat = config.getLeafMaterial();
        } else {
            mat = small ? Material.OAK_LEAVES : Material.JUNGLE_LEAVES;
        }
        return new ItemStack(mat);
    }
    
    /**
     * Creates a custom bud item based on the visual config and bud type.
     * This supports skulls, heads, and special materials for unique strains.
     */
    private ItemStack createCustomBudItem(StarRating rating, BudType budType, StrainVisualConfig config, boolean isMainCola) {
        if (budType == null) budType = BudType.NORMAL;
        
        // Use the bud type's default material
        Material budMaterial = budType.getDefaultMaterial();
        
        // For normal type, adjust based on quality
        if (budType == BudType.NORMAL) {
            if (rating == null) {
                budMaterial = Material.GREEN_WOOL;
            } else {
                budMaterial = switch (rating) {
                    case ONE_STAR -> Material.MOSS_BLOCK;
                    case TWO_STAR -> Material.GREEN_WOOL;
                    case THREE_STAR -> Material.LIME_WOOL;
                    case FOUR_STAR -> Material.LIME_CONCRETE;
                    case FIVE_STAR -> Material.EMERALD_BLOCK;
                    case SIX_STAR -> Material.DIAMOND_BLOCK;
                };
            }
        }
        
        ItemStack budItem = new ItemStack(budMaterial);
        
        // For player head type, set custom texture or owner
        if (budType == BudType.PLAYER_HEAD && config != null) {
            if (config.getCustomHeadOwner() != null) {
                SkullMeta meta = (SkullMeta) budItem.getItemMeta();
                if (meta != null) {
                    meta.setOwner(config.getCustomHeadOwner());
                    budItem.setItemMeta(meta);
                }
            }
        }
        
        return budItem;
    }

    /**
     * Creates a bud item based on quality rating (legacy method).
     */
    private ItemStack createBudItem(StarRating rating, boolean isMainCola) {
        return createCustomBudItem(rating, BudType.NORMAL, null, isMainCola);
    }

    /**
     * Gets a color for the strain based on rarity.
     */
    private Color getStrainColor(Plant plant) {
        if (strainManager == null) return Color.GREEN;
        
        Strain strain = strainManager.getStrain(plant.getStrainId());
        if (strain == null) return Color.GREEN;
        
        return switch (strain.getRarity()) {
            case COMMON -> Color.fromRGB(34, 139, 34);      // Forest green
            case UNCOMMON -> Color.fromRGB(50, 205, 50);    // Lime green
            case RARE -> Color.fromRGB(138, 43, 226);       // Blue violet
            case LEGENDARY -> Color.fromRGB(255, 215, 0);   // Gold
        };
    }

    /**
     * Removes all armor stands for a plant.
     * Also ensures all related visual configs are cleaned up to prevent memory leaks
     * and lingering particle effects.
     */
    public void removeVisualization(String locKey) {
        List<UUID> ids = plantArmorStands.remove(locKey);
        plantVisualConfigs.remove(locKey);
        if (ids == null) return;
        
        // Remove all armor stands associated with this plant
        for (UUID id : ids) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity instanceof ArmorStand) {
                entity.remove();
            }
        }
    }

    /**
     * Removes visualization at a specific location.
     */
    public void removeVisualization(Location loc) {
        removeVisualization(getLocationKey(loc));
    }

    /**
     * Starts the animation task with professional, smooth animations.
     * ENHANCED: More fluid and realistic plant movements with better performance.
     * Professional quality animations that look natural and polished.
     */
    private void startAnimationTask() {
        animationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long time = System.currentTimeMillis();
            int plantCount = plantArmorStands.size();
            
            // Get detail level based on plant count
            DetailLevel lod = getDetailLevel(plantCount);
            
            // Professional smooth animation - always enabled for quality
            // Only reduce in extreme LOW detail scenarios
            if (lod == DetailLevel.LOW && time % 3000 > 1500) {
                // Skip animation only 50% of time in extreme LOW mode
                return;
            }
            
            // Process more plants per tick for smoother overall effect
            int maxProcessPerTick = switch (lod) {
                case LOW -> 10;     // Increased from 5 for better quality
                case MEDIUM -> 25;  // Increased from 15 for smoother animations
                case HIGH -> 80;    // Increased from 50 for maximum smoothness
            };
            
            int processed = 0;
            
            for (Map.Entry<String, List<UUID>> entry : plantArmorStands.entrySet()) {
                if (processed >= maxProcessPerTick) break;
                processed++;
                
                String locKey = entry.getKey();
                StrainVisualConfig config = plantVisualConfigs.get(locKey);
                
                AnimationStyle style = config != null ? config.getAnimationStyle() : AnimationStyle.GENTLE_SWAY;
                double speed = config != null ? config.getAnimationSpeed() : 1.0;
                
                // Skip frozen plants
                if (style == AnimationStyle.FROZEN) continue;
                
                List<UUID> armorStands = entry.getValue();
                if (armorStands.isEmpty()) continue;
                
                // PROFESSIONAL: Enhanced animation intensity for more impressive visuals
                double intensityMult = switch (lod) {
                    case LOW -> 0.7;    // 70% intensity (increased from 30%)
                    case MEDIUM -> 0.9; // 90% intensity (increased from 60%)
                    case HIGH -> 1.3;   // 130% intensity - more dramatic and cool!
                };
                
                // Animate MORE armor stands for smoother, more professional look
                int updateInterval = switch (lod) {
                    case LOW -> 2;      // Every 2nd stand (was 4th)
                    case MEDIUM -> 1;   // All stands (was 2nd)
                    case HIGH -> 1;     // All stands with enhanced quality
                };
                
                // PROFESSIONAL: More fluid and natural animations with improved formulas
                double sway = 0;
                double bounce = 0;
                double spin = 0;
                
                switch (style) {
                    case GENTLE_SWAY -> sway = Math.sin(time / 1200.0 * speed) * 0.035 * intensityMult; // Smoother, more visible
                    case AGGRESSIVE -> sway = Math.sin(time / 180.0 * speed) * 0.10 * intensityMult; // More dramatic
                    case PULSE -> bounce = Math.sin(time / 600.0 * speed) * 0.045 * intensityMult; // Enhanced pulse
                    case SPIN -> spin = (time / 2500.0 * speed) % (Math.PI * 2) * intensityMult; // Slower, smoother spin
                    case BOUNCE -> bounce = Math.abs(Math.sin(time / 350.0 * speed)) * 0.06 * intensityMult; // More bounce
                    case WAVE -> sway = Math.sin(time / 900.0 * speed) * 0.05 * intensityMult; // Enhanced wave
                    case SHAKE -> sway = (Math.random() - 0.5) * 0.04 * speed * intensityMult; // Subtle shake
                    case FLOAT -> bounce = Math.sin(time / 1800.0 * speed) * 0.04 * intensityMult; // Ethereal float
                    case HEARTBEAT -> {
                        double beat = (time % 1200) / 1200.0;
                        bounce = (beat < 0.12 || (beat > 0.24 && beat < 0.36)) ? 0.05 * intensityMult : 0;
                    }
                    default -> sway = Math.sin(time / 1200.0 * speed) * 0.035 * intensityMult;
                }
                
                // Update armor stands with smooth, professional animations
                for (int i = 0; i < armorStands.size(); i += updateInterval) {
                    UUID id = armorStands.get(i);
                    Entity entity = Bukkit.getEntity(id);
                    if (entity instanceof ArmorStand stand) {
                        EulerAngle current = stand.getHeadPose();
                        // Enhanced animation with better visual appeal
                        EulerAngle newPose = new EulerAngle(
                            current.getX() + sway * 0.15,  // Increased from 0.1 for more visibility
                            current.getY() + spin,
                            current.getZ() + sway * 0.08   // Increased from 0.05 for better motion
                        );
                        stand.setHeadPose(newPose);
                    }
                }
            }
        }, 10L, 10L); // PROFESSIONAL: Back to 10L (0.5 sec) for smoother, more responsive animations
    }
    
    /**
     * Starts the particle effect task for professional ambient effects.
     * ENHANCED: More beautiful and impressive particle effects for premium visual quality.
     */
    private void startParticleTask() {
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // Clean up any stale entries (plants that no longer exist)
            plantArmorStands.entrySet().removeIf(entry -> {
                List<UUID> ids = entry.getValue();
                if (ids == null || ids.isEmpty()) return true;
                
                // Check if at least one armor stand still exists
                boolean hasValidStand = false;
                for (UUID id : ids) {
                    Entity entity = Bukkit.getEntity(id);
                    if (entity != null && !entity.isDead()) {
                        hasValidStand = true;
                        break;
                    }
                }
                return !hasValidStand;
            });
            
            int plantCount = plantArmorStands.size();
            DetailLevel lod = getDetailLevel(plantCount);
            
            // PROFESSIONAL: More plants get beautiful particles for better visuals
            int maxParticlesPerCycle = switch (lod) {
                case LOW -> 8;      // Increased from 3 for better visual quality
                case MEDIUM -> 20;  // Increased from 10 for richer effects
                case HIGH -> 50;    // Increased from 30 for stunning visuals
            };
            
            int particlesSpawned = 0;
            
            for (Map.Entry<String, List<UUID>> entry : plantArmorStands.entrySet()) {
                if (particlesSpawned >= maxParticlesPerCycle) break;
                
                String locKey = entry.getKey();
                StrainVisualConfig config = plantVisualConfigs.get(locKey);
                
                if (config == null) continue;
                
                Particle particle = config.getAmbientParticle();
                int intensity = config.getParticleIntensity();
                
                if (particle == null || intensity <= 0) continue;
                
                // PROFESSIONAL: Reduced skipping for more consistent, impressive effects
                if (lod == DetailLevel.LOW && Math.random() > 0.6) continue;      // Only 40% skip (was 70%)
                if (lod == DetailLevel.MEDIUM && Math.random() > 0.8) continue;   // Only 20% skip (was 40%)
                
                String[] parts = locKey.split(",");
                if (parts.length != 4) continue;
                
                try {
                    World world = Bukkit.getWorld(parts[0]);
                    if (world == null) continue;
                    
                    // Check if chunk is loaded
                    int chunkX = Integer.parseInt(parts[1]) >> 4;
                    int chunkZ = Integer.parseInt(parts[3]) >> 4;
                    if (!world.isChunkLoaded(chunkX, chunkZ)) continue;
                    
                    double x = Double.parseDouble(parts[1]) + 0.5;
                    double y = Double.parseDouble(parts[2]) + 0.5;
                    double z = Double.parseDouble(parts[3]) + 0.5;
                    
                    Location loc = new Location(world, x, y, z);
                    
                    // PROFESSIONAL: More particles for richer, more impressive visuals
                    int count = switch (lod) {
                        case LOW -> Math.max(2, intensity / 3);             // More visible (was 1)
                        case MEDIUM -> Math.max(3, intensity / 2);          // 50% of normal (was 25%)
                        case HIGH -> Math.max(4, (int)(intensity * 0.8));   // 80% of normal (was 50%)
                    };
                    
                    // Enhanced particle spread for more natural, professional look
                    world.spawnParticle(particle, loc, count, 0.25, 0.4, 0.25, 0.015);
                    particlesSpawned++;
                    
                } catch (NumberFormatException e) {
                    // Skip invalid locations
                }
            }
        }, 30L, 60L); // PROFESSIONAL: More frequent updates (was 40L, 100L) for richer ambient effects
    }

    /**
     * Gets a unique key for a location.
     */
    private String getLocationKey(Location loc) {
        return loc.getWorld().getName() + "," + 
               loc.getBlockX() + "," + 
               loc.getBlockY() + "," + 
               loc.getBlockZ();
    }
    
    /**
     * NEW: Spawns special premium effects for high-quality plants
     * Professional visual feedback for legendary/5-star plants
     */
    public void spawnPremiumPlantEffects(Location loc, StarRating rating, Strain strain) {
        if (loc.getWorld() == null) return;
        
        // Only for high-quality plants (4-5 stars)
        if (rating == null || rating.getStars() < 4) return;
        
        int stars = rating.getStars();
        Location effectLoc = loc.clone().add(0.5, 1.0, 0.5);
        World world = loc.getWorld();
        
        // 5-star legendary effects
        if (stars >= 5) {
            // Golden sparkles
            world.spawnParticle(Particle.END_ROD, effectLoc, 3, 0.3, 0.5, 0.3, 0.02);
            world.spawnParticle(Particle.FIREWORKS_SPARK, effectLoc, 2, 0.2, 0.3, 0.2, 0.01);
            
            // Legendary glow
            if (Math.random() < 0.1) { // 10% chance per tick
                world.spawnParticle(Particle.SPELL_WITCH, effectLoc, 5, 0.4, 0.6, 0.4, 0.05);
                world.playSound(effectLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.2f, 1.8f);
            }
        }
        
        // 4-star premium effects
        if (stars >= 4) {
            // Green sparkles
            world.spawnParticle(Particle.VILLAGER_HAPPY, effectLoc, 2, 0.3, 0.4, 0.3, 0.01);
            
            // Rare glow pulse
            if (Math.random() < 0.05) { // 5% chance
                world.spawnParticle(Particle.ENCHANTMENT_TABLE, effectLoc, 8, 0.5, 0.5, 0.5, 0.5);
            }
        }
        
        // Strain-specific color effects (legendary strains only)
        if (strain != null && strain.getRarity() == Strain.Rarity.LEGENDARY) {
            // Purple/pink aura for legendary strains
            world.spawnParticle(Particle.DRAGON_BREATH, effectLoc, 1, 0.2, 0.3, 0.2, 0.01);
            
            // Occasional burst
            if (Math.random() < 0.02) {  // 2% chance
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, effectLoc, 10, 0.4, 0.6, 0.4, 0.02);
                world.playSound(effectLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.15f, 2.0f);
            }
        }
    }

    /**
     * Cleans up all visualizations (called on plugin disable).
     */
    public void shutdown() {
        if (animationTask != null) {
            animationTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
        
        // Remove all armor stands
        for (List<UUID> ids : plantArmorStands.values()) {
            for (UUID id : ids) {
                Entity entity = Bukkit.getEntity(id);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        plantArmorStands.clear();
        plantVisualConfigs.clear();
    }

    /**
     * Reloads all plant visualizations (e.g., after server restart).
     */
    public void reloadAllVisualizations(Collection<Plant> plants) {
        for (Plant plant : plants) {
            updatePlantVisual(plant);
        }
    }
}
