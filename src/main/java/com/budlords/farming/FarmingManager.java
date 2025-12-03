package com.budlords.farming;

import com.budlords.BudLords;
import com.budlords.data.DataManager;
import com.budlords.quality.GrowingPot;
import com.budlords.quality.StarRating;
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
import java.util.concurrent.ThreadLocalRandom;

public class FarmingManager {

    private final BudLords plugin;
    private final DataManager dataManager;
    private final StrainManager strainManager;
    
    private final Map<String, Plant> plants; // locationString -> Plant
    private final Map<String, GrowingPot> pots; // locationString -> GrowingPot
    private BukkitTask growthTask;
    private BukkitTask particleTask;
    private BukkitTask careDecayTask;

    public FarmingManager(BudLords plugin, DataManager dataManager, StrainManager strainManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.strainManager = strainManager;
        this.plants = new ConcurrentHashMap<>();
        this.pots = new ConcurrentHashMap<>();
        
        loadPlants();
        startGrowthTask();
        startParticleTask();
        startCareDecayTask();
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
                
                // Load star ratings (new pot-based system)
                StarRating potRating = parseStarRating(plantSection.getString("pot-rating"));
                StarRating seedRating = parseStarRating(plantSection.getString("seed-rating"));
                StarRating lampRating = parseStarRating(plantSection.getString("lamp-rating"));
                StarRating fertilizerRating = parseStarRating(plantSection.getString("fertilizer-rating"));
                double waterLevel = plantSection.getDouble("water-level", 0.5);
                double nutrientLevel = plantSection.getDouble("nutrient-level", 0.3);
                String potIdStr = plantSection.getString("pot-id");
                UUID potId = potIdStr != null ? UUID.fromString(potIdStr) : null;
                
                Plant plant = new Plant(id, strainId, location, ownerUuid, plantedTime, 
                    growthStage, quality, lastGrowthUpdate, potRating, seedRating, 
                    lampRating, fertilizerRating, waterLevel, nutrientLevel, potId);
                plants.put(plant.getLocationString(), plant);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load plant: " + key);
            }
        }
    }
    
    private StarRating parseStarRating(String value) {
        if (value == null) return null;
        try {
            return StarRating.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
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
            
            // Save star ratings
            if (plant.getPotRating() != null) {
                config.set(key + ".pot-rating", plant.getPotRating().name());
            }
            if (plant.getSeedRating() != null) {
                config.set(key + ".seed-rating", plant.getSeedRating().name());
            }
            if (plant.getLampRating() != null) {
                config.set(key + ".lamp-rating", plant.getLampRating().name());
            }
            if (plant.getFertilizerRating() != null) {
                config.set(key + ".fertilizer-rating", plant.getFertilizerRating().name());
            }
            config.set(key + ".water-level", plant.getWaterLevel());
            config.set(key + ".nutrient-level", plant.getNutrientLevel());
            if (plant.getPotId() != null) {
                config.set(key + ".pot-id", plant.getPotId().toString());
            }
        }

        dataManager.savePlants();
    }

    private void startGrowthTask() {
        int intervalTicks = plugin.getConfig().getInt("farming.growth-check-interval-seconds", 60) * 20;
        
        growthTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            long baseGrowthIntervalMs = plugin.getConfig().getLong("farming.growth-interval-seconds", 300) * 1000;

            for (Plant plant : plants.values()) {
                if (plant.isFullyGrown()) continue;

                // Calculate effective growth interval based on plant's quality bonuses
                double growthMultiplier = plant.getGrowthSpeedMultiplier();
                long effectiveInterval = (long) (baseGrowthIntervalMs / growthMultiplier);

                if (currentTime - plant.getLastGrowthUpdate() >= effectiveInterval) {
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
    
    private void startCareDecayTask() {
        // Decay water and nutrients over time
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Plant plant : plants.values()) {
                // Slowly decrease water and nutrients
                plant.setWaterLevel(plant.getWaterLevel() - 0.02);
                plant.setNutrientLevel(plant.getNutrientLevel() - 0.01);
            }
        }, 1200L, 1200L); // Every minute
    }

    private void processGrowth(Plant plant) {
        Location loc = plant.getLocation();
        
        // Check if chunk is loaded
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }

        // Calculate quality modifiers
        int qualityBonus = 0;

        // Light level bonus (enhanced by lamp rating)
        int lightLevel = loc.getBlock().getLightLevel();
        if (plant.getLampRating() != null) {
            lightLevel = Math.min(15, lightLevel + plant.getLampRating().getStars());
        }
        
        if (lightLevel >= 12) {
            qualityBonus += 5;
        } else if (lightLevel < 8) {
            qualityBonus -= 5;
        }

        // Water and nutrient bonus (pot-based system)
        if (plant.hasPot()) {
            if (plant.getWaterLevel() >= 0.7) {
                qualityBonus += 5;
            } else if (plant.getWaterLevel() < 0.3) {
                qualityBonus -= 5;
            }
            
            if (plant.getNutrientLevel() >= 0.7) {
                qualityBonus += 5;
            }
            
            // Pot rating bonus
            if (plant.getPotRating() != null) {
                qualityBonus += plant.getPotRating().getStars();
            }
            
            // Fertilizer rating bonus
            if (plant.getFertilizerRating() != null) {
                qualityBonus += plant.getFertilizerRating().getStars() * 2;
            }
        } else {
            // Legacy farmland support
            Block below = loc.getBlock().getRelative(BlockFace.DOWN);
            if (below.getType() == Material.FARMLAND) {
                if (below.getBlockData() instanceof Farmland farmland) {
                    if (farmland.getMoisture() == farmland.getMaximumMoisture()) {
                        qualityBonus += 5;
                    }
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

        // Update visual with enhanced effects
        updatePlantVisual(plant);
        
        // Spawn growth transition particles
        spawnGrowthTransitionParticles(plant);
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
        
        // Set block to wheat for plant visualization
        if (block.getType() != Material.WHEAT && block.getType() != Material.FLOWER_POT) {
            block.setType(Material.WHEAT);
        }
        
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
    
    private void spawnGrowthTransitionParticles(Plant plant) {
        Location loc = plant.getLocation();
        Location particleLoc = loc.clone().add(0.5, 0.8, 0.5);
        World world = loc.getWorld();
        
        // Determine particle intensity based on star rating
        int particleCount = plant.getPotRating() != null ? 
            5 + (plant.getPotRating().getStars() * 3) : 8;
        
        // Growth stage transition effect
        switch (plant.getGrowthStage()) {
            case 1 -> { // Sprouting
                world.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, particleCount, 0.3, 0.3, 0.3, 0.05);
                world.spawnParticle(Particle.COMPOSTER, particleLoc, particleCount / 2, 0.2, 0.2, 0.2, 0.02);
            }
            case 2 -> { // Growing
                world.spawnParticle(Particle.COMPOSTER, particleLoc, particleCount, 0.4, 0.4, 0.4, 0.03);
                world.spawnParticle(Particle.END_ROD, particleLoc.add(0, 0.3, 0), 3, 0.1, 0.1, 0.1, 0.01);
            }
            case 3 -> { // Mature - big celebration!
                spawnMatureParticles(plant, particleLoc);
            }
        }
    }

    private void spawnGrowthParticles(Plant plant) {
        Location loc = plant.getLocation();
        
        // Check if chunk is loaded
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }

        Location particleLoc = loc.clone().add(0.5, 0.5, 0.5);
        World world = loc.getWorld();
        
        // Get strain for special effects
        Strain strain = strainManager.getStrain(plant.getStrainId());
        boolean isLegendary = strain != null && strain.getRarity() == Strain.Rarity.LEGENDARY;
        
        if (plant.isFullyGrown()) {
            // Enhanced mature plant particles
            spawnMatureParticles(plant, particleLoc);
        } else {
            // Growing particles based on stage with enhanced effects
            switch (plant.getGrowthStage()) {
                case 0 -> {
                    // Seed stage - water droplets
                    if (plant.getWaterLevel() > 0.5) {
                        world.spawnParticle(Particle.DRIP_WATER, particleLoc, 2, 0.2, 0.1, 0.2, 0);
                    }
                }
                case 1 -> {
                    // Sprout stage - small green particles
                    world.spawnParticle(Particle.COMPOSTER, particleLoc, 3, 0.15, 0.2, 0.15, 0);
                    if (plant.getLampRating() != null) {
                        // Lamp glow effect
                        spawnLampGlowParticles(plant, particleLoc);
                    }
                }
                case 2 -> {
                    // Growing stage - more active particles
                    world.spawnParticle(Particle.COMPOSTER, particleLoc, 4, 0.25, 0.3, 0.25, 0.01);
                    if (isLegendary) {
                        // Special legendary growing effect
                        world.spawnParticle(Particle.END_ROD, particleLoc.add(0, 0.3, 0), 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }
        }
    }
    
    private void spawnMatureParticles(Plant plant, Location particleLoc) {
        World world = particleLoc.getWorld();
        Strain strain = strainManager.getStrain(plant.getStrainId());
        
        if (strain == null) {
            world.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 5, 0.3, 0.3, 0.3, 0);
            return;
        }
        
        // Calculate final star rating for visual intensity
        StarRating finalRating = plant.calculateFinalBudRating(null);
        int intensity = finalRating != null ? finalRating.getStars() : 1;
        
        switch (strain.getRarity()) {
            case LEGENDARY -> {
                // Golden sparkles and special effects
                world.spawnParticle(Particle.END_ROD, particleLoc, 3 + intensity, 0.3, 0.4, 0.3, 0.03);
                world.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 5 + intensity, 0.4, 0.4, 0.4, 0.02);
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    world.spawnParticle(Particle.TOTEM, particleLoc.add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0.1);
                }
                // Play occasional sound for legendary
                if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                    world.playSound(particleLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.2f);
                }
            }
            case RARE -> {
                // Blue-ish sparkles
                world.spawnParticle(Particle.END_ROD, particleLoc, 2 + intensity, 0.25, 0.35, 0.25, 0.02);
                world.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 4 + intensity, 0.35, 0.35, 0.35, 0.01);
            }
            case UNCOMMON -> {
                // Green particles
                world.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 4 + intensity, 0.3, 0.3, 0.3, 0.01);
                world.spawnParticle(Particle.COMPOSTER, particleLoc, 2, 0.2, 0.2, 0.2, 0);
            }
            default -> {
                // Common - simple green sparkles
                world.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 3, 0.3, 0.3, 0.3, 0);
            }
        }
    }
    
    private void spawnLampGlowParticles(Plant plant, Location particleLoc) {
        if (plant.getLampRating() == null) return;
        
        World world = particleLoc.getWorld();
        
        // Warm glow based on lamp quality
        int glowIntensity = plant.getLampRating().getStars();
        world.spawnParticle(Particle.GLOW, particleLoc.clone().add(0, 0.3, 0), glowIntensity, 0.2, 0.1, 0.2, 0);
    }

    public boolean plantSeed(Player player, Location location, String strainId) {
        return plantSeed(player, location, strainId, null, null);
    }
    
    /**
     * Plants a seed in a pot with star quality ratings.
     */
    public boolean plantSeed(Player player, Location location, String strainId, 
                             StarRating potRating, StarRating seedRating) {
        Strain strain = strainManager.getStrain(strainId);
        if (strain == null) {
            return false;
        }

        Block targetBlock = location.getBlock();
        Block below = targetBlock.getRelative(BlockFace.DOWN);

        // Check for pot-based or legacy farmland planting
        boolean isPotPlanting = potRating != null;
        
        if (!isPotPlanting) {
            // Legacy mode: Must be placed on farmland
            if (below.getType() != Material.FARMLAND) {
                player.sendMessage("§cSeeds can only be planted on farmland or in Growing Pots!");
                return false;
            }
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
        
        // Set star ratings if using pot-based growing
        if (isPotPlanting) {
            plant.setPotRating(potRating);
            plant.setSeedRating(seedRating != null ? seedRating : StarRating.ONE_STAR);
            plant.setWaterLevel(0.7);  // Start with good water level in pot
            plant.setNutrientLevel(0.5);
        }
        
        plants.put(plant.getLocationString(), plant);

        // Set initial visual
        targetBlock.setType(Material.WHEAT);

        // Send message with star rating info
        if (isPotPlanting) {
            String ratingInfo = potRating.getDisplay() + " §7pot";
            if (seedRating != null) {
                ratingInfo += " + " + seedRating.getDisplay() + " §7seed";
            }
            player.sendMessage("§aPlanted " + strain.getName() + " in " + ratingInfo + "!");
        } else {
            player.sendMessage("§aPlanted " + strain.getName() + " seed!");
        }
        
        // Spawn planting particles
        Location particleLoc = location.clone().add(0.5, 0.3, 0.5);
        location.getWorld().spawnParticle(Particle.COMPOSTER, particleLoc, 10, 0.3, 0.1, 0.3, 0.02);
        
        return true;
    }
    
    /**
     * Waters a plant at the given location.
     */
    public boolean waterPlant(Player player, Location location) {
        Plant plant = getPlantAt(location);
        if (plant == null) {
            player.sendMessage("§cNo plant found at this location!");
            return false;
        }
        
        plant.water();
        player.sendMessage("§aWatered the plant! §7Water level: §b" + 
            String.format("%.0f%%", plant.getWaterLevel() * 100));
        
        // Water particles
        Location particleLoc = location.clone().add(0.5, 0.5, 0.5);
        location.getWorld().spawnParticle(Particle.DRIP_WATER, particleLoc, 15, 0.3, 0.2, 0.3, 0);
        location.getWorld().playSound(location, Sound.ITEM_BUCKET_EMPTY, 0.5f, 1.2f);
        
        return true;
    }
    
    /**
     * Fertilizes a plant at the given location.
     */
    public boolean fertilizePlant(Player player, Location location, StarRating fertilizerRating) {
        Plant plant = getPlantAt(location);
        if (plant == null) {
            player.sendMessage("§cNo plant found at this location!");
            return false;
        }
        
        plant.fertilize(fertilizerRating);
        player.sendMessage("§aFertilized the plant with " + fertilizerRating.getDisplay() + " §afertilizer!");
        player.sendMessage("§7Nutrient level: §e" + String.format("%.0f%%", plant.getNutrientLevel() * 100));
        
        // Fertilizer particles
        Location particleLoc = location.clone().add(0.5, 0.3, 0.5);
        location.getWorld().spawnParticle(Particle.COMPOSTER, particleLoc, 20, 0.3, 0.2, 0.3, 0.02);
        location.getWorld().playSound(location, Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.0f);
        
        return true;
    }
    
    /**
     * Adds a lamp to a plant at the given location.
     */
    public boolean addLamp(Player player, Location location, StarRating lampRating) {
        Plant plant = getPlantAt(location);
        if (plant == null) {
            player.sendMessage("§cNo plant found at this location!");
            return false;
        }
        
        plant.setLampRating(lampRating);
        player.sendMessage("§aAdded " + lampRating.getDisplay() + " §aGrow Lamp to the plant!");
        
        // Lamp glow particles
        Location particleLoc = location.clone().add(0.5, 1.0, 0.5);
        location.getWorld().spawnParticle(Particle.GLOW, particleLoc, 15, 0.3, 0.3, 0.3, 0.01);
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 0.3f, 1.5f);
        
        return true;
    }

    public Plant harvestPlant(Player player, Location location) {
        return harvestPlant(player, location, null);
    }
    
    public Plant harvestPlant(Player player, Location location, StarRating scissorsRating) {
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

        // Spawn enhanced harvest particles based on strain rarity
        spawnHarvestParticles(plant, location);

        return plant;
    }
    
    private void spawnHarvestParticles(Plant plant, Location location) {
        Location particleLoc = location.clone().add(0.5, 0.5, 0.5);
        World world = location.getWorld();
        
        Strain strain = strainManager.getStrain(plant.getStrainId());
        StarRating finalRating = plant.calculateFinalBudRating(null);
        int intensity = finalRating != null ? finalRating.getStars() * 3 : 5;
        
        if (strain != null && strain.getRarity() == Strain.Rarity.LEGENDARY) {
            // Legendary harvest celebration!
            world.spawnParticle(Particle.TOTEM, particleLoc, 30, 0.5, 0.7, 0.5, 0.15);
            world.spawnParticle(Particle.END_ROD, particleLoc, 20, 0.4, 0.5, 0.4, 0.08);
            world.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        } else {
            world.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 15 + intensity, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.COMPOSTER, particleLoc, 10, 0.4, 0.4, 0.4, 0.05);
        }
        
        world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.3f);
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
        if (careDecayTask != null) {
            careDecayTask.cancel();
        }
        savePlants();
    }
}
