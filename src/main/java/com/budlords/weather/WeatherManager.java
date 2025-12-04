package com.budlords.weather;

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

import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages weather effects on plant growth in BudLords v2.0.0.
 * Weather conditions affect growth speed, quality, and water levels.
 */
public class WeatherManager {

    private final BudLords plugin;
    private final FarmingManager farmingManager;
    
    private BukkitTask weatherTask;
    private BukkitTask effectsTask;
    
    // Weather multipliers
    private double currentGrowthMultiplier = 1.0;
    private double currentQualityMultiplier = 1.0;
    private double currentWaterGainRate = 0.0;
    
    // Current weather state
    private WeatherType currentWeather = WeatherType.CLEAR;

    public WeatherManager(BudLords plugin, FarmingManager farmingManager) {
        this.plugin = plugin;
        this.farmingManager = farmingManager;
        
        startWeatherTasks();
        
        plugin.getLogger().info("✦ Weather System initialized");
    }

    private void startWeatherTasks() {
        // Check weather every minute
        weatherTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateWeatherState();
            applyWeatherEffects();
        }, 20L * 60, 20L * 60);
        
        // Visual effects every 5 seconds
        effectsTask = Bukkit.getScheduler().runTaskTimer(plugin, 
            this::spawnWeatherParticles, 100L, 100L);
    }

    /**
     * Updates the current weather state based on world weather.
     */
    private void updateWeatherState() {
        // Check primary world weather
        World primaryWorld = Bukkit.getWorlds().stream()
            .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
            .findFirst()
            .orElse(null);
        
        if (primaryWorld == null) return;
        
        WeatherType newWeather;
        
        if (primaryWorld.isThundering()) {
            newWeather = WeatherType.THUNDERSTORM;
        } else if (primaryWorld.hasStorm()) {
            newWeather = WeatherType.RAIN;
        } else {
            // Check time of day
            long time = primaryWorld.getTime();
            if (time >= 12000 && time < 24000) {
                newWeather = WeatherType.NIGHT;
            } else if (time >= 6000 && time < 12000) {
                // Random chance for sunny or cloudy
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    newWeather = WeatherType.CLOUDY;
                } else {
                    newWeather = WeatherType.SUNNY;
                }
            } else {
                newWeather = WeatherType.CLEAR;
            }
        }
        
        if (newWeather != currentWeather) {
            currentWeather = newWeather;
            updateMultipliers();
            broadcastWeatherChange();
        }
    }

    private void updateMultipliers() {
        currentGrowthMultiplier = currentWeather.getGrowthMultiplier();
        currentQualityMultiplier = currentWeather.getQualityMultiplier();
        currentWaterGainRate = currentWeather.getWaterGainRate();
    }

    private void broadcastWeatherChange() {
        if (!plugin.getConfig().getBoolean("weather.broadcast-changes", true)) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("");
            player.sendMessage("§7☁ Weather: " + currentWeather.getColoredDisplay());
            player.sendMessage("§7  Growth: " + formatMultiplier(currentGrowthMultiplier));
            player.sendMessage("§7  Quality: " + formatMultiplier(currentQualityMultiplier));
            if (currentWaterGainRate > 0) {
                player.sendMessage("§7  Plants are being watered!");
            }
            player.sendMessage("");
            
            player.playSound(player.getLocation(), currentWeather.getSound(), 0.5f, 1.0f);
        }
    }

    private String formatMultiplier(double mult) {
        if (mult >= 1.0) {
            return "§a+" + String.format("%.0f%%", (mult - 1.0) * 100);
        } else {
            return "§c" + String.format("%.0f%%", (mult - 1.0) * 100);
        }
    }

    /**
     * Applies weather effects to all plants.
     */
    private void applyWeatherEffects() {
        if (currentWaterGainRate > 0) {
            // Rain/thunderstorm waters plants
            for (Plant plant : farmingManager.getAllPlants()) {
                Location loc = plant.getLocation();
                
                // Check if plant is exposed to sky
                if (loc.getWorld() != null && loc.getWorld().getHighestBlockYAt(loc) <= loc.getBlockY() + 1) {
                    double currentWater = plant.getWaterLevel();
                    plant.setWaterLevel(Math.min(1.0, currentWater + currentWaterGainRate));
                }
            }
        }
    }

    /**
     * Spawns weather-appropriate particles near plants.
     */
    private void spawnWeatherParticles() {
        if (!plugin.getConfig().getBoolean("weather.particle-effects", true)) return;
        
        for (Plant plant : farmingManager.getAllPlants()) {
            Location loc = plant.getLocation();
            if (loc.getWorld() == null) continue;
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
            
            // Only show particles if exposed to sky
            if (loc.getWorld().getHighestBlockYAt(loc) > loc.getBlockY() + 1) continue;
            
            Location particleLoc = loc.clone().add(0.5, 0.8, 0.5);
            
            switch (currentWeather) {
                case RAIN -> {
                    loc.getWorld().spawnParticle(Particle.DRIP_WATER, particleLoc.add(0, 0.5, 0), 
                        3, 0.3, 0.1, 0.3, 0);
                }
                case THUNDERSTORM -> {
                    loc.getWorld().spawnParticle(Particle.DRIP_WATER, particleLoc.add(0, 0.5, 0), 
                        5, 0.4, 0.1, 0.4, 0);
                    if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                        loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, particleLoc, 
                            3, 0.2, 0.2, 0.2, 0.01);
                    }
                }
                case SUNNY -> {
                    if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                        loc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 
                            1, 0.2, 0.1, 0.2, 0.01);
                    }
                }
                case NIGHT -> {
                    if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                        loc.getWorld().spawnParticle(Particle.GLOW, particleLoc.add(0, 0.2, 0), 
                            1, 0.3, 0.1, 0.3, 0);
                    }
                }
                default -> {} // No special particles
            }
        }
    }

    /**
     * Gets the current weather type.
     */
    public WeatherType getCurrentWeather() {
        return currentWeather;
    }

    /**
     * Gets the current growth multiplier from weather.
     */
    public double getGrowthMultiplier() {
        return currentGrowthMultiplier;
    }

    /**
     * Gets the current quality multiplier from weather.
     */
    public double getQualityMultiplier() {
        return currentQualityMultiplier;
    }

    /**
     * Shuts down the weather manager.
     */
    public void shutdown() {
        if (weatherTask != null) {
            weatherTask.cancel();
        }
        if (effectsTask != null) {
            effectsTask.cancel();
        }
    }

    /**
     * Weather types and their effects.
     */
    public enum WeatherType {
        CLEAR("Clear", "§f☀", 1.0, 1.0, 0.0, Sound.BLOCK_GRASS_BREAK),
        SUNNY("Sunny", "§e☀", 1.15, 1.1, 0.0, Sound.ENTITY_BEE_LOOP),
        CLOUDY("Cloudy", "§7☁", 0.95, 1.0, 0.0, Sound.WEATHER_RAIN),
        RAIN("Rain", "§b🌧", 1.1, 1.05, 0.05, Sound.WEATHER_RAIN),
        THUNDERSTORM("Thunderstorm", "§9⛈", 1.2, 1.15, 0.08, Sound.ENTITY_LIGHTNING_BOLT_THUNDER),
        NIGHT("Night", "§8🌙", 0.9, 1.05, 0.0, Sound.AMBIENT_CAVE);

        private final String name;
        private final String symbol;
        private final double growthMultiplier;
        private final double qualityMultiplier;
        private final double waterGainRate;
        private final Sound sound;

        WeatherType(String name, String symbol, double growthMultiplier, 
                    double qualityMultiplier, double waterGainRate, Sound sound) {
            this.name = name;
            this.symbol = symbol;
            this.growthMultiplier = growthMultiplier;
            this.qualityMultiplier = qualityMultiplier;
            this.waterGainRate = waterGainRate;
            this.sound = sound;
        }

        public String getName() {
            return name;
        }

        public String getSymbol() {
            return symbol;
        }

        public double getGrowthMultiplier() {
            return growthMultiplier;
        }

        public double getQualityMultiplier() {
            return qualityMultiplier;
        }

        public double getWaterGainRate() {
            return waterGainRate;
        }

        public Sound getSound() {
            return sound;
        }

        public String getColoredDisplay() {
            String color = switch (this) {
                case SUNNY -> "§e";
                case RAIN -> "§b";
                case THUNDERSTORM -> "§9";
                case NIGHT -> "§8";
                case CLOUDY -> "§7";
                default -> "§f";
            };
            return color + symbol + " " + name;
        }
    }
}
