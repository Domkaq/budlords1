package com.budlords.farming;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages enhanced ambient visual effects for plants and the farming environment.
 * Creates an immersive atmosphere with particles, sounds, and visual feedback.
 */
public class AmbientEffectsManager {

    private final BudLords plugin;
    private final FarmingManager farmingManager;
    private final StrainManager strainManager;
    
    private BukkitTask ambientTask;
    private BukkitTask weatherEffectTask;
    private BukkitTask fireflyTask;
    
    // Effect settings - optimized for better performance with many plants
    private static final int AMBIENT_INTERVAL_TICKS = 100;     // 5 seconds (increased from 3)
    private static final int WEATHER_INTERVAL_TICKS = 120;     // 6 seconds (increased from 5)
    private static final int FIREFLY_INTERVAL_TICKS = 60;      // 3 seconds (increased from 2)
    
    // Reduced spawn chances for better performance
    private static final double FIREFLY_SPAWN_CHANCE = 0.25;   // Reduced from 0.4
    private static final double POLLEN_SPAWN_CHANCE = 0.2;     // Reduced from 0.3
    private static final double SPARKLE_CHANCE = 0.15;         // Reduced from 0.2
    
    // Performance limits
    private static final int MAX_PARTICLES_PER_CYCLE = 30;     // Maximum particles per update cycle
    private static final int MANY_PLANTS_THRESHOLD = 15;       // Consider "many plants" threshold
    private static final double MANY_PLANTS_SPAWN_REDUCTION = 0.5; // Spawn chance reduction factor when many plants

    public AmbientEffectsManager(BudLords plugin, FarmingManager farmingManager, StrainManager strainManager) {
        this.plugin = plugin;
        this.farmingManager = farmingManager;
        this.strainManager = strainManager;
        
        startAmbientEffects();
        startWeatherEffects();
        startFireflyEffects();
    }

    private void startAmbientEffects() {
        ambientTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Collection<Plant> plants = farmingManager.getAllPlants();
            
            // Performance optimization: limit particle spawning when many plants exist
            int plantCount = plants.size();
            boolean manyPlants = plantCount > MANY_PLANTS_THRESHOLD;
            int particlesSpawned = 0;
            
            for (Plant plant : plants) {
                if (!isChunkLoaded(plant.getLocation())) continue;
                
                // Limit total particles when there are many plants
                if (manyPlants && particlesSpawned >= MAX_PARTICLES_PER_CYCLE) break;
                
                // Spawn ambient particles based on plant state
                spawnAmbientParticles(plant);
                particlesSpawned++;
            }
        }, AMBIENT_INTERVAL_TICKS, AMBIENT_INTERVAL_TICKS);
    }

    private void startWeatherEffects() {
        weatherEffectTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Collection<Plant> plants = farmingManager.getAllPlants();
            
            // Performance optimization: limit particle spawning when many plants exist
            int plantCount = plants.size();
            boolean manyPlants = plantCount > MANY_PLANTS_THRESHOLD;
            int particlesSpawned = 0;
            
            for (Plant plant : plants) {
                if (!isChunkLoaded(plant.getLocation())) continue;
                
                // Limit total particles when there are many plants
                if (manyPlants && particlesSpawned >= MAX_PARTICLES_PER_CYCLE) break;
                
                World world = plant.getLocation().getWorld();
                if (world == null) continue;
                
                // Weather-based effects
                if (world.hasStorm()) {
                    spawnRainEffects(plant);
                } else if (world.getTime() >= 13000 && world.getTime() <= 23000) {
                    // Night time effects
                    spawnNightEffects(plant);
                } else {
                    // Day time effects
                    spawnDayEffects(plant);
                }
                particlesSpawned++;
            }
        }, WEATHER_INTERVAL_TICKS, WEATHER_INTERVAL_TICKS);
    }

    private void startFireflyEffects() {
        fireflyTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Collection<Plant> plants = farmingManager.getAllPlants();
            
            // Performance optimization: limit fireflies when many plants exist
            int plantCount = plants.size();
            boolean manyPlants = plantCount > MANY_PLANTS_THRESHOLD;
            int firefliesSpawned = 0;
            int maxFireflies = manyPlants ? 10 : 20; // Limit fireflies when many plants
            
            for (Plant plant : plants) {
                if (!isChunkLoaded(plant.getLocation())) continue;
                
                // Limit total fireflies when there are many plants
                if (manyPlants && firefliesSpawned >= maxFireflies) break;
                
                World world = plant.getLocation().getWorld();
                if (world == null) continue;
                
                // Fireflies only at night near mature plants
                if (world.getTime() >= 13000 && world.getTime() <= 23000 && plant.isFullyGrown()) {
                    double spawnChance = manyPlants ? FIREFLY_SPAWN_CHANCE * MANY_PLANTS_SPAWN_REDUCTION : FIREFLY_SPAWN_CHANCE;
                    if (ThreadLocalRandom.current().nextDouble() < spawnChance) {
                        spawnFirefly(plant.getLocation());
                        firefliesSpawned++;
                    }
                }
            }
        }, FIREFLY_INTERVAL_TICKS, FIREFLY_INTERVAL_TICKS);
    }

    public void shutdown() {
        if (ambientTask != null) ambientTask.cancel();
        if (weatherEffectTask != null) weatherEffectTask.cancel();
        if (fireflyTask != null) fireflyTask.cancel();
    }

    // ===== PARTICLE EFFECTS =====

    private void spawnAmbientParticles(Plant plant) {
        Location loc = plant.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        Location particleLoc = loc.clone().add(0.5, 0.3, 0.5);
        
        Strain strain = strainManager.getStrain(plant.getStrainId());
        boolean isLegendary = strain != null && strain.getRarity() == Strain.Rarity.LEGENDARY;
        
        // Water level visual indicator
        if (plant.getWaterLevel() > 0.7) {
            // Well-watered - show moisture particles
            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                world.spawnParticle(Particle.DRIP_WATER, particleLoc.clone().add(0, 0.3, 0), 
                    1, 0.1, 0.1, 0.1, 0);
            }
        } else if (plant.getWaterLevel() < 0.3) {
            // Needs water - show dry/smoke particles
            if (ThreadLocalRandom.current().nextDouble() < 0.4) {
                world.spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 
                    2, 0.15, 0.1, 0.15, 0.01);
            }
        }
        
        // Nutrient level indicator
        if (plant.getNutrientLevel() > 0.7) {
            // Well-fed - green sparkles
            if (ThreadLocalRandom.current().nextDouble() < 0.25) {
                world.spawnParticle(Particle.COMPOSTER, particleLoc.clone().add(0, 0.2, 0), 
                    2, 0.1, 0.1, 0.1, 0);
            }
        }
        
        // Growth stage effects
        if (plant.isFullyGrown()) {
            spawnMaturePlantAmbient(plant, particleLoc, isLegendary);
        } else {
            spawnGrowingPlantAmbient(plant, particleLoc);
        }
        
        // Lamp glow effect
        if (plant.getLampRating() != null) {
            spawnLampAmbient(plant, particleLoc);
        }
    }

    private void spawnMaturePlantAmbient(Plant plant, Location loc, boolean isLegendary) {
        World world = loc.getWorld();
        if (world == null) return;
        
        // Pollen particles for mature plants
        if (ThreadLocalRandom.current().nextDouble() < POLLEN_SPAWN_CHANCE) {
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), 
                1, 0.3, 0.3, 0.3, 0.005);
        }
        
        // Occasional happy sparkle
        if (ThreadLocalRandom.current().nextDouble() < SPARKLE_CHANCE) {
            world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 0.3, 0), 
                2, 0.2, 0.2, 0.2, 0);
        }
        
        // Legendary plants have special aura
        if (isLegendary) {
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.8, 0), 
                2, 0.2, 0.3, 0.2, 0.02);
            
            // Occasional golden particles
            if (ThreadLocalRandom.current().nextDouble() < 0.15) {
                world.spawnParticle(Particle.TOTEM, loc.clone().add(0, 0.5, 0), 
                    1, 0.1, 0.1, 0.1, 0.05);
            }
        }
        
        // Five-star plants glow
        StarRating rating = plant.calculateFinalBudRating(null);
        if (rating == StarRating.FIVE_STAR) {
            world.spawnParticle(Particle.GLOW, loc.clone().add(0, 0.4, 0), 
                1, 0.15, 0.15, 0.15, 0);
        }
    }

    private void spawnGrowingPlantAmbient(Plant plant, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        
        // Small growth particles
        if (ThreadLocalRandom.current().nextDouble() < 0.15) {
            world.spawnParticle(Particle.COMPOSTER, loc, 
                1, 0.1, 0.1, 0.1, 0);
        }
    }

    private void spawnLampAmbient(Plant plant, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        
        StarRating lampRating = plant.getLampRating();
        if (lampRating == null) return;
        
        // Warm glow based on lamp quality
        int intensity = lampRating.getStars();
        
        // Glow particles
        if (ThreadLocalRandom.current().nextDouble() < 0.2 + (intensity * 0.05)) {
            world.spawnParticle(Particle.GLOW, loc.clone().add(0, 0.6, 0), 
                intensity, 0.2, 0.1, 0.2, 0);
        }
        
        // High quality lamps have special effects
        if (lampRating == StarRating.FIVE_STAR) {
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.8, 0), 
                1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private void spawnFirefly(Location baseLoc) {
        World world = baseLoc.getWorld();
        if (world == null) return;
        
        // Random offset for firefly position
        double offsetX = ThreadLocalRandom.current().nextDouble(-1.5, 1.5);
        double offsetY = ThreadLocalRandom.current().nextDouble(0.3, 1.5);
        double offsetZ = ThreadLocalRandom.current().nextDouble(-1.5, 1.5);
        
        Location fireflyLoc = baseLoc.clone().add(0.5 + offsetX, offsetY, 0.5 + offsetZ);
        
        // Firefly particle (warm glow effect)
        world.spawnParticle(Particle.END_ROD, fireflyLoc, 1, 0, 0, 0, 0);
        
        // Slight movement trail
        world.spawnParticle(Particle.WAX_OFF, fireflyLoc.clone().add(0, -0.1, 0), 
            1, 0.02, 0.02, 0.02, 0);
    }

    // ===== WEATHER EFFECTS =====

    private void spawnRainEffects(Plant plant) {
        Location loc = plant.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        Location particleLoc = loc.clone().add(0.5, 1.0, 0.5);
        
        // Rain drops hitting the plant
        if (ThreadLocalRandom.current().nextDouble() < 0.4) {
            world.spawnParticle(Particle.DRIP_WATER, particleLoc, 
                3, 0.3, 0.2, 0.3, 0);
        }
        
        // Splash effects
        if (ThreadLocalRandom.current().nextDouble() < 0.2) {
            world.spawnParticle(Particle.WATER_DROP, particleLoc.clone().add(0, -0.5, 0), 
                2, 0.2, 0.1, 0.2, 0);
        }
        
        // During rain, plants get water bonus (handled by FarmingManager)
    }

    private void spawnNightEffects(Plant plant) {
        Location loc = plant.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        // Mystical night particles for mature plants
        if (plant.isFullyGrown()) {
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                Location particleLoc = loc.clone().add(0.5, 0.5, 0.5);
                world.spawnParticle(Particle.ENCHANTMENT_TABLE, particleLoc, 
                    5, 0.3, 0.4, 0.3, 0.5);
            }
        }
    }

    private void spawnDayEffects(Plant plant) {
        Location loc = plant.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        // Sunlight shimmer on well-maintained plants
        if (plant.getWaterLevel() > 0.5 && plant.getNutrientLevel() > 0.5) {
            if (ThreadLocalRandom.current().nextDouble() < 0.08) {
                Location particleLoc = loc.clone().add(0.5, 0.6, 0.5);
                world.spawnParticle(Particle.WAX_ON, particleLoc, 
                    2, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    // ===== CELEBRATION EFFECTS =====

    /**
     * Plays a harvest celebration effect at the given location.
     * Optimized to reduce particle spam.
     */
    public void playHarvestCelebration(Location loc, Strain.Rarity rarity, StarRating quality) {
        World world = loc.getWorld();
        if (world == null) return;
        
        Location effectLoc = loc.clone().add(0.5, 0.5, 0.5);
        
        // Reduced base particles for better performance
        world.spawnParticle(Particle.VILLAGER_HAPPY, effectLoc, 
            8, 0.3, 0.3, 0.3, 0.1);
        world.spawnParticle(Particle.COMPOSTER, effectLoc, 
            5, 0.3, 0.3, 0.3, 0.05);
        
        // Quality-based effects (reduced)
        if (quality == StarRating.FIVE_STAR) {
            world.spawnParticle(Particle.END_ROD, effectLoc, 
                10, 0.4, 0.5, 0.4, 0.05);
            world.spawnParticle(Particle.TOTEM, effectLoc.clone().add(0, 0.3, 0), 
                3, 0.2, 0.2, 0.2, 0.1);
        }
        
        // Rarity-based effects (reduced)
        if (rarity == Strain.Rarity.LEGENDARY) {
            world.spawnParticle(Particle.TOTEM, effectLoc, 
                15, 0.5, 0.7, 0.5, 0.15);
            world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        } else if (rarity == Strain.Rarity.RARE) {
            world.spawnParticle(Particle.END_ROD, effectLoc, 
                8, 0.4, 0.5, 0.4, 0.03);
            world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);
        } else {
            world.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        }
    }

    /**
     * Plays a planting celebration effect.
     */
    public void playPlantingEffect(Location loc, StarRating seedRating) {
        World world = loc.getWorld();
        if (world == null) return;
        
        Location effectLoc = loc.clone().add(0.5, 0.3, 0.5);
        
        // Base planting particles
        world.spawnParticle(Particle.COMPOSTER, effectLoc, 
            10, 0.3, 0.1, 0.3, 0.02);
        
        // Quality-based extra effects
        if (seedRating != null && seedRating.getStars() >= 4) {
            world.spawnParticle(Particle.END_ROD, effectLoc.clone().add(0, 0.2, 0), 
                5, 0.2, 0.2, 0.2, 0.01);
        }
        
        world.playSound(loc, Sound.BLOCK_GRASS_PLACE, 0.5f, 1.0f);
    }

    /**
     * Plays a watering effect.
     */
    public void playWateringEffect(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        
        Location effectLoc = loc.clone().add(0.5, 0.5, 0.5);
        
        world.spawnParticle(Particle.DRIP_WATER, effectLoc, 
            15, 0.3, 0.2, 0.3, 0);
        world.spawnParticle(Particle.WATER_SPLASH, effectLoc.clone().add(0, -0.2, 0), 
            8, 0.2, 0.1, 0.2, 0.02);
        
        world.playSound(loc, Sound.ITEM_BUCKET_EMPTY, 0.5f, 1.2f);
    }

    /**
     * Plays a fertilizing effect.
     */
    public void playFertilizingEffect(Location loc, StarRating fertilizerRating) {
        World world = loc.getWorld();
        if (world == null) return;
        
        Location effectLoc = loc.clone().add(0.5, 0.3, 0.5);
        
        int intensity = fertilizerRating != null ? fertilizerRating.getStars() * 4 : 10;
        
        world.spawnParticle(Particle.COMPOSTER, effectLoc, 
            intensity, 0.3, 0.2, 0.3, 0.02);
        
        if (fertilizerRating != null && fertilizerRating.getStars() >= 4) {
            world.spawnParticle(Particle.VILLAGER_HAPPY, effectLoc.clone().add(0, 0.2, 0), 
                5, 0.2, 0.1, 0.2, 0);
        }
        
        world.playSound(loc, Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.0f);
    }

    // ===== UTILITY =====

    private boolean isChunkLoaded(Location loc) {
        if (loc.getWorld() == null) return false;
        return loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }
}
