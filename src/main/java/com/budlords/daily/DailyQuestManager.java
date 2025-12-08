package com.budlords.daily;

import com.budlords.BudLords;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages daily quests, login streaks, and timed events to create
 * an addictive game loop that keeps players engaged.
 */
public class DailyQuestManager {
    
    private final BudLords plugin;
    private final Map<UUID, DailyProgress> playerProgress;
    private final Map<UUID, LoginStreak> loginStreaks;
    private final Map<UUID, Long> lastLoginTime;
    private final Map<UUID, TimedBonus> activeTimedBonuses;
    
    // Daily quest types with varying rewards
    private static final DailyQuestType[] QUEST_TYPES = {
        new DailyQuestType("harvest_plants", "Harvest {amount} plants", 10, 30, 500, 50),
        new DailyQuestType("sell_products", "Sell {amount} products", 5, 15, 750, 75),
        new DailyQuestType("earn_money", "Earn ${amount}", 1000, 5000, 1000, 100),
        new DailyQuestType("smoke_joints", "Smoke {amount} joints", 3, 10, 600, 60),
        new DailyQuestType("perfect_harvest", "Get {amount} perfect minigame harvests", 3, 8, 800, 80),
        new DailyQuestType("five_star_buds", "Harvest {amount} 5-star buds", 2, 5, 1200, 120),
        new DailyQuestType("trade_with_buyers", "Trade with {amount} different buyers", 3, 8, 900, 90),
        new DailyQuestType("crossbreed_strains", "Crossbreed {amount} new strains", 1, 3, 1500, 150)
    };
    
    public DailyQuestManager(BudLords plugin) {
        this.plugin = plugin;
        this.playerProgress = new ConcurrentHashMap<>();
        this.loginStreaks = new ConcurrentHashMap<>();
        this.lastLoginTime = new ConcurrentHashMap<>();
        this.activeTimedBonuses = new ConcurrentHashMap<>();
        
        startDailyResetTask();
        startTimedEventSystem();
    }
    
    /**
     * Called when player logs in - handle streaks and notifications
     */
    public void handlePlayerLogin(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastLogin = lastLoginTime.get(uuid);
        
        // Update login time
        lastLoginTime.put(uuid, now);
        
        // Check and update streak
        LoginStreak streak = loginStreaks.computeIfAbsent(uuid, k -> new LoginStreak());
        boolean streakContinued = streak.checkAndUpdateStreak(lastLogin);
        
        // Get or create daily progress
        DailyProgress progress = playerProgress.computeIfAbsent(uuid, k -> generateDailyQuests());
        
        // Welcome back message with streak info
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage("");
                player.sendMessage("§8§m═══════════════════════════════════════");
                player.sendMessage("§a§l  ✦ WELCOME BACK TO BUDLORDS! ✦");
                player.sendMessage("§8§m═══════════════════════════════════════");
                player.sendMessage("");
                
                // Show streak
                if (streakContinued) {
                    player.sendMessage("§6🔥 Login Streak: §e§l" + streak.getDays() + " days!");
                    if (streak.getDays() % 7 == 0) {
                        player.sendMessage("§6  ✦ Weekly Bonus Unlocked!");
                        giveStreakBonus(player, streak.getDays());
                    }
                } else if (streak.getDays() == 1) {
                    player.sendMessage("§a✨ New streak started!");
                } else {
                    player.sendMessage("§c💔 Streak broken! Start a new one today.");
                }
                
                // Show daily quest progress
                player.sendMessage("");
                player.sendMessage("§e§l📋 TODAY'S QUESTS:");
                for (int i = 0; i < progress.quests.length; i++) {
                    DailyQuest quest = progress.quests[i];
                    if (!quest.completed) {
                        String bar = getProgressBar(quest.progress, quest.target, 10);
                        player.sendMessage(String.format("§7%d. %s %s §e%d§7/§e%d", 
                            i + 1, quest.name, bar, quest.progress, quest.target));
                    } else {
                        player.sendMessage(String.format("§a%d. ✓ %s §8(Completed!)", i + 1, quest.name));
                    }
                }
                
                // Show active timed bonuses
                TimedBonus bonus = activeTimedBonuses.get(uuid);
                if (bonus != null && !bonus.isExpired()) {
                    long remaining = (bonus.expiresAt - now) / 60000; // minutes
                    player.sendMessage("");
                    player.sendMessage("§d§l⚡ ACTIVE BONUS: §f" + bonus.name);
                    player.sendMessage("§7  " + bonus.description);
                    player.sendMessage("§7  Time left: §e" + remaining + " minutes");
                }
                
                player.sendMessage("");
                player.sendMessage("§8§m═══════════════════════════════════════");
                player.sendMessage("");
                
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            }
        }.runTaskLater(plugin, 40L); // Show after 2 seconds
    }
    
    /**
     * Track quest progress
     */
    public void trackProgress(Player player, String questType, int amount) {
        UUID uuid = player.getUniqueId();
        DailyProgress progress = playerProgress.get(uuid);
        if (progress == null) return;
        
        for (DailyQuest quest : progress.quests) {
            if (quest.type.equals(questType) && !quest.completed) {
                quest.progress += amount;
                
                if (quest.progress >= quest.target && !quest.completed) {
                    quest.completed = true;
                    
                    // Give rewards
                    plugin.getEconomyManager().deposit(player, quest.moneyReward);
                    
                    if (plugin.getSkillManager() != null) {
                        // Give XP to relevant skill tree
                        com.budlords.skills.Skill.SkillTree tree = getRelevantSkillTree(questType);
                        if (tree != null) {
                            plugin.getSkillManager().addTreeXP(uuid, tree, quest.xpReward);
                        }
                    }
                    
                    // Celebration
                    player.sendMessage("");
                    player.sendMessage("§a§l✓ QUEST COMPLETED!");
                    player.sendMessage("§e  " + quest.name);
                    player.sendMessage("§a  +$" + quest.moneyReward + " §7| §b+" + quest.xpReward + " XP");
                    player.sendMessage("");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                    player.spawnParticle(org.bukkit.Particle.TOTEM, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                    
                    // Check if all quests complete for bonus
                    if (progress.allQuestsComplete()) {
                        giveDailyCompletionBonus(player);
                    }
                }
            }
        }
    }
    
    private com.budlords.skills.Skill.SkillTree getRelevantSkillTree(String questType) {
        return switch (questType) {
            case "harvest_plants", "perfect_harvest", "five_star_buds" -> com.budlords.skills.Skill.SkillTree.FARMING;
            case "sell_products", "trade_with_buyers", "earn_money" -> com.budlords.skills.Skill.SkillTree.TRADING;
            case "crossbreed_strains" -> com.budlords.skills.Skill.SkillTree.GENETICS;
            default -> com.budlords.skills.Skill.SkillTree.FARMING;
        };
    }
    
    private void giveDailyCompletionBonus(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════════");
        player.sendMessage("§e§l  ★ ALL DAILY QUESTS COMPLETE! ★");
        player.sendMessage("§6§l═══════════════════════════════════════");
        player.sendMessage("");
        player.sendMessage("§a  Bonus Reward: §e$5,000 §7+ §b500 XP");
        player.sendMessage("§d  Special Bonus: §f2x Earnings for 30 minutes!");
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════════");
        player.sendMessage("");
        
        plugin.getEconomyManager().deposit(player, 5000);
        
        // Give timed bonus
        TimedBonus bonus = new TimedBonus(
            "Daily Champion",
            "§e2x §7earnings from all sales",
            System.currentTimeMillis() + 1800000, // 30 minutes
            2.0
        );
        activeTimedBonuses.put(player.getUniqueId(), bonus);
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    }
    
    private void giveStreakBonus(Player player, int streakDays) {
        double moneyBonus = streakDays * 100;
        int xpBonus = streakDays * 10;
        
        plugin.getEconomyManager().deposit(player, moneyBonus);
        
        player.sendMessage("§6  Streak Bonus: §a+$" + moneyBonus + " §7+ §b" + xpBonus + " XP");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
    }
    
    private DailyProgress generateDailyQuests() {
        DailyQuest[] quests = new DailyQuest[3];
        List<DailyQuestType> types = new ArrayList<>(Arrays.asList(QUEST_TYPES));
        Collections.shuffle(types);
        
        for (int i = 0; i < 3; i++) {
            DailyQuestType type = types.get(i);
            int target = ThreadLocalRandom.current().nextInt(type.minAmount, type.maxAmount + 1);
            quests[i] = new DailyQuest(
                type.id,
                type.nameTemplate.replace("{amount}", String.valueOf(target)),
                target,
                type.moneyReward,
                type.xpReward
            );
        }
        
        return new DailyProgress(quests);
    }
    
    private void startDailyResetTask() {
        new BukkitRunnable() {
            private LocalDate lastReset = LocalDate.now();
            
            @Override
            public void run() {
                LocalDate now = LocalDate.now();
                if (!now.equals(lastReset)) {
                    lastReset = now;
                    resetDailyQuests();
                }
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60); // Check every minute
    }
    
    private void resetDailyQuests() {
        for (UUID uuid : playerProgress.keySet()) {
            playerProgress.put(uuid, generateDailyQuests());
        }
        
        // Notify online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("§a§l⚡ New daily quests available! Type §e/daily §a§lto view.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.5f);
        }
    }
    
    private void startTimedEventSystem() {
        // Random bonus events every 1-3 hours
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().size() > 0) {
                    triggerRandomEvent();
                }
            }
        }.runTaskTimer(plugin, 20L * 60 * 60, 20L * 60 * ThreadLocalRandom.current().nextInt(60, 180));
    }
    
    private void triggerRandomEvent() {
        String[] events = {
            "DOUBLE_MONEY:2x Money from sales for 15 minutes!:900000:2.0",
            "TRIPLE_XP:3x XP from all actions for 10 minutes!:600000:3.0",
            "LUCKY_HOUR:2x chance for rare drops for 20 minutes!:1200000:2.0",
            "SPEED_BOOST:Plants grow 50% faster for 15 minutes!:900000:1.5"
        };
        
        String event = events[ThreadLocalRandom.current().nextInt(events.length)];
        String[] parts = event.split(":");
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l═════════════════════════════════════");
        Bukkit.broadcastMessage("§e§l  ⚡ TIMED EVENT STARTED! ⚡");
        Bukkit.broadcastMessage("§6§l═════════════════════════════════════");
        Bukkit.broadcastMessage("§f  " + parts[1]);
        Bukkit.broadcastMessage("§6§l═════════════════════════════════════");
        Bukkit.broadcastMessage("");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
        }
    }
    
    public TimedBonus getActiveBonus(UUID uuid) {
        TimedBonus bonus = activeTimedBonuses.get(uuid);
        if (bonus != null && bonus.isExpired()) {
            activeTimedBonuses.remove(uuid);
            return null;
        }
        return bonus;
    }
    
    public DailyProgress getProgress(UUID uuid) {
        return playerProgress.computeIfAbsent(uuid, k -> generateDailyQuests());
    }
    
    public LoginStreak getStreak(UUID uuid) {
        return loginStreaks.computeIfAbsent(uuid, k -> new LoginStreak());
    }
    
    private String getProgressBar(int current, int target, int length) {
        int filled = (int) (((double) current / target) * length);
        filled = Math.min(filled, length);
        
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("§a▮");
            } else {
                bar.append("§7▯");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }
    
    // Inner classes
    private static class DailyQuestType {
        String id, nameTemplate;
        int minAmount, maxAmount;
        double moneyReward;
        int xpReward;
        
        DailyQuestType(String id, String nameTemplate, int minAmount, int maxAmount, double moneyReward, int xpReward) {
            this.id = id;
            this.nameTemplate = nameTemplate;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.moneyReward = moneyReward;
            this.xpReward = xpReward;
        }
    }
    
    public static class DailyQuest {
        String type, name;
        int target, progress;
        double moneyReward;
        int xpReward;
        boolean completed;
        
        DailyQuest(String type, String name, int target, double moneyReward, int xpReward) {
            this.type = type;
            this.name = name;
            this.target = target;
            this.moneyReward = moneyReward;
            this.xpReward = xpReward;
            this.progress = 0;
            this.completed = false;
        }
    }
    
    public static class DailyProgress {
        DailyQuest[] quests;
        
        DailyProgress(DailyQuest[] quests) {
            this.quests = quests;
        }
        
        boolean allQuestsComplete() {
            for (DailyQuest quest : quests) {
                if (!quest.completed) return false;
            }
            return true;
        }
    }
    
    public static class LoginStreak {
        private int days = 0;
        private LocalDate lastLogin = null;
        
        boolean checkAndUpdateStreak(Long lastLoginMillis) {
            LocalDate today = LocalDate.now();
            
            if (lastLogin == null) {
                // First login
                days = 1;
                lastLogin = today;
                return false;
            }
            
            long daysBetween = ChronoUnit.DAYS.between(lastLogin, today);
            
            if (daysBetween == 0) {
                // Same day login
                return true;
            } else if (daysBetween == 1) {
                // Consecutive day
                days++;
                lastLogin = today;
                return true;
            } else {
                // Streak broken
                days = 1;
                lastLogin = today;
                return false;
            }
        }
        
        public int getDays() {
            return days;
        }
    }
    
    public static class TimedBonus {
        String name, description;
        long expiresAt;
        double multiplier;
        
        TimedBonus(String name, String description, long expiresAt, double multiplier) {
            this.name = name;
            this.description = description;
            this.expiresAt = expiresAt;
            this.multiplier = multiplier;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
        
        public double getMultiplier() {
            return multiplier;
        }
    }
}
