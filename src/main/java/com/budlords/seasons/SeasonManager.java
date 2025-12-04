package com.budlords.seasons;

import com.budlords.BudLords;
import com.budlords.strain.SeedType.Season;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the seasonal system in BudLords v2.0.0.
 * Seasons affect plant growth, quality, and provide special bonuses.
 */
public class SeasonManager {

    private final BudLords plugin;
    private Season currentSeason;
    private long seasonStartTime;
    private long seasonDurationMs;
    private BukkitTask seasonTask;
    private BukkitTask ambientTask;

    // Default: 7 real-world days per season
    private static final long DEFAULT_SEASON_DURATION = 7L * 24 * 60 * 60 * 1000;
    
    // Season-specific ambient particle rates
    private static final int AMBIENT_PARTICLE_INTERVAL_TICKS = 100; // 5 seconds

    public SeasonManager(BudLords plugin) {
        this.plugin = plugin;
        this.seasonDurationMs = plugin.getConfig().getLong("seasons.duration-hours", 168) * 60 * 60 * 1000;
        
        // Initialize season based on config or determine from time
        String savedSeason = plugin.getConfig().getString("seasons.current", null);
        if (savedSeason != null) {
            try {
                this.currentSeason = Season.valueOf(savedSeason.toUpperCase());
            } catch (IllegalArgumentException e) {
                this.currentSeason = determineSeasonFromTime();
            }
        } else {
            this.currentSeason = determineSeasonFromTime();
        }
        
        this.seasonStartTime = plugin.getConfig().getLong("seasons.start-time", System.currentTimeMillis());
        
        startSeasonTasks();
        
        plugin.getLogger().info("✦ Season System initialized - Current season: " + currentSeason.getDisplayName());
    }

    /**
     * Determines season based on real-world time or Minecraft time.
     */
    private Season determineSeasonFromTime() {
        // Use day of year to determine season
        java.time.LocalDate now = java.time.LocalDate.now();
        int dayOfYear = now.getDayOfYear();
        
        if (dayOfYear >= 80 && dayOfYear < 172) {
            return Season.SPRING;
        } else if (dayOfYear >= 172 && dayOfYear < 266) {
            return Season.SUMMER;
        } else if (dayOfYear >= 266 && dayOfYear < 356) {
            return Season.AUTUMN;
        } else {
            return Season.WINTER;
        }
    }

    private void startSeasonTasks() {
        // Check for season change every hour
        seasonTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = System.currentTimeMillis() - seasonStartTime;
            
            if (elapsed >= seasonDurationMs) {
                advanceSeason();
            }
        }, 20L * 60 * 60, 20L * 60 * 60); // Every hour
        
        // Ambient particle effects based on season
        ambientTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnSeasonalAmbientEffects, 
            AMBIENT_PARTICLE_INTERVAL_TICKS, AMBIENT_PARTICLE_INTERVAL_TICKS);
    }

    /**
     * Advances to the next season.
     */
    private void advanceSeason() {
        Season previousSeason = currentSeason;
        currentSeason = currentSeason.next();
        seasonStartTime = System.currentTimeMillis();
        
        // Broadcast season change
        broadcastSeasonChange(previousSeason, currentSeason);
        
        // Save new season
        plugin.getConfig().set("seasons.current", currentSeason.name());
        plugin.getConfig().set("seasons.start-time", seasonStartTime);
        plugin.saveConfig();
        
        plugin.getLogger().info("Season changed from " + previousSeason.name() + " to " + currentSeason.name());
    }

    private void broadcastSeasonChange(Season from, Season to) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("");
            player.sendMessage("§8§l═══════════════════════════════════════════");
            player.sendMessage("§f§l   ✦ SEASON CHANGE ✦");
            player.sendMessage("");
            player.sendMessage("   " + from.getDisplayName() + " §7→ " + to.getDisplayName());
            player.sendMessage("");
            player.sendMessage("§7   Growth Speed: " + formatMultiplier(to.getGrowthMultiplier()));
            player.sendMessage("§7   Quality Bonus: " + formatMultiplier(to.getQualityMultiplier()));
            player.sendMessage("§7   Potency Bonus: " + formatMultiplier(to.getPotencyMultiplier()));
            player.sendMessage("§8§l═══════════════════════════════════════════");
            player.sendMessage("");
            
            // Play season-appropriate sound
            Sound sound = switch (to) {
                case SPRING -> Sound.ENTITY_BEE_POLLINATE;
                case SUMMER -> Sound.BLOCK_FIRE_AMBIENT;
                case AUTUMN -> Sound.BLOCK_AZALEA_LEAVES_FALL;
                case WINTER -> Sound.BLOCK_POWDER_SNOW_STEP;
            };
            player.playSound(player.getLocation(), sound, 0.8f, 1.0f);
            
            // Spawn season particles around player
            spawnSeasonChangeParticles(player, to);
        }
    }

    private String formatMultiplier(double mult) {
        if (mult >= 1.0) {
            return "§a+" + String.format("%.0f%%", (mult - 1.0) * 100);
        } else {
            return "§c" + String.format("%.0f%%", (mult - 1.0) * 100);
        }
    }

    private void spawnSeasonChangeParticles(Player player, Season season) {
        World world = player.getWorld();
        org.bukkit.Location loc = player.getLocation().add(0, 1, 0);
        
        switch (season) {
            case SPRING -> {
                world.spawnParticle(Particle.COMPOSTER, loc, 50, 3, 2, 3, 0.05);
                world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 30, 2, 1.5, 2, 0.02);
            }
            case SUMMER -> {
                world.spawnParticle(Particle.FLAME, loc, 40, 2, 1.5, 2, 0.02);
                world.spawnParticle(Particle.END_ROD, loc, 20, 2, 1.5, 2, 0.05);
            }
            case AUTUMN -> {
                world.spawnParticle(Particle.FALLING_HONEY, loc, 50, 3, 2, 3, 0);
                world.spawnParticle(Particle.COMPOSTER, loc, 30, 2, 1.5, 2, 0.01);
            }
            case WINTER -> {
                world.spawnParticle(Particle.SNOWFLAKE, loc, 60, 3, 2, 3, 0.02);
                world.spawnParticle(Particle.END_ROD, loc, 15, 2, 1.5, 2, 0.01);
            }
        }
    }

    private void spawnSeasonalAmbientEffects() {
        if (!plugin.getConfig().getBoolean("seasons.ambient-effects", true)) {
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (ThreadLocalRandom.current().nextDouble() > 0.3) continue; // 30% chance per player
            
            World world = player.getWorld();
            org.bukkit.Location loc = player.getLocation();
            
            // Random offset around player
            double offsetX = ThreadLocalRandom.current().nextDouble(-10, 10);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-10, 10);
            loc = loc.add(offsetX, ThreadLocalRandom.current().nextDouble(0, 3), offsetZ);
            
            switch (currentSeason) {
                case SPRING -> {
                    // Pollen and flower particles
                    world.spawnParticle(Particle.COMPOSTER, loc, 3, 1, 0.5, 1, 0.01);
                }
                case SUMMER -> {
                    // Heat shimmer and occasional firefly
                    if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                        world.spawnParticle(Particle.GLOW, loc, 1, 0.1, 0.1, 0.1, 0);
                    }
                }
                case AUTUMN -> {
                    // Falling leaves effect
                    world.spawnParticle(Particle.FALLING_HONEY, loc.add(0, 5, 0), 2, 2, 0, 2, 0);
                }
                case WINTER -> {
                    // Snowfall
                    world.spawnParticle(Particle.SNOWFLAKE, loc.add(0, 5, 0), 5, 3, 0, 3, 0.01);
                }
            }
        }
    }

    /**
     * Gets the current season.
     */
    public Season getCurrentSeason() {
        return currentSeason;
    }

    /**
     * Gets time remaining in current season.
     */
    public long getTimeRemainingMs() {
        long elapsed = System.currentTimeMillis() - seasonStartTime;
        return Math.max(0, seasonDurationMs - elapsed);
    }

    /**
     * Gets time remaining formatted as string.
     */
    public String getTimeRemainingFormatted() {
        long remaining = getTimeRemainingMs();
        long hours = remaining / (1000 * 60 * 60);
        long days = hours / 24;
        hours = hours % 24;
        
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        return hours + "h";
    }

    /**
     * Gets the growth speed multiplier for current season.
     */
    public double getGrowthMultiplier() {
        return currentSeason.getGrowthMultiplier();
    }

    /**
     * Gets the quality multiplier for current season.
     */
    public double getQualityMultiplier() {
        return currentSeason.getQualityMultiplier();
    }

    /**
     * Gets the potency multiplier for current season.
     */
    public double getPotencyMultiplier() {
        return currentSeason.getPotencyMultiplier();
    }

    /**
     * Forces a season change (admin command).
     */
    public void forceSeason(Season season) {
        Season previous = currentSeason;
        currentSeason = season;
        seasonStartTime = System.currentTimeMillis();
        
        broadcastSeasonChange(previous, season);
        
        plugin.getConfig().set("seasons.current", season.name());
        plugin.getConfig().set("seasons.start-time", seasonStartTime);
        plugin.saveConfig();
    }

    /**
     * Shuts down the season manager.
     */
    public void shutdown() {
        if (seasonTask != null) {
            seasonTask.cancel();
        }
        if (ambientTask != null) {
            ambientTask.cancel();
        }
        
        // Save current state
        plugin.getConfig().set("seasons.current", currentSeason.name());
        plugin.getConfig().set("seasons.start-time", seasonStartTime);
        plugin.saveConfig();
    }
}
