package com.budlords.events;

import com.budlords.BudLords;
import com.budlords.farming.FarmingManager;
import com.budlords.farming.Plant;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages random events that affect gameplay.
 * Events add excitement and challenge to the farming experience.
 */
public class RandomEventManager {

    private final BudLords plugin;
    private final FarmingManager farmingManager;
    
    private BukkitTask eventCheckTask;
    private RandomEvent activeEvent;
    private long eventEndTime;
    
    // Event probabilities (per hour)
    private static final double DROUGHT_CHANCE = 0.05;
    private static final double BLIGHT_CHANCE = 0.03;
    private static final double GOLDEN_HOUR_CHANCE = 0.08;
    private static final double NUTRIENT_RAIN_CHANCE = 0.06;
    private static final double POLICE_RAID_CHANCE = 0.02;
    private static final double MARKET_BOOM_CHANCE = 0.04;

    public RandomEventManager(BudLords plugin, FarmingManager farmingManager) {
        this.plugin = plugin;
        this.farmingManager = farmingManager;
        startEventCheck();
    }

    private void startEventCheck() {
        // Check for random events every 5 minutes
        eventCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Check if there's an active event
            if (activeEvent != null) {
                if (System.currentTimeMillis() > eventEndTime) {
                    endEvent();
                }
                return;
            }
            
            // Roll for random event (scaled to 5-minute checks, so 12 checks per hour)
            double roll = ThreadLocalRandom.current().nextDouble();
            double accumulatedChance = 0;
            
            // Check each event type
            if ((accumulatedChance += DROUGHT_CHANCE / 12) > roll) {
                startEvent(RandomEvent.DROUGHT);
            } else if ((accumulatedChance += BLIGHT_CHANCE / 12) > roll) {
                startEvent(RandomEvent.BLIGHT);
            } else if ((accumulatedChance += GOLDEN_HOUR_CHANCE / 12) > roll) {
                startEvent(RandomEvent.GOLDEN_HOUR);
            } else if ((accumulatedChance += NUTRIENT_RAIN_CHANCE / 12) > roll) {
                startEvent(RandomEvent.NUTRIENT_RAIN);
            } else if ((accumulatedChance += POLICE_RAID_CHANCE / 12) > roll) {
                startEvent(RandomEvent.POLICE_RAID_WARNING);
            } else if ((accumulatedChance += MARKET_BOOM_CHANCE / 12) > roll) {
                startEvent(RandomEvent.MARKET_BOOM);
            }
        }, 20L * 60 * 5, 20L * 60 * 5); // Every 5 minutes
    }

    public void shutdown() {
        if (eventCheckTask != null) {
            eventCheckTask.cancel();
        }
        if (activeEvent != null) {
            endEvent();
        }
    }

    private void startEvent(RandomEvent event) {
        this.activeEvent = event;
        this.eventEndTime = System.currentTimeMillis() + event.getDurationMinutes() * 60 * 1000L;
        
        // Announce event
        broadcastEvent(event, true);
        
        // Apply event effects
        switch (event) {
            case DROUGHT -> applyDroughtEffect();
            case BLIGHT -> applyBlightEffect();
            case GOLDEN_HOUR -> applyGoldenHourEffect();
            case NUTRIENT_RAIN -> applyNutrientRainEffect();
            case POLICE_RAID_WARNING -> applyPoliceRaidWarning();
            case MARKET_BOOM -> {}  // Effect applied during trading
        }
    }

    private void endEvent() {
        if (activeEvent != null) {
            broadcastEvent(activeEvent, false);
            activeEvent = null;
            eventEndTime = 0;
        }
    }

    private void broadcastEvent(RandomEvent event, boolean starting) {
        String prefix = starting ? "§c§l⚠ " : "§a§l✓ ";
        String action = starting ? " has begun!" : " has ended.";
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("");
            player.sendMessage("§8§l═══════════════════════════════════");
            player.sendMessage(prefix + event.getColor() + event.getName() + "§r" + action);
            if (starting) {
                player.sendMessage("§7" + event.getDescription());
                player.sendMessage("§7Duration: §e" + event.getDurationMinutes() + " minutes");
            }
            player.sendMessage("§8§l═══════════════════════════════════");
            player.sendMessage("");
            
            if (starting) {
                player.playSound(player.getLocation(), event.getStartSound(), 0.8f, event.getSoundPitch());
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
            }
        }
    }

    // ===== EVENT EFFECTS =====

    private void applyDroughtEffect() {
        // Drain water from all plants faster
        for (Plant plant : farmingManager.getAllPlants()) {
            double currentWater = plant.getWaterLevel();
            plant.setWaterLevel(currentWater * 0.5); // Halve water levels
        }
        
        // Schedule ongoing drought effects
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (activeEvent != RandomEvent.DROUGHT) {
                task.cancel();
                return;
            }
            
            for (Plant plant : farmingManager.getAllPlants()) {
                plant.setWaterLevel(plant.getWaterLevel() - 0.05); // Extra water drain
                
                // Spawn dry particles
                Location loc = plant.getLocation();
                if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0.5, 0.3, 0.5), 
                        3, 0.2, 0.1, 0.2, 0.01);
                }
            }
        }, 0L, 20L * 60); // Every minute
    }

    private void applyBlightEffect() {
        // Random chance for plants to lose quality
        for (Plant plant : farmingManager.getAllPlants()) {
            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                plant.addQuality(-10);
                
                // Spawn sick particles
                Location loc = plant.getLocation();
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.SLIME, loc.clone().add(0.5, 0.5, 0.5), 
                        5, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }
    }

    private void applyGoldenHourEffect() {
        // Boost all plants quality
        for (Plant plant : farmingManager.getAllPlants()) {
            plant.addQuality(15);
            
            // Spawn golden particles
            Location loc = plant.getLocation();
            if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0.5, 0.8, 0.5), 
                    8, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    private void applyNutrientRainEffect() {
        // Boost all plants nutrient levels
        for (Plant plant : farmingManager.getAllPlants()) {
            plant.setNutrientLevel(Math.min(1.0, plant.getNutrientLevel() + 0.3));
            plant.setWaterLevel(Math.min(1.0, plant.getWaterLevel() + 0.2));
            
            // Spawn rain particles
            Location loc = plant.getLocation();
            if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                loc.getWorld().spawnParticle(Particle.DRIP_WATER, loc.clone().add(0.5, 1.0, 0.5), 
                    10, 0.3, 0.5, 0.3, 0);
                loc.getWorld().spawnParticle(Particle.COMPOSTER, loc.clone().add(0.5, 0.5, 0.5), 
                    5, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    private void applyPoliceRaidWarning() {
        // Just a warning - no immediate effect, but trading is riskier
        // Schedule police activity particles in trading areas
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (activeEvent != RandomEvent.POLICE_RAID_WARNING) {
                task.cancel();
                return;
            }
            
            // Spawn warning particles near online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.3f, 0.5f);
                }
            }
        }, 0L, 20L * 30); // Every 30 seconds
    }

    // ===== PUBLIC METHODS =====

    /**
     * Gets the currently active event, or null if none.
     */
    public RandomEvent getActiveEvent() {
        return activeEvent;
    }

    /**
     * Checks if a specific event is currently active.
     */
    public boolean isEventActive(RandomEvent event) {
        return activeEvent == event;
    }

    /**
     * Gets the trade success modifier from current events.
     */
    public double getTradeSuccessModifier() {
        if (activeEvent == RandomEvent.POLICE_RAID_WARNING) {
            return -0.20; // -20% success during raids
        }
        return 0;
    }

    /**
     * Gets the trade price modifier from current events.
     */
    public double getTradePriceModifier() {
        if (activeEvent == RandomEvent.MARKET_BOOM) {
            return 0.50; // +50% prices during boom
        }
        return 0;
    }

    /**
     * Gets the growth speed modifier from current events.
     */
    public double getGrowthSpeedModifier() {
        if (activeEvent == RandomEvent.GOLDEN_HOUR) {
            return 0.30; // +30% growth during golden hour
        }
        if (activeEvent == RandomEvent.DROUGHT) {
            return -0.20; // -20% growth during drought
        }
        return 0;
    }

    /**
     * Gets time remaining for current event in minutes.
     */
    public int getEventTimeRemaining() {
        if (activeEvent == null) return 0;
        long remaining = eventEndTime - System.currentTimeMillis();
        return Math.max(0, (int) (remaining / 60000));
    }

    /**
     * Enum of possible random events.
     */
    public enum RandomEvent {
        DROUGHT("Drought", "§c", 
            "Water drains faster from all plants! Water your crops frequently.", 
            15, Sound.ENTITY_BLAZE_AMBIENT, 0.5f),
        
        BLIGHT("Crop Blight", "§8", 
            "A mysterious blight affects some plants, reducing quality.", 
            10, Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 0.8f),
        
        GOLDEN_HOUR("Golden Hour", "§6", 
            "Perfect growing conditions! All plants gain bonus quality.", 
            20, Sound.BLOCK_BEACON_POWER_SELECT, 1.2f),
        
        NUTRIENT_RAIN("Nutrient Rain", "§a", 
            "Special rain boosts water and nutrient levels for all plants!", 
            15, Sound.WEATHER_RAIN_ABOVE, 1.0f),
        
        POLICE_RAID_WARNING("Police Activity", "§4", 
            "Increased police presence! Trading is riskier during this time.", 
            30, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f),
        
        MARKET_BOOM("Market Boom", "§e", 
            "High demand! All products sell for 50% more during this event.", 
            25, Sound.ENTITY_VILLAGER_CELEBRATE, 1.2f);

        private final String name;
        private final String color;
        private final String description;
        private final int durationMinutes;
        private final Sound startSound;
        private final float soundPitch;

        RandomEvent(String name, String color, String description, 
                    int durationMinutes, Sound startSound, float soundPitch) {
            this.name = name;
            this.color = color;
            this.description = description;
            this.durationMinutes = durationMinutes;
            this.startSound = startSound;
            this.soundPitch = soundPitch;
        }

        public String getName() {
            return name;
        }

        public String getColor() {
            return color;
        }

        public String getDescription() {
            return description;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public Sound getStartSound() {
            return startSound;
        }

        public float getSoundPitch() {
            return soundPitch;
        }
    }
}
