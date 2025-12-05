package com.budlords.joint;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the joint rolling minigame system.
 * Players roll joints through a series of interactive minigames.
 */
public class JointRollingManager implements InventoryHolder {

    private final BudLords plugin;
    private final StrainManager strainManager;
    private final Map<UUID, JointRollingSession> activeSessions;
    private final Map<UUID, BukkitTask> activeTasks;
    private final Map<UUID, Integer> clickerPosition;  // For timing minigames
    private final Set<UUID> transitioning;  // Players transitioning between stages (don't cleanup on close)

    public JointRollingManager(BudLords plugin, StrainManager strainManager) {
        this.plugin = plugin;
        this.strainManager = strainManager;
        this.activeSessions = new ConcurrentHashMap<>();
        this.activeTasks = new ConcurrentHashMap<>();
        this.clickerPosition = new ConcurrentHashMap<>();
        this.transitioning = ConcurrentHashMap.newKeySet();
    }

    /**
     * Starts a new joint rolling session for a player.
     */
    public boolean startRolling(Player player, ItemStack grindedBud, ItemStack rollingPaper, ItemStack tobacco) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou are already rolling a joint!");
            return false;
        }

        // Validate items
        if (!JointItems.isGrindedBud(grindedBud)) {
            player.sendMessage("§cYou need grinded bud to roll a joint!");
            return false;
        }
        if (!JointItems.isRollingPaper(rollingPaper)) {
            player.sendMessage("§cYou need rolling paper to roll a joint!");
            return false;
        }
        if (!JointItems.isTobacco(tobacco)) {
            player.sendMessage("§cYou need tobacco to roll a joint!");
            return false;
        }

        String strainId = JointItems.getGrindedBudStrainId(grindedBud);
        StarRating budRating = JointItems.getGrindedBudRating(grindedBud);
        
        if (strainId == null || budRating == null) {
            player.sendMessage("§cInvalid grinded bud!");
            return false;
        }

        Strain strain = strainManager.getStrain(strainId);
        if (strain == null) {
            player.sendMessage("§cStrain not found!");
            return false;
        }

        // Create session
        JointRollingSession session = new JointRollingSession(
            player.getUniqueId(),
            strainId,
            strain.getName(),
            budRating,
            strain.getPotency()
        );
        activeSessions.put(player.getUniqueId(), session);

        // Consume items
        grindedBud.setAmount(grindedBud.getAmount() - 1);
        rollingPaper.setAmount(rollingPaper.getAmount() - 1);
        tobacco.setAmount(tobacco.getAmount() - 1);

        // Open minigame GUI
        openMinigameGUI(player, session);

        player.sendMessage("");
        player.sendMessage("§a§l✦ Joint Rolling Started!");
        player.sendMessage("§7Rolling: §f" + strain.getName() + " §7" + budRating.getDisplay());
        player.sendMessage("§7Complete 4 stages to create your joint!");
        player.sendMessage("");

        return true;
    }

    /**
     * Opens the minigame GUI for the current stage.
     */
    // Using deprecated Inventory title API for Bukkit/Spigot compatibility
    // Paper servers can replace with Adventure API's title(Component) method
    @SuppressWarnings("deprecation")
    public void openMinigameGUI(Player player, JointRollingSession session) {
        String title = getMinigameTitle(session.getCurrentStage());
        Inventory inv = Bukkit.createInventory(this, 54, title);
        
        updateMinigameGUI(inv, player, session);
        player.openInventory(inv);
        
        // Start the minigame animation task
        startMinigameTask(player, session, inv);
        
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
    }

    private String getMinigameTitle(JointRollingSession.RollingStage stage) {
        return switch (stage) {
            case PAPER_PULL -> "§f✦ Paper Pull - Stage 1/4";
            case TOBACCO_ROLL -> "§6✦ Tobacco Roll - Stage 2/4";
            case GRIND -> "§a✦ Grinding - Stage 3/4";
            case FINAL_ROLL -> "§e✦ Final Roll - Stage 4/4";
        };
    }

    private void updateMinigameGUI(Inventory inv, Player player, JointRollingSession session) {
        inv.clear();
        
        JointRollingSession.RollingStage stage = session.getCurrentStage();
        
        // Border
        ItemStack borderDark = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        ItemStack borderColor = createGuiItem(getBorderMaterial(stage), " ", null);
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderColor : borderDark);
            inv.setItem(45 + i, i % 2 == 0 ? borderColor : borderDark);
        }
        // Side borders
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, borderDark);
            inv.setItem(i + 8, borderDark);
        }
        
        // Header info
        ItemStack header = createGuiItem(Material.PAPER, 
            stage.getDisplayName(),
            Arrays.asList(
                "",
                "§7" + stage.getDescription(),
                "",
                "§7Stage: §e" + stage.getStageNumber() + "/4",
                "§7Score: §a" + session.getStageScore() + "/100"
            ));
        inv.setItem(4, header);
        
        // Progress indicator
        ItemStack progress = createGuiItem(Material.EXPERIENCE_BOTTLE,
            "§e§lOverall Progress",
            Arrays.asList(
                "",
                createProgressBar(session.getOverallProgress()),
                "§7" + String.format("%.0f%%", session.getOverallProgress() * 100) + " Complete",
                "",
                "§7Total Score: §a" + session.getTotalScore()
            ));
        inv.setItem(49, progress);
        
        // Stage-specific minigame elements
        switch (stage) {
            case PAPER_PULL -> setupPaperPullMinigame(inv, session);
            case TOBACCO_ROLL -> setupTobaccoRollMinigame(inv, session);
            case GRIND -> setupGrindMinigame(inv, session);
            case FINAL_ROLL -> setupFinalRollMinigame(inv, session);
        }
        
        // Cancel button
        inv.setItem(45, createGuiItem(Material.BARRIER, "§c§l✗ Cancel",
            Arrays.asList("", "§7Click to cancel", "§cYou will lose materials!")));
    }

    private Material getBorderMaterial(JointRollingSession.RollingStage stage) {
        return switch (stage) {
            case PAPER_PULL -> Material.WHITE_STAINED_GLASS_PANE;
            case TOBACCO_ROLL -> Material.ORANGE_STAINED_GLASS_PANE;
            case GRIND -> Material.LIME_STAINED_GLASS_PANE;
            case FINAL_ROLL -> Material.YELLOW_STAINED_GLASS_PANE;
        };
    }

    // ====== PAPER PULL MINIGAME ======
    // Click the moving target when it's in the center zone
    
    private void setupPaperPullMinigame(Inventory inv, JointRollingSession session) {
        // Create the "track" for the moving indicator
        int[] trackSlots = {19, 20, 21, 22, 23, 24, 25}; // 7 slots
        int centerSlot = 22;
        
        // Fill track with gray
        for (int slot : trackSlots) {
            if (slot == centerSlot) {
                // Green zone - target area
                inv.setItem(slot, createGuiItem(Material.LIME_STAINED_GLASS_PANE, 
                    "§a§l★ TARGET ZONE ★", 
                    Arrays.asList("", "§7Click when the paper is here!")));
            } else {
                inv.setItem(slot, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null));
            }
        }
        
        // Moving indicator (paper)
        int currentPos = clickerPosition.getOrDefault(session.getPlayerId(), 0);
        int actualSlot = trackSlots[Math.abs(currentPos) % trackSlots.length];
        inv.setItem(actualSlot, createGuiItem(Material.PAPER, 
            "§f§l✦ PAPER §f§l✦",
            Arrays.asList("", "§e>>> CLICK ME! <<<")));
        
        // Instructions
        inv.setItem(31, createGuiItem(Material.BOOK, "§e§lHow to Play",
            Arrays.asList(
                "",
                "§7Click the §fPAPER §7when it",
                "§7reaches the §aGREEN ZONE§7!",
                "",
                "§7Better timing = Higher score!",
                "",
                "§7Need: §a80+ §7to proceed"
            )));
        
        // Score display
        inv.setItem(40, createGuiItem(Material.NETHER_STAR, 
            "§e§lCurrent Score: §a" + session.getStageScore(),
            Arrays.asList("", getScoreDescription(session.getStageScore()))));
    }

    // ====== TOBACCO ROLL MINIGAME ======
    // Click rapidly to fill the progress bar
    
    private void setupTobaccoRollMinigame(Inventory inv, JointRollingSession session) {
        // Progress bar display
        int progressSlots = 7;
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        int filledSlots = (int) ((session.getMinigameProgress() / 100.0) * progressSlots);
        
        for (int i = 0; i < slots.length; i++) {
            if (i < filledSlots) {
                inv.setItem(slots[i], createGuiItem(Material.ORANGE_STAINED_GLASS_PANE, 
                    "§6█", null));
            } else {
                inv.setItem(slots[i], createGuiItem(Material.GRAY_STAINED_GLASS_PANE, 
                    "§7░", null));
            }
        }
        
        // Click target
        inv.setItem(31, createGuiItem(Material.DRIED_KELP_BLOCK, 
            "§6§l✦ ROLL TOBACCO ✦",
            Arrays.asList(
                "",
                "§e>>> CLICK RAPIDLY! <<<",
                "",
                createProgressBar(session.getMinigameProgress() / 100.0),
                "§7Progress: §e" + session.getMinigameProgress() + "/100"
            )));
        
        // Instructions
        inv.setItem(13, createGuiItem(Material.BOOK, "§e§lHow to Play",
            Arrays.asList(
                "",
                "§7Click the §6TOBACCO §7rapidly!",
                "§7Fill the bar before time runs out!",
                "",
                "§7Speed matters for bonus points!"
            )));
        
        // Timer (if session has started)
        if (session.isMinigameActive()) {
            long elapsed = System.currentTimeMillis() - session.getMinigameStartTime();
            long remaining = 5000 - elapsed; // 5 second timer
            inv.setItem(40, createGuiItem(Material.CLOCK, 
                "§c§lTime: " + String.format("%.1f", remaining / 1000.0) + "s",
                Arrays.asList("", "§7Hurry!")));
        }
    }

    // ====== GRIND MINIGAME ======
    // Click in a rotating pattern
    
    private void setupGrindMinigame(Inventory inv, JointRollingSession session) {
        // Grinder visual (center area)
        int[] grinderSlots = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
        int targetSlot = grinderSlots[session.getTargetProgress() % grinderSlots.length];
        
        for (int slot : grinderSlots) {
            if (slot == targetSlot) {
                inv.setItem(slot, createGuiItem(Material.LIME_CONCRETE, 
                    "§a§l★ CLICK HERE ★",
                    Arrays.asList("", "§e>>> GRIND! <<<")));
            } else if (slot == 31) {
                // Center - grinder
                inv.setItem(slot, createGuiItem(Material.CAULDRON, 
                    "§a§l✦ GRINDER ✦", null));
            } else {
                inv.setItem(slot, createGuiItem(Material.GREEN_STAINED_GLASS_PANE, " ", null));
            }
        }
        
        // Progress
        inv.setItem(13, createGuiItem(Material.LIME_DYE, 
            "§a§lGrinding Progress",
            Arrays.asList(
                "",
                createProgressBar(session.getMinigameProgress() / 100.0),
                "§7Progress: §e" + session.getMinigameProgress() + "/100",
                "",
                "§7Clicks: §a" + session.getCompletedStages() // using as click counter
            )));
        
        // Instructions
        inv.setItem(40, createGuiItem(Material.BOOK, "§e§lHow to Play",
            Arrays.asList(
                "",
                "§7Click the §agreen button§7!",
                "§7It moves around - follow it!",
                "",
                "§7Complete 10 clicks to finish!"
            )));
    }

    // ====== FINAL ROLL MINIGAME ======
    // Hold click in perfect zone
    
    private void setupFinalRollMinigame(Inventory inv, JointRollingSession session) {
        // Rolling animation visual
        int progress = session.getMinigameProgress();
        
        // Joint forming animation
        int[] jointSlots = {21, 22, 23};
        for (int i = 0; i < jointSlots.length; i++) {
            if (progress >= (i + 1) * 33) {
                inv.setItem(jointSlots[i], createGuiItem(Material.STICK, 
                    "§e§l✦", null));
            } else {
                inv.setItem(jointSlots[i], createGuiItem(Material.GRAY_DYE, 
                    "§7...", null));
            }
        }
        
        // Power meter
        int[] powerSlots = {28, 29, 30, 31, 32, 33, 34};
        int powerLevel = clickerPosition.getOrDefault(session.getPlayerId(), 0);
        int perfectZone = 3; // Middle slot
        
        for (int i = 0; i < powerSlots.length; i++) {
            Material mat;
            String name;
            if (i == perfectZone) {
                mat = Material.LIME_STAINED_GLASS_PANE;
                name = "§a§l★ PERFECT ★";
            } else if (i == perfectZone - 1 || i == perfectZone + 1) {
                mat = Material.YELLOW_STAINED_GLASS_PANE;
                name = "§e★ GOOD ★";
            } else {
                mat = Material.RED_STAINED_GLASS_PANE;
                name = "§c☆";
            }
            
            if (i == powerLevel) {
                inv.setItem(powerSlots[i], createGuiItem(Material.GOLD_BLOCK, 
                    "§6§l>>> POWER <<<", 
                    Arrays.asList("", "§7Click to lock in!")));
            } else {
                inv.setItem(powerSlots[i], createGuiItem(mat, name, null));
            }
        }
        
        // Main roll button
        inv.setItem(40, createGuiItem(Material.STICK, 
            "§e§l✦ ROLL JOINT ✦",
            Arrays.asList(
                "",
                "§7Click when power is in the",
                "§aGREEN §7zone for best results!",
                "",
                createProgressBar(progress / 100.0),
                "§7Progress: §e" + progress + "/100"
            )));
        
        // Score preview
        inv.setItem(13, createGuiItem(Material.NETHER_STAR, 
            "§e§lFinal Quality Preview",
            Arrays.asList(
                "",
                "§7Total Score: §a" + session.getTotalScore(),
                "§7Current Stage: §a" + session.getStageScore(),
                "",
                "§7Expected: " + session.calculateFinalRating().getDisplay()
            )));
    }

    /**
     * Handles clicks in the minigame GUI.
     */
    public void handleMinigameClick(Player player, int slot) {
        JointRollingSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        // Cancel button
        if (slot == 45) {
            cancelSession(player);
            return;
        }

        switch (session.getCurrentStage()) {
            case PAPER_PULL -> handlePaperPullClick(player, session, slot);
            case TOBACCO_ROLL -> handleTobaccoRollClick(player, session, slot);
            case GRIND -> handleGrindClick(player, session, slot);
            case FINAL_ROLL -> handleFinalRollClick(player, session, slot);
        }
    }

    private void handlePaperPullClick(Player player, JointRollingSession session, int slot) {
        int[] trackSlots = {19, 20, 21, 22, 23, 24, 25};
        int centerSlot = 22;
        
        // Check if clicked on the paper (current position)
        int currentPos = clickerPosition.getOrDefault(session.getPlayerId(), 0);
        int actualSlot = trackSlots[Math.abs(currentPos) % trackSlots.length];
        
        if (slot == actualSlot) {
            // Calculate score based on distance from center
            int distance = Math.abs(actualSlot - centerSlot);
            int score;
            if (distance == 0) {
                score = 100;
                player.sendMessage("§a§l★ PERFECT! §a+100");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            } else if (distance == 1) {
                score = 75;
                player.sendMessage("§e★ GREAT! §e+75");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
            } else if (distance == 2) {
                score = 50;
                player.sendMessage("§6★ GOOD! §6+50");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            } else {
                score = 25;
                player.sendMessage("§c☆ OK! §c+25");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
            
            session.setStageScore(score);
            completeStage(player, session);
        }
    }

    private void handleTobaccoRollClick(Player player, JointRollingSession session, int slot) {
        if (slot == 31) { // Tobacco click target
            if (!session.isMinigameActive()) {
                session.setMinigameActive(true);
                session.setTargetProgress(100);
            }
            
            session.addMinigameProgress(8 + ThreadLocalRandom.current().nextInt(5));
            player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_STEP, 0.3f, 1.5f);
            
            // Particles
            player.spawnParticle(Particle.FALLING_DUST, 
                player.getLocation().add(0, 1, 0), 
                3, 0.3, 0.2, 0.3, 
                Material.SAND.createBlockData());
            
            if (session.getMinigameProgress() >= 100) {
                long elapsed = System.currentTimeMillis() - session.getMinigameStartTime();
                int timeBonus = Math.max(0, (int) ((5000 - elapsed) / 50)); // Faster = more points
                int score = 70 + Math.min(30, timeBonus);
                session.setStageScore(score);
                
                player.sendMessage("§6§l✦ Tobacco Rolled! §e+" + score);
                completeStage(player, session);
            }
        }
    }

    private void handleGrindClick(Player player, JointRollingSession session, int slot) {
        int[] grinderSlots = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
        int targetSlot = grinderSlots[session.getTargetProgress() % grinderSlots.length];
        
        if (slot == targetSlot) {
            session.addMinigameProgress(10);
            session.setTargetProgress(session.getTargetProgress() + 1);
            
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.3f, 1.0f + (session.getMinigameProgress() / 100.0f));
            
            // Particles
            player.spawnParticle(Particle.VILLAGER_HAPPY, 
                player.getLocation().add(0, 1.5, 0), 
                5, 0.3, 0.2, 0.3, 0);
            
            if (session.getMinigameProgress() >= 100) {
                int score = 80 + ThreadLocalRandom.current().nextInt(21); // 80-100
                session.setStageScore(score);
                
                player.sendMessage("§a§l✦ Grinding Complete! §e+" + score);
                completeStage(player, session);
            }
            
            // Refresh display
            Inventory inv = player.getOpenInventory().getTopInventory();
            if (inv.getHolder() instanceof JointRollingManager) {
                updateMinigameGUI(inv, player, session);
            }
        } else {
            // Wrong slot clicked
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.5f);
        }
    }

    private void handleFinalRollClick(Player player, JointRollingSession session, int slot) {
        if (slot == 40) { // Main roll button
            int[] powerSlots = {28, 29, 30, 31, 32, 33, 34};
            int powerLevel = clickerPosition.getOrDefault(session.getPlayerId(), 0);
            int perfectZone = 3;
            
            int distance = Math.abs(powerLevel - perfectZone);
            int points;
            if (distance == 0) {
                points = 35;
                player.sendMessage("§a§l★ PERFECT ROLL! ★");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.8f);
            } else if (distance == 1) {
                points = 25;
                player.sendMessage("§e★ Great roll!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.4f);
            } else {
                points = 15;
                player.sendMessage("§6☆ Okay roll");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            }
            
            session.addMinigameProgress(points);
            session.addStageScore(points);
            
            if (session.getMinigameProgress() >= 100) {
                player.sendMessage("§e§l✦ Joint Rolled! §a+" + session.getStageScore());
                completeSession(player, session);
            } else {
                // Refresh display
                Inventory inv = player.getOpenInventory().getTopInventory();
                if (inv.getHolder() instanceof JointRollingManager) {
                    updateMinigameGUI(inv, player, session);
                }
            }
        }
    }

    private void completeStage(Player player, JointRollingSession session) {
        boolean allComplete = session.completeStage();
        
        // Stop current task
        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        clickerPosition.remove(player.getUniqueId());
        
        if (allComplete) {
            completeSession(player, session);
        } else {
            // Mark as transitioning so inventory close doesn't cleanup the session
            transitioning.add(player.getUniqueId());
            
            // Open next stage
            player.sendMessage("");
            player.sendMessage("§a✦ Stage Complete! Moving to: " + session.getCurrentStage().getDisplayName());
            player.sendMessage("");
            
            // Brief delay before next stage
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                transitioning.remove(player.getUniqueId());
                if (activeSessions.containsKey(player.getUniqueId())) {
                    openMinigameGUI(player, session);
                }
            }, 20L);
        }
    }

    private void completeSession(Player player, JointRollingSession session) {
        // Calculate final rating
        StarRating finalRating = session.calculateFinalRating();
        
        // Create joint item
        ItemStack joint = JointItems.createJoint(
            session.getStrainId(),
            session.getStrainName(),
            finalRating,
            session.getPotency(),
            1
        );
        
        // Give to player
        player.getInventory().addItem(joint);
        
        // Cleanup
        cleanupSession(player);
        player.closeInventory();
        
        // Success effects
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        player.spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 
            30, 0.5, 0.5, 0.5, 0.1);
        
        player.sendMessage("");
        player.sendMessage("§a§l╔══════════════════════════════╗");
        player.sendMessage("§a§l║  §e§l✦ JOINT ROLLED! ✦  §a§l         ║");
        player.sendMessage("§a§l╠══════════════════════════════╣");
        player.sendMessage("§a§l║ §7Strain: §f" + session.getStrainName());
        player.sendMessage("§a§l║ §7Quality: " + finalRating.getDisplay());
        player.sendMessage("§a§l║ §7Potency: §e" + session.getPotency() + "%");
        player.sendMessage("§a§l║ §7Total Score: §a" + session.getTotalScore());
        player.sendMessage("§a§l╚══════════════════════════════╝");
        player.sendMessage("");
    }

    public void cancelSession(Player player) {
        player.sendMessage("§cJoint rolling cancelled. Materials lost!");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
        cleanupSession(player);
        player.closeInventory();
    }
    
    /**
     * Handles when a player closes the rolling GUI (either manually or due to death, etc.)
     * This ensures the session is properly cleaned up so the player can roll again.
     */
    public void handleInventoryClose(Player player) {
        // Don't cleanup if player is transitioning between stages
        if (transitioning.contains(player.getUniqueId())) {
            return;
        }
        if (activeSessions.containsKey(player.getUniqueId())) {
            forceCleanup(player.getUniqueId());
        }
    }
    
    /**
     * Forces cleanup of a rolling session for a player (e.g., on death or disconnect).
     * This method does not send messages or close inventory, just cleans up data.
     */
    public void forceCleanup(UUID playerId) {
        transitioning.remove(playerId);
        activeSessions.remove(playerId);
        BukkitTask task = activeTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        clickerPosition.remove(playerId);
    }

    private void cleanupSession(Player player) {
        forceCleanup(player.getUniqueId());
    }

    /**
     * Starts the animation task for the current minigame.
     */
    private void startMinigameTask(Player player, JointRollingSession session, Inventory inv) {
        BukkitTask existingTask = activeTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        clickerPosition.put(player.getUniqueId(), 0);
        
        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            int direction = 1;
            
            @Override
            public void run() {
                if (!activeSessions.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }
                
                JointRollingSession sess = activeSessions.get(player.getUniqueId());
                if (sess == null || !player.isOnline()) {
                    cancel();
                    cleanupSession(player);
                    return;
                }
                
                // Update position based on stage
                int pos = clickerPosition.getOrDefault(player.getUniqueId(), 0);
                
                switch (sess.getCurrentStage()) {
                    case PAPER_PULL -> {
                        // Oscillate back and forth
                        if (tick % 4 == 0) {
                            pos += direction;
                            if (pos >= 6 || pos <= 0) direction *= -1;
                            clickerPosition.put(player.getUniqueId(), pos);
                        }
                    }
                    case TOBACCO_ROLL -> {
                        // Check timeout
                        if (sess.isMinigameActive()) {
                            long elapsed = System.currentTimeMillis() - sess.getMinigameStartTime();
                            if (elapsed > 5000) {
                                // Time's up!
                                int score = Math.min(100, sess.getMinigameProgress());
                                sess.setStageScore(score);
                                player.sendMessage("§c§lTime's up! §7Score: " + score);
                                completeStage(player, sess);
                                cancel();
                                return;
                            }
                        }
                    }
                    case FINAL_ROLL -> {
                        // Oscillate power meter
                        if (tick % 3 == 0) {
                            pos += direction;
                            if (pos >= 6 || pos <= 0) direction *= -1;
                            clickerPosition.put(player.getUniqueId(), pos);
                        }
                    }
                    default -> {}
                }
                
                // Refresh GUI
                Inventory currentInv = player.getOpenInventory().getTopInventory();
                if (currentInv.getHolder() instanceof JointRollingManager) {
                    updateMinigameGUI(currentInv, player, sess);
                }
                
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        activeTasks.put(player.getUniqueId(), task);
    }

    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public JointRollingSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    // ====== UTILITY METHODS ======

    private ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String createProgressBar(double progress) {
        StringBuilder bar = new StringBuilder("§8[");
        int filled = (int) (progress * 20);
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                if (i < 6) bar.append("§c");
                else if (i < 13) bar.append("§e");
                else bar.append("§a");
                bar.append("█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }

    private String getScoreDescription(int score) {
        if (score >= 90) return "§a★★★★★ LEGENDARY!";
        if (score >= 75) return "§9★★★★☆ Excellent!";
        if (score >= 60) return "§e★★★☆☆ Good!";
        if (score >= 40) return "§6★★☆☆☆ Okay";
        return "§c★☆☆☆☆ Keep trying...";
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
