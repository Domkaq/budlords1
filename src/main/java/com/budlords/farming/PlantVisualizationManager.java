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
    private static final double POT_Y_OFFSET = -1.35;
    
    // Specific Y offsets for early growth stages (stages 0-2) to ensure they appear inside the pot
    // These stages need deeper positioning as they're smaller and should be more contained
    private static final double SEED_Y_OFFSET = -1.5;    // Seeds deep inside pot
    private static final double SPROUT_Y_OFFSET = -1.45; // Sprouts slightly higher but still in pot
    private static final double VEG_Y_OFFSET = -1.5;     // Vegetative - lower to prevent floating

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
        
        // Note: The flower pot block is already placed at the block BELOW the plant location
        // by the FarmingListener when the player initially places the pot.
        // We don't need to set it here - the armor stands provide the visual.
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
     * A realistic cannabis seed sitting in soil with a small dirt mound appearance.
     * Professional visualization with proper depth and detail.
     * Plants appear INSIDE the pot, not floating above.
     * 
     * OPTIMIZED: Reduced from 2 armor stands to 1 for performance.
     * 
     * @param plantCount The expected total plant count (used for LOD calculation)
     */
    private List<UUID> createSeedVisual(Plant plant, Location loc, StrainVisualConfig config, int plantCount) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        boolean glowing = config != null && config.isGlowing();
        
        // Base location - centered on the pot block
        // Add random offset for variety (-0.05 to 0.05)
        double randomX = (Math.random() - 0.5) * 0.1;
        double randomZ = (Math.random() - 0.5) * 0.1;
        Location baseLoc = loc.clone().add(0.5 + randomX, SEED_Y_OFFSET, 0.5 + randomZ);
        
        // Single seed with random rotation for variety
        ArmorStand seed = createBaseArmorStand(world, baseLoc);
        seed.setHelmet(new ItemStack(Material.COCOA_BEANS));
        seed.setSmall(true);
        if (glowing) seed.setGlowing(true);
        
        // Random tilt angle for visual variety
        double randomAngle = Math.random() * Math.PI * 2;
        seed.setHeadPose(new EulerAngle(Math.toRadians(25), randomAngle, 0));
        ids.add(seed.getUniqueId());
        
        return ids;
    }

    /**
     * Creates the sprout stage visual (Stage 1).
     * A realistic cannabis seedling with cotyledons (seed leaves) and first true leaves emerging.
     * Shows the characteristic double-round seed leaves with the first serrated cannabis leaves above.
     * 
     * OPTIMIZED: Reduced from 6 armor stands to 3-4 based on LOD.
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
        
        // Stem (always shown)
        ArmorStand stem = createBaseArmorStand(world, baseLoc.clone().add(0, 0.12 * heightScale, 0));
        stem.setHelmet(new ItemStack(Material.END_ROD));
        stem.setSmall(true);
        if (glowing) stem.setGlowing(true);
        ids.add(stem.getUniqueId());
        
        // Cotyledons - simplified to 2 leaves in all LOD modes
        for (int i = 0; i < 2; i++) {
            double angle = randomRotation + (Math.PI * i);
            double offsetX = Math.cos(angle) * 0.1 * leafScale;
            double offsetZ = Math.sin(angle) * 0.1 * leafScale;
            
            ArmorStand coty = createBaseArmorStand(world, baseLoc.clone().add(offsetX, 0.22 * heightScale, offsetZ));
            coty.setHelmet(new ItemStack(Material.LILY_PAD));
            coty.setSmall(true);
            coty.setHeadPose(new EulerAngle(Math.toRadians(55), angle + Math.toRadians(90), Math.toRadians(15)));
            ids.add(coty.getUniqueId());
        }
        
        // Growing tip - only show in HIGH and MEDIUM LOD
        if (lod != DetailLevel.LOW) {
            ArmorStand growingTip = createBaseArmorStand(world, baseLoc.clone().add(0, 0.42 * heightScale, 0));
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
     * This stage shows rapid vegetative growth with lush green foliage.
     * 
     * OPTIMIZED: Reduced from 30+ armor stands to 6-15 based on LOD.
     * - LOW: 6 stands (stem + main leaves only)
     * - MEDIUM: 10 stands (stem + leaves, no fingers)
     * - HIGH: 15 stands (stem + leaves + simplified fingers)
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
        
        // ===== MAIN STEM (2-3 segments based on LOD) =====
        ArmorStand stemLower = createBaseArmorStand(world, baseLoc.clone().add(0, 0.08 * heightScale, 0));
        stemLower.setHelmet(new ItemStack(Material.BAMBOO));
        stemLower.setSmall(true);
        if (glowing) stemLower.setGlowing(true);
        ids.add(stemLower.getUniqueId());
        
        if (lod != DetailLevel.LOW) {
            ArmorStand stemMiddle = createBaseArmorStand(world, baseLoc.clone().add(0, 0.28 * heightScale, 0));
            stemMiddle.setHelmet(new ItemStack(Material.STICK));
            stemMiddle.setSmall(true);
            ids.add(stemMiddle.getUniqueId());
        }
        
        ArmorStand stemUpper = createBaseArmorStand(world, baseLoc.clone().add(0, 0.48 * heightScale, 0));
        stemUpper.setHelmet(new ItemStack(Material.END_ROD));
        stemUpper.setSmall(true);
        ids.add(stemUpper.getUniqueId());
        
        // ===== NODE 1 - BOTTOM FAN LEAVES =====
        double node1Height = 0.18 * heightScale;
        int node1Leaves = (lod == DetailLevel.LOW) ? 2 : 2;
        for (int i = 0; i < node1Leaves; i++) {
            double angle = randomRotation + (Math.PI * i);
            double offsetX = Math.cos(angle) * 0.2 * leafScale;
            double offsetZ = Math.sin(angle) * 0.2 * leafScale;
            
            ArmorStand fanLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, node1Height, offsetZ));
            fanLeaf.setHelmet(new ItemStack(Material.JUNGLE_LEAVES));
            fanLeaf.setSmall(true);
            fanLeaf.setHeadPose(new EulerAngle(Math.toRadians(55), angle + Math.toRadians(90), Math.toRadians(12)));
            ids.add(fanLeaf.getUniqueId());
            
            // Add finger details only in HIGH LOD mode (simplified to 2 fingers instead of 4)
            if (lod == DetailLevel.HIGH) {
                for (int j = -1; j <= 1; j += 2) { // Only -1 and 1
                    double fingerAngle = angle + Math.toRadians(j * 18);
                    double fingerDist = 0.12 * leafScale;
                    double fingerX = Math.cos(fingerAngle) * fingerDist;
                    double fingerZ = Math.sin(fingerAngle) * fingerDist;
                    
                    ArmorStand finger = createBaseArmorStand(world, baseLoc.clone().add(fingerX, node1Height + 0.02, fingerZ));
                    finger.setHelmet(new ItemStack(Material.OAK_LEAVES));
                    finger.setSmall(true);
                    finger.setHeadPose(new EulerAngle(Math.toRadians(50), fingerAngle + Math.toRadians(90), Math.toRadians(j * 6)));
                    ids.add(finger.getUniqueId());
                }
            }
        }
        
        // ===== NODE 2 - MIDDLE LEAVES (only in MEDIUM and HIGH) =====
        if (lod != DetailLevel.LOW) {
            double node2Height = 0.38 * heightScale;
            for (int i = 0; i < 2; i++) {
                double angle = randomRotation + Math.PI * i + Math.toRadians(90);
                double offsetX = Math.cos(angle) * 0.16 * leafScale;
                double offsetZ = Math.sin(angle) * 0.16 * leafScale;
                
                ArmorStand fanLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, node2Height, offsetZ));
                fanLeaf.setHelmet(new ItemStack(leafMaterial));
                fanLeaf.setSmall(true);
                fanLeaf.setHeadPose(new EulerAngle(Math.toRadians(42), angle + Math.toRadians(90), Math.toRadians(8)));
                ids.add(fanLeaf.getUniqueId());
            }
        }
        
        // ===== NODE 3 - UPPER YOUNG LEAVES (only in HIGH) =====
        if (lod == DetailLevel.HIGH) {
            double node3Height = 0.55 * heightScale;
            for (int i = 0; i < 4; i++) {
                double angle = randomRotation + (Math.PI / 2) * i;
                double offsetX = Math.cos(angle) * 0.09 * leafScale;
                double offsetZ = Math.sin(angle) * 0.09 * leafScale;
                
                ArmorStand youngLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, node3Height, offsetZ));
                youngLeaf.setHelmet(new ItemStack(Material.OAK_LEAVES));
                youngLeaf.setSmall(true);
                youngLeaf.setHeadPose(new EulerAngle(Math.toRadians(28), angle + Math.toRadians(90), 0));
                if (glowing) youngLeaf.setGlowing(true);
                ids.add(youngLeaf.getUniqueId());
            }
        }
        
        // ===== GROWING TIP =====
        ArmorStand growingTip = createBaseArmorStand(world, baseLoc.clone().add(0, 0.65 * heightScale, 0));
        growingTip.setHelmet(new ItemStack(Material.SWEET_BERRIES));
        growingTip.setSmall(true);
        if (glowing) growingTip.setGlowing(true);
        ids.add(growingTip.getUniqueId());
        
        return ids;
    }

    /**
     * Creates the flowering/mature stage visual (Stage 3).
     * A professional, highly detailed cannabis plant in full flower with:
     * - Multiple cola structures (main + side colas)
     * - Dense bud formations with visible calyxes
     * - Sugar leaves interspersed with buds
     * - Lower fan leaves for canopy structure
     * - Proper trichome representation on high-quality plants
     * - Realistic proportions matching actual cannabis plants
     * 
     * OPTIMIZED: Reduced from 40+ armor stands to 8-25 based on LOD.
     * - LOW: 8 stands (stem + basic buds + 2 leaves)
     * - MEDIUM: 15 stands (stem + buds + leaves, no fingers/calyxes)
     * - HIGH: 25 stands (stem + buds + leaves + simplified details)
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
        
        // ===== BOTTOM FAN LEAVES (simplified) =====
        double fanLeafLevel = 0.18 * heightScale;
        int leafCount = (lod == DetailLevel.LOW) ? 2 : 4;
        for (int i = 0; i < leafCount; i++) {
            double angle = randomRotation + (Math.PI * 2 / leafCount) * i;
            double offsetX = Math.cos(angle) * 0.22 * leafScale;
            double offsetZ = Math.sin(angle) * 0.22 * leafScale;
            
            ArmorStand fanLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, fanLeafLevel, offsetZ));
            fanLeaf.setHelmet(new ItemStack(Material.JUNGLE_LEAVES));
            fanLeaf.setSmall(true);
            fanLeaf.setHeadPose(new EulerAngle(Math.toRadians(58), angle + Math.toRadians(90), Math.toRadians(10)));
            ids.add(fanLeaf.getUniqueId());
            
            // Only add fingers in HIGH LOD mode (and only 2 fingers instead of 4)
            if (lod == DetailLevel.HIGH && i < 2) {
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
        
        // ===== BRANCH BUDS (reduced count based on LOD) =====
        int branchBudCount;
        if (lod == DetailLevel.LOW) {
            branchBudCount = 2; // Minimal buds
        } else if (lod == DetailLevel.MEDIUM) {
            branchBudCount = 3; // Medium buds
        } else {
            branchBudCount = Math.min(qualityLevel + 1, 4); // Reduced from qualityLevel + 2
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
        
        // ===== MAIN COLA (simplified based on LOD) =====
        double colaBase = 0.68 * heightScale;
        
        // Sugar leaves only in MEDIUM and HIGH
        if (lod != DetailLevel.LOW) {
            int sugarLeafCount = (lod == DetailLevel.MEDIUM) ? 2 : 4;
            for (int i = 0; i < sugarLeafCount; i++) {
                double angle = randomRotation + (Math.PI * 2 / sugarLeafCount) * i;
                ArmorStand colaSugarLeaf = createBaseArmorStand(world, baseLoc.clone().add(
                    Math.cos(angle) * 0.06, colaBase, Math.sin(angle) * 0.06));
                colaSugarLeaf.setHelmet(new ItemStack(Material.BIRCH_LEAVES));
                colaSugarLeaf.setSmall(true);
                colaSugarLeaf.setHeadPose(new EulerAngle(Math.toRadians(32), angle + Math.toRadians(90), 0));
                ids.add(colaSugarLeaf.getUniqueId());
            }
        }
        
        // Main cola segments (3 in HIGH, 2 in MEDIUM, 1 in LOW)
        int colaSegments = (lod == DetailLevel.LOW) ? 1 : (lod == DetailLevel.MEDIUM) ? 2 : 3;
        for (int i = 0; i < colaSegments; i++) {
            double yOffset = colaBase + (0.05 + i * 0.07) * heightScale;
            ArmorStand colaSegment = createBaseArmorStand(world, baseLoc.clone().add(0, yOffset, 0));
            colaSegment.setHelmet(createCustomBudItem(rating, budType, config, true));
            colaSegment.setSmall(true);
            if (glowing) colaSegment.setGlowing(true);
            ids.add(colaSegment.getUniqueId());
        }
        
        // ===== PISTILS (only in HIGH and only for 4+ star plants) =====
        if (lod == DetailLevel.HIGH && qualityLevel >= 4) {
            for (int i = 0; i < 4; i++) {
                double angle = randomRotation + (Math.PI / 2) * i;
                ArmorStand pistil = createBaseArmorStand(world, baseLoc.clone().add(
                    Math.cos(angle) * 0.035, colaBase + 0.16 * heightScale, Math.sin(angle) * 0.035));
                pistil.setHelmet(new ItemStack(Material.ORANGE_WOOL));
                pistil.setSmall(true);
                pistil.setHeadPose(new EulerAngle(Math.toRadians(18), angle, Math.toRadians(25)));
                ids.add(pistil.getUniqueId());
            }
        }
        
        // ===== TRICHOMES (only in HIGH and only for 5+ star plants) =====
        if (lod == DetailLevel.HIGH && qualityLevel >= 5) {
            ArmorStand trichomes = createBaseArmorStand(world, baseLoc.clone().add(0, colaBase + 0.14 * heightScale, 0));
            trichomes.setHelmet(new ItemStack(Material.WHITE_STAINED_GLASS));
            trichomes.setSmall(true);
            trichomes.setGlowing(true);
            ids.add(trichomes.getUniqueId());
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
     * Starts the animation task with custom animation styles.
     * HEAVILY OPTIMIZED to reduce lag when many plants are present.
     * Now uses distance checks and dramatically reduced update frequency.
     */
    private void startAnimationTask() {
        animationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long time = System.currentTimeMillis();
            int plantCount = plantArmorStands.size();
            
            // Get detail level based on plant count
            DetailLevel lod = getDetailLevel(plantCount);
            
            // In LOW detail mode, reduce animation frequency dramatically
            if (lod == DetailLevel.LOW && time % 2000 > 1000) {
                // Skip animation 50% of the time in LOW mode
                return;
            }
            
            // Limit how many plants we process per tick
            int maxProcessPerTick = switch (lod) {
                case LOW -> 5;      // Process only 5 plants per tick
                case MEDIUM -> 15;  // Process 15 plants per tick
                case HIGH -> 50;    // Process up to 50 plants per tick (reasonable upper limit)
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
                
                // Reduce animation intensity based on LOD
                double intensityMult = switch (lod) {
                    case LOW -> 0.3;    // 30% animation intensity
                    case MEDIUM -> 0.6; // 60% animation intensity
                    case HIGH -> 1.0;   // Full animation
                };
                
                // Only animate a subset of armor stands per plant based on LOD
                int updateInterval = switch (lod) {
                    case LOW -> 4;      // Animate every 4th armor stand
                    case MEDIUM -> 2;   // Animate every 2nd armor stand
                    case HIGH -> 1;     // Animate all armor stands
                };
                
                // Calculate animation based on style with reduced intensity
                double sway = 0;
                double bounce = 0;
                double spin = 0;
                
                switch (style) {
                    case GENTLE_SWAY -> sway = Math.sin(time / 1000.0 * speed) * 0.02 * intensityMult;
                    case AGGRESSIVE -> sway = Math.sin(time / 200.0 * speed) * 0.08 * intensityMult;
                    case PULSE -> bounce = Math.sin(time / 500.0 * speed) * 0.03 * intensityMult;
                    case SPIN -> spin = (time / 2000.0 * speed) % (Math.PI * 2) * intensityMult;
                    case BOUNCE -> bounce = Math.abs(Math.sin(time / 300.0 * speed)) * 0.05 * intensityMult;
                    case WAVE -> sway = Math.sin(time / 800.0 * speed) * 0.04 * intensityMult;
                    case SHAKE -> sway = (Math.random() - 0.5) * 0.06 * speed * intensityMult;
                    case FLOAT -> bounce = Math.sin(time / 1500.0 * speed) * 0.02 * intensityMult;
                    case HEARTBEAT -> {
                        double beat = (time % 1000) / 1000.0;
                        bounce = (beat < 0.1 || (beat > 0.2 && beat < 0.3)) ? 0.03 * intensityMult : 0;
                    }
                    default -> sway = Math.sin(time / 1000.0 * speed) * 0.02 * intensityMult;
                }
                
                // Only update subset of armor stands
                for (int i = 0; i < armorStands.size(); i += updateInterval) {
                    UUID id = armorStands.get(i);
                    Entity entity = Bukkit.getEntity(id);
                    if (entity instanceof ArmorStand stand) {
                        EulerAngle current = stand.getHeadPose();
                        EulerAngle newPose = new EulerAngle(
                            current.getX() + sway * 0.1,
                            current.getY() + spin,
                            current.getZ() + sway * 0.05
                        );
                        stand.setHeadPose(newPose);
                    }
                }
            }
        }, 20L, 20L); // Increased from 10L to 20L (1 second instead of 0.5) for better performance
    }
    
    /**
     * Starts the particle effect task for ambient effects.
     * HEAVILY OPTIMIZED to dramatically reduce particle spam and improve performance.
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
            
            // Dramatically reduce particle spawning based on LOD
            int maxParticlesPerCycle = switch (lod) {
                case LOW -> 3;      // Only 3 plants get particles per cycle in LOW mode
                case MEDIUM -> 10;  // 10 plants in MEDIUM mode
                case HIGH -> 30;    // 30 plants in HIGH mode
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
                
                // In LOW and MEDIUM modes, randomly skip some particles for extra performance
                if (lod == DetailLevel.LOW && Math.random() > 0.3) continue;      // 70% chance to skip
                if (lod == DetailLevel.MEDIUM && Math.random() > 0.6) continue;   // 40% chance to skip
                
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
                    
                    // Drastically reduce particle count based on LOD
                    int count = switch (lod) {
                        case LOW -> 1;                                      // Minimal particles
                        case MEDIUM -> Math.max(1, intensity / 4);          // 25% of normal
                        case HIGH -> Math.max(1, intensity / 2);            // 50% of normal
                    };
                    
                    world.spawnParticle(particle, loc, count, 0.2, 0.3, 0.2, 0.02);
                    particlesSpawned++;
                    
                } catch (NumberFormatException e) {
                    // Skip invalid locations
                }
            }
        }, 40L, 100L); // Increased from 40L to 100L (5 seconds instead of 2) for massive performance boost
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
