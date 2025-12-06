package com.budlords.farming;

import com.budlords.BudLords;
import com.budlords.data.DataManager;
import com.budlords.quality.GrowingPot;
import com.budlords.quality.PlacedLamp;
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
    private final Map<String, PlacedLamp> placedLamps; // locationString -> PlacedLamp
    private BukkitTask growthTask;
    private BukkitTask particleTask;
    private BukkitTask careDecayTask;
    private BukkitTask lampEffectTask;

    public FarmingManager(BudLords plugin, DataManager dataManager, StrainManager strainManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.strainManager = strainManager;
        this.plants = new ConcurrentHashMap<>();
        this.pots = new ConcurrentHashMap<>();
        this.placedLamps = new ConcurrentHashMap<>();
        
        loadPlants();
        loadPots();
        loadPlacedLamps();
        startGrowthTask();
        startParticleTask();
        startCareDecayTask();
        startLampEffectTask();
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
                
                UUID ownerUuid = plant.getOwnerUuid();
                
                // Apply skill-based growth speed bonus (Green Fingers, Accelerated Growth)
                if (ownerUuid != null && plugin.getSkillManager() != null) {
                    double skillGrowthBonus = plugin.getSkillManager().getBonusMultiplier(ownerUuid, 
                        com.budlords.skills.Skill.BonusType.GROWTH_SPEED);
                    growthMultiplier *= skillGrowthBonus;
                }
                
                // Apply prestige growth speed bonus if available
                if (plugin.getPrestigeManager() != null && plugin.getStatsManager() != null) {
                    if (ownerUuid != null) {
                        com.budlords.stats.PlayerStats stats = plugin.getStatsManager().getStats(ownerUuid);
                        if (stats != null && stats.getPrestigeLevel() > 0) {
                            double prestigeMult = plugin.getPrestigeManager().getGrowthSpeedMultiplier(stats.getPrestigeLevel());
                            growthMultiplier *= prestigeMult;
                        }
                    }
                }
                
                // Apply cooperative farming bonus (nearby players boost growth)
                double coopBonus = getCooperativeFarmingBonus(plant.getLocation());
                growthMultiplier *= coopBonus;
                
                long effectiveInterval = (long) (baseGrowthIntervalMs / growthMultiplier);

                if (currentTime - plant.getLastGrowthUpdate() >= effectiveInterval) {
                    processGrowth(plant);
                }
            }
        }, intervalTicks, intervalTicks);
    }
    
    /**
     * Calculates cooperative farming bonus based on nearby online players.
     * More players nearby = faster growth and better quality!
     * @param location The plant location
     * @return Multiplier for growth speed (1.0 to 1.5)
     */
    private static final int COOPERATIVE_FARMING_RADIUS = 20; // blocks
    
    private double getCooperativeFarmingBonus(Location location) {
        if (location.getWorld() == null) return 1.0;
        
        // Count nearby players within configured radius
        int nearbyPlayers = 0;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= COOPERATIVE_FARMING_RADIUS) {
                nearbyPlayers++;
            }
        }
        
        // Bonus caps at 4 players for max 50% boost
        // 1 player: +10%, 2 players: +20%, 3 players: +35%, 4+ players: +50%
        return switch (nearbyPlayers) {
            case 0 -> 1.0;
            case 1 -> 1.10;
            case 2 -> 1.20;
            case 3 -> 1.35;
            default -> 1.50;
        };
    }

    private void startParticleTask() {
        int intervalTicks = plugin.getConfig().getInt("farming.particle-interval-ticks", 40);
        
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Plant plant : plants.values()) {
                spawnGrowthParticles(plant);
            }
        }, intervalTicks, intervalTicks);
    }
    
    // Decay rate constants for plant care system
    // These values determine how quickly water and nutrients deplete per minute
    private static final double WATER_DECAY_RATE = 0.02;    // 2% per minute
    private static final double NUTRIENT_DECAY_RATE = 0.01; // 1% per minute
    private static final long DECAY_INTERVAL_TICKS = 1200L; // 1 minute in ticks
    
    private void startCareDecayTask() {
        // Decay water and nutrients over time
        careDecayTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Plant plant : plants.values()) {
                // Slowly decrease water and nutrients
                plant.setWaterLevel(plant.getWaterLevel() - WATER_DECAY_RATE);
                plant.setNutrientLevel(plant.getNutrientLevel() - NUTRIENT_DECAY_RATE);
            }
        }, DECAY_INTERVAL_TICKS, DECAY_INTERVAL_TICKS);
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
        
        // Apply prestige quality bonus
        if (plugin.getPrestigeManager() != null && plugin.getStatsManager() != null) {
            UUID ownerUuid = plant.getOwnerUuid();
            if (ownerUuid != null) {
                com.budlords.stats.PlayerStats stats = plugin.getStatsManager().getStats(ownerUuid);
                if (stats != null && stats.getPrestigeLevel() > 0) {
                    double qualityMult = plugin.getPrestigeManager().getQualityMultiplier(stats.getPrestigeLevel());
                    qualityBonus = (int) Math.round(qualityBonus * qualityMult);
                }
            }
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
        // Check if 3D visualization is enabled
        PlantVisualizationManager vizManager = plugin.getPlantVisualizationManager();
        if (vizManager != null && plugin.getConfig().getBoolean("farming.3d-visualization", true)) {
            // Use new armor stand-based 3D visualization
            vizManager.updatePlantVisual(plant);
            return;
        }
        
        // Fallback to original wheat block visualization
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

        // Set initial visual - only set WHEAT block if 3D visualization is disabled
        PlantVisualizationManager vizManager = plugin.getPlantVisualizationManager();
        if (vizManager != null && plugin.getConfig().getBoolean("farming.3d-visualization", true)) {
            // Use armor stand-based 3D visualization - set block to AIR
            // The armor stands will provide the visual
            targetBlock.setType(Material.AIR);
            vizManager.updatePlantVisual(plant);
        } else {
            // Fallback: use wheat block for visual
            targetBlock.setType(Material.WHEAT);
        }

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
        return waterPlant(player, location, null);
    }
    
    /**
     * Gets the configured watering bonus cooldown in milliseconds.
     */
    private long getWateringBonusCooldownMs() {
        return plugin.getConfig().getLong("farming.watering-bonus-cooldown-seconds", 60) * 1000L;
    }
    
    /**
     * Waters a plant at the given location with a specific watering can quality.
     * @param wateringCanRating The star rating of the watering can (null for standard water bucket)
     */
    public boolean waterPlant(Player player, Location location, StarRating wateringCanRating) {
        Plant plant = getPlantAt(location);
        if (plant == null) {
            player.sendMessage("§cNo plant found at this location!");
            return false;
        }
        
        long cooldownMs = getWateringBonusCooldownMs();
        
        // Use quality-aware watering if a watering can was used
        if (wateringCanRating != null) {
            boolean gotBonus = plant.water(wateringCanRating, cooldownMs);
            if (gotBonus) {
                String qualityBonus = " §a(+" + wateringCanRating.getStars() + " quality bonus!)";
                player.sendMessage("§aWatered the plant! §7Water level: §b" + 
                    String.format("%.0f%%", plant.getWaterLevel() * 100) + qualityBonus);
            } else {
                // On cooldown - still water but no quality bonus
                long cooldownRemaining = plant.getWateringBonusCooldownRemaining(cooldownMs);
                player.sendMessage("§aWatered the plant! §7Water level: §b" + 
                    String.format("%.0f%%", plant.getWaterLevel() * 100));
                player.sendMessage("§7Quality bonus on cooldown (§e" + cooldownRemaining + "s§7 remaining)");
            }
        } else {
            plant.water();
            player.sendMessage("§aWatered the plant! §7Water level: §b" + 
                String.format("%.0f%%", plant.getWaterLevel() * 100));
        }
        
        // Update challenge progress
        if (plugin.getChallengeManager() != null) {
            plugin.getChallengeManager().updateProgress(player, 
                com.budlords.challenges.Challenge.ChallengeType.WATER_PLANTS, 1);
        }
        
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
        
        // Update challenge progress
        if (plugin.getChallengeManager() != null) {
            plugin.getChallengeManager().updateProgress(player, 
                com.budlords.challenges.Challenge.ChallengeType.USE_FERTILIZER, 1);
        }
        
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
        
        // Clean up 3D visualization armor stands
        PlantVisualizationManager vizManager = plugin.getPlantVisualizationManager();
        if (vizManager != null) {
            vizManager.removeVisualization(location);
        }

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
        
        // Clean up 3D visualization if enabled
        PlantVisualizationManager vizManager = plugin.getPlantVisualizationManager();
        if (vizManager != null) {
            vizManager.removeVisualization(location);
        }
    }

    public int getPlantCount() {
        return plants.size();
    }

    public Collection<Plant> getAllPlants() {
        return Collections.unmodifiableCollection(plants.values());
    }
    
    /**
     * Gets all plants within a certain radius of a location.
     * @param location The center location
     * @param radius The radius in blocks
     * @return List of plants within the radius
     */
    public List<Plant> getNearbyPlants(Location location, int radius) {
        List<Plant> nearbyPlants = new ArrayList<>();
        for (Plant plant : plants.values()) {
            Location plantLoc = plant.getLocation();
            if (plantLoc.getWorld().equals(location.getWorld())) {
                double distance = plantLoc.distance(location);
                if (distance <= radius) {
                    nearbyPlants.add(plant);
                }
            }
        }
        return nearbyPlants;
    }
    
    // ====== POT MANAGEMENT ======
    
    private void loadPots() {
        FileConfiguration config = dataManager.getPlantsConfig();
        ConfigurationSection potsSection = config.getConfigurationSection("pots");
        
        if (potsSection == null) {
            return;
        }

        for (String key : potsSection.getKeys(false)) {
            try {
                ConfigurationSection potSection = potsSection.getConfigurationSection(key);
                if (potSection == null) continue;

                String worldName = potSection.getString("world");
                int x = potSection.getInt("x");
                int y = potSection.getInt("y");
                int z = potSection.getInt("z");
                String ratingStr = potSection.getString("rating", "ONE_STAR");
                String ownerStr = potSection.getString("owner");

                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                Location location = new Location(world, x, y, z);
                StarRating rating;
                try {
                    rating = StarRating.valueOf(ratingStr);
                } catch (IllegalArgumentException e) {
                    rating = StarRating.ONE_STAR;
                }
                UUID owner = ownerStr != null ? UUID.fromString(ownerStr) : null;
                
                GrowingPot pot = new GrowingPot(UUID.randomUUID(), rating, location, owner);
                pots.put(getLocationKey(location), pot);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load pot: " + key);
            }
        }
        
        plugin.getLogger().info("Loaded " + pots.size() + " placed pots.");
    }
    
    public void savePots() {
        FileConfiguration config = dataManager.getPlantsConfig();
        config.set("pots", null); // Clear existing

        int counter = 0;
        for (Map.Entry<String, GrowingPot> entry : pots.entrySet()) {
            GrowingPot pot = entry.getValue();
            Location loc = pot.getLocation();
            if (loc == null || loc.getWorld() == null) continue;
            
            String key = "pots.pot_" + counter;
            config.set(key + ".world", loc.getWorld().getName());
            config.set(key + ".x", loc.getBlockX());
            config.set(key + ".y", loc.getBlockY());
            config.set(key + ".z", loc.getBlockZ());
            config.set(key + ".rating", pot.getStarRating().name());
            if (pot.getOwnerUuid() != null) {
                config.set(key + ".owner", pot.getOwnerUuid().toString());
            }
            counter++;
        }

        dataManager.savePlants();
    }
    
    /**
     * Places a pot at the specified location with the given star rating.
     */
    public void placePot(Location location, StarRating rating, UUID ownerUuid) {
        String key = getLocationKey(location);
        GrowingPot pot = new GrowingPot(UUID.randomUUID(), rating, location, ownerUuid);
        pots.put(key, pot);
    }
    
    /**
     * Gets the pot at the specified location.
     */
    public GrowingPot getPotAt(Location location) {
        return pots.get(getLocationKey(location));
    }
    
    /**
     * Gets the star rating of the pot at the specified location.
     */
    public StarRating getPotRatingAt(Location location) {
        GrowingPot pot = getPotAt(location);
        return pot != null ? pot.getStarRating() : null;
    }
    
    /**
     * Removes the pot at the specified location and returns it.
     */
    public GrowingPot removePot(Location location) {
        return pots.remove(getLocationKey(location));
    }
    
    /**
     * Checks if there is a pot at the specified location.
     */
    public boolean hasPotAt(Location location) {
        return pots.containsKey(getLocationKey(location));
    }
    
    private String getLocationKey(Location location) {
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }
    
    // ====== PLACED LAMP MANAGEMENT ======
    
    private void loadPlacedLamps() {
        FileConfiguration config = dataManager.getPlantsConfig();
        ConfigurationSection lampsSection = config.getConfigurationSection("placed-lamps");
        
        if (lampsSection == null) {
            return;
        }

        for (String key : lampsSection.getKeys(false)) {
            try {
                ConfigurationSection lampSection = lampsSection.getConfigurationSection(key);
                if (lampSection == null) continue;

                String worldName = lampSection.getString("world");
                int x = lampSection.getInt("x");
                int y = lampSection.getInt("y");
                int z = lampSection.getInt("z");
                String ratingStr = lampSection.getString("rating", "ONE_STAR");
                String ownerStr = lampSection.getString("owner");
                long placedTime = lampSection.getLong("placed-time", System.currentTimeMillis());

                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                Location location = new Location(world, x, y, z);
                StarRating rating;
                try {
                    rating = StarRating.valueOf(ratingStr);
                } catch (IllegalArgumentException e) {
                    rating = StarRating.ONE_STAR;
                }
                UUID owner = ownerStr != null ? UUID.fromString(ownerStr) : null;
                
                PlacedLamp lamp = new PlacedLamp(UUID.randomUUID(), location, rating, owner, placedTime);
                placedLamps.put(getLocationKey(location), lamp);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load placed lamp: " + key);
            }
        }
        
        plugin.getLogger().info("Loaded " + placedLamps.size() + " placed lamps.");
    }
    
    public void savePlacedLamps() {
        FileConfiguration config = dataManager.getPlantsConfig();
        config.set("placed-lamps", null); // Clear existing

        int counter = 0;
        for (Map.Entry<String, PlacedLamp> entry : placedLamps.entrySet()) {
            PlacedLamp lamp = entry.getValue();
            Location loc = lamp.getLocation();
            if (loc == null || loc.getWorld() == null) continue;
            
            String key = "placed-lamps.lamp_" + counter;
            config.set(key + ".world", loc.getWorld().getName());
            config.set(key + ".x", loc.getBlockX());
            config.set(key + ".y", loc.getBlockY());
            config.set(key + ".z", loc.getBlockZ());
            config.set(key + ".rating", lamp.getStarRating().name());
            if (lamp.getOwnerUuid() != null) {
                config.set(key + ".owner", lamp.getOwnerUuid().toString());
            }
            config.set(key + ".placed-time", lamp.getPlacedTime());
            counter++;
        }

        dataManager.savePlants();
    }
    
    /**
     * Places a lamp at the specified location.
     * @param location The location to place the lamp
     * @param rating The star rating of the lamp
     * @param ownerUuid The player who placed the lamp
     * @return true if placement was successful
     */
    public boolean placeLamp(Location location, StarRating rating, UUID ownerUuid) {
        String key = getLocationKey(location);
        if (placedLamps.containsKey(key)) {
            return false; // Already a lamp here
        }
        
        PlacedLamp lamp = new PlacedLamp(UUID.randomUUID(), location, rating, ownerUuid);
        placedLamps.put(key, lamp);
        
        // Apply lamp effects to all plants in range immediately
        applyLampEffectsToNearbyPlants(lamp);
        
        return true;
    }
    
    /**
     * Removes a placed lamp at the specified location.
     * @param location The location of the lamp
     * @return The removed lamp, or null if no lamp was there
     */
    public PlacedLamp removePlacedLamp(Location location) {
        PlacedLamp lamp = placedLamps.remove(getLocationKey(location));
        
        if (lamp != null) {
            // Remove lamp effects from plants that were in range
            removeLampEffectsFromNearbyPlants(lamp);
        }
        
        return lamp;
    }
    
    /**
     * Gets the placed lamp at the specified location.
     */
    public PlacedLamp getPlacedLampAt(Location location) {
        return placedLamps.get(getLocationKey(location));
    }
    
    /**
     * Checks if there is a placed lamp at the specified location.
     */
    public boolean hasPlacedLampAt(Location location) {
        return placedLamps.containsKey(getLocationKey(location));
    }
    
    /**
     * Applies lamp effects to all plants within range of a lamp.
     */
    private void applyLampEffectsToNearbyPlants(PlacedLamp lamp) {
        for (Plant plant : plants.values()) {
            if (lamp.isLocationInRange(plant.getLocation())) {
                // Only apply if this lamp has a better rating than current lamp
                StarRating currentLamp = plant.getLampRating();
                if (currentLamp == null || lamp.getStarRating().getStars() > currentLamp.getStars()) {
                    plant.setLampRating(lamp.getStarRating());
                }
            }
        }
    }
    
    /**
     * Removes lamp effects from plants when a lamp is removed.
     * Recalculates lamp effects from other lamps in range.
     */
    private void removeLampEffectsFromNearbyPlants(PlacedLamp removedLamp) {
        for (Plant plant : plants.values()) {
            if (removedLamp.isLocationInRange(plant.getLocation())) {
                // Check if any other lamp affects this plant
                StarRating bestLampRating = null;
                for (PlacedLamp otherLamp : placedLamps.values()) {
                    if (otherLamp.isLocationInRange(plant.getLocation())) {
                        if (bestLampRating == null || otherLamp.getStarRating().getStars() > bestLampRating.getStars()) {
                            bestLampRating = otherLamp.getStarRating();
                        }
                    }
                }
                plant.setLampRating(bestLampRating); // May be null if no other lamps in range
            }
        }
    }
    
    /**
     * Gets the best lamp rating affecting a specific location.
     * @param location The location to check
     * @return The best StarRating from lamps in range, or null if no lamps
     */
    public StarRating getBestLampRatingAt(Location location) {
        StarRating best = null;
        for (PlacedLamp lamp : placedLamps.values()) {
            if (lamp.isLocationInRange(location)) {
                if (best == null || lamp.getStarRating().getStars() > best.getStars()) {
                    best = lamp.getStarRating();
                }
            }
        }
        return best;
    }
    
    /**
     * Shows the range of a placed lamp using particles.
     * @param lamp The lamp to show range for
     */
    public void showLampRange(PlacedLamp lamp) {
        Location center = lamp.getLocation();
        World world = center.getWorld();
        if (world == null) return;
        
        int range = lamp.getRange();
        
        // Draw circle at lamp height
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) {
            double x = center.getBlockX() + 0.5 + range * Math.cos(angle);
            double z = center.getBlockZ() + 0.5 + range * Math.sin(angle);
            Location particleLoc = new Location(world, x, center.getBlockY() + 0.5, z);
            world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // Draw vertical lines down to show the affected area
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            double x = center.getBlockX() + 0.5 + range * Math.cos(angle);
            double z = center.getBlockZ() + 0.5 + range * Math.sin(angle);
            for (int y = 0; y <= 5 && center.getBlockY() - y >= 0; y++) {
                Location particleLoc = new Location(world, x, center.getBlockY() - y + 0.5, z);
                world.spawnParticle(Particle.GLOW, particleLoc, 1, 0, 0, 0, 0);
            }
        }
        
        // Central beam
        for (int y = 0; y <= 5 && center.getBlockY() - y >= 0; y++) {
            Location particleLoc = center.clone().add(0.5, -y + 0.5, 0.5);
            world.spawnParticle(Particle.GLOW, particleLoc, 3, 0.1, 0.1, 0.1, 0);
        }
        
        // Sound effect
        world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
    }
    
    /**
     * Gets all placed lamps in the tracking system.
     */
    public Collection<PlacedLamp> getAllPlacedLamps() {
        return Collections.unmodifiableCollection(placedLamps.values());
    }
    
    /**
     * Starts the lamp effect task that periodically applies lamp bonuses to plants.
     * This ensures newly planted seeds get lamp effects from existing lamps.
     */
    private void startLampEffectTask() {
        // Run every 5 seconds (100 ticks) to apply lamp effects
        lampEffectTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (PlacedLamp lamp : placedLamps.values()) {
                applyLampEffectsToNearbyPlants(lamp);
            }
        }, 100L, 100L);
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
        if (lampEffectTask != null) {
            lampEffectTask.cancel();
        }
        savePlants();
        savePots();
        savePlacedLamps();
    }
}
