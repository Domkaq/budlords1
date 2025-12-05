package com.budlords.farming;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages realistic 3D plant visualizations using armor stands.
 * Creates a multi-layered visual representation of marijuana plants
 * that grows and develops through different stages.
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
    
    // Animation task for swaying
    private BukkitTask animationTask;
    
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
        startAnimationTask();
    }

    /**
     * Creates or updates the visual representation of a plant.
     */
    public void updatePlantVisual(Plant plant) {
        Location loc = plant.getLocation();
        String locKey = getLocationKey(loc);
        
        // Remove old visualization
        removeVisualization(locKey);
        
        // Create new visualization based on growth stage
        List<UUID> armorStandIds = new ArrayList<>();
        
        switch (plant.getGrowthStage()) {
            case 0 -> armorStandIds.addAll(createSeedVisual(plant, loc));
            case 1 -> armorStandIds.addAll(createSproutVisual(plant, loc));
            case 2 -> armorStandIds.addAll(createVegetativeVisual(plant, loc));
            case 3 -> armorStandIds.addAll(createFloweringVisual(plant, loc));
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
     * Creates the seed stage visual (Stage 0).
     * A small seed sitting in the pot.
     */
    private List<UUID> createSeedVisual(Plant plant, Location loc) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        // Create a tiny armor stand with a seed item
        Location seedLoc = loc.clone().add(0.5, 0.0, 0.5);
        ArmorStand seed = createBaseArmorStand(world, seedLoc);
        
        // Use wheat seeds as the visual
        ItemStack seedItem = new ItemStack(Material.WHEAT_SEEDS);
        seed.setHelmet(seedItem);
        seed.setSmall(true);
        
        // Position adjustments
        seed.teleport(seedLoc.add(0, -0.7, 0));
        
        ids.add(seed.getUniqueId());
        return ids;
    }

    /**
     * Creates the sprout stage visual (Stage 1).
     * A small green sprout emerging from the pot.
     */
    private List<UUID> createSproutVisual(Plant plant, Location loc) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        // Main sprout
        Location sproutLoc = loc.clone().add(0.5, 0.0, 0.5);
        ArmorStand sprout = createBaseArmorStand(world, sproutLoc);
        
        // Use fern or small plant as sprout visual
        ItemStack sproutItem = new ItemStack(Material.FERN);
        sprout.setHelmet(sproutItem);
        sprout.setSmall(true);
        
        // Position
        sprout.teleport(sproutLoc.add(0, -0.5, 0));
        
        // Add small leaves using colored leather helmet
        ArmorStand leaves = createBaseArmorStand(world, sproutLoc.clone().add(0, 0.1, 0));
        leaves.setHelmet(createColoredLeaf(getStrainColor(plant), true));
        leaves.setSmall(true);
        
        ids.add(sprout.getUniqueId());
        ids.add(leaves.getUniqueId());
        
        return ids;
    }

    /**
     * Creates the vegetative stage visual (Stage 2).
     * Medium sized plant with spreading leaves.
     */
    private List<UUID> createVegetativeVisual(Plant plant, Location loc) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        Location baseLoc = loc.clone().add(0.5, 0.0, 0.5);
        Color strainColor = getStrainColor(plant);
        
        // Main stem (central armor stand)
        ArmorStand stem = createBaseArmorStand(world, baseLoc);
        stem.setHelmet(new ItemStack(Material.BAMBOO));
        stem.setSmall(false);
        stem.teleport(baseLoc.add(0, -0.9, 0));
        ids.add(stem.getUniqueId());
        
        // Lower leaves (spreading out)
        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI / 2) * i;
            double offsetX = Math.cos(angle) * 0.15;
            double offsetZ = Math.sin(angle) * 0.15;
            
            Location leafLoc = baseLoc.clone().add(offsetX, 0.2, offsetZ);
            ArmorStand leaf = createBaseArmorStand(world, leafLoc);
            leaf.setHelmet(createColoredLeaf(strainColor, false));
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
            double offsetX = Math.cos(angle) * 0.1;
            double offsetZ = Math.sin(angle) * 0.1;
            
            Location leafLoc = baseLoc.clone().add(offsetX, 0.5, offsetZ);
            ArmorStand leaf = createBaseArmorStand(world, leafLoc);
            leaf.setHelmet(createColoredLeaf(strainColor, true));
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
     */
    private List<UUID> createFloweringVisual(Plant plant, Location loc) {
        List<UUID> ids = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return ids;
        
        Location baseLoc = loc.clone().add(0.5, 0.0, 0.5);
        Color strainColor = getStrainColor(plant);
        StarRating rating = plant.calculateFinalBudRating(null);
        
        // Main stem (taller for mature plant)
        ArmorStand stem = createBaseArmorStand(world, baseLoc);
        stem.setHelmet(new ItemStack(Material.BAMBOO));
        stem.setSmall(false);
        stem.teleport(baseLoc.add(0, -0.7, 0));
        ids.add(stem.getUniqueId());
        
        // Large fan leaves at bottom
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI / 3) * i;
            double offsetX = Math.cos(angle) * 0.2;
            double offsetZ = Math.sin(angle) * 0.2;
            
            Location leafLoc = baseLoc.clone().add(offsetX, 0.1, offsetZ);
            ArmorStand leaf = createBaseArmorStand(world, leafLoc);
            leaf.setHelmet(createColoredLeaf(strainColor, false));
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
            double offsetX = Math.cos(angle) * 0.15;
            double offsetZ = Math.sin(angle) * 0.15;
            
            Location leafLoc = baseLoc.clone().add(offsetX, 0.45, offsetZ);
            ArmorStand leaf = createBaseArmorStand(world, leafLoc);
            leaf.setHelmet(createColoredLeaf(strainColor, true));
            leaf.setSmall(true);
            
            ids.add(leaf.getUniqueId());
        }
        
        // BUDS! (The prize)
        // Main cola (top bud)
        Location colaLoc = baseLoc.clone().add(0, 0.8, 0);
        ArmorStand mainCola = createBaseArmorStand(world, colaLoc);
        mainCola.setHelmet(createBudItem(rating, true));
        mainCola.setSmall(true);
        ids.add(mainCola.getUniqueId());
        
        // Side buds
        int budCount = rating != null ? Math.min(rating.getStars() + 2, 6) : 3;
        for (int i = 0; i < budCount; i++) {
            double angle = (Math.PI * 2 / budCount) * i;
            double offsetX = Math.cos(angle) * 0.18;
            double offsetZ = Math.sin(angle) * 0.18;
            double height = 0.55 + (i % 2) * 0.15;
            
            Location budLoc = baseLoc.clone().add(offsetX, height, offsetZ);
            ArmorStand bud = createBaseArmorStand(world, budLoc);
            bud.setHelmet(createBudItem(rating, false));
            bud.setSmall(true);
            
            // Angle buds slightly outward
            EulerAngle pose = new EulerAngle(
                Math.toRadians(20),
                angle,
                0
            );
            bud.setHeadPose(pose);
            
            ids.add(bud.getUniqueId());
        }
        
        // Add sparkle effect for high quality plants
        if (rating != null && rating.getStars() >= 4) {
            // Schedule occasional particle effects
            scheduleQualityParticles(loc, rating);
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
     * Creates a colored leaf item using dyed leather helmet.
     */
    private ItemStack createColoredLeaf(Color color, boolean small) {
        // Use different materials for variety
        Material mat = small ? Material.OAK_LEAVES : Material.JUNGLE_LEAVES;
        ItemStack leaf = new ItemStack(mat);
        return leaf;
    }

    /**
     * Creates a bud item based on quality rating.
     */
    private ItemStack createBudItem(StarRating rating, boolean isMainCola) {
        // Use different items to represent bud quality
        Material budMaterial;
        
        if (rating == null) {
            budMaterial = Material.GREEN_WOOL;
        } else {
            budMaterial = switch (rating) {
                case ONE_STAR -> Material.MOSS_BLOCK;
                case TWO_STAR -> Material.GREEN_WOOL;
                case THREE_STAR -> Material.LIME_WOOL;
                case FOUR_STAR -> Material.LIME_CONCRETE;
                case FIVE_STAR -> Material.EMERALD_BLOCK;
                case SIX_STAR -> Material.DIAMOND_BLOCK; // Legendary mutation quality
            };
        }
        
        return new ItemStack(budMaterial);
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
     * Schedules quality sparkle particles for high-tier plants.
     */
    private void scheduleQualityParticles(Location loc, StarRating rating) {
        // This will be called periodically by the particle task
        // Store location for the animation task to reference
    }

    /**
     * Removes all armor stands for a plant.
     */
    public void removeVisualization(String locKey) {
        List<UUID> ids = plantArmorStands.remove(locKey);
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
     * Starts the animation task for gentle swaying.
     */
    private void startAnimationTask() {
        animationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long time = System.currentTimeMillis();
            
            for (Map.Entry<String, List<UUID>> entry : plantArmorStands.entrySet()) {
                // Gentle swaying animation
                double sway = Math.sin(time / 1000.0) * 0.02;
                
                for (UUID id : entry.getValue()) {
                    Entity entity = Bukkit.getEntity(id);
                    if (entity instanceof ArmorStand stand) {
                        // Apply subtle head rotation for swaying effect
                        EulerAngle current = stand.getHeadPose();
                        EulerAngle newPose = new EulerAngle(
                            current.getX() + sway * 0.1,
                            current.getY(),
                            current.getZ() + sway * 0.05
                        );
                        stand.setHeadPose(newPose);
                    }
                }
            }
        }, 20L, 5L); // Every 0.25 seconds
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
