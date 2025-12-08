package com.budlords.diseases;

import com.budlords.BudLords;
import com.budlords.farming.FarmingManager;
import com.budlords.farming.Plant;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages plant diseases, infections, and cures.
 * Part of BudLords v2.0.0 Major Update.
 */
public class DiseaseManager {

    private final BudLords plugin;
    private final FarmingManager farmingManager;
    
    // Track infected plants: plantLocationKey -> Disease
    private final Map<String, PlantDisease> infectedPlants;
    
    // Track infection severity: plantLocationKey -> severity (0.0 to 1.0)
    private final Map<String, Double> infectionSeverity;
    
    // Disease check task
    private BukkitTask diseaseTask;
    private BukkitTask particleTask;

    // Configuration
    private static final long DISEASE_CHECK_INTERVAL_TICKS = 20L * 60 * 2; // Every 2 minutes
    private static final long PARTICLE_INTERVAL_TICKS = 40L; // Every 2 seconds

    public DiseaseManager(BudLords plugin, FarmingManager farmingManager) {
        this.plugin = plugin;
        this.farmingManager = farmingManager;
        this.infectedPlants = new ConcurrentHashMap<>();
        this.infectionSeverity = new ConcurrentHashMap<>();
        
        startDiseaseTasks();
        
        plugin.getLogger().info("✦ Disease System initialized with " + PlantDisease.values().length + " diseases");
    }

    private void startDiseaseTasks() {
        // Check for new infections and spread
        diseaseTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkForNewInfections();
            spreadDiseases();
            progressInfections();
        }, DISEASE_CHECK_INTERVAL_TICKS, DISEASE_CHECK_INTERVAL_TICKS);
        
        // Visual effects for infected plants
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, 
            this::spawnDiseaseParticles, PARTICLE_INTERVAL_TICKS, PARTICLE_INTERVAL_TICKS);
    }

    /**
     * Checks all plants for potential new infections.
     */
    private void checkForNewInfections() {
        for (Plant plant : farmingManager.getAllPlants()) {
            String key = plant.getLocationString();
            
            // Skip already infected plants
            if (infectedPlants.containsKey(key)) continue;
            
            // Check infection conditions
            PlantDisease disease = rollForInfection(plant);
            if (disease != null) {
                infectPlant(plant, disease);
            }
        }
    }

    /**
     * Rolls for a potential infection based on plant conditions.
     */
    private PlantDisease rollForInfection(Plant plant) {
        // Base infection chance modified by plant conditions
        double baseChance = 0.01; // 1% base chance per check
        
        // Water level affects fungal diseases
        if (plant.getWaterLevel() > 0.9) {
            baseChance *= 2.0; // Overwatering increases risk
        } else if (plant.getWaterLevel() < 0.2) {
            baseChance *= 1.5; // Underwatering also risky
        }
        
        // Nutrient level affects bacterial diseases
        if (plant.getNutrientLevel() > 0.95) {
            baseChance *= 1.8; // Over-fertilization
        }
        
        // Low quality plants are more susceptible
        if (plant.getQuality() < 50) {
            baseChance *= 1.5;
        }
        
        // Roll for each disease
        for (PlantDisease disease : PlantDisease.values()) {
            double effectiveChance = baseChance * disease.getInfectionChance() * 10;
            
            // Season affects certain diseases
            if (plugin.getSeasonManager() != null) {
                com.budlords.strain.SeedType.Season season = plugin.getSeasonManager().getCurrentSeason();
                effectiveChance *= getSeasonalDiseaseMod(disease, season);
            }
            
            if (ThreadLocalRandom.current().nextDouble() < effectiveChance) {
                return disease;
            }
        }
        
        return null;
    }

    /**
     * Gets seasonal modifier for disease infection chance.
     */
    private double getSeasonalDiseaseMod(PlantDisease disease, com.budlords.strain.SeedType.Season season) {
        return switch (disease.getCategory()) {
            case FUNGAL -> switch (season) {
                case SPRING -> 1.5;  // Wet spring = more fungus
                case SUMMER -> 0.7;
                case AUTUMN -> 1.2;
                case WINTER -> 0.8;
            };
            case PEST -> switch (season) {
                case SPRING -> 1.3;
                case SUMMER -> 1.8;  // Summer = more pests
                case AUTUMN -> 1.0;
                case WINTER -> 0.3;  // Winter kills pests
            };
            case ENVIRONMENTAL -> switch (season) {
                case SPRING -> 0.8;
                case SUMMER -> 1.5;  // Heat stress more common
                case AUTUMN -> 0.9;
                case WINTER -> 1.2;  // Cold stress
            };
            default -> 1.0;
        };
    }

    /**
     * Infects a plant with a disease.
     * Only notifies the plant owner if they're online and have the plant monitoring phone app.
     */
    public void infectPlant(Plant plant, PlantDisease disease) {
        String key = plant.getLocationString();
        infectedPlants.put(key, disease);
        infectionSeverity.put(key, 0.1); // Start with 10% severity
        
        // Notify ONLY the plant owner (no public chat broadcast)
        Player owner = Bukkit.getPlayer(plant.getOwnerUuid());
        if (owner != null && owner.isOnline()) {
            // Check if player has plant monitoring app (costs $20,000)
            // For now, we'll assume they have it if they're the owner
            // In future, add check for phone app purchase
            
            Location loc = plant.getLocation();
            
            // Send notification
            owner.sendMessage("");
            owner.sendMessage("§c§l⚠ Disease Alert!");
            owner.sendMessage("§7Your plant has been infected with " + disease.getColoredDisplay());
            owner.sendMessage("§7Location: §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            owner.sendMessage("§7Use a §e" + disease.getRecommendedCure().getDisplayName() + " §7to treat it!");
            owner.sendMessage("");
            
            // Play phone notification sound (like receiving a text/call)
            owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 2.0f);
            owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.5f);
        }
        
        // Update challenge progress
        if (plugin.getChallengeManager() != null && owner != null) {
            // Could track disease encounters for achievements
        }
    }

    /**
     * Spreads diseases to nearby plants.
     */
    private void spreadDiseases() {
        List<String> keysToInfect = new ArrayList<>();
        
        for (Map.Entry<String, PlantDisease> entry : infectedPlants.entrySet()) {
            String key = entry.getKey();
            PlantDisease disease = entry.getValue();
            
            // Get severity - higher severity = more spread
            double severity = infectionSeverity.getOrDefault(key, 0.0);
            
            if (ThreadLocalRandom.current().nextDouble() < disease.getSpreadChance() * severity) {
                // Try to spread to nearby plants
                Plant infectedPlant = farmingManager.getPlantAt(parseLocation(key));
                if (infectedPlant == null) continue;
                
                Location loc = infectedPlant.getLocation();
                
                // Check nearby plants (3 block radius)
                for (Plant nearbyPlant : farmingManager.getAllPlants()) {
                    if (nearbyPlant.getLocation().distance(loc) < 3 && 
                        nearbyPlant.getLocation().distance(loc) > 0) {
                        String nearbyKey = nearbyPlant.getLocationString();
                        if (!infectedPlants.containsKey(nearbyKey)) {
                            keysToInfect.add(nearbyKey + ":" + disease.name());
                        }
                    }
                }
            }
        }
        
        // Apply spread infections
        for (String spread : keysToInfect) {
            String[] parts = spread.split(":");
            if (parts.length >= 2) {
                String locationKey = parts[0];
                PlantDisease disease = PlantDisease.valueOf(parts[1]);
                Plant plant = farmingManager.getPlantAt(parseLocation(locationKey));
                if (plant != null) {
                    infectPlant(plant, disease);
                }
            }
        }
    }

    /**
     * Progresses infection severity over time.
     */
    private void progressInfections() {
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : infectionSeverity.entrySet()) {
            String key = entry.getKey();
            double severity = entry.getValue();
            
            // Increase severity
            severity += 0.05; // +5% per check
            
            if (severity >= 1.0) {
                // Plant may die at max severity
                PlantDisease disease = infectedPlants.get(key);
                if (disease != null && ThreadLocalRandom.current().nextDouble() > disease.getSeverity().getSurvivalRate()) {
                    // Plant dies from disease
                    Location loc = parseLocation(key);
                    if (loc != null) {
                        farmingManager.removePlant(loc);
                        loc.getBlock().setType(org.bukkit.Material.AIR);
                        
                        // Spawn death particles
                        loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0.5, 0.5, 0.5), 
                            30, 0.3, 0.3, 0.3, 0.05);
                        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DEATH, 0.5f, 0.5f);
                        
                        toRemove.add(key);
                        
                        // Notify ONLY the plant owner (no public broadcast)
                        Plant deadPlant = farmingManager.getPlantAt(loc);
                        if (deadPlant != null) {
                            Player plantOwner = Bukkit.getPlayer(deadPlant.getOwnerUuid());
                            if (plantOwner != null && plantOwner.isOnline()) {
                                plantOwner.sendMessage("§4§l☠ §cYour plant has died from " + disease.getDisplayName() + "!");
                                plantOwner.playSound(plantOwner.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f);
                            }
                        }
                    }
                }
            }
            
            infectionSeverity.put(key, Math.min(1.0, severity));
        }
        
        // Clean up dead plants
        for (String key : toRemove) {
            infectedPlants.remove(key);
            infectionSeverity.remove(key);
        }
    }

    /**
     * Spawns visual particles for infected plants.
     */
    private void spawnDiseaseParticles() {
        for (Map.Entry<String, PlantDisease> entry : infectedPlants.entrySet()) {
            Location loc = parseLocation(entry.getKey());
            if (loc == null || loc.getWorld() == null) continue;
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
            
            PlantDisease disease = entry.getValue();
            double severity = infectionSeverity.getOrDefault(entry.getKey(), 0.0);
            
            // Particle intensity based on severity
            int particleCount = (int) (3 + severity * 10);
            
            Location particleLoc = loc.clone().add(0.5, 0.5, 0.5);
            loc.getWorld().spawnParticle(disease.getParticle(), particleLoc, 
                particleCount, 0.2, 0.2, 0.2, 0.01);
        }
    }

    /**
     * Attempts to cure a plant's disease.
     * Universal cure works for all diseases with high effectiveness.
     */
    public boolean curePlant(Player player, Location location, PlantDisease.Cure cure) {
        String key = location.getWorld().getName() + "," + 
                    location.getBlockX() + "," + 
                    location.getBlockY() + "," + 
                    location.getBlockZ();
        
        PlantDisease disease = infectedPlants.get(key);
        if (disease == null) {
            player.sendMessage("§cThis plant is not infected!");
            return false;
        }
        
        // Universal cure works for all diseases with consistent effectiveness
        double effectiveness = cure.getEffectiveness();
        
        // Roll for cure success
        if (ThreadLocalRandom.current().nextDouble() < effectiveness) {
            // Cure successful!
            infectedPlants.remove(key);
            infectionSeverity.remove(key);
            
            player.sendMessage("");
            player.sendMessage("§a§l✓ Treatment Successful!");
            player.sendMessage("§7The " + disease.getColoredDisplay() + " §7has been cured!");
            player.sendMessage("");
            
            // Celebration particles
            location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, 
                location.clone().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            
            // Update stats
            if (plugin.getStatsManager() != null) {
                plugin.getStatsManager().getStats(player).incrementDiseasesCured();
            }
            
            return true;
        } else {
            // Cure failed
            player.sendMessage("§c§l✗ Treatment Failed!");
            player.sendMessage("§7The disease is too severe. Try again!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return false;
        }
    }

    /**
     * Checks if a plant is infected.
     */
    public boolean isInfected(Plant plant) {
        return infectedPlants.containsKey(plant.getLocationString());
    }

    /**
     * Gets the disease affecting a plant.
     */
    public PlantDisease getDisease(Plant plant) {
        return infectedPlants.get(plant.getLocationString());
    }

    /**
     * Gets the infection severity for a plant (0.0 to 1.0).
     */
    public double getSeverity(Plant plant) {
        return infectionSeverity.getOrDefault(plant.getLocationString(), 0.0);
    }

    /**
     * Gets the growth modifier from disease (negative = slower).
     */
    public double getDiseaseGrowthModifier(Plant plant) {
        PlantDisease disease = getDisease(plant);
        if (disease == null) return 1.0;
        
        double severity = getSeverity(plant);
        return 1.0 + (disease.getGrowthModifier() * severity);
    }

    /**
     * Gets the quality modifier from disease.
     */
    public double getDiseaseQualityModifier(Plant plant) {
        PlantDisease disease = getDisease(plant);
        if (disease == null) return 1.0;
        
        double severity = getSeverity(plant);
        return 1.0 + (disease.getQualityModifier() * severity);
    }

    /**
     * Gets the mutation modifier from disease.
     */
    public double getDiseaseMutationModifier(Plant plant) {
        PlantDisease disease = getDisease(plant);
        if (disease == null) return 0.0;
        
        double severity = getSeverity(plant);
        return disease.getMutationModifier() * severity;
    }

    /**
     * Parses a location string back to Location.
     */
    private Location parseLocation(String key) {
        String[] parts = key.split(",");
        if (parts.length != 4) return null;
        
        org.bukkit.World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets count of currently infected plants.
     */
    public int getInfectedCount() {
        return infectedPlants.size();
    }

    /**
     * Shuts down the disease manager.
     */
    public void shutdown() {
        if (diseaseTask != null) {
            diseaseTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
    }
}
