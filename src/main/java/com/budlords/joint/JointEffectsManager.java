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

        // Apply base high effects
        applyHighEffects(player, session);
        
        // Apply strain-specific special effects!
        if (strain != null && plugin.getStrainEffectsManager() != null) {
            plugin.getStrainEffectsManager().applyStrainEffects(player, strain, rating, totalDuration);
        }

        // ═══════════════════════════════════════
        // ENHANCED MESSAGE DISPLAY - PROFESSIONAL!
        // ═══════════════════════════════════════
        
        String rarityColor = getRarityColor(strain != null ? strain.getRarity() : Strain.Rarity.COMMON);
        String qualityStars = rating.getDisplay();
        
        player.sendMessage("");
        player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("  " + rarityColor + "§l✦ " + strainName + " Joint " + qualityStars);
        player.sendMessage("");
        player.sendMessage("  §7💨 You take a deep, satisfying hit...");
        player.sendMessage("");
        player.sendMessage("  §7Potency: §e" + potency + "% §8│ §7Duration: §e" + (totalDuration / 20) + "s");
        
        // Show rarity badge
        if (strain != null) {
            String rarityBadge = switch (strain.getRarity()) {
                case LEGENDARY -> "§6§l★ LEGENDARY ★";
                case RARE -> "§9§l◆ RARE ◆";
                case UNCOMMON -> "§a§l♦ UNCOMMON ♦";
                case COMMON -> "§7○ Common";
            };
            player.sendMessage("  §7Rarity: " + rarityBadge);
        }
        
        // Show active strain effects
        if (strain != null && !strain.getEffects().isEmpty()) {
            player.sendMessage("");
            player.sendMessage("  §d§lStrain Effects:");
            for (com.budlords.effects.StrainEffect effect : strain.getEffects()) {
                player.sendMessage("    " + effect.getCompactDisplay());
            }
        }
        
        player.sendMessage("");
        player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
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
        Strain strain = plugin.getStrainManager().getStrain(session.getStrainId());
        Strain.Rarity rarity = strain != null ? strain.getRarity() : Strain.Rarity.COMMON;

        // ═══════════════════════════════════════
        // ENHANCED EFFECT SYSTEM - MORE FUN & USEFUL!
        // ═══════════════════════════════════════
        List<PotionEffect> effects = new ArrayList<>();
        List<String> activeEffects = new ArrayList<>();

        // ═══ CORE EFFECTS - ALWAYS PRESENT ═══
        
        // Night vision - essential for weed experience
        if (rating.getStars() >= 2) {
            effects.add(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
            activeEffects.add("§b👁 Enhanced Vision");
        }
        
        // Hunger (munchies!) - iconic effect
        effects.add(new PotionEffect(PotionEffectType.HUNGER, duration / 2, 1, false, false, true));
        effects.add(new PotionEffect(PotionEffectType.SATURATION, 60, 1, false, false, true));
        activeEffects.add("§6🍕 The Munchies");

        // ═══ POTENCY-BASED EFFECTS ═══
        
        if (potency > 80) {
            // VERY HIGH POTENCY - Extreme effects with fun twist
            effects.add(new PotionEffect(PotionEffectType.SLOW, duration / 2, 1, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.CONFUSION, duration / 3, 0, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.JUMP, duration, 2, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.SPEED, duration, 0, false, false, true));
            activeEffects.add("§5⚡ Ultra High - Mixed Signals");
            activeEffects.add("§a🦘 Super Jump");
        } else if (potency > 60) {
            // HIGH POTENCY - Strong but pleasant
            effects.add(new PotionEffect(PotionEffectType.SLOW, duration / 3, 0, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.JUMP, duration, 1, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.SPEED, duration / 2, 0, false, false, true));
            activeEffects.add("§d✨ Strong High");
            activeEffects.add("§a🏃 Enhanced Movement");
        } else if (potency > 40) {
            // MEDIUM POTENCY - Balanced and fun
            effects.add(new PotionEffect(PotionEffectType.JUMP, duration, 0, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.SPEED, duration / 2, 0, false, false, true));
            activeEffects.add("§e✨ Smooth High");
            activeEffects.add("§a🏃 Light Step");
        } else {
            // LOW POTENCY - Mild and comfortable
            effects.add(new PotionEffect(PotionEffectType.SPEED, duration / 2, 0, false, false, true));
            activeEffects.add("§7✨ Mild Buzz");
        }

        // ═══ QUALITY-BASED POSITIVE EFFECTS ═══
        
        if (rating.getStars() >= 5) {
            // ★★★★★ LEGENDARY QUALITY
            effects.add(new PotionEffect(PotionEffectType.REGENERATION, duration, 1, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.ABSORPTION, duration, 2, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, 0, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.LUCK, duration * 2, 1, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false, true));
            activeEffects.add("§6§l★ LEGENDARY BENEFITS ★");
            activeEffects.add("§c💗 Super Regen");
            activeEffects.add("§e🛡 Absorption Shield");
            activeEffects.add("§a🍀 Double Luck");
        } else if (rating.getStars() >= 4) {
            // ★★★★ HIGH QUALITY
            effects.add(new PotionEffect(PotionEffectType.REGENERATION, duration, 0, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.ABSORPTION, duration, 1, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.LUCK, duration, 0, false, false, true));
            activeEffects.add("§d✦ Premium Quality");
            activeEffects.add("§c💗 Health Regen");
            activeEffects.add("§e🛡 Extra Hearts");
        } else if (rating.getStars() >= 3) {
            // ★★★ GOOD QUALITY
            effects.add(new PotionEffect(PotionEffectType.REGENERATION, duration / 2, 0, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.ABSORPTION, duration / 2, 0, false, false, true));
            activeEffects.add("§a✦ Good Quality");
            activeEffects.add("§c💗 Healing");
        }

        // ═══ RARITY-BASED SPECIAL EFFECTS ═══
        
        if (rarity == Strain.Rarity.LEGENDARY) {
            // LEGENDARY STRAINS - Flying ability!
            int levDuration = duration / 4; // Controlled duration
            effects.add(new PotionEffect(PotionEffectType.LEVITATION, levDuration, 0, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, duration, 0, false, false, true));
            activeEffects.add("§6✈ FLYING HIGH!");
            activeEffects.add("§b🌊 Water Grace");
            
            // Extra special ability
            player.sendMessage("");
            player.sendMessage("§6§l★═══════════════════★");
            player.sendMessage("§6§l   LEGENDARY STRAIN ACTIVATED!");
            player.sendMessage("§e✈ You can briefly FLY!");
            player.sendMessage("§e🌟 Special abilities unlocked!");
            player.sendMessage("§6§l★═══════════════════★");
            player.sendMessage("");
            
        } else if (rarity == Strain.Rarity.RARE) {
            // RARE STRAINS - Enhanced mobility
            effects.add(new PotionEffect(PotionEffectType.JUMP, duration, 2, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false, true));
            effects.add(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, duration / 2, 0, false, false, true));
            activeEffects.add("§9🚀 Super Mobility");
            activeEffects.add("§b🌊 Swim Speed");
            
        } else if (rarity == Strain.Rarity.UNCOMMON) {
            // UNCOMMON - Utility boost
            effects.add(new PotionEffect(PotionEffectType.FAST_DIGGING, duration, 0, false, false, true));
            activeEffects.add("§a⚒ Mining Speed");
        }

        // ═══ RANDOM FUN EFFECTS (Potency-based chance) ═══
        
        int randomChance = ThreadLocalRandom.current().nextInt(100);
        if (randomChance < potency / 2) {
            List<FunEffect> possibleEffects = new ArrayList<>();
            
            // Build list based on quality
            possibleEffects.add(new FunEffect(PotionEffectType.GLOWING, duration, 0, "§e✨ Glowing Aura"));
            possibleEffects.add(new FunEffect(PotionEffectType.WATER_BREATHING, duration, 0, "§b🐟 Water Breathing"));
            
            if (rating.getStars() >= 3) {
                possibleEffects.add(new FunEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration, 0, "§a🏘 Village Hero"));
            }
            
            if (rating.getStars() >= 4) {
                possibleEffects.add(new FunEffect(PotionEffectType.CONDUIT_POWER, duration, 0, "§b⚡ Conduit Power"));
            }
            
            FunEffect chosen = possibleEffects.get(ThreadLocalRandom.current().nextInt(possibleEffects.size()));
            effects.add(new PotionEffect(chosen.type, chosen.duration, chosen.amplifier, false, false, true));
            activeEffects.add("§d✦ Bonus: " + chosen.display);
        }

        // Apply all effects
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
        
        // Display active effects summary
        if (!activeEffects.isEmpty()) {
            player.sendMessage("§7Active Effects:");
            for (String effect : activeEffects) {
                player.sendMessage("  " + effect);
            }
        }
    }
    
    /**
     * Helper class for random fun effects.
     */
    private record FunEffect(PotionEffectType type, int duration, int amplifier, String display) {}

    private void playSmokingAnimation(Player player, HighSession session) {
        Location loc = player.getLocation();
        
        // Get strain for color-coded effects
        Strain strain = plugin.getStrainManager().getStrain(session.getStrainId());
        Strain.Rarity rarity = strain != null ? strain.getRarity() : Strain.Rarity.COMMON;
        int potency = session.getPotency();
        int stars = session.getRating().getStars();

        // ═══════════════════════════════════════
        // INITIAL SMOKING SEQUENCE - SPECTACULAR!
        // ═══════════════════════════════════════
        
        // Multiple sound layers for depth
        player.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.5f);
        player.playSound(loc, Sound.ENTITY_GHAST_SHOOT, 0.3f, 2.0f);
        player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.8f);
        
        // Massive initial smoke burst with swirl effect
        new BukkitRunnable() {
            int burst = 0;
            @Override
            public void run() {
                if (burst >= 5 || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                Location smokeLoc = player.getLocation().add(0, 1.6, 0);
                
                // Swirling smoke effect
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i / 8.0) + (burst * 0.5);
                    double radius = 0.3 + (burst * 0.1);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    player.getWorld().spawnParticle(
                        Particle.CAMPFIRE_COSY_SMOKE,
                        smokeLoc.clone().add(x, 0, z),
                        0, x * 0.1, 0.2, z * 0.1, 0.02
                    );
                }
                
                // Central smoke plume
                player.getWorld().spawnParticle(
                    Particle.CLOUD,
                    smokeLoc,
                    5, 0.15, 0.1, 0.15, 0.01
                );
                
                burst++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        // Rarity-based color explosion
        Particle rarityParticle = getRarityParticle(rarity);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                Location effectLoc = player.getLocation().add(0, 1.2, 0);
                
                // Expanding ring of colored particles
                for (int i = 0; i < 20; i++) {
                    double angle = Math.PI * 2 * i / 20.0;
                    double radius = 0.8;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    player.getWorld().spawnParticle(
                        rarityParticle,
                        effectLoc.clone().add(x, 0, z),
                        1, 0, 0, 0, 0
                    );
                }
                
                // Upward spiral
                player.getWorld().spawnParticle(
                    Particle.END_ROD,
                    effectLoc,
                    10, 0.3, 0.5, 0.3, 0.05
                );
            }
        }.runTaskLater(plugin, 10L);

        // Cancel existing particle task
        BukkitTask existing = particleTasks.remove(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }

        // ═══════════════════════════════════════
        // CONTINUOUS AMBIENT EFFECTS - PROFESSIONAL!
        // ═══════════════════════════════════════
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = session.getDuration();
            double spiralAngle = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    activeSessions.remove(player.getUniqueId());
                    particleTasks.remove(player.getUniqueId());
                    
                    // Ending particle burst
                    if (player.isOnline()) {
                        Location endLoc = player.getLocation().add(0, 1.5, 0);
                        player.getWorld().spawnParticle(
                            Particle.CLOUD,
                            endLoc,
                            20, 0.3, 0.3, 0.3, 0.05
                        );
                        player.playSound(endLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 0.8f);
                    }
                    cancel();
                    return;
                }

                Location playerLoc = player.getLocation();

                // ═══ CONSTANT SMOKE AURA ═══
                if (ticks % 10 == 0) {
                    // Gentle smoke wisps from head
                    player.getWorld().spawnParticle(
                        Particle.CAMPFIRE_SIGNAL_SMOKE,
                        playerLoc.clone().add(0, 1.6, 0),
                        2, 0.12, 0.08, 0.12, 0.005
                    );
                }

                // ═══ RARITY-BASED SPIRAL AURA ═══
                if (ticks % 5 == 0) {
                    double radius = 0.6 + (Math.sin(ticks * 0.1) * 0.2);
                    double height = 0.5 + ((ticks % 60) * 0.03);
                    
                    double x = Math.cos(spiralAngle) * radius;
                    double z = Math.sin(spiralAngle) * radius;
                    
                    player.getWorld().spawnParticle(
                        rarityParticle,
                        playerLoc.clone().add(x, height, z),
                        1, 0, 0, 0, 0
                    );
                    
                    spiralAngle += 0.4;
                }

                // ═══ POTENCY EFFECTS (Higher = More Intense) ═══
                if (potency > 70 && ticks % 15 == 0) {
                    // Intense swirling particles for high potency
                    for (int i = 0; i < 3; i++) {
                        double angle = (Math.PI * 2 * i / 3.0) + (ticks * 0.1);
                        double x = Math.cos(angle) * 0.5;
                        double z = Math.sin(angle) * 0.5;
                        
                        player.getWorld().spawnParticle(
                            Particle.SPELL_WITCH,
                            playerLoc.clone().add(x, 1.2, z),
                            1, 0, 0, 0, 0
                        );
                    }
                } else if (potency > 40 && ticks % 25 == 0) {
                    // Medium potency sparkles
                    player.getWorld().spawnParticle(
                        Particle.ENCHANTMENT_TABLE,
                        playerLoc.clone().add(0, 1.2, 0),
                        5, 0.3, 0.3, 0.3, 1
                    );
                }

                // ═══ QUALITY EFFECTS (5-star = Best) ═══
                if (stars >= 5 && ticks % 20 == 0) {
                    // Legendary quality golden sparkles
                    player.getWorld().spawnParticle(
                        Particle.TOTEM,
                        playerLoc.clone().add(0, 2, 0),
                        3, 0.3, 0.2, 0.3, 0
                    );
                    player.getWorld().spawnParticle(
                        Particle.END_ROD,
                        playerLoc.clone().add(0, 0.5, 0),
                        1, 0.2, 0.2, 0.2, 0.02
                    );
                } else if (stars >= 4 && ticks % 30 == 0) {
                    // High quality heart particles
                    player.getWorld().spawnParticle(
                        Particle.HEART,
                        playerLoc.clone().add(0, 2.1, 0),
                        1, 0.25, 0.15, 0.25, 0
                    );
                } else if (stars >= 3 && ticks % 40 == 0) {
                    // Good quality villager particles
                    player.getWorld().spawnParticle(
                        Particle.VILLAGER_HAPPY,
                        playerLoc.clone().add(0, 1.8, 0),
                        2, 0.3, 0.2, 0.3, 0
                    );
                }
                
                // ═══ LEGENDARY RARITY SPECIAL EFFECT ═══
                if (rarity == Strain.Rarity.LEGENDARY && ticks % 30 == 0) {
                    // Epic ground slam effect
                    Location groundLoc = playerLoc.clone().add(0, 0.1, 0);
                    for (int i = 0; i < 12; i++) {
                        double angle = Math.PI * 2 * i / 12.0;
                        double radius = 1.5;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        
                        player.getWorld().spawnParticle(
                            Particle.DRAGON_BREATH,
                            groundLoc.clone().add(x, 0, z),
                            1, 0, 0, 0, 0
                        );
                    }
                    player.playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.2f, 2.0f);
                }
                
                // ═══ AMBIENT SOUND EFFECTS ═══
                if (ticks % 100 == 0 && potency > 60) {
                    player.playSound(playerLoc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.3f, 0.7f);
                }

                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        particleTasks.put(player.getUniqueId(), task);

        // Schedule end message with fade effect
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage("§7§o The high from " + session.getStrainName() + " is fading away...");
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 1.2f);
                }
            }
        }.runTaskLater(plugin, session.getDuration() - 40);
    }
    
    /**
     * Gets the particle type based on strain rarity for color-coded effects.
     */
    private Particle getRarityParticle(Strain.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Particle.ASH;
            case UNCOMMON -> Particle.VILLAGER_HAPPY;
            case RARE -> Particle.ENCHANTMENT_TABLE;
            case LEGENDARY -> Particle.END_ROD;
        };
    }
    
    /**
     * Gets the color code based on strain rarity.
     */
    private String getRarityColor(Strain.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> "§7";
            case UNCOMMON -> "§a";
            case RARE -> "§9";
            case LEGENDARY -> "§6";
        };
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
