package com.budlords.challenges;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
import com.budlords.progression.Rank;
import com.budlords.stats.PlayerStats;
import com.budlords.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages daily and weekly challenges for players.
 * Provides engaging goals and rewards for regular play.
 * Challenges are scaled based on player rank.
 */
public class ChallengeManager implements InventoryHolder {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final StatsManager statsManager;
    
    // Active challenges
    private final List<Challenge> dailyChallenges;
    private final List<Challenge> weeklyChallenges;
    
    // Player progress tracking
    private final Map<UUID, Map<String, Challenge.PlayerChallengeProgress>> playerProgress;
    
    // Refresh task
    private BukkitTask refreshTask;
    private long lastDailyRefresh;
    private long lastWeeklyRefresh;

    // Challenge templates
    private static final List<ChallengeTemplate> CHALLENGE_TEMPLATES = Arrays.asList(
        // Easy challenges
        new ChallengeTemplate("harvest_5", "First Harvest", "Harvest 5 plants", 
            Challenge.ChallengeType.HARVEST_PLANTS, Challenge.ChallengeDifficulty.EASY, 5, 100, 10),
        new ChallengeTemplate("sell_3", "Small Time Dealer", "Sell 3 products", 
            Challenge.ChallengeType.SELL_PRODUCTS, Challenge.ChallengeDifficulty.EASY, 3, 150, 15),
        new ChallengeTemplate("water_10", "Hydration Station", "Water 10 plants", 
            Challenge.ChallengeType.WATER_PLANTS, Challenge.ChallengeDifficulty.EASY, 10, 80, 8),
        
        // Medium challenges
        new ChallengeTemplate("harvest_15", "Green Thumb", "Harvest 15 plants", 
            Challenge.ChallengeType.HARVEST_PLANTS, Challenge.ChallengeDifficulty.MEDIUM, 15, 300, 30),
        new ChallengeTemplate("roll_5", "Rolling Master", "Roll 5 joints", 
            Challenge.ChallengeType.ROLL_JOINTS, Challenge.ChallengeDifficulty.MEDIUM, 5, 400, 40),
        new ChallengeTemplate("earn_1000", "Money Maker", "Earn $1,000", 
            Challenge.ChallengeType.EARN_MONEY, Challenge.ChallengeDifficulty.MEDIUM, 1000, 200, 25),
        new ChallengeTemplate("fertilize_5", "Nutrient Provider", "Use fertilizer 5 times", 
            Challenge.ChallengeType.USE_FERTILIZER, Challenge.ChallengeDifficulty.MEDIUM, 5, 250, 25),
        
        // Hard challenges
        new ChallengeTemplate("perfect_3", "Perfectionist", "Get 3 perfect harvests", 
            Challenge.ChallengeType.PERFECT_HARVESTS, Challenge.ChallengeDifficulty.HARD, 3, 750, 75),
        new ChallengeTemplate("fivestar_5", "Quality Control", "Harvest 5 five-star buds", 
            Challenge.ChallengeType.FIVE_STAR_BUDS, Challenge.ChallengeDifficulty.HARD, 5, 1000, 100),
        new ChallengeTemplate("trades_10", "Business Mogul", "Complete 10 successful trades", 
            Challenge.ChallengeType.SUCCESSFUL_TRADES, Challenge.ChallengeDifficulty.HARD, 10, 800, 80),
        new ChallengeTemplate("earn_5000", "Big Earner", "Earn $5,000", 
            Challenge.ChallengeType.EARN_MONEY, Challenge.ChallengeDifficulty.HARD, 5000, 500, 50),
        
        // Legendary challenges
        new ChallengeTemplate("legendary_3", "Legend Hunter", "Harvest 3 legendary strains", 
            Challenge.ChallengeType.LEGENDARY_HARVESTS, Challenge.ChallengeDifficulty.LEGENDARY, 3, 2500, 250),
        new ChallengeTemplate("crossbreed_1", "Mad Scientist", "Crossbreed a new strain", 
            Challenge.ChallengeType.CROSSBREED_STRAINS, Challenge.ChallengeDifficulty.LEGENDARY, 1, 3000, 300),
        new ChallengeTemplate("perfect_10", "Master Grower", "Get 10 perfect harvests", 
            Challenge.ChallengeType.PERFECT_HARVESTS, Challenge.ChallengeDifficulty.LEGENDARY, 10, 5000, 500)
    );

    public ChallengeManager(BudLords plugin, EconomyManager economyManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.statsManager = statsManager;
        this.dailyChallenges = new ArrayList<>();
        this.weeklyChallenges = new ArrayList<>();
        this.playerProgress = new ConcurrentHashMap<>();
        
        // Initialize with current challenges
        refreshDailyChallenges();
        refreshWeeklyChallenges();
        
        // Start refresh task
        startRefreshTask();
    }

    private void startRefreshTask() {
        // Check every hour for challenge refresh
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            
            // Daily refresh (24 hours)
            if (now - lastDailyRefresh > 24 * 60 * 60 * 1000) {
                refreshDailyChallenges();
            }
            
            // Weekly refresh (7 days)
            if (now - lastWeeklyRefresh > 7 * 24 * 60 * 60 * 1000) {
                refreshWeeklyChallenges();
            }
        }, 20L * 60 * 60, 20L * 60 * 60); // Every hour
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
    }

    private void refreshDailyChallenges() {
        dailyChallenges.clear();
        lastDailyRefresh = System.currentTimeMillis();
        long expiration = lastDailyRefresh + 24 * 60 * 60 * 1000; // 24 hours
        
        // Pick 3 random challenges of varying difficulty
        List<ChallengeTemplate> available = new ArrayList<>(CHALLENGE_TEMPLATES);
        Collections.shuffle(available);
        
        int easy = 0, medium = 0, hard = 0;
        for (ChallengeTemplate template : available) {
            if (dailyChallenges.size() >= 3) break;
            
            // Ensure variety
            if (template.difficulty == Challenge.ChallengeDifficulty.EASY && easy < 1) {
                dailyChallenges.add(template.toChallenge("daily_" + template.id, expiration));
                easy++;
            } else if (template.difficulty == Challenge.ChallengeDifficulty.MEDIUM && medium < 1) {
                dailyChallenges.add(template.toChallenge("daily_" + template.id, expiration));
                medium++;
            } else if (template.difficulty == Challenge.ChallengeDifficulty.HARD && hard < 1) {
                dailyChallenges.add(template.toChallenge("daily_" + template.id, expiration));
                hard++;
            }
        }
        
        // Clear player progress for old challenges
        for (Map<String, Challenge.PlayerChallengeProgress> progress : playerProgress.values()) {
            progress.keySet().removeIf(key -> key.startsWith("daily_"));
        }
        
        plugin.getLogger().info("Refreshed " + dailyChallenges.size() + " daily challenges");
    }

    private void refreshWeeklyChallenges() {
        weeklyChallenges.clear();
        lastWeeklyRefresh = System.currentTimeMillis();
        long expiration = lastWeeklyRefresh + 7 * 24 * 60 * 60 * 1000; // 7 days
        
        // Pick 3 harder challenges for weekly
        List<ChallengeTemplate> available = new ArrayList<>(CHALLENGE_TEMPLATES);
        Collections.shuffle(available);
        
        int count = 0;
        for (ChallengeTemplate template : available) {
            if (count >= 3) break;
            
            // Prefer harder challenges for weekly
            if (template.difficulty == Challenge.ChallengeDifficulty.HARD || 
                template.difficulty == Challenge.ChallengeDifficulty.LEGENDARY) {
                // Scale up targets for weekly
                ChallengeTemplate scaled = new ChallengeTemplate(
                    template.id, template.name, template.description,
                    template.type, template.difficulty,
                    template.target * 3, // Triple the target
                    template.rewardMoney * 5, // 5x rewards
                    template.rewardXP * 5
                );
                weeklyChallenges.add(scaled.toChallenge("weekly_" + template.id, expiration));
                count++;
            }
        }
        
        // Clear player progress for old weekly challenges
        for (Map<String, Challenge.PlayerChallengeProgress> progress : playerProgress.values()) {
            progress.keySet().removeIf(key -> key.startsWith("weekly_"));
        }
        
        plugin.getLogger().info("Refreshed " + weeklyChallenges.size() + " weekly challenges");
    }

    /**
     * Opens the challenges GUI for a player.
     */
    @SuppressWarnings("deprecation")
    public void openChallengesGUI(Player player) {
        Inventory inv = Bukkit.createInventory(this, 54, "§e§l✦ Daily & Weekly Challenges ✦");
        
        // Border
        ItemStack borderYellow = createItem(Material.YELLOW_STAINED_GLASS_PANE, " ", null);
        ItemStack borderOrange = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderYellow : borderOrange);
            inv.setItem(45 + i, i % 2 == 0 ? borderYellow : borderOrange);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, borderYellow);
            inv.setItem(i + 8, borderYellow);
        }
        
        // Daily challenges header
        inv.setItem(11, createItem(Material.SUNFLOWER, 
            "§e§l☀ Daily Challenges",
            Arrays.asList(
                "",
                "§7Reset every 24 hours",
                "§7Time remaining: " + formatTimeRemaining(lastDailyRefresh + 24 * 60 * 60 * 1000),
                ""
            )));
        
        // Display daily challenges
        int dailySlot = 19;
        for (Challenge challenge : dailyChallenges) {
            inv.setItem(dailySlot, createChallengeItem(player, challenge));
            dailySlot += 2;
        }
        
        // Weekly challenges header
        inv.setItem(15, createItem(Material.CLOCK, 
            "§6§l⌚ Weekly Challenges",
            Arrays.asList(
                "",
                "§7Reset every 7 days",
                "§7Time remaining: " + formatTimeRemaining(lastWeeklyRefresh + 7 * 24 * 60 * 60 * 1000),
                ""
            )));
        
        // Display weekly challenges
        int weeklySlot = 29;
        for (Challenge challenge : weeklyChallenges) {
            inv.setItem(weeklySlot, createChallengeItem(player, challenge));
            weeklySlot += 2;
        }
        
        // Stats summary
        PlayerStats stats = statsManager.getStats(player);
        inv.setItem(40, createItem(Material.EMERALD, 
            "§a§l★ Your Progress",
            Arrays.asList(
                "",
                "§7Daily Challenges Completed: §e" + stats.getDailyChallengesCompleted(),
                "§7Weekly Challenges Completed: §e" + stats.getWeeklyChallengesCompleted(),
                "§7Total Challenges: §a" + stats.getChallengesCompleted(),
                ""
            )));
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
    }
    
    /**
     * Gets the rank-based multiplier for scaling challenge targets.
     * Higher rank players get higher challenge targets but also higher rewards.
     */
    private double getRankScaleMultiplier(Player player) {
        if (plugin.getRankManager() == null) return 1.0;
        
        Rank rank = plugin.getRankManager().getRank(player);
        if (rank == null) return 1.0;
        
        // Scale based on rank name (or could use required earnings)
        String rankName = rank.name();
        return switch (rankName) {
            case "Novice" -> 0.5;      // Easier challenges for new players
            case "Dealer" -> 0.75;
            case "Supplier" -> 1.0;
            case "Distributor" -> 1.25;
            case "Kingpin" -> 1.5;
            case "Cartel Boss" -> 1.75;
            case "BudLord" -> 2.0;     // Harder challenges for top players
            default -> 1.0;
        };
    }
    
    /**
     * Gets a player-scaled version of a challenge.
     * Lower rank players get easier versions, higher rank players get harder versions with better rewards.
     */
    private Challenge getScaledChallenge(Player player, Challenge baseChallenge) {
        double scale = getRankScaleMultiplier(player);
        
        // Scale target and rewards
        int scaledTarget = Math.max(1, (int)(baseChallenge.getTargetAmount() * scale));
        double scaledReward = baseChallenge.getRewardMoney() * scale;
        int scaledXP = (int)(baseChallenge.getRewardXP() * scale);
        
        return new Challenge(
            baseChallenge.getId(),
            baseChallenge.getName(),
            baseChallenge.getDescription(),
            baseChallenge.getType(),
            baseChallenge.getDifficulty(),
            scaledTarget,
            scaledReward,
            scaledXP,
            baseChallenge.getRequiredStrain(),
            baseChallenge.getExpiration()
        );
    }

    private ItemStack createChallengeItem(Player player, Challenge challenge) {
        // Get rank-scaled version of the challenge
        Challenge scaledChallenge = getScaledChallenge(player, challenge);
        Challenge.PlayerChallengeProgress progress = getProgress(player.getUniqueId(), challenge.getId());
        
        Material material;
        String status;
        if (progress.isClaimed()) {
            material = Material.LIME_WOOL;
            status = "§a✓ CLAIMED";
        } else if (progress.getCurrentProgress() >= scaledChallenge.getTargetAmount()) {
            // Use scaled target for completion check
            progress.setCompleted(true);
            material = Material.GOLD_BLOCK;
            status = "§e★ CLICK TO CLAIM";
        } else if (progress.isCompleted()) {
            material = Material.GOLD_BLOCK;
            status = "§e★ CLICK TO CLAIM";
        } else {
            material = Material.PAPER;
            status = "§7In Progress";
        }
        
        int progressPercent = (int) ((double) progress.getCurrentProgress() / scaledChallenge.getTargetAmount() * 100);
        progressPercent = Math.min(100, progressPercent);
        
        // Show rank scaling info
        double scale = getRankScaleMultiplier(player);
        String rankInfo = scale != 1.0 ? 
            (scale < 1.0 ? "§a(Scaled easier)" : "§6(Scaled harder)") : "";
        
        return createItem(material, scaledChallenge.getDisplayName(), Arrays.asList(
            "",
            "§7" + scaledChallenge.getDescription(),
            "",
            "§7Progress: " + createProgressBar(progressPercent / 100.0),
            "§7" + progress.getCurrentProgress() + "/" + scaledChallenge.getTargetAmount(),
            "",
            "§7Difficulty: " + scaledChallenge.getDifficulty().getDisplay() + " " + rankInfo,
            "§7Rewards: §e$" + String.format("%,.0f", scaledChallenge.getRewardMoney()) + 
                " §7+ §a" + scaledChallenge.getRewardXP() + " XP",
            "",
            status
        ));
    }

    /**
     * Handles clicking a challenge to claim rewards.
     */
    public void handleChallengeClick(Player player, int slot) {
        Challenge clickedChallenge = null;
        
        // Find which challenge was clicked
        int dailySlot = 19;
        for (Challenge c : dailyChallenges) {
            if (slot == dailySlot) {
                clickedChallenge = c;
                break;
            }
            dailySlot += 2;
        }
        
        if (clickedChallenge == null) {
            int weeklySlot = 29;
            for (Challenge c : weeklyChallenges) {
                if (slot == weeklySlot) {
                    clickedChallenge = c;
                    break;
                }
                weeklySlot += 2;
            }
        }
        
        if (clickedChallenge == null) return;
        
        // Get rank-scaled version for rewards
        Challenge scaledChallenge = getScaledChallenge(player, clickedChallenge);
        Challenge.PlayerChallengeProgress progress = getProgress(player.getUniqueId(), clickedChallenge.getId());
        
        if (progress.isClaimed()) {
            player.sendMessage("§cYou've already claimed this reward!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        
        // Check completion with scaled target
        if (progress.getCurrentProgress() < scaledChallenge.getTargetAmount()) {
            player.sendMessage("§cComplete the challenge first!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        
        // Claim reward with scaled rewards
        progress.setClaimed(true);
        progress.setCompleted(true);
        economyManager.addBalance(player, scaledChallenge.getRewardMoney());
        
        // Update stats
        PlayerStats stats = statsManager.getStats(player);
        if (clickedChallenge.getId().startsWith("daily_")) {
            stats.incrementDailyChallenges();
        } else {
            stats.incrementWeeklyChallenges();
        }
        
        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        player.spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 
            20, 0.5, 0.5, 0.5, 0);
        
        player.sendMessage("");
        player.sendMessage("§a§l✓ Challenge Complete!");
        player.sendMessage("§7Received: §e$" + String.format("%,.0f", scaledChallenge.getRewardMoney()) + 
            " §7+ §a" + scaledChallenge.getRewardXP() + " XP");
        player.sendMessage("");
        
        // Refresh GUI
        openChallengesGUI(player);
    }

    /**
     * Updates progress for a player on a specific challenge type.
     */
    public void updateProgress(Player player, Challenge.ChallengeType type, int amount) {
        UUID playerId = player.getUniqueId();
        
        // Check all active challenges
        for (Challenge challenge : dailyChallenges) {
            if (challenge.getType() == type) {
                updateChallengeProgress(playerId, challenge, amount);
            }
        }
        
        for (Challenge challenge : weeklyChallenges) {
            if (challenge.getType() == type) {
                updateChallengeProgress(playerId, challenge, amount);
            }
        }
    }

    private void updateChallengeProgress(UUID playerId, Challenge challenge, int amount) {
        Challenge.PlayerChallengeProgress progress = getProgress(playerId, challenge.getId());
        
        if (progress.isCompleted()) return;
        
        progress.addProgress(amount);
        
        if (progress.getCurrentProgress() >= challenge.getTargetAmount()) {
            progress.setCompleted(true);
            
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage("§a§l✓ Challenge Completed: §e" + challenge.getName());
                player.sendMessage("§7Use §e/challenges §7to claim your reward!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            }
        }
    }

    private Challenge.PlayerChallengeProgress getProgress(UUID playerId, String challengeId) {
        Map<String, Challenge.PlayerChallengeProgress> playerProgressMap = 
            playerProgress.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        return playerProgressMap.computeIfAbsent(challengeId, 
            id -> new Challenge.PlayerChallengeProgress(playerId, id));
    }

    private String formatTimeRemaining(long endTime) {
        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) return "§cExpired";
        
        long hours = remaining / (1000 * 60 * 60);
        long minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 24) {
            return "§e" + (hours / 24) + "d " + (hours % 24) + "h";
        }
        return "§e" + hours + "h " + minutes + "m";
    }

    private String createProgressBar(double progress) {
        StringBuilder bar = new StringBuilder("§8[");
        int filled = (int) (progress * 10);
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
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

    @Override
    public Inventory getInventory() {
        return null;
    }

    /**
     * Template for generating challenges.
     */
    private static class ChallengeTemplate {
        final String id;
        final String name;
        final String description;
        final Challenge.ChallengeType type;
        final Challenge.ChallengeDifficulty difficulty;
        final int target;
        final double rewardMoney;
        final int rewardXP;

        ChallengeTemplate(String id, String name, String description, 
                          Challenge.ChallengeType type, Challenge.ChallengeDifficulty difficulty,
                          int target, double rewardMoney, int rewardXP) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.difficulty = difficulty;
            this.target = target;
            this.rewardMoney = rewardMoney;
            this.rewardXP = rewardXP;
        }

        Challenge toChallenge(String fullId, long expiration) {
            return new Challenge(fullId, name, description, type, difficulty, 
                target, rewardMoney * difficulty.getRewardMultiplier(), rewardXP, null, expiration);
        }
    }
}
