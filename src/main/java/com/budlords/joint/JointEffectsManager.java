package com.budlords.joint;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the effects when players smoke joints.
 * Different strains and potencies provide different effects.
 */
public class JointEffectsManager implements Listener {

    private final BudLords plugin;
    private final Map<UUID, HighSession> activeSessions;
    private final Map<UUID, BukkitTask> particleTasks;
    private final Map<UUID, Long> smokeCooldowns;

    public JointEffectsManager(BudLords plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.particleTasks = new ConcurrentHashMap<>();
        this.smokeCooldowns = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!JointItems.isJoint(item)) return;
        if (player.isSneaking()) return; // Sneaking bypasses to allow other actions

        event.setCancelled(true);

        // Check cooldown
        if (isOnCooldown(player)) {
            long remaining = getCooldownRemaining(player);
            player.sendMessage("§cYou need to wait " + (remaining / 1000) + " seconds before smoking again!");
            return;
        }

        // Start smoking the joint
        smokeJoint(player, item);
    }

    public void smokeJoint(Player player, ItemStack joint) {
        String strainId = JointItems.getJointStrainId(joint);
        StarRating rating = JointItems.getJointRating(joint);
        int potency = JointItems.getJointPotency(joint);

        if (strainId == null || rating == null) {
            player.sendMessage("§cThis joint seems damaged...");
            return;
        }

        Strain strain = plugin.getStrainManager().getStrain(strainId);
        String strainName = strain != null ? strain.getName() : "Unknown Strain";

        // Consume the joint
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (joint.getAmount() == 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                joint.setAmount(joint.getAmount() - 1);
            }
        }

        // Apply cooldown (shorter for lower potency)
        int cooldownSeconds = 30 + (potency / 5);
        applyCooldown(player, cooldownSeconds);

        // Calculate effect duration based on potency and quality
        int baseDuration = 200; // 10 seconds base
        int durationBonus = (potency / 10) * 40 + (rating.getStars() * 40);
        int totalDuration = baseDuration + durationBonus;

        // Create high session
        HighSession session = new HighSession(
            player.getUniqueId(),
            strainId,
            strainName,
            rating,
            potency,
            totalDuration
        );
        activeSessions.put(player.getUniqueId(), session);

        // Play smoking animation
        playSmokingAnimation(player, session);

        // Apply effects
        applyHighEffects(player, session);

        // Send message
        player.sendMessage("");
        player.sendMessage("§a§l✦ " + strainName + " Joint " + rating.getDisplay());
        player.sendMessage("§7You take a deep hit...");
        player.sendMessage("§7Potency: §e" + potency + "% §7| Duration: §e" + (totalDuration / 20) + "s");
        player.sendMessage("");

        // Update stats
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().getStats(player).incrementJointsSmoked();
        }
    }

    private void applyHighEffects(Player player, HighSession session) {
        int potency = session.getPotency();
        StarRating rating = session.getRating();
        int duration = session.getDuration();

        // Base effects - all joints give some effects
        List<PotionEffect> effects = new ArrayList<>();

        // Speed or Slowness based on strain characteristics
        if (potency > 70) {
            // High potency - more intense effects
            effects.add(new PotionEffect(PotionEffectType.SLOW, duration, 0, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.CONFUSION, duration / 2, 0, false, false, true));
        } else if (potency > 40) {
            // Medium potency - balanced effects
            effects.add(new PotionEffect(PotionEffectType.SLOW, duration / 2, 0, false, false, true));
        }

        // Positive effects based on quality
        if (rating.getStars() >= 3) {
            effects.add(new PotionEffect(PotionEffectType.REGENERATION, duration, 0, false, false, true));
        }
        if (rating.getStars() >= 4) {
            effects.add(new PotionEffect(PotionEffectType.ABSORPTION, duration, 0, false, false, true));
        }
        if (rating.getStars() >= 5) {
            effects.add(new PotionEffect(PotionEffectType.LUCK, duration * 2, 0, false, false, true));
        }

        // Hunger effect (munchies!)
        effects.add(new PotionEffect(PotionEffectType.HUNGER, duration / 2, 0, false, false, true));
        effects.add(new PotionEffect(PotionEffectType.SATURATION, 40, 0, false, false, true));

        // Night vision for better strains
        if (potency > 50 && rating.getStars() >= 2) {
            effects.add(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
        }

        // Random special effects based on strain type
        int randomEffect = ThreadLocalRandom.current().nextInt(100);
        if (randomEffect < potency / 2) {
            // Chance for special effects
            PotionEffectType[] specialEffects = {
                PotionEffectType.JUMP,
                PotionEffectType.DOLPHINS_GRACE,
                PotionEffectType.GLOWING,
                PotionEffectType.LEVITATION
            };
            PotionEffectType special = specialEffects[ThreadLocalRandom.current().nextInt(specialEffects.length)];
            
            // Short duration for levitation to prevent issues
            int specialDuration = special == PotionEffectType.LEVITATION ? 60 : duration / 2;
            int amplifier = special == PotionEffectType.LEVITATION ? 0 : 0;
            
            effects.add(new PotionEffect(special, specialDuration, amplifier, false, false, true));
            
            player.sendMessage("§d✦ Special Effect: §f" + formatEffectName(special) + "!");
        }

        // Apply all effects
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
    }

    private void playSmokingAnimation(Player player, HighSession session) {
        Location loc = player.getLocation();

        // Initial smoke burst
        player.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.add(0, 1.5, 0), 5, 0.2, 0.1, 0.2, 0.02);

        // Cancel existing particle task
        BukkitTask existing = particleTasks.remove(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }

        // Start ambient high particles
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = session.getDuration();

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    activeSessions.remove(player.getUniqueId());
                    particleTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                Location playerLoc = player.getLocation();

                // Periodic smoke from mouth area
                if (ticks % 20 == 0) {
                    player.getWorld().spawnParticle(
                        Particle.CAMPFIRE_SIGNAL_SMOKE,
                        playerLoc.add(0, 1.6, 0),
                        1, 0.1, 0.05, 0.1, 0.01
                    );
                }

                // Ambient particles based on potency
                if (session.getPotency() > 60 && ticks % 40 == 0) {
                    player.getWorld().spawnParticle(
                        Particle.SPELL_MOB,
                        playerLoc.add(0, 1.2, 0),
                        3, 0.3, 0.3, 0.3, 1
                    );
                }

                // Happy particles for high quality
                if (session.getRating().getStars() >= 4 && ticks % 60 == 0) {
                    player.getWorld().spawnParticle(
                        Particle.HEART,
                        playerLoc.add(0, 2, 0),
                        1, 0.2, 0.1, 0.2, 0
                    );
                }

                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        particleTasks.put(player.getUniqueId(), task);

        // Schedule end message
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage("§7The high from " + session.getStrainName() + " is wearing off...");
                }
            }
        }.runTaskLater(plugin, session.getDuration());
    }

    private String formatEffectName(PotionEffectType type) {
        String name = type.getName().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private boolean isOnCooldown(Player player) {
        Long cooldownEnd = smokeCooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return false;
        return System.currentTimeMillis() < cooldownEnd;
    }

    private long getCooldownRemaining(Player player) {
        Long cooldownEnd = smokeCooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return 0;
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }

    private void applyCooldown(Player player, int seconds) {
        smokeCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean isHigh(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public HighSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void cleanup(Player player) {
        activeSessions.remove(player.getUniqueId());
        BukkitTask task = particleTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Represents an active "high" session for a player.
     */
    public static class HighSession {
        private final UUID playerId;
        private final String strainId;
        private final String strainName;
        private final StarRating rating;
        private final int potency;
        private final int duration;
        private final long startTime;

        public HighSession(UUID playerId, String strainId, String strainName, StarRating rating, int potency, int duration) {
            this.playerId = playerId;
            this.strainId = strainId;
            this.strainName = strainName;
            this.rating = rating;
            this.potency = potency;
            this.duration = duration;
            this.startTime = System.currentTimeMillis();
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getStrainId() {
            return strainId;
        }

        public String getStrainName() {
            return strainName;
        }

        public StarRating getRating() {
            return rating;
        }

        public int getPotency() {
            return potency;
        }

        public int getDuration() {
            return duration;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getRemainingTime() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.max(0, (duration * 50L) - elapsed);
        }
    }
}
