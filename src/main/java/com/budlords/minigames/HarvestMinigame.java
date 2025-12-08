package com.budlords.minigames;

import com.budlords.BudLords;
import com.budlords.farming.Plant;
import com.budlords.quality.StarRating;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Harvest Mini-game - An interactive timing-based mini-game when harvesting plants.
 * 
 * Game Mechanics:
 * - Player must click at the right time when visual cues appear
 * - Success = bonus quality, yield, and potential rare drops
 * - Failure = normal harvest with no bonuses
 * - Difficulty scales with plant quality
 * 
 * Visual Feedback:
 * - Action bar progress display
 * - Particle effects for timing windows
 * - Sound effects for hits/misses
 * - Title messages for results
 */
public class HarvestMinigame {
    
    private final BudLords plugin;
    
    // Track active mini-games: player UUID -> game instance
    private final Map<UUID, MinigameSession> activeSessions;
    
    public HarvestMinigame(BudLords plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
    }
    
    /**
     * Starts a harvest mini-game for a player.
     * @param onComplete Callback to execute when the minigame completes successfully
     */
    public void startMinigame(Player player, Plant plant, Location plantLocation, Runnable onComplete) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou already have a mini-game in progress!");
            return;
        }
        
        StarRating rating = plant.calculateFinalBudRating(null);
        int difficulty = rating != null ? rating.getStars() : 1;
        
        MinigameSession session = new MinigameSession(player, plant, plantLocation, difficulty, onComplete);
        activeSessions.put(player.getUniqueId(), session);
        
        session.start();
    }
    
    /**
     * Handles a click event during the mini-game.
     * Returns true if the click was during an active game.
     */
    public boolean handleClick(Player player) {
        MinigameSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return false;
        
        session.registerClick();
        return true;
    }
    
    /**
     * Checks if a player has an active mini-game.
     */
    public boolean hasActiveGame(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    
    /**
     * Gets the result of a mini-game if completed.
     */
    public MinigameResult getResult(Player player) {
        MinigameSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isCompleted()) return null;
        return session.getResult();
    }
    
    /**
     * Cleans up a completed mini-game session.
     */
    public void cleanupSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }
    
    /**
     * Represents a single mini-game session.
     */
    private class MinigameSession {
        private final Player player;
        private final Plant plant;
        private final Location location;
        private final int difficulty;
        private final Runnable onComplete;
        
        private int currentRound = 0;
        private int successfulHits = 0;
        private int totalRounds;
        private boolean completed = false;
        private MinigameResult result;
        
        private boolean isHitWindow = false;
        private long hitWindowStart = 0;
        private static final long HIT_WINDOW_MS = 500; // 500ms window to click
        
        public MinigameSession(Player player, Plant plant, Location location, int difficulty, Runnable onComplete) {
            this.player = player;
            this.plant = plant;
            this.location = location;
            this.difficulty = difficulty;
            this.onComplete = onComplete;
            this.totalRounds = 3 + difficulty; // More rounds for higher quality plants
        }
        
        public void start() {
            player.sendTitle("§a§lHARVEST MINI-GAME", "§7Click when you see the green sparkles!", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            
            // Start first round after a brief delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        cleanupSession(player);
                        return;
                    }
                    startRound();
                }
            }.runTaskLater(plugin, 40L); // 2 second delay
        }
        
        private void startRound() {
            currentRound++;
            
            if (currentRound > totalRounds) {
                complete();
                return;
            }
            
            // Wait random time before showing hit window (1-3 seconds)
            long delayTicks = 20L + (long)(Math.random() * 40L);
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        cleanupSession(player);
                        return;
                    }
                    showHitWindow();
                }
            }.runTaskLater(plugin, delayTicks);
            
            // Show round indicator with safe repeat counts
            String progress = "§a" + "█".repeat(successfulHits) + 
                            "§7" + "█".repeat(Math.max(0, currentRound - successfulHits - 1)) + 
                            "§8" + "█".repeat(Math.max(0, totalRounds - currentRound + 1));
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§eRound " + currentRound + "/" + totalRounds + " §7| " + progress));
        }
        
        private void showHitWindow() {
            isHitWindow = true;
            hitWindowStart = System.currentTimeMillis();
            
            // Visual cues
            location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, 
                location.clone().add(0.5, 1, 0.5), 20, 0.3, 0.3, 0.3, 0.1);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 2.0f);
            
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a§l>>> CLICK NOW! <<<"));
            
            // Auto-fail if not clicked in time
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (isHitWindow) {
                        registerMiss();
                    }
                }
            }.runTaskLater(plugin, (HIT_WINDOW_MS * 20L) / 1000L); // Convert ms to ticks properly
        }
        
        public void registerClick() {
            if (!isHitWindow) {
                // Early click - don't penalize but give feedback
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c§lToo early! Wait for the signal..."));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                return;
            }
            
            // Prevent double-clicking/spam clicking
            if (isHitWindow) {
                isHitWindow = false; // Close window immediately to prevent spam
            } else {
                return; // Already processed this click
            }
            
            long clickTime = System.currentTimeMillis() - hitWindowStart;
            
            if (clickTime <= HIT_WINDOW_MS) {
                // Successful hit!
                successfulHits++;
                
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a§l✓ PERFECT! §e+" + successfulHits + " bonus"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 2.0f);
                location.getWorld().spawnParticle(Particle.TOTEM, 
                    location.clone().add(0.5, 1, 0.5), 15, 0.3, 0.3, 0.3, 0.1);
                
                // Continue to next round
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        startRound();
                    }
                }.runTaskLater(plugin, 20L);
            } else {
                registerMiss();
            }
        }
        
        private void registerMiss() {
            isHitWindow = false;
            
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c§l✗ MISSED!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            location.getWorld().spawnParticle(Particle.SMOKE_NORMAL, 
                location.clone().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.02);
            
            // Continue to next round
            new BukkitRunnable() {
                @Override
                public void run() {
                    startRound();
                }
            }.runTaskLater(plugin, 20L);
        }
        
        private void complete() {
            completed = true;
            
            // Calculate performance
            double successRate = (double) successfulHits / totalRounds;
            
            // Determine grade and bonuses
            String grade;
            int qualityBonus;
            double yieldMultiplier;
            boolean rareDropChance;
            
            if (successRate >= 0.9) {
                grade = "§6§lS"; // Perfect or near-perfect
                qualityBonus = 15;
                yieldMultiplier = 1.5;
                rareDropChance = true;
            } else if (successRate >= 0.7) {
                grade = "§a§lA"; // Great
                qualityBonus = 10;
                yieldMultiplier = 1.3;
                rareDropChance = Math.random() < 0.5;
            } else if (successRate >= 0.5) {
                grade = "§e§lB"; // Good
                qualityBonus = 5;
                yieldMultiplier = 1.15;
                rareDropChance = Math.random() < 0.25;
            } else if (successRate >= 0.3) {
                grade = "§6§lC"; // Average
                qualityBonus = 2;
                yieldMultiplier = 1.05;
                rareDropChance = false;
            } else {
                grade = "§c§lD"; // Poor
                qualityBonus = 0;
                yieldMultiplier = 1.0;
                rareDropChance = false;
            }
            
            result = new MinigameResult(grade, successfulHits, totalRounds, 
                qualityBonus, yieldMultiplier, rareDropChance);
            
            // Show results
            player.sendTitle(grade + " §7GRADE!", 
                "§7" + successfulHits + "/" + totalRounds + " perfect hits", 10, 60, 20);
            
            player.sendMessage("");
            player.sendMessage("§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("§a§l      HARVEST COMPLETE!");
            player.sendMessage("");
            player.sendMessage("  §7Grade: " + grade);
            player.sendMessage("  §7Score: §e" + successfulHits + "§7/§e" + totalRounds + " §7perfect hits");
            player.sendMessage("");
            player.sendMessage("  §7Bonuses:");
            player.sendMessage("  §8• §aQuality: §e+" + qualityBonus + "%");
            player.sendMessage("  §8• §aYield: §e×" + String.format("%.1f", yieldMultiplier));
            if (rareDropChance) {
                player.sendMessage("  §8• §6Rare Drop: §e✓ Bonus!");
            }
            player.sendMessage("§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("");
            
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            // Celebratory particles
            location.getWorld().spawnParticle(Particle.TOTEM, 
                location.clone().add(0.5, 1, 0.5), 50, 0.5, 0.5, 0.5, 0.15);
            
            // Trigger the harvest callback after a short delay to let animations play
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && onComplete != null) {
                        onComplete.run();
                    }
                }
            }.runTaskLater(plugin, 40L); // 2 second delay to let results display
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public MinigameResult getResult() {
            return result;
        }
    }
    
    /**
     * Result data from a completed mini-game.
     */
    public static class MinigameResult {
        private final String grade;
        private final int successfulHits;
        private final int totalRounds;
        private final int qualityBonus;
        private final double yieldMultiplier;
        private final boolean rareDropChance;
        
        public MinigameResult(String grade, int successfulHits, int totalRounds,
                            int qualityBonus, double yieldMultiplier, boolean rareDropChance) {
            this.grade = grade;
            this.successfulHits = successfulHits;
            this.totalRounds = totalRounds;
            this.qualityBonus = qualityBonus;
            this.yieldMultiplier = yieldMultiplier;
            this.rareDropChance = rareDropChance;
        }
        
        public String getGrade() { return grade; }
        public int getSuccessfulHits() { return successfulHits; }
        public int getTotalRounds() { return totalRounds; }
        public int getQualityBonus() { return qualityBonus; }
        public double getYieldMultiplier() { return yieldMultiplier; }
        public boolean hasRareDropChance() { return rareDropChance; }
    }
    
    /**
     * Cleanup on plugin disable.
     */
    public void shutdown() {
        for (UUID playerId : activeSessions.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage("§cHarvest mini-game cancelled due to server restart.");
            }
        }
        activeSessions.clear();
    }
}
