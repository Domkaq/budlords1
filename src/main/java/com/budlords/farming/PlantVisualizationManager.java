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
     * A small seed sitting in the pot.
     */
    private List<UUID> createSeedVisual(Plant plant, Location loc, StrainVisualConfig config) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        
        // Create a tiny armor stand with a seed item
        Location seedLoc = loc.clone().add(0.5, 0.0, 0.5);
        ArmorStand seed = createBaseArmorStand(world, seedLoc);
        
        // Use wheat seeds as the visual
        ItemStack seedItem = new ItemStack(Material.WHEAT_SEEDS);
        seed.setHelmet(seedItem);
        seed.setSmall(true);
        
        // Add glow if enabled
        if (config != null && config.isGlowing()) {
            seed.setGlowing(true);
        }
        
        // Position adjustments
        seed.teleport(seedLoc.add(0, -0.7 * heightScale, 0));
        
        ids.add(seed.getUniqueId());
        return ids;
    }

    /**
     * Creates the sprout stage visual (Stage 1).
     * A small green sprout emerging from the pot.
     */
    private List<UUID> createSproutVisual(Plant plant, Location loc, StrainVisualConfig config) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        double leafScale = config != null ? config.getLeafScale() : 1.0;
        Material leafMaterial = config != null ? config.getLeafMaterial() : Material.FERN;
        
        // Main sprout
        Location sproutLoc = loc.clone().add(0.5, 0.0, 0.5);
        ArmorStand sprout = createBaseArmorStand(world, sproutLoc);
        
        // Use configured leaf material or fern as sprout visual
        ItemStack sproutItem = new ItemStack(leafMaterial != null ? leafMaterial : Material.FERN);
        sprout.setHelmet(sproutItem);
        sprout.setSmall(true);
        
        // Add glow if enabled
        if (config != null && config.isGlowing()) {
            sprout.setGlowing(true);
        }
        
        // Position
        sprout.teleport(sproutLoc.add(0, -0.5 * heightScale, 0));
        
        // Add small leaves using the config color or strain color
        Color leafColor = config != null && config.getLeafColorPrimary() != null ? 
            config.getLeafColorPrimary() : getStrainColor(plant);
        ArmorStand leaves = createBaseArmorStand(world, sproutLoc.clone().add(0, 0.1 * heightScale, 0));
        leaves.setHelmet(createColoredLeaf(leafColor, true, config));
        leaves.setSmall(true);
        
        ids.add(sprout.getUniqueId());
        ids.add(leaves.getUniqueId());
        
        return ids;
    }

    /**
     * Creates the vegetative stage visual (Stage 2).
     * Medium sized plant with spreading leaves.
     */
    private List<UUID> createVegetativeVisual(Plant plant, Location loc, StrainVisualConfig config) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        Location baseLoc = loc.clone().add(0.5, 0.0, 0.5);
        
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        double leafScale = config != null ? config.getLeafScale() : 1.0;
        Material stemMaterial = config != null && config.getStemMaterial() != null ? 
            config.getStemMaterial() : Material.BAMBOO;
        Color strainColor = config != null && config.getLeafColorPrimary() != null ? 
            config.getLeafColorPrimary() : getStrainColor(plant);
        
        // Main stem (central armor stand)
        ArmorStand stem = createBaseArmorStand(world, baseLoc);
        stem.setHelmet(new ItemStack(stemMaterial));
        stem.setSmall(false);
        
        // Add glow if enabled
        if (config != null && config.isGlowing()) {
            stem.setGlowing(true);
        }
        
        stem.teleport(baseLoc.add(0, -0.9 * heightScale, 0));
        ids.add(stem.getUniqueId());
        
        // Lower leaves (spreading out)
        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI / 2) * i;
            double offsetX = Math.cos(angle) * 0.15 * leafScale;
            double offsetZ = Math.sin(angle) * 0.15 * leafScale;
            
            Location leafLoc = baseLoc.clone().add(offsetX, 0.2 * heightScale, offsetZ);
            ArmorStand leaf = createBaseArmorStand(world, leafLoc);
            leaf.setHelmet(createColoredLeaf(strainColor, false, config));
            leaf.setSmall(true);
            
            // Angle leaves outward
            EulerAngle headPose = new EulerAngle(
                Math.toRadians(30),  // Tilt down slightly
                angle,               // Rotate around
                Math.toRadians(15)   // Slight roll
            );
            leaf.setHeadPose(headPose);
            
            ids.add(leaf.getUniqueId());
        }
        
        // Upper leaves (more vertical)
        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI / 4) + (Math.PI / 2) * i;
            double offsetX = Math.cos(angle) * 0.1 * leafScale;
            double offsetZ = Math.sin(angle) * 0.1 * leafScale;
            
            Location leafLoc = baseLoc.clone().add(offsetX, 0.5 * heightScale, offsetZ);
            ArmorStand leaf = createBaseArmorStand(world, leafLoc);
            leaf.setHelmet(createColoredLeaf(strainColor, true, config));
            leaf.setSmall(true);
            
            EulerAngle headPose = new EulerAngle(
                Math.toRadians(15),
                angle,
                0
            );
            leaf.setHeadPose(headPose);
            
            ids.add(leaf.getUniqueId());
        }
        
        return ids;
    }

    /**
     * Creates the flowering/mature stage visual (Stage 3).
     * Full plant with buds ready for harvest.
     * Uses custom visual config for unique strain appearances.
     */
    private List<UUID> createFloweringVisual(Plant plant, Location loc, StrainVisualConfig config) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        Location baseLoc = loc.clone().add(0.5, 0.0, 0.5);
        StarRating rating = plant.calculateFinalBudRating(null);
        
        // Get visual settings from config
        double heightScale = config != null ? config.getHeightScale() : 1.0;
        double leafScale = config != null ? config.getLeafScale() : 1.0;
        double budScale = config != null ? config.getBudScale() : 1.0;
        
        Material stemMaterial = config != null && config.getStemMaterial() != null ? 
            config.getStemMaterial() : Material.BAMBOO;
        Color strainColor = config != null && config.getLeafColorPrimary() != null ? 
            config.getLeafColorPrimary() : getStrainColor(plant);
        BudType budType = config != null ? config.getBudType() : BudType.NORMAL;
        boolean glowing = config != null && config.isGlowing();
        
        // Main stem (taller for mature plant)
        ArmorStand stem = createBaseArmorStand(world, baseLoc);
        stem.setHelmet(new ItemStack(stemMaterial));
        stem.setSmall(false);
        if (glowing) stem.setGlowing(true);
        stem.teleport(baseLoc.add(0, -0.7 * heightScale, 0));
        ids.add(stem.getUniqueId());
        
        // Large fan leaves at bottom
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI / 3) * i;
            double offsetX = Math.cos(angle) * 0.2 * leafScale;
            double offsetZ = Math.sin(angle) * 0.2 * leafScale;
            
            Location leafLoc = baseLoc.clone().add(offsetX, 0.1 * heightScale, offsetZ);
            ArmorStand leaf = createBaseArmorStand(world, leafLoc);
            leaf.setHelmet(createColoredLeaf(strainColor, false, config));
            leaf.setSmall(true);
            
            EulerAngle headPose = new EulerAngle(
                Math.toRadians(40),
                angle,
                Math.toRadians(10)
            );
            leaf.setHeadPose(headPose);
            
            ids.add(leaf.getUniqueId());
        }
        
        // Mid-level leaves
        for (int i = 0; i < 5; i++) {
            double angle = (Math.PI / 5) * i * 2;
            double offsetX = Math.cos(angle) * 0.15 * leafScale;
            double offsetZ = Math.sin(angle) * 0.15 * leafScale;
            
            Location leafLoc = baseLoc.clone().add(offsetX, 0.45 * heightScale, offsetZ);
            ArmorStand leaf = createBaseArmorStand(world, leafLoc);
            leaf.setHelmet(createColoredLeaf(strainColor, true, config));
            leaf.setSmall(true);
            
            ids.add(leaf.getUniqueId());
        }
        
        // BUDS! (The prize) - Now using custom bud types!
        // Main cola (top bud)
        Location colaLoc = baseLoc.clone().add(0, 0.8 * heightScale, 0);
        ArmorStand mainCola = createBaseArmorStand(world, colaLoc);
        mainCola.setHelmet(createCustomBudItem(rating, budType, config, true));
        mainCola.setSmall(true);
        if (glowing) mainCola.setGlowing(true);
        ids.add(mainCola.getUniqueId());
        
        // Side buds
        int budCount = rating != null ? Math.min(rating.getStars() + 2, 6) : 3;
        for (int i = 0; i < budCount; i++) {
            double angle = (Math.PI * 2 / budCount) * i;
            double offsetX = Math.cos(angle) * 0.18 * budScale;
            double offsetZ = Math.sin(angle) * 0.18 * budScale;
            double height = (0.55 + (i % 2) * 0.15) * heightScale;
            
            Location budLoc = baseLoc.clone().add(offsetX, height, offsetZ);
            ArmorStand bud = createBaseArmorStand(world, budLoc);
            bud.setHelmet(createCustomBudItem(rating, budType, config, false));
            bud.setSmall(true);
            if (glowing) bud.setGlowing(true);
            
            // Angle buds slightly outward
            EulerAngle pose = new EulerAngle(
                Math.toRadians(20),
                angle,
                0
            );
            bud.setHeadPose(pose);
            
            ids.add(bud.getUniqueId());
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
