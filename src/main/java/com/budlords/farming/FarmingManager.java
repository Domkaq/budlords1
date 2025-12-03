package com.budlords.farming;

import com.budlords.BudLords;
import com.budlords.data.DataManager;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FarmingManager {

    private final BudLords plugin;
    private final DataManager dataManager;
    private final StrainManager strainManager;
    
    private final Map<String, Plant> plants; // locationString -> Plant
    private BukkitTask growthTask;
    private BukkitTask particleTask;

    public FarmingManager(BudLords plugin, DataManager dataManager, StrainManager strainManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.strainManager = strainManager;
        this.plants = new ConcurrentHashMap<>();
        
        loadPlants();
        startGrowthTask();
        startParticleTask();
    }

    private void loadPlants() {
        FileConfiguration config = dataManager.getPlantsConfig();
        ConfigurationSection plantsSection = config.getConfigurationSection("plants");
        
        if (plantsSection == null) {
            return;
        }

        for (String key : plantsSection.getKeys(false)) {
            try {
                ConfigurationSection plantSection = plantsSection.getConfigurationSection(key);
                if (plantSection == null) continue;

                UUID id = UUID.fromString(plantSection.getString("id", UUID.randomUUID().toString()));
                String strainId = plantSection.getString("strain-id");
                UUID ownerUuid = UUID.fromString(plantSection.getString("owner"));
                long plantedTime = plantSection.getLong("planted-time");
                int growthStage = plantSection.getInt("growth-stage");
                int quality = plantSection.getInt("quality");
                long lastGrowthUpdate = plantSection.getLong("last-growth-update");

                // Parse location
                String worldName = plantSection.getString("world");
                int x = plantSection.getInt("x");
                int y = plantSection.getInt("y");
                int z = plantSection.getInt("z");

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("World not found for plant: " + worldName);
                    continue;
                }

                Location location = new Location(world, x, y, z);
                Plant plant = new Plant(id, strainId, location, ownerUuid, plantedTime, growthStage, quality, lastGrowthUpdate);
                plants.put(plant.getLocationString(), plant);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load plant: " + key);
            }
        }
    }

    public void savePlants() {
        FileConfiguration config = dataManager.getPlantsConfig();
        config.set("plants", null); // Clear existing

        for (Plant plant : plants.values()) {
            String key = "plants." + plant.getId().toString();
            config.set(key + ".id", plant.getId().toString());
            config.set(key + ".strain-id", plant.getStrainId());
            config.set(key + ".owner", plant.getOwnerUuid().toString());
            config.set(key + ".planted-time", plant.getPlantedTime());
            config.set(key + ".growth-stage", plant.getGrowthStage());
            config.set(key + ".quality", plant.getQuality());
            config.set(key + ".last-growth-update", plant.getLastGrowthUpdate());
            config.set(key + ".world", plant.getLocation().getWorld().getName());
            config.set(key + ".x", plant.getLocation().getBlockX());
            config.set(key + ".y", plant.getLocation().getBlockY());
            config.set(key + ".z", plant.getLocation().getBlockZ());
        }

        dataManager.savePlants();
    }

    private void startGrowthTask() {
        int intervalTicks = plugin.getConfig().getInt("farming.growth-check-interval-seconds", 60) * 20;
        
        growthTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            long growthIntervalMs = plugin.getConfig().getLong("farming.growth-interval-seconds", 300) * 1000;

            for (Plant plant : plants.values()) {
                if (plant.isFullyGrown()) continue;

                if (currentTime - plant.getLastGrowthUpdate() >= growthIntervalMs) {
                    processGrowth(plant);
                }
            }
        }, intervalTicks, intervalTicks);
    }

    private void startParticleTask() {
        int intervalTicks = plugin.getConfig().getInt("farming.particle-interval-ticks", 40);
        
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Plant plant : plants.values()) {
                spawnGrowthParticles(plant);
            }
        }, intervalTicks, intervalTicks);
    }

    private void processGrowth(Plant plant) {
        Location loc = plant.getLocation();
        
        // Check if chunk is loaded
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }

        // Calculate quality modifiers
        int qualityBonus = 0;

        // Light level bonus
        int lightLevel = loc.getBlock().getLightLevel();
        if (lightLevel >= 12) {
            qualityBonus += 5;
        } else if (lightLevel < 8) {
            qualityBonus -= 5;
        }

        // Water/hydration bonus (farmland moisture)
        Block below = loc.getBlock().getRelative(BlockFace.DOWN);
        if (below.getType() == Material.FARMLAND) {
            if (below.getBlockData() instanceof Farmland farmland) {
                if (farmland.getMoisture() == farmland.getMaximumMoisture()) {
                    qualityBonus += 5;
                }
            }
        }

        // Wall bonus (enclosed growing)
        int wallCount = countSurroundingWalls(loc);
        if (wallCount >= 3) {
            qualityBonus += 10;
        } else if (wallCount >= 2) {
            qualityBonus += 5;
        }

        plant.addQuality(qualityBonus);
        plant.grow();

        // Update visual
        updatePlantVisual(plant);
    }

    private int countSurroundingWalls(Location loc) {
        int count = 0;
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        
        for (BlockFace face : faces) {
            Block adjacent = loc.getBlock().getRelative(face);
            if (adjacent.getType().isSolid() && adjacent.getType().isOccluding()) {
                count++;
            }
        }
        
        return count;
    }

    private void updatePlantVisual(Plant plant) {
        Location loc = plant.getLocation();
        Block block = loc.getBlock();
        
        // Set block to wheat first
        block.setType(Material.WHEAT);
        
        // Determine age based on growth stage
        int targetAge = switch (plant.getGrowthStage()) {
            case 0 -> 0;  // Seed
            case 1 -> 2;  // Small
            case 2 -> 4;  // Mid
            case 3 -> 7;  // Full (max age)
            default -> 0;
        };
        
        // Apply age to the crop block
        if (block.getBlockData() instanceof Ageable ageable) {
            int age = Math.min(targetAge, ageable.getMaximumAge());
            ageable.setAge(age);
            block.setBlockData(ageable);
        }
    }

    private void spawnGrowthParticles(Plant plant) {
        Location loc = plant.getLocation();
        
        // Check if chunk is loaded
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }

        Location particleLoc = loc.clone().add(0.5, 0.5, 0.5);
        
        if (plant.isFullyGrown()) {
            // Green sparkles for fully grown
            loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 3, 0.3, 0.3, 0.3, 0);
        } else {
            // Growing particles based on stage
            switch (plant.getGrowthStage()) {
                case 0 -> loc.getWorld().spawnParticle(Particle.DRIP_WATER, particleLoc, 1, 0.2, 0.2, 0.2, 0);
                case 1 -> loc.getWorld().spawnParticle(Particle.COMPOSTER, particleLoc, 2, 0.2, 0.2, 0.2, 0);
                case 2 -> loc.getWorld().spawnParticle(Particle.COMPOSTER, particleLoc, 3, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    public boolean plantSeed(Player player, Location location, String strainId) {
        Strain strain = strainManager.getStrain(strainId);
        if (strain == null) {
            return false;
        }

        Block targetBlock = location.getBlock();
        Block below = targetBlock.getRelative(BlockFace.DOWN);

        // Must be placed on farmland
        if (below.getType() != Material.FARMLAND) {
            player.sendMessage("§cSeeds can only be planted on farmland!");
            return false;
        }

        // Check if location already has a plant
        String locationKey = location.getWorld().getName() + "," + 
                            location.getBlockX() + "," + 
                            location.getBlockY() + "," + 
                            location.getBlockZ();
        
        if (plants.containsKey(locationKey)) {
            player.sendMessage("§cThere is already a plant here!");
            return false;
        }

        // Create and register plant
        Plant plant = new Plant(strainId, location, player.getUniqueId());
        plants.put(plant.getLocationString(), plant);

        // Set initial visual
        targetBlock.setType(Material.WHEAT);

        player.sendMessage("§aPlanted " + strain.getName() + " seed!");
        return true;
    }

    public Plant harvestPlant(Player player, Location location) {
        String locationKey = location.getWorld().getName() + "," + 
                            location.getBlockX() + "," + 
                            location.getBlockY() + "," + 
                            location.getBlockZ();
        
        Plant plant = plants.get(locationKey);
        if (plant == null) {
            return null;
        }

        if (!plant.isFullyGrown()) {
            player.sendMessage("§cThis plant is not ready for harvest yet!");
            return null;
        }

        // Remove plant
        plants.remove(locationKey);
        location.getBlock().setType(Material.AIR);

        // Spawn harvest particles
        Location particleLoc = location.clone().add(0.5, 0.5, 0.5);
        location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 20, 0.5, 0.5, 0.5, 0.1);

        return plant;
    }

    public Plant getPlantAt(Location location) {
        String locationKey = location.getWorld().getName() + "," + 
                            location.getBlockX() + "," + 
                            location.getBlockY() + "," + 
                            location.getBlockZ();
        return plants.get(locationKey);
    }

    public void removePlant(Location location) {
        String locationKey = location.getWorld().getName() + "," + 
                            location.getBlockX() + "," + 
                            location.getBlockY() + "," + 
                            location.getBlockZ();
        plants.remove(locationKey);
    }

    public int getPlantCount() {
        return plants.size();
    }

    public Collection<Plant> getAllPlants() {
        return Collections.unmodifiableCollection(plants.values());
    }

    public void shutdown() {
        if (growthTask != null) {
            growthTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
        savePlants();
    }
}
