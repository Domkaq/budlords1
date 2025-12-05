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
    
    // Y offset to position plants inside the pot rather than floating above
    // Plant location is 1 block above pot, so we offset down to appear inside pot
    private static final double POT_Y_OFFSET = -0.9;

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
        
        // Create new visualization based on growth stage
        List<UUID> armorStandIds = new ArrayList<>();
        
        switch (plant.getGrowthStage()) {
            case 0 -> armorStandIds.addAll(createSeedVisual(plant, loc, visualConfig));
            case 1 -> armorStandIds.addAll(createSproutVisual(plant, loc, visualConfig));
            case 2 -> armorStandIds.addAll(createVegetativeVisual(plant, loc, visualConfig));
            case 3 -> armorStandIds.addAll(createFloweringVisual(plant, loc, visualConfig));
        }
        
        plantArmorStands.put(locKey, armorStandIds);
        
        // Set the block underneath to a flower pot if using pot system
        Block block = loc.getBlock();
        if (plant.hasPot() && block.getType() != Material.FLOWER_POT) {
            // Place an actual flower pot block at the base
            Block below = loc.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
            if (below.getType().isSolid()) {
                block.setType(Material.FLOWER_POT);
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
     * A realistic cannabis seed sitting in soil with a small dirt mound appearance.
     * Professional visualization with proper depth and detail.
     * Plants appear INSIDE the pot, not floating above.
     */
    private List<UUID> createSeedVisual(Plant plant, Location loc, StrainVisualConfig config) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        boolean glowing = config != null && config.isGlowing();
        
        // Base location - centered on the pot block (one below the tracked plant location)
        // Plant location is 1 block above pot, so we adjust down to be inside the pot
        Location baseLoc = loc.clone().add(0.5, POT_Y_OFFSET, 0.5);
        
        // Soil/dirt layer - represents the growing medium inside pot
        ArmorStand soilLayer = createBaseArmorStand(world, baseLoc.clone().add(0, -0.1, 0));
        soilLayer.setHelmet(new ItemStack(Material.COARSE_DIRT));
        soilLayer.setSmall(true);
        ids.add(soilLayer.getUniqueId());
        
        // The seed - positioned inside the pot, just visible in soil
        ArmorStand seed = createBaseArmorStand(world, baseLoc.clone().add(0, 0.0 * heightScale, 0));
        seed.setHelmet(new ItemStack(Material.COCOA_BEANS)); // Brown seed appearance
        seed.setSmall(true);
        if (glowing) seed.setGlowing(true);
        
        // Slight tilt to look like it's about to sprout
        seed.setHeadPose(new EulerAngle(Math.toRadians(15), Math.toRadians(45), 0));
        ids.add(seed.getUniqueId());
        
        // Tiny crack/sprout indicator - showing life beginning (just above soil)
        ArmorStand sproutTip = createBaseArmorStand(world, baseLoc.clone().add(0, 0.1 * heightScale, 0));
        sproutTip.setHelmet(new ItemStack(Material.SMALL_DRIPLEAF));
        sproutTip.setSmall(true);
        if (glowing) sproutTip.setGlowing(true);
        ids.add(sproutTip.getUniqueId());
        
        return ids;
    }

    /**
     * Creates the sprout stage visual (Stage 1).
     * A realistic cannabis seedling with cotyledons (seed leaves) and first true leaves emerging.
     * Shows the characteristic double-round seed leaves with the first serrated cannabis leaves above.
     * Now includes bamboo decoration in pot for better texture.
     */
    private List<UUID> createSproutVisual(Plant plant, Location loc, StrainVisualConfig config) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        double leafScale = config != null ? config.getLeafScale() : 1.0;
        Material leafMaterial = config != null && config.getLeafMaterial() != null ? 
            config.getLeafMaterial() : Material.FERN;
        Color strainColor = config != null && config.getLeafColorPrimary() != null ? 
            config.getLeafColorPrimary() : getStrainColor(plant);
        boolean glowing = config != null && config.isGlowing();
        
        // Base location - centered inside the pot
        Location baseLoc = loc.clone().add(0.5, POT_Y_OFFSET, 0.5);
        
        // Soil base for realism inside pot
        ArmorStand soilLayer = createBaseArmorStand(world, baseLoc.clone().add(0, -0.1, 0));
        soilLayer.setHelmet(new ItemStack(Material.COARSE_DIRT));
        soilLayer.setSmall(true);
        ids.add(soilLayer.getUniqueId());
        
        // Small bamboo shoot decoration inside pot
        ArmorStand bambooDecor = createBaseArmorStand(world, baseLoc.clone().add(0.1, 0.0, 0.1));
        bambooDecor.setHelmet(new ItemStack(Material.BAMBOO));
        bambooDecor.setSmall(true);
        bambooDecor.setHeadPose(new EulerAngle(Math.toRadians(10), Math.toRadians(45), 0));
        ids.add(bambooDecor.getUniqueId());
        
        // Thin stem emerging from soil
        ArmorStand stem = createBaseArmorStand(world, baseLoc.clone().add(0, 0.15 * heightScale, 0));
        stem.setHelmet(new ItemStack(Material.END_ROD)); // Thin white-green stem
        stem.setSmall(true);
        if (glowing) stem.setGlowing(true);
        ids.add(stem.getUniqueId());
        
        // Cotyledons (seed leaves) - round leaves that emerge first
        // Left cotyledon
        ArmorStand leftCoty = createBaseArmorStand(world, baseLoc.clone().add(-0.08 * leafScale, 0.25 * heightScale, 0));
        leftCoty.setHelmet(new ItemStack(Material.LILY_PAD)); // Round leaf shape
        leftCoty.setSmall(true);
        leftCoty.setHeadPose(new EulerAngle(Math.toRadians(60), Math.toRadians(-90), Math.toRadians(10)));
        ids.add(leftCoty.getUniqueId());
        
        // Right cotyledon  
        ArmorStand rightCoty = createBaseArmorStand(world, baseLoc.clone().add(0.08 * leafScale, 0.25 * heightScale, 0));
        rightCoty.setHelmet(new ItemStack(Material.LILY_PAD));
        rightCoty.setSmall(true);
        rightCoty.setHeadPose(new EulerAngle(Math.toRadians(60), Math.toRadians(90), Math.toRadians(-10)));
        ids.add(rightCoty.getUniqueId());
        
        // First true leaves (cannabis leaves) - emerging above cotyledons
        // These show the characteristic serrated edge pattern
        ArmorStand firstLeaf1 = createBaseArmorStand(world, baseLoc.clone().add(0, 0.35 * heightScale, 0.06 * leafScale));
        firstLeaf1.setHelmet(new ItemStack(leafMaterial));
        firstLeaf1.setSmall(true);
        firstLeaf1.setHeadPose(new EulerAngle(Math.toRadians(45), 0, 0));
        if (glowing) firstLeaf1.setGlowing(true);
        ids.add(firstLeaf1.getUniqueId());
        
        ArmorStand firstLeaf2 = createBaseArmorStand(world, baseLoc.clone().add(0, 0.35 * heightScale, -0.06 * leafScale));
        firstLeaf2.setHelmet(new ItemStack(leafMaterial));
        firstLeaf2.setSmall(true);
        firstLeaf2.setHeadPose(new EulerAngle(Math.toRadians(45), Math.toRadians(180), 0));
        if (glowing) firstLeaf2.setGlowing(true);
        ids.add(firstLeaf2.getUniqueId());
        
        // Growing tip (apical meristem)
        ArmorStand growingTip = createBaseArmorStand(world, baseLoc.clone().add(0, 0.45 * heightScale, 0));
        growingTip.setHelmet(new ItemStack(Material.SWEET_BERRIES)); // Small green bud
        growingTip.setSmall(true);
        if (glowing) growingTip.setGlowing(true);
        ids.add(growingTip.getUniqueId());
        
        return ids;
    }

    /**
     * Creates the vegetative stage visual (Stage 2).
     * A detailed cannabis plant in vegetative growth with multiple node levels,
     * realistic fan leaves with proper finger arrangement, and a structured stem system.
     * This stage shows rapid vegetative growth with lush green foliage.
     * Includes bamboo and small sapling decorations in pot for professional look.
     */
    private List<UUID> createVegetativeVisual(Plant plant, Location loc, StrainVisualConfig config) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        // Base location centered inside the pot
        Location baseLoc = loc.clone().add(0.5, POT_Y_OFFSET, 0.5);
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        double leafScale = config != null ? config.getLeafScale() : 1.0;
        Material leafMaterial = config != null && config.getLeafMaterial() != null ? 
            config.getLeafMaterial() : Material.FERN;
        Color strainColor = config != null && config.getLeafColorPrimary() != null ? 
            config.getLeafColorPrimary() : getStrainColor(plant);
        boolean glowing = config != null && config.isGlowing();
        
        // ===== SOIL BASE =====
        ArmorStand soilLayer = createBaseArmorStand(world, baseLoc.clone().add(0, -0.1, 0));
        soilLayer.setHelmet(new ItemStack(Material.COARSE_DIRT));
        soilLayer.setSmall(true);
        ids.add(soilLayer.getUniqueId());
        
        // ===== POT DECORATIONS - Bamboo and small plants =====
        // Bamboo decoration left side
        ArmorStand bamboo1 = createBaseArmorStand(world, baseLoc.clone().add(-0.12, 0.0, 0.08));
        bamboo1.setHelmet(new ItemStack(Material.BAMBOO));
        bamboo1.setSmall(true);
        bamboo1.setHeadPose(new EulerAngle(Math.toRadians(8), Math.toRadians(-30), 0));
        ids.add(bamboo1.getUniqueId());
        
        // Small sapling decoration right side
        ArmorStand sapling = createBaseArmorStand(world, baseLoc.clone().add(0.12, 0.0, -0.08));
        sapling.setHelmet(new ItemStack(Material.OAK_SAPLING));
        sapling.setSmall(true);
        sapling.setHeadPose(new EulerAngle(Math.toRadians(5), Math.toRadians(60), 0));
        ids.add(sapling.getUniqueId());
        
        // ===== MAIN STEM STRUCTURE =====
        // Lower stem segment
        ArmorStand stemLower = createBaseArmorStand(world, baseLoc.clone().add(0, 0.1 * heightScale, 0));
        stemLower.setHelmet(new ItemStack(Material.BAMBOO)); // Thicker base
        stemLower.setSmall(true);
        if (glowing) stemLower.setGlowing(true);
        ids.add(stemLower.getUniqueId());
        
        // Middle stem segment
        ArmorStand stemMiddle = createBaseArmorStand(world, baseLoc.clone().add(0, 0.3 * heightScale, 0));
        stemMiddle.setHelmet(new ItemStack(Material.STICK)); // Thinner middle
        stemMiddle.setSmall(true);
        if (glowing) stemMiddle.setGlowing(true);
        ids.add(stemMiddle.getUniqueId());
        
        // Upper stem segment
        ArmorStand stemUpper = createBaseArmorStand(world, baseLoc.clone().add(0, 0.5 * heightScale, 0));
        stemUpper.setHelmet(new ItemStack(Material.END_ROD)); // Thinnest top
        stemUpper.setSmall(true);
        if (glowing) stemUpper.setGlowing(true);
        ids.add(stemUpper.getUniqueId());
        
        // ===== NODE 1 - BOTTOM FAN LEAVES (largest, oldest) =====
        // Cannabis plants have opposite leaves that become alternate later
        double node1Height = 0.2 * heightScale;
        for (int i = 0; i < 2; i++) {
            double angle = Math.PI * i; // Opposite arrangement
            double offsetX = Math.cos(angle) * 0.18 * leafScale;
            double offsetZ = Math.sin(angle) * 0.18 * leafScale;
            
            ArmorStand fanLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, node1Height, offsetZ));
            fanLeaf.setHelmet(new ItemStack(Material.JUNGLE_LEAVES)); // Large fan leaves
            fanLeaf.setSmall(true);
            
            // Fan leaves angle downward and outward
            EulerAngle pose = new EulerAngle(
                Math.toRadians(50),  // Drooping down
                angle + Math.toRadians(90),
                Math.toRadians(15)
            );
            fanLeaf.setHeadPose(pose);
            ids.add(fanLeaf.getUniqueId());
            
            // Secondary leaflets for each fan leaf (cannabis has 5-7 fingers)
            for (int j = -1; j <= 1; j++) {
                if (j == 0) continue; // Skip center (main leaf)
                double fingerAngle = angle + Math.toRadians(j * 25);
                double fingerX = Math.cos(fingerAngle) * 0.12 * leafScale;
                double fingerZ = Math.sin(fingerAngle) * 0.12 * leafScale;
                
                ArmorStand finger = createBaseArmorStand(world, baseLoc.clone().add(fingerX, node1Height + 0.03, fingerZ));
                finger.setHelmet(new ItemStack(Material.OAK_LEAVES)); // Smaller fingers
                finger.setSmall(true);
                finger.setHeadPose(new EulerAngle(Math.toRadians(55), fingerAngle + Math.toRadians(90), Math.toRadians(j * 10)));
                ids.add(finger.getUniqueId());
            }
        }
        
        // ===== NODE 2 - MIDDLE FAN LEAVES =====
        double node2Height = 0.4 * heightScale;
        for (int i = 0; i < 2; i++) {
            double angle = Math.PI * i + Math.toRadians(90); // Rotated 90° from node 1
            double offsetX = Math.cos(angle) * 0.15 * leafScale;
            double offsetZ = Math.sin(angle) * 0.15 * leafScale;
            
            ArmorStand fanLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, node2Height, offsetZ));
            fanLeaf.setHelmet(new ItemStack(leafMaterial));
            fanLeaf.setSmall(true);
            
            EulerAngle pose = new EulerAngle(
                Math.toRadians(40),
                angle + Math.toRadians(90),
                Math.toRadians(10)
            );
            fanLeaf.setHeadPose(pose);
            ids.add(fanLeaf.getUniqueId());
        }
        
        // ===== NODE 3 - UPPER GROWING LEAVES (smallest, youngest) =====
        double node3Height = 0.55 * heightScale;
        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI / 2) * i + Math.toRadians(45);
            double offsetX = Math.cos(angle) * 0.08 * leafScale;
            double offsetZ = Math.sin(angle) * 0.08 * leafScale;
            
            ArmorStand youngLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, node3Height, offsetZ));
            youngLeaf.setHelmet(new ItemStack(Material.OAK_LEAVES));
            youngLeaf.setSmall(true);
            
            EulerAngle pose = new EulerAngle(
                Math.toRadians(25), // More upright - reaching for light
                angle + Math.toRadians(90),
                0
            );
            youngLeaf.setHeadPose(pose);
            if (glowing) youngLeaf.setGlowing(true);
            ids.add(youngLeaf.getUniqueId());
        }
        
        // ===== APICAL MERISTEM (growing tip) =====
        ArmorStand growingTip = createBaseArmorStand(world, baseLoc.clone().add(0, 0.65 * heightScale, 0));
        growingTip.setHelmet(new ItemStack(Material.SWEET_BERRIES)); // Bright green growth
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
     * - Bamboo and decorative plants in pot base for professional look
     */
    private List<UUID> createFloweringVisual(Plant plant, Location loc, StrainVisualConfig config) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        // Base location centered inside the pot
        Location baseLoc = loc.clone().add(0.5, POT_Y_OFFSET, 0.5);
        StarRating rating = plant.calculateFinalBudRating(null);
        int qualityLevel = rating != null ? rating.getStars() : 2;
        
        // Get visual settings from config
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        double leafScale = config != null ? config.getLeafScale() : 1.0;
        double budScale = config != null ? config.getBudScale() : 1.0;
        
        Material leafMaterial = config != null && config.getLeafMaterial() != null ? 
            config.getLeafMaterial() : Material.FERN;
        Color strainColor = config != null && config.getLeafColorPrimary() != null ? 
            config.getLeafColorPrimary() : getStrainColor(plant);
        BudType budType = config != null ? config.getBudType() : BudType.NORMAL;
        boolean glowing = config != null && config.isGlowing();
        
        // ===== SOIL BASE INSIDE POT =====
        ArmorStand soilLayer = createBaseArmorStand(world, baseLoc.clone().add(0, -0.1, 0));
        soilLayer.setHelmet(new ItemStack(Material.COARSE_DIRT));
        soilLayer.setSmall(true);
        ids.add(soilLayer.getUniqueId());
        
        // ===== POT DECORATIONS - Bamboo shoots for professional look =====
        // Bamboo decoration left back
        ArmorStand bamboo1 = createBaseArmorStand(world, baseLoc.clone().add(-0.15, 0.0, 0.1));
        bamboo1.setHelmet(new ItemStack(Material.BAMBOO));
        bamboo1.setSmall(true);
        bamboo1.setHeadPose(new EulerAngle(Math.toRadians(8), Math.toRadians(-45), 0));
        ids.add(bamboo1.getUniqueId());
        
        // Bamboo decoration right back
        ArmorStand bamboo2 = createBaseArmorStand(world, baseLoc.clone().add(0.15, 0.0, 0.1));
        bamboo2.setHelmet(new ItemStack(Material.BAMBOO));
        bamboo2.setSmall(true);
        bamboo2.setHeadPose(new EulerAngle(Math.toRadians(8), Math.toRadians(45), 0));
        ids.add(bamboo2.getUniqueId());
        
        // Small fern/grass decoration in front
        ArmorStand fern = createBaseArmorStand(world, baseLoc.clone().add(0, 0.0, -0.12));
        fern.setHelmet(new ItemStack(Material.FERN));
        fern.setSmall(true);
        fern.setHeadPose(new EulerAngle(Math.toRadians(10), 0, 0));
        ids.add(fern.getUniqueId());
        
        // ===== MAIN STEM STRUCTURE (3 segments for realism) =====
        // Thick base stem
        ArmorStand stemBase = createBaseArmorStand(world, baseLoc.clone().add(0, 0.1 * heightScale, 0));
        stemBase.setHelmet(new ItemStack(Material.BAMBOO));
        stemBase.setSmall(true);
        if (glowing) stemBase.setGlowing(true);
        ids.add(stemBase.getUniqueId());
        
        // Middle stem
        ArmorStand stemMiddle = createBaseArmorStand(world, baseLoc.clone().add(0, 0.35 * heightScale, 0));
        stemMiddle.setHelmet(new ItemStack(Material.STICK));
        stemMiddle.setSmall(true);
        ids.add(stemMiddle.getUniqueId());
        
        // Upper stem (leads to main cola)
        ArmorStand stemUpper = createBaseArmorStand(world, baseLoc.clone().add(0, 0.55 * heightScale, 0));
        stemUpper.setHelmet(new ItemStack(Material.END_ROD));
        stemUpper.setSmall(true);
        ids.add(stemUpper.getUniqueId());
        
        // ===== BOTTOM FAN LEAVES (CANOPY BASE) =====
        // Large fan leaves provide structure and show plant health
        double fanLeafLevel = 0.2 * heightScale;
        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI / 2) * i + Math.toRadians(45);
            double offsetX = Math.cos(angle) * 0.22 * leafScale;
            double offsetZ = Math.sin(angle) * 0.22 * leafScale;
            
            ArmorStand fanLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, fanLeafLevel, offsetZ));
            fanLeaf.setHelmet(new ItemStack(Material.JUNGLE_LEAVES)); // Large mature fan leaves
            fanLeaf.setSmall(true);
            
            EulerAngle pose = new EulerAngle(
                Math.toRadians(55), // Drooping - characteristic of flowering
                angle + Math.toRadians(90),
                Math.toRadians(10)
            );
            fanLeaf.setHeadPose(pose);
            ids.add(fanLeaf.getUniqueId());
            
            // Leaf fingers/segments (cannabis has 5-7-9 fingers)
            for (int f = -1; f <= 1; f++) {
                double fingerAngle = angle + Math.toRadians(f * 20);
                double fingerX = Math.cos(fingerAngle) * 0.15 * leafScale;
                double fingerZ = Math.sin(fingerAngle) * 0.15 * leafScale;
                
                ArmorStand finger = createBaseArmorStand(world, baseLoc.clone().add(fingerX, fanLeafLevel + 0.02, fingerZ));
                finger.setHelmet(new ItemStack(Material.OAK_LEAVES));
                finger.setSmall(true);
                finger.setHeadPose(new EulerAngle(Math.toRadians(50), fingerAngle + Math.toRadians(90), Math.toRadians(f * 8)));
                ids.add(finger.getUniqueId());
            }
        }
        
        // ===== MIDDLE SUGAR LEAVES & SMALL BUDS =====
        // Sugar leaves are smaller, covered in trichomes, intermingled with buds
        double sugarLeafLevel = 0.45 * heightScale;
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI / 3) * i;
            double offsetX = Math.cos(angle) * 0.12 * leafScale;
            double offsetZ = Math.sin(angle) * 0.12 * leafScale;
            
            ArmorStand sugarLeaf = createBaseArmorStand(world, baseLoc.clone().add(offsetX, sugarLeafLevel, offsetZ));
            sugarLeaf.setHelmet(new ItemStack(leafMaterial)); // Strain-specific leaves
            sugarLeaf.setSmall(true);
            
            EulerAngle pose = new EulerAngle(
                Math.toRadians(35),
                angle + Math.toRadians(90),
                Math.toRadians(5)
            );
            sugarLeaf.setHeadPose(pose);
            ids.add(sugarLeaf.getUniqueId());
        }
        
        // ===== BRANCH BUDS (lateral colas) =====
        // Number of side colas based on quality
        int branchBudCount = Math.min(qualityLevel + 2, 6);
        for (int i = 0; i < branchBudCount; i++) {
            double angle = (Math.PI * 2 / branchBudCount) * i;
            double radius = 0.14 * budScale;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double height = (0.55 + (i % 2) * 0.06) * heightScale;
            
            // Branch stem connecting to main stem
            ArmorStand branchStem = createBaseArmorStand(world, baseLoc.clone().add(offsetX * 0.5, height - 0.08, offsetZ * 0.5));
            branchStem.setHelmet(new ItemStack(Material.STICK));
            branchStem.setSmall(true);
            branchStem.setHeadPose(new EulerAngle(Math.toRadians(45), angle, 0));
            ids.add(branchStem.getUniqueId());
            
            // Side bud (cola)
            ArmorStand sideBud = createBaseArmorStand(world, baseLoc.clone().add(offsetX, height, offsetZ));
            sideBud.setHelmet(createCustomBudItem(rating, budType, config, false));
            sideBud.setSmall(true);
            if (glowing) sideBud.setGlowing(true);
            
            // Buds tilt slightly outward
            EulerAngle budPose = new EulerAngle(
                Math.toRadians(20),
                angle,
                Math.toRadians(10)
            );
            sideBud.setHeadPose(budPose);
            ids.add(sideBud.getUniqueId());
            
            // Calyx clusters around each bud (for detail)
            if (qualityLevel >= 3) {
                for (int c = 0; c < 2; c++) {
                    double calyxAngle = angle + Math.toRadians(c * 60 - 30);
                    double calyxX = Math.cos(calyxAngle) * (radius + 0.04);
                    double calyxZ = Math.sin(calyxAngle) * (radius + 0.04);
                    
                    ArmorStand calyx = createBaseArmorStand(world, baseLoc.clone().add(calyxX, height + 0.02, calyxZ));
                    calyx.setHelmet(new ItemStack(Material.SWEET_BERRIES)); // Calyx texture
                    calyx.setSmall(true);
                    calyx.setHeadPose(new EulerAngle(Math.toRadians(30), calyxAngle, 0));
                    ids.add(calyx.getUniqueId());
                }
            }
        }
        
        // ===== MAIN COLA (top bud - the prize) =====
        // Cola base with sugar leaves
        double colaBase = 0.7 * heightScale;
        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI / 2) * i + Math.toRadians(22.5);
            ArmorStand colaSugarLeaf = createBaseArmorStand(world, baseLoc.clone().add(
                Math.cos(angle) * 0.06, colaBase, Math.sin(angle) * 0.06));
            colaSugarLeaf.setHelmet(new ItemStack(Material.OAK_LEAVES));
            colaSugarLeaf.setSmall(true);
            colaSugarLeaf.setHeadPose(new EulerAngle(Math.toRadians(30), angle + Math.toRadians(90), 0));
            ids.add(colaSugarLeaf.getUniqueId());
        }
        
        // Main cola lower section
        ArmorStand colaLower = createBaseArmorStand(world, baseLoc.clone().add(0, colaBase + 0.05, 0));
        colaLower.setHelmet(createCustomBudItem(rating, budType, config, true));
        colaLower.setSmall(true);
        if (glowing) colaLower.setGlowing(true);
        ids.add(colaLower.getUniqueId());
        
        // Main cola middle section (larger)  
        ArmorStand colaMiddle = createBaseArmorStand(world, baseLoc.clone().add(0, colaBase + 0.12 * heightScale, 0));
        colaMiddle.setHelmet(createCustomBudItem(rating, budType, config, true));
        colaMiddle.setSmall(true);
        if (glowing) colaMiddle.setGlowing(true);
        ids.add(colaMiddle.getUniqueId());
        
        // Main cola top (crown) - uses same bud item for consistency
        ArmorStand colaTop = createBaseArmorStand(world, baseLoc.clone().add(0, colaBase + 0.2 * heightScale, 0));
        colaTop.setHelmet(createCustomBudItem(rating, budType, config, true));
        colaTop.setSmall(true);
        if (glowing) colaTop.setGlowing(true);
        ids.add(colaTop.getUniqueId());
        
        // ===== PISTILS/HAIRS (for high quality plants) =====
        if (qualityLevel >= 4) {
            // Orange/red pistils protruding from buds
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 / 3) * i;
                ArmorStand pistil = createBaseArmorStand(world, baseLoc.clone().add(
                    Math.cos(angle) * 0.03, colaBase + 0.18 * heightScale, Math.sin(angle) * 0.03));
                pistil.setHelmet(new ItemStack(Material.ORANGE_WOOL)); // Orange pistils
                pistil.setSmall(true);
                pistil.setHeadPose(new EulerAngle(Math.toRadians(15), angle, Math.toRadians(30)));
                ids.add(pistil.getUniqueId());
            }
        }
        
        // ===== TRICHOME SPARKLE (for legendary quality) =====
        if (qualityLevel >= 5) {
            // Crystal-like trichomes covering the buds
            ArmorStand trichomes = createBaseArmorStand(world, baseLoc.clone().add(0, colaBase + 0.15 * heightScale, 0));
            trichomes.setHelmet(new ItemStack(Material.WHITE_STAINED_GLASS)); // Crystalline appearance
            trichomes.setSmall(true);
            trichomes.setGlowing(true); // Sparkle effect
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
     */
    public void removeVisualization(String locKey) {
        List<UUID> ids = plantArmorStands.remove(locKey);
        plantVisualConfigs.remove(locKey);
        if (ids == null) return;
        
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
     */
    private void startAnimationTask() {
        animationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long time = System.currentTimeMillis();
            
            for (Map.Entry<String, List<UUID>> entry : plantArmorStands.entrySet()) {
                String locKey = entry.getKey();
                StrainVisualConfig config = plantVisualConfigs.get(locKey);
                
                AnimationStyle style = config != null ? config.getAnimationStyle() : AnimationStyle.GENTLE_SWAY;
                double speed = config != null ? config.getAnimationSpeed() : 1.0;
                
                // Skip frozen plants
                if (style == AnimationStyle.FROZEN) continue;
                
                // Calculate animation based on style
                double sway = 0;
                double bounce = 0;
                double spin = 0;
                
                switch (style) {
                    case GENTLE_SWAY -> sway = Math.sin(time / 1000.0 * speed) * 0.02;
                    case AGGRESSIVE -> sway = Math.sin(time / 200.0 * speed) * 0.08;
                    case PULSE -> bounce = Math.sin(time / 500.0 * speed) * 0.03;
                    case SPIN -> spin = (time / 2000.0 * speed) % (Math.PI * 2);
                    case BOUNCE -> bounce = Math.abs(Math.sin(time / 300.0 * speed)) * 0.05;
                    case WAVE -> sway = Math.sin(time / 800.0 * speed) * 0.04;
                    case SHAKE -> sway = (Math.random() - 0.5) * 0.06 * speed;
                    case FLOAT -> bounce = Math.sin(time / 1500.0 * speed) * 0.02;
                    case HEARTBEAT -> {
                        double beat = (time % 1000) / 1000.0;
                        bounce = (beat < 0.1 || (beat > 0.2 && beat < 0.3)) ? 0.03 : 0;
                    }
                    default -> sway = Math.sin(time / 1000.0 * speed) * 0.02;
                }
                
                for (UUID id : entry.getValue()) {
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
        }, 20L, 5L); // Every 0.25 seconds
    }
    
    /**
     * Starts the particle effect task for ambient effects.
     */
    private void startParticleTask() {
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<String, List<UUID>> entry : plantArmorStands.entrySet()) {
                String locKey = entry.getKey();
                StrainVisualConfig config = plantVisualConfigs.get(locKey);
                
                if (config == null) continue;
                
                Particle particle = config.getAmbientParticle();
                int intensity = config.getParticleIntensity();
                
                if (particle == null || intensity <= 0) continue;
                
                // Parse location from key
                String[] parts = locKey.split(",");
                if (parts.length != 4) continue;
                
                try {
                    World world = Bukkit.getWorld(parts[0]);
                    if (world == null) continue;
                    
                    double x = Double.parseDouble(parts[1]) + 0.5;
                    double y = Double.parseDouble(parts[2]) + 0.5;
                    double z = Double.parseDouble(parts[3]) + 0.5;
                    
                    Location loc = new Location(world, x, y, z);
                    
                    // Spawn particles based on intensity (1-10 -> 1-5 particles)
                    int count = Math.max(1, intensity / 2);
                    world.spawnParticle(particle, loc, count, 0.2, 0.3, 0.2, 0.02);
                    
                } catch (NumberFormatException e) {
                    // Skip invalid locations
                }
            }
        }, 40L, 20L); // Every second
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
