package com.budlords.effects;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages all strain effects, including applying effects to players,
 * tracking active effects, and spawning visual particles.
 */
public class StrainEffectsManager implements Listener {
    
    private final BudLords plugin;
    private final Map<UUID, ActiveEffectSession> activeSessions;
    private final Map<UUID, BukkitTask> particleTasks;
    private final Map<UUID, Long> lastMovementTime;
    
    public StrainEffectsManager(BudLords plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.particleTasks = new ConcurrentHashMap<>();
        this.lastMovementTime = new ConcurrentHashMap<>();
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Applies all effects from a strain to a player.
     */
    public void applyStrainEffects(Player player, Strain strain, StarRating quality, int duration) {
        List<StrainEffect> effects = strain.getEffects();
        if (effects == null || effects.isEmpty()) {
            return;
        }
        
        // Create session
        ActiveEffectSession session = new ActiveEffectSession(player.getUniqueId(), strain, quality, duration);
        activeSessions.put(player.getUniqueId(), session);
        
        // Apply each effect
        for (StrainEffect effect : effects) {
            // Check trigger chance
            if (ThreadLocalRandom.current().nextDouble() > effect.getChance()) {
                continue;
            }
            
            applyEffect(player, effect, quality, duration);
            session.addActiveEffect(effect);
        }
        
        // Start visual particle task
        startParticleTask(player, session);
        
        // Schedule cleanup
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupSession(player);
            }
        }.runTaskLater(plugin, duration);
    }
    
    /**
     * Applies a single effect to a player.
     */
    public void applyEffect(Player player, StrainEffect effect, StarRating quality, int baseDuration) {
        StrainEffectType type = effect.getType();
        int intensity = effect.getIntensity();
        int qualityBonus = quality != null ? quality.getStars() : 1;
        
        // Calculate modified duration
        int duration = (int) (baseDuration * effect.getDurationMultiplier() * (0.8 + qualityBonus * 0.1));
        int amplifier = effect.getPotionAmplifier();
        
        // Send activation message
        player.sendMessage(type.getCategoryColor() + type.getSymbol() + " " + type.getActivationMessage());
        
        // Apply potion effects based on effect type
        switch (type) {
            // MOVEMENT EFFECTS
            case BUNNY_HOP -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, amplifier + 2, false, false, true));
            }
            case SPEED_DEMON -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier + 1, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, duration / 2, 0, false, false, true));
            }
            case SLOW_MO -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, amplifier, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false, true));
            }
            case MOON_GRAVITY -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, amplifier + 1, false, false, true));
            }
            case DOLPHIN_SWIM -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, duration, amplifier, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration, 0, false, false, true));
            }
            
            // PERCEPTION EFFECTS
            case THIRD_EYE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration / 2, 0, false, false, true));
            }
            case MATRIX_VISION -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration / 2, 0, false, false, true));
            }
            case DRUNK_VISION -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration / 2, 0, false, false, true));
            }
            case EAGLE_SIGHT -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
            }
            case THERMAL_VISION -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
                // Make nearby entities glow
                makeNearbyEntitiesGlow(player, duration);
            }
            
            // GAMEPLAY EFFECTS
            case LUCKY_CHARM -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration, amplifier, false, false, true));
            }
            case MIDAS_TOUCH -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration, amplifier + 1, false, false, true));
            }
            case GREEN_THUMB -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration / 2, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration, 0, false, false, true));
            }
            case IRON_LUNGS -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration * 2, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false, true));
            }
            case COTTON_MOUTH, MUNCHIES -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration / 2, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration / 3, 0, false, false, true));
            }
            
            // COMBAT EFFECTS
            case BERSERKER -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, amplifier + 1, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, false, false, true));
                // No resistance for berserker - high damage output but vulnerable
            }
            case TANK_MODE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, amplifier, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, duration, amplifier, false, false, true));
            }
            case NINJA_MODE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
            }
            case VAMPIRE -> {
                // Handled via damage listener
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 0, false, false, true));
            }
            case THORNS -> {
                // Handled via damage listener
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, 0, false, false, true));
            }
            
            // MYSTICAL EFFECTS
            case ASTRAL_PROJECTION -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 0, false, false, true)); // Brief float
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false, true));
            }
            case TIME_WARP -> {
                // Random speed changes
                scheduleTimeWarpEffects(player, duration);
            }
            case DREAM_STATE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration / 2, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 0, false, false, true));
            }
            case MEDITATION -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, duration / 2, 0, false, false, true));
            }
            case ENLIGHTENMENT -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, amplifier + 2, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0, false, false, true));
            }
            
            // LEGENDARY EFFECTS
            case PHOENIX_REBIRTH -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier + 1, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, amplifier + 3, false, false, true));
            }
            case DRAGON_BREATH -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, amplifier + 1, false, false, true));
            }
            case VOID_WALKER -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration / 2, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false, true));
            }
            case CELESTIAL_BEING -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, amplifier + 2, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false, true));
            }
            case REALITY_BENDER -> {
                scheduleRealityBenderEffects(player, duration);
            }
            
            // FROST AURA
            case FROST_AURA -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration / 3, 0, false, false, true));
            }
            
            // WATER WALK EFFECT
            case WATER_WALK -> {
                // Start water walking task
                scheduleWaterWalkEffect(player, duration);
            }
            
            // PLANT GROWTH EFFECT
            case PLANT_GROWTH -> {
                // Boost plant growth nearby (handled via listener)
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration, amplifier, false, false, true));
                schedulePlantGrowthBoost(player, duration);
            }
            
            // FEATHER FALL
            case FEATHER_FALL -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false, true));
            }
            
            // Other visual effects don't need potion effects, just particles
            default -> {
                // Visual-only effects are handled by the particle task
            }
        }
        
        // Play activation sound
        playActivationSound(player, type);
    }
    
    private void makeNearbyEntitiesGlow(Player player, int duration) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
                    if (entity instanceof LivingEntity living) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false, true));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void scheduleTimeWarpEffects(Player player, int totalDuration) {
        new BukkitRunnable() {
            int elapsed = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || elapsed >= totalDuration) {
                    cancel();
                    return;
                }
                
                // Randomly apply speed or slowness
                if (ThreadLocalRandom.current().nextBoolean()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2, false, false, true));
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 0, false, false, true));
                }
                
                elapsed += 80;
            }
        }.runTaskTimer(plugin, 0L, 80L);
    }
    
    private void scheduleRealityBenderEffects(Player player, int totalDuration) {
        new BukkitRunnable() {
            int elapsed = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || elapsed >= totalDuration) {
                    cancel();
                    return;
                }
                
                // Random teleport within small radius
                if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                    Location loc = player.getLocation();
                    double offsetX = ThreadLocalRandom.current().nextDouble(-3, 3);
                    double offsetZ = ThreadLocalRandom.current().nextDouble(-3, 3);
                    Location newLoc = loc.clone().add(offsetX, 0, offsetZ);
                    
                    // Check if safe
                    if (newLoc.getBlock().isPassable()) {
                        player.teleport(newLoc);
                        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 30, 0.5, 1, 0.5, 0.1);
                        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, newLoc, 30, 0.5, 1, 0.5, 0.1);
                        player.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
                    }
                }
                
                // Random visual effects
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20, 0, false, false, true));
                
                elapsed += 40;
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }
    
    /**
     * Schedules the water walk effect - allows player to walk on water.
     */
    private void scheduleWaterWalkEffect(Player player, int totalDuration) {
        new BukkitRunnable() {
            int elapsed = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || elapsed >= totalDuration) {
                    cancel();
                    return;
                }
                
                Location playerLoc = player.getLocation();
                Location below = playerLoc.clone().subtract(0, 0.1, 0);
                
                // Check if player is standing on or in water
                boolean onWater = below.getBlock().getType() == Material.WATER || 
                    below.getBlock().getType() == Material.WATER_CAULDRON;
                boolean inWater = playerLoc.getBlock().getType() == Material.WATER;
                
                if (onWater || inWater) {
                    // Visual effect - frost particles on water
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, 
                        playerLoc.clone().add(0, 0.1, 0), 3, 0.3, 0.05, 0.3, 0);
                    
                    // Only apply velocity boost if player is sinking into water
                    // and not already moving upward
                    if (inWater && player.getVelocity().getY() < 0.1) {
                        org.bukkit.util.Vector currentVel = player.getVelocity();
                        // Gentle upward push to keep them above water, preserving horizontal movement
                        player.setVelocity(currentVel.setY(Math.max(currentVel.getY(), 0.08)));
                    }
                }
                
                elapsed += 10; // Check every 10 ticks (half a second)
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    /**
     * Schedules plant growth boost for nearby plants.
     */
    private void schedulePlantGrowthBoost(Player player, int totalDuration) {
        new BukkitRunnable() {
            int elapsed = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || elapsed >= totalDuration) {
                    cancel();
                    return;
                }
                
                Location center = player.getLocation();
                int radius = 3; // Reduced radius for better performance
                int blocksProcessed = 0;
                int maxBlocksPerTick = 5; // Limit blocks processed per check
                
                // Boost nearby crops with limited processing
                outerLoop:
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -1; y <= 1; y++) { // Reduced Y range
                        for (int z = -radius; z <= radius; z++) {
                            if (blocksProcessed >= maxBlocksPerTick) break outerLoop;
                            
                            Location blockLoc = center.clone().add(x, y, z);
                            org.bukkit.block.Block block = blockLoc.getBlock();
                            
                            // Check if it's a crop
                            if (block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
                                if (ageable.getAge() < ageable.getMaximumAge()) {
                                    // 5% chance to grow one stage per check (reduced from 10%)
                                    if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                                        ageable.setAge(Math.min(ageable.getMaximumAge(), ageable.getAge() + 1));
                                        block.setBlockData(ageable);
                                        blocksProcessed++;
                                        
                                        // Particles
                                        player.getWorld().spawnParticle(Particle.COMPOSTER, 
                                            blockLoc.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0);
                                    }
                                }
                            }
                        }
                    }
                }
                
                elapsed += 60; // Check every 3 seconds (increased from 2)
            }
        }.runTaskTimer(plugin, 0L, 60L); // Increased interval
    }
    
    private void playActivationSound(Player player, StrainEffectType type) {
        Sound sound = switch (type.getCategory()) {
            case LEGENDARY -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case MYSTICAL -> Sound.BLOCK_BEACON_POWER_SELECT;
            case TRANSFORMATION -> Sound.ENTITY_ENDER_DRAGON_GROWL;
            case COMBAT -> Sound.ENTITY_WARDEN_SONIC_BOOM;
            case MOVEMENT -> Sound.ENTITY_PLAYER_LEVELUP;
            default -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
        
        float pitch = type.isLegendary() ? 0.8f : 1.2f;
        player.playSound(player.getLocation(), sound, 0.5f, pitch);
    }
    
    /**
     * Starts the visual particle task for a player.
     */
    private void startParticleTask(Player player, ActiveEffectSession session) {
        // Cancel existing task
        BukkitTask existingTask = particleTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeSessions.containsKey(player.getUniqueId())) {
                    cancel();
                    particleTasks.remove(player.getUniqueId());
                    return;
                }
                
                // Spawn particles for each active effect
                for (StrainEffect effect : session.getActiveEffects()) {
                    spawnEffectParticles(player, effect);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L); // Every 4 ticks
        
        particleTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Spawns visual particles for an effect.
     */
    private void spawnEffectParticles(Player player, StrainEffect effect) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        StrainEffectType type = effect.getType();
        int intensity = effect.getIntensity();
        int particleCount = (int) (5 * effect.getParticleMultiplier());
        
        switch (type) {
            case GHOST_RIDER -> {
                // Fire around head
                Location headLoc = loc.clone().add(0, 1.8, 0);
                world.spawnParticle(Particle.FLAME, headLoc, 8 + intensity * 2, 0.25, 0.3, 0.25, 0.03);
                world.spawnParticle(Particle.LAVA, headLoc, 1, 0.2, 0.2, 0.2, 0);
                
                // Occasional soul fire for intensity 4+
                if (intensity >= 4 && ThreadLocalRandom.current().nextDouble() < 0.3) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, headLoc, 4, 0.2, 0.2, 0.2, 0.02);
                }
            }
            
            case RAINBOW_AURA -> {
                // Cycling colored particles around player
                for (int i = 0; i < particleCount; i++) {
                    double angle = (System.currentTimeMillis() / 100.0 + i * 0.5) % (2 * Math.PI);
                    double x = Math.cos(angle) * 0.8;
                    double z = Math.sin(angle) * 0.8;
                    Location partLoc = loc.clone().add(x, 1 + Math.sin(angle * 2) * 0.3, z);
                    
                    // RGB based on angle
                    world.spawnParticle(Particle.SPELL_MOB, partLoc, 0, 
                        (float) ((Math.sin(angle) + 1) / 2),
                        (float) ((Math.sin(angle + 2.09) + 1) / 2),
                        (float) ((Math.sin(angle + 4.18) + 1) / 2), 
                        1);
                }
            }
            
            case SHADOW_WALKER -> {
                // Dark smoke trail
                world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.5, 0), 
                    3 + intensity, 0.3, 0.5, 0.3, 0.01);
                world.spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 0.3, 0), 
                    1, 0.2, 0.1, 0.2, 0.01);
            }
            
            case ANGEL_WINGS -> {
                // White particles forming wing shapes
                for (double yaw = -30; yaw <= 30; yaw += 15) {
                    double radYaw = Math.toRadians(loc.getYaw() + 90 + yaw);
                    double x = Math.cos(radYaw) * 0.8;
                    double z = Math.sin(radYaw) * 0.8;
                    world.spawnParticle(Particle.END_ROD, loc.clone().add(x, 1.2, z), 
                        1, 0.05, 0.1, 0.05, 0);
                    
                    radYaw = Math.toRadians(loc.getYaw() + 270 + yaw);
                    x = Math.cos(radYaw) * 0.8;
                    z = Math.sin(radYaw) * 0.8;
                    world.spawnParticle(Particle.END_ROD, loc.clone().add(x, 1.2, z), 
                        1, 0.05, 0.1, 0.05, 0);
                }
            }
            
            case DEMON_HORNS -> {
                // Red flame horns on head
                Location head = loc.clone().add(0, 2, 0);
                double yawRad = Math.toRadians(loc.getYaw());
                
                // Left horn
                double lx = Math.cos(yawRad + Math.PI/6) * 0.15;
                double lz = Math.sin(yawRad + Math.PI/6) * 0.15;
                world.spawnParticle(Particle.FLAME, head.clone().add(lx, 0.2, lz), 
                    2 + intensity, 0.05, 0.1, 0.05, 0.01);
                
                // Right horn
                double rx = Math.cos(yawRad - Math.PI/6) * 0.15;
                double rz = Math.sin(yawRad - Math.PI/6) * 0.15;
                world.spawnParticle(Particle.FLAME, head.clone().add(rx, 0.2, rz), 
                    2 + intensity, 0.05, 0.1, 0.05, 0.01);
            }
            
            case FROST_AURA -> {
                world.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 
                    particleCount, 0.5, 0.5, 0.5, 0.02);
                if (intensity >= 3) {
                    world.spawnParticle(Particle.SNOW_SHOVEL, loc.clone().add(0, 0.3, 0), 
                        2, 0.3, 0.1, 0.3, 0.01);
                }
            }
            
            case FIRE_TRAIL -> {
                // Only spawn trail when moving
                Long lastMove = lastMovementTime.get(player.getUniqueId());
                if (lastMove != null && System.currentTimeMillis() - lastMove < 200) {
                    world.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.1, 0), 
                        4 + intensity, 0.2, 0.05, 0.2, 0.02);
                }
            }
            
            case LIGHTNING_STRIKE -> {
                // Occasional lightning visual
                if (ThreadLocalRandom.current().nextDouble() < 0.02 * intensity) {
                    world.spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 2, 0), 
                        50, 0.1, 0.5, 0.1, 0.5);
                    world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 2.0f);
                }
            }
            
            case GALAXY_PORTAL -> {
                // Swirling purple portal particles
                for (int i = 0; i < intensity + 2; i++) {
                    double angle = (System.currentTimeMillis() / 50.0 + i * 1.2) % (2 * Math.PI);
                    double radius = 0.5 + Math.sin(angle * 3) * 0.2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = 1 + Math.sin(System.currentTimeMillis() / 200.0 + i) * 0.3;
                    world.spawnParticle(Particle.PORTAL, loc.clone().add(x, y, z), 
                        2, 0.05, 0.05, 0.05, 0.1);
                }
            }
            
            case HEART_TRAIL -> {
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    world.spawnParticle(Particle.HEART, loc.clone().add(
                        ThreadLocalRandom.current().nextDouble(-0.3, 0.3),
                        1 + ThreadLocalRandom.current().nextDouble(0.5),
                        ThreadLocalRandom.current().nextDouble(-0.3, 0.3)
                    ), 1, 0, 0, 0, 0);
                }
            }
            
            case CONFETTI -> {
                if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                    world.spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 2, 0), 
                        5 + intensity * 2, 0.5, 0.3, 0.5, 0.08);
                }
            }
            
            case BUBBLE_AURA -> {
                world.spawnParticle(Particle.BUBBLE_POP, loc.clone().add(0, 1, 0), 
                    particleCount, 0.4, 0.5, 0.4, 0.02);
            }
            
            case MUSIC_NOTES -> {
                if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                    world.spawnParticle(Particle.NOTE, loc.clone().add(
                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                        1.5,
                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
                    ), 1, 0, 0, 0, 1);
                }
            }
            
            case DISCO_FEVER -> {
                // Colorful dancing particles
                for (int i = 0; i < 3; i++) {
                    world.spawnParticle(Particle.SPELL_MOB, loc.clone().add(
                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                        ThreadLocalRandom.current().nextDouble(0.5, 1.5),
                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
                    ), 0, 
                        ThreadLocalRandom.current().nextFloat(),
                        ThreadLocalRandom.current().nextFloat(),
                        ThreadLocalRandom.current().nextFloat(), 
                        1);
                }
            }
            
            case FLOWER_POWER -> {
                if (ThreadLocalRandom.current().nextDouble() < 0.15) {
                    world.spawnParticle(Particle.COMPOSTER, loc.clone().add(
                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                        0.5,
                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
                    ), 3, 0.1, 0.1, 0.1, 0);
                }
            }
            
            case AURORA_BOREALIS -> {
                // Colorful aurora above player
                for (int i = 0; i < 3; i++) {
                    double angle = (System.currentTimeMillis() / 100.0 + i * 2) % (2 * Math.PI);
                    world.spawnParticle(Particle.SPELL_MOB, loc.clone().add(
                        Math.cos(angle) * 0.7,
                        2.5 + Math.sin(angle * 2) * 0.3,
                        Math.sin(angle) * 0.7
                    ), 0, 
                        (float) ((Math.sin(System.currentTimeMillis() / 500.0) + 1) / 2),
                        (float) ((Math.sin(System.currentTimeMillis() / 500.0 + 2) + 1) / 2),
                        (float) ((Math.sin(System.currentTimeMillis() / 500.0 + 4) + 1) / 2), 
                        1);
                }
            }
            
            case PHOENIX_REBIRTH -> {
                // Fire wings
                Location back = loc.clone().add(0, 1.2, 0);
                for (double yaw = -45; yaw <= 45; yaw += 10) {
                    double radYaw = Math.toRadians(loc.getYaw() + 90 + yaw);
                    double dist = 0.3 + Math.abs(yaw) / 60.0;
                    double x = Math.cos(radYaw) * dist;
                    double z = Math.sin(radYaw) * dist;
                    double yOff = Math.cos(Math.toRadians(yaw * 2)) * 0.3;
                    world.spawnParticle(Particle.FLAME, back.clone().add(x, yOff, z), 
                        2, 0.05, 0.1, 0.05, 0.01);
                    
                    radYaw = Math.toRadians(loc.getYaw() + 270 + yaw);
                    x = Math.cos(radYaw) * dist;
                    z = Math.sin(radYaw) * dist;
                    world.spawnParticle(Particle.FLAME, back.clone().add(x, yOff, z), 
                        2, 0.05, 0.1, 0.05, 0.01);
                }
            }
            
            case DRAGON_BREATH -> {
                // Purple dragon particles in front of player
                double yawRad = Math.toRadians(loc.getYaw());
                double frontX = -Math.sin(yawRad) * 0.5;
                double frontZ = Math.cos(yawRad) * 0.5;
                world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(frontX, 1.5, frontZ), 
                    particleCount, 0.2, 0.1, 0.2, 0.02);
            }
            
            case VOID_WALKER -> {
                world.spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 
                    8 + intensity * 2, 0.4, 0.5, 0.4, 0.5);
                if (intensity >= 4) {
                    world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 0.5, 0), 
                        3, 0.3, 0.2, 0.3, 0.3);
                }
            }
            
            case CELESTIAL_BEING -> {
                // Star particles and golden glow
                world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 2, 0), 
                    3, 0.3, 0.5, 0.3, 0.02);
                world.spawnParticle(Particle.GLOW, loc.clone().add(0, 1, 0), 
                    5, 0.4, 0.5, 0.4, 0.01);
                
                // Light rays from above occasionally
                if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                    world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 3, 0), 
                        20, 0.1, 1, 0.1, 0.01);
                }
            }
            
            case REALITY_BENDER -> {
                world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 
                    5 + intensity, 0.5, 0.5, 0.5, 0.3);
            }
            
            // Default particle effect based on type
            default -> {
                Particle particle = type.getDefaultParticle();
                world.spawnParticle(particle, loc.clone().add(0, 1, 0), 
                    particleCount, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }
    
    /**
     * Cleans up a player's effect session.
     */
    private void cleanupSession(Player player) {
        activeSessions.remove(player.getUniqueId());
        BukkitTask task = particleTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        lastMovementTime.remove(player.getUniqueId());
    }
    
    /**
     * Checks if a player has an active effect session.
     */
    public boolean hasActiveEffects(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    
    /**
     * Gets a player's active effect session.
     */
    public ActiveEffectSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }
    
    /**
     * Gets all active effects for a player.
     */
    public List<StrainEffect> getActiveEffects(Player player) {
        ActiveEffectSession session = activeSessions.get(player.getUniqueId());
        return session != null ? session.getActiveEffects() : Collections.emptyList();
    }
    
    // ===== EVENT HANDLERS FOR SPECIAL EFFECTS =====
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (activeSessions.containsKey(player.getUniqueId())) {
            lastMovementTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }
    
    @EventHandler
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        
        ActiveEffectSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;
        
        // Check for Vampire effect - lifesteal
        for (StrainEffect effect : session.getActiveEffects()) {
            if (effect.getType() == StrainEffectType.VAMPIRE) {
                double damage = event.getFinalDamage();
                // Intensity 1 = 15%, intensity 5 = 35%
                double heal = damage * (0.10 + effect.getIntensity() * 0.05);
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
                
                player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, 
                    player.getLocation().add(0, 1.5, 0), 
                    3, 0.2, 0.2, 0.2, 0);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        ActiveEffectSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;
        
        // Check for Thorns effect - reflect damage
        for (StrainEffect effect : session.getActiveEffects()) {
            if (effect.getType() == StrainEffectType.THORNS) {
                if (event.getDamager() instanceof LivingEntity attacker) {
                    double reflectDamage = event.getDamage() * (0.1 + effect.getIntensity() * 0.05);
                    attacker.damage(reflectDamage, player);
                    
                    player.getWorld().spawnParticle(Particle.CRIT, 
                        attacker.getLocation().add(0, 1, 0), 
                        5, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
    }
    
    /**
     * Represents an active effect session for a player.
     */
    public static class ActiveEffectSession {
        private final UUID playerId;
        private final Strain strain;
        private final StarRating quality;
        private final int duration;
        private final long startTime;
        private final List<StrainEffect> activeEffects;
        
        public ActiveEffectSession(UUID playerId, Strain strain, StarRating quality, int duration) {
            this.playerId = playerId;
            this.strain = strain;
            this.quality = quality;
            this.duration = duration;
            this.startTime = System.currentTimeMillis();
            this.activeEffects = new ArrayList<>();
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public Strain getStrain() {
            return strain;
        }
        
        public StarRating getQuality() {
            return quality;
        }
        
        public int getDuration() {
            return duration;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public List<StrainEffect> getActiveEffects() {
            return Collections.unmodifiableList(activeEffects);
        }
        
        public void addActiveEffect(StrainEffect effect) {
            activeEffects.add(effect);
        }
        
        public long getRemainingTime() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.max(0, (duration * 50L) - elapsed);
        }
    }
}
