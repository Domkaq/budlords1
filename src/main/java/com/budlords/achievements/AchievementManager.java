package com.budlords.achievements;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
import com.budlords.stats.PlayerStats;
import com.budlords.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player achievements in BudLords v2.0.0.
 */
public class AchievementManager implements InventoryHolder {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final StatsManager statsManager;
    
    // Player achievements: UUID -> Set of unlocked achievement names
    private final Map<UUID, Set<String>> playerAchievements;
    
    // Achievement progress tracking: UUID -> (Achievement -> progress)
    private final Map<UUID, Map<String, Integer>> achievementProgress;
    
    // Data file
    private File achievementsFile;
    private FileConfiguration achievementsConfig;

    public AchievementManager(BudLords plugin, EconomyManager economyManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.statsManager = statsManager;
        this.playerAchievements = new ConcurrentHashMap<>();
        this.achievementProgress = new ConcurrentHashMap<>();
        
        loadAchievements();
        
        plugin.getLogger().info("✦ Achievement System initialized with " + Achievement.values().length + " achievements");
    }

    private void loadAchievements() {
        achievementsFile = new File(plugin.getDataFolder(), "achievements.yml");
        if (!achievementsFile.exists()) {
            try {
                achievementsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create achievements file: " + e.getMessage());
            }
        }
        achievementsConfig = YamlConfiguration.loadConfiguration(achievementsFile);
        
        ConfigurationSection playersSection = achievementsConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> unlocked = playersSection.getStringList(uuidStr + ".unlocked");
                    playerAchievements.put(uuid, new HashSet<>(unlocked));
                    
                    ConfigurationSection progressSection = playersSection.getConfigurationSection(uuidStr + ".progress");
                    if (progressSection != null) {
                        Map<String, Integer> progress = new HashMap<>();
                        for (String achievementName : progressSection.getKeys(false)) {
                            progress.put(achievementName, progressSection.getInt(achievementName));
                        }
                        achievementProgress.put(uuid, progress);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load achievements for " + uuidStr);
                }
            }
        }
    }

    public void saveAchievements() {
        achievementsConfig.set("players", null);
        
        for (Map.Entry<UUID, Set<String>> entry : playerAchievements.entrySet()) {
            String path = "players." + entry.getKey().toString();
            achievementsConfig.set(path + ".unlocked", new ArrayList<>(entry.getValue()));
            
            Map<String, Integer> progress = achievementProgress.get(entry.getKey());
            if (progress != null) {
                for (Map.Entry<String, Integer> progressEntry : progress.entrySet()) {
                    achievementsConfig.set(path + ".progress." + progressEntry.getKey(), progressEntry.getValue());
                }
            }
        }
        
        try {
            achievementsConfig.save(achievementsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save achievements: " + e.getMessage());
        }
    }

    /**
     * Updates progress for a specific achievement type.
     */
    public void updateProgress(Player player, Achievement achievement, int amount) {
        UUID uuid = player.getUniqueId();
        
        // Check if already unlocked
        Set<String> unlocked = playerAchievements.computeIfAbsent(uuid, k -> new HashSet<>());
        if (unlocked.contains(achievement.name())) return;
        
        // Update progress
        Map<String, Integer> progress = achievementProgress.computeIfAbsent(uuid, k -> new HashMap<>());
        int current = progress.getOrDefault(achievement.name(), 0);
        current += amount;
        progress.put(achievement.name(), current);
        
        // Check if completed
        if (current >= achievement.getRequirement()) {
            unlockAchievement(player, achievement);
        }
    }

    /**
     * Directly sets progress (for achievements that track totals from stats).
     */
    public void setProgress(Player player, Achievement achievement, int amount) {
        UUID uuid = player.getUniqueId();
        
        Set<String> unlocked = playerAchievements.computeIfAbsent(uuid, k -> new HashSet<>());
        if (unlocked.contains(achievement.name())) return;
        
        Map<String, Integer> progress = achievementProgress.computeIfAbsent(uuid, k -> new HashMap<>());
        progress.put(achievement.name(), amount);
        
        if (amount >= achievement.getRequirement()) {
            unlockAchievement(player, achievement);
        }
    }

    /**
     * Unlocks an achievement for a player.
     */
    public void unlockAchievement(Player player, Achievement achievement) {
        UUID uuid = player.getUniqueId();
        Set<String> unlocked = playerAchievements.computeIfAbsent(uuid, k -> new HashSet<>());
        
        if (unlocked.contains(achievement.name())) return;
        
        unlocked.add(achievement.name());
        
        // Give rewards
        economyManager.addBalance(player, achievement.getRewardMoney());
        
        // Update stats
        if (statsManager != null) {
            PlayerStats stats = statsManager.getStats(player);
            stats.incrementAchievements();
        }
        
        // Send notification
        sendAchievementNotification(player, achievement);
        
        // Check for Completionist achievement
        checkCompletionist(player);
        
        // Save
        saveAchievements();
    }

    private void sendAchievementNotification(Player player, Achievement achievement) {
        player.sendMessage("");
        player.sendMessage("§8§l═══════════════════════════════════════════");
        player.sendMessage("§6§l   ✦ ACHIEVEMENT UNLOCKED! ✦");
        player.sendMessage("");
        player.sendMessage("   " + achievement.getColoredDisplay());
        player.sendMessage("   §7" + achievement.getDescription());
        player.sendMessage("");
        player.sendMessage("   §7Rewards: §e$" + String.format("%,.0f", achievement.getRewardMoney()) + 
                          " §7+ §a" + achievement.getRewardXP() + " XP");
        player.sendMessage("§8§l═══════════════════════════════════════════");
        player.sendMessage("");
        
        // Play sound
        player.playSound(player.getLocation(), achievement.getUnlockSound(), 1.0f, 1.0f);
        
        // Spawn particles based on rarity
        int particleCount = switch (achievement.getRarity()) {
            case LEGENDARY -> 100;
            case EPIC -> 60;
            case RARE -> 40;
            case UNCOMMON -> 25;
            default -> 15;
        };
        
        player.spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 
            particleCount, 0.5, 1, 0.5, 0.1);
        
        // Broadcast for rare+ achievements
        if (achievement.getRarity().ordinal() >= Achievement.AchievementRarity.RARE.ordinal()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) {
                    p.sendMessage("§6★ §e" + player.getName() + " §7unlocked §6" + achievement.getDisplayName() + "§7!");
                }
            }
        }
    }

    /**
     * Checks if player has unlocked all achievements (for Completionist).
     */
    private void checkCompletionist(Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> unlocked = playerAchievements.get(uuid);
        if (unlocked == null) return;
        
        // Count all non-completionist achievements
        int total = 0;
        for (Achievement a : Achievement.values()) {
            if (a != Achievement.COMPLETIONIST && a != Achievement.ULTIMATE_BUDLORD) {
                total++;
            }
        }
        
        // Check if all unlocked (excluding completionist and ultimate)
        int unlockedCount = 0;
        for (Achievement a : Achievement.values()) {
            if (a != Achievement.COMPLETIONIST && a != Achievement.ULTIMATE_BUDLORD) {
                if (unlocked.contains(a.name())) {
                    unlockedCount++;
                }
            }
        }
        
        if (unlockedCount >= total && !unlocked.contains(Achievement.COMPLETIONIST.name())) {
            unlockAchievement(player, Achievement.COMPLETIONIST);
        }
    }

    /**
     * Syncs achievement progress with player stats.
     */
    public void syncWithStats(Player player) {
        PlayerStats stats = statsManager.getStats(player);
        if (stats == null) return;
        
        // Farming achievements
        setProgress(player, Achievement.FIRST_HARVEST, stats.getPlantsHarvested());
        setProgress(player, Achievement.GREEN_THUMB, stats.getPlantsHarvested());
        setProgress(player, Achievement.MASTER_GARDENER, stats.getPlantsHarvested());
        setProgress(player, Achievement.LEGENDARY_GROWER, stats.getPlantsHarvested());
        setProgress(player, Achievement.PERFECT_HARVEST, stats.getFiveStarHarvests());
        setProgress(player, Achievement.QUALITY_CONTROL, stats.getFiveStarHarvests());
        setProgress(player, Achievement.STAR_COLLECTOR, stats.getFiveStarHarvests());
        
        // Economy achievements
        setProgress(player, Achievement.FIRST_SALE, stats.getSuccessfulTrades());
        setProgress(player, Achievement.ENTREPRENEUR, (int) stats.getTotalEarnings());
        setProgress(player, Achievement.BUSINESSMAN, (int) stats.getTotalEarnings());
        setProgress(player, Achievement.MILLIONAIRE, (int) stats.getTotalEarnings());
        
        // Trading achievements
        setProgress(player, Achievement.TRADER, stats.getSuccessfulTrades());
        setProgress(player, Achievement.DEALER, stats.getSuccessfulTrades());
        
        // Rolling achievements
        setProgress(player, Achievement.FIRST_ROLL, stats.getJointsRolled());
        setProgress(player, Achievement.ROLLER, stats.getJointsRolled());
        setProgress(player, Achievement.MASTER_ROLLER, stats.getJointsRolled());
        
        // Challenge achievements
        int totalChallenges = stats.getDailyChallengesCompleted() + stats.getWeeklyChallengesCompleted();
        setProgress(player, Achievement.CHALLENGER, stats.getDailyChallengesCompleted());
        setProgress(player, Achievement.DAILY_DEVOTEE, stats.getDailyChallengesCompleted());
        setProgress(player, Achievement.WEEKLY_WARRIOR, stats.getWeeklyChallengesCompleted());
        setProgress(player, Achievement.CHALLENGE_MASTER, totalChallenges);
        
        // Prestige achievements
        setProgress(player, Achievement.FIRST_PRESTIGE, stats.getPrestigeLevel());
        setProgress(player, Achievement.HIGH_PRESTIGE, stats.getPrestigeLevel());
        setProgress(player, Achievement.MAX_PRESTIGE, stats.getPrestigeLevel());
        
        // Crossbreeding achievements
        setProgress(player, Achievement.STRAIN_DISCOVERER, stats.getCrossbreeds());
        setProgress(player, Achievement.CROSSBREEDER, stats.getCrossbreeds());
        setProgress(player, Achievement.GENETICIST, stats.getCrossbreeds());
        setProgress(player, Achievement.MUTATION_HUNTER, stats.getMutations());
        setProgress(player, Achievement.RARE_BREEDER, stats.getMutations());
        
        // Disease achievements
        setProgress(player, Achievement.DISEASE_DOCTOR, stats.getDiseasesCured());
    }

    /**
     * Opens the achievements GUI for a player.
     */
    @SuppressWarnings("deprecation")
    public void openAchievementsGUI(Player player, Achievement.AchievementCategory category) {
        Inventory inv = Bukkit.createInventory(this, 54, 
            "§6§l✦ Achievements - " + category.getDisplayName() + " ✦");
        
        // Border
        ItemStack border = createItem(Material.GOLD_NUGGET, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
        
        // Category navigation
        int categorySlot = 46;
        for (Achievement.AchievementCategory cat : Achievement.AchievementCategory.values()) {
            Material mat = cat == category ? Material.EMERALD : Material.COAL;
            ItemStack catItem = createItem(mat, cat.getColor() + cat.getDisplayName(),
                Arrays.asList("", "§7" + cat.getDescription(), "", "§eClick to view"));
            inv.setItem(categorySlot++, catItem);
            if (categorySlot >= 53) break;
        }
        
        // Get player's achievements
        UUID uuid = player.getUniqueId();
        Set<String> unlocked = playerAchievements.getOrDefault(uuid, new HashSet<>());
        Map<String, Integer> progress = achievementProgress.getOrDefault(uuid, new HashMap<>());
        
        // Display achievements in category
        int slot = 10;
        for (Achievement achievement : Achievement.values()) {
            if (achievement.getCategory() != category) continue;
            if (slot >= 44) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            
            boolean isUnlocked = unlocked.contains(achievement.name());
            int currentProgress = progress.getOrDefault(achievement.name(), 0);
            
            Material material = isUnlocked ? achievement.getIconMaterial() : Material.GRAY_DYE;
            String name = isUnlocked ? achievement.getColoredDisplay() : "§8" + achievement.getDisplayName();
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7" + achievement.getDescription());
            lore.add("");
            
            if (isUnlocked) {
                lore.add("§a✓ Unlocked!");
            } else {
                int required = achievement.getRequirement();
                int progressPercent = Math.min(100, (currentProgress * 100) / required);
                lore.add("§7Progress: " + createProgressBar(progressPercent / 100.0));
                lore.add("§7" + currentProgress + "/" + required);
            }
            
            lore.add("");
            lore.add("§7Rarity: " + achievement.getRarity().getDisplay());
            lore.add("§7Rewards: §e$" + String.format("%,.0f", achievement.getRewardMoney()));
            
            inv.setItem(slot++, createItem(material, name, lore));
        }
        
        // Summary
        int unlockedCount = 0;
        int totalInCategory = 0;
        for (Achievement a : Achievement.values()) {
            if (a.getCategory() == category) {
                totalInCategory++;
                if (unlocked.contains(a.name())) unlockedCount++;
            }
        }
        
        inv.setItem(4, createItem(Material.BOOK, 
            "§e" + category.getDisplayName() + " Progress",
            Arrays.asList(
                "",
                "§7Unlocked: §a" + unlockedCount + "§7/" + totalInCategory,
                "§7Progress: " + createProgressBar((double) unlockedCount / totalInCategory),
                ""
            )));
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
    }

    /**
     * Gets the count of unlocked achievements for a player.
     */
    public int getUnlockedCount(UUID uuid) {
        Set<String> unlocked = playerAchievements.get(uuid);
        return unlocked != null ? unlocked.size() : 0;
    }

    /**
     * Gets the total number of achievements.
     */
    public int getTotalAchievements() {
        return Achievement.values().length;
    }

    /**
     * Checks if a player has unlocked an achievement.
     */
    public boolean hasAchievement(UUID uuid, Achievement achievement) {
        Set<String> unlocked = playerAchievements.get(uuid);
        return unlocked != null && unlocked.contains(achievement.name());
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
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
