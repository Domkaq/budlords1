package com.budlords.prestige;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
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

import java.util.Arrays;
import java.util.List;

/**
 * Manages the prestige system for BudLords.
 * Players can reset their progress to gain permanent bonuses.
 */
public class PrestigeManager implements InventoryHolder {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final StatsManager statsManager;

    // Prestige configuration (read from config)
    private final double basePrestigeCost;
    private final int maxPrestige;
    private final double earningsBonusPerLevel;
    private final double growthSpeedBonusPerLevel;
    private final double qualityBonusPerLevel;
    private final double successBonusPerLevel;

    public PrestigeManager(BudLords plugin, EconomyManager economyManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.statsManager = statsManager;
        
        // Load configuration values
        this.basePrestigeCost = plugin.getConfig().getDouble("prestige.base-cost", 500000.0);
        this.maxPrestige = plugin.getConfig().getInt("prestige.max-level", 10);
        this.earningsBonusPerLevel = plugin.getConfig().getDouble("prestige.bonuses.earnings-percent", 10) / 100.0;
        this.growthSpeedBonusPerLevel = plugin.getConfig().getDouble("prestige.bonuses.growth-speed-percent", 5) / 100.0;
        this.qualityBonusPerLevel = plugin.getConfig().getDouble("prestige.bonuses.quality-percent", 8) / 100.0;
        this.successBonusPerLevel = plugin.getConfig().getDouble("prestige.bonuses.trade-success-percent", 2) / 100.0;
    }

    /**
     * Opens the prestige GUI for a player.
     */
    @SuppressWarnings("deprecation")
    public void openPrestigeGUI(Player player) {
        Inventory inv = Bukkit.createInventory(this, 45, "§5§l✦ Prestige Menu ✦");
        
        PlayerStats stats = statsManager.getStats(player);
        int currentPrestige = stats.getPrestigeLevel();
        double currentBalance = economyManager.getBalance(player);
        double prestigeCost = getPrestigeCost(currentPrestige);
        boolean canPrestige = currentBalance >= prestigeCost && currentPrestige < maxPrestige;
        
        // Border
        ItemStack borderPurple = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null);
        ItemStack borderMagenta = createItem(Material.MAGENTA_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderPurple : borderMagenta);
            inv.setItem(36 + i, i % 2 == 0 ? borderPurple : borderMagenta);
        }
        for (int i = 9; i < 36; i += 9) {
            inv.setItem(i, borderPurple);
            inv.setItem(i + 8, borderPurple);
        }
        
        // Current status
        inv.setItem(4, createItem(Material.NETHER_STAR, 
            "§5§l✦ Your Prestige Status ✦",
            Arrays.asList(
                "",
                "§7Current Prestige: " + getPrestigeDisplay(currentPrestige),
                "§7Total Prestiges: §e" + stats.getTotalPrestiges(),
                "",
                "§8━━━━━━━━━━━━━━━━━━━━━━"
            )));
        
        // Current bonuses display
        inv.setItem(20, createItem(Material.GOLD_INGOT, 
            "§e§l💰 Current Bonuses",
            Arrays.asList(
                "",
                "§7Earnings Bonus: §a+" + String.format("%.0f%%", getEarningsMultiplier(currentPrestige) * 100 - 100),
                "§7Growth Speed: §a+" + String.format("%.0f%%", getGrowthSpeedMultiplier(currentPrestige) * 100 - 100),
                "§7Quality Bonus: §a+" + String.format("%.0f%%", getQualityMultiplier(currentPrestige) * 100 - 100),
                "§7Trade Success: §a+" + String.format("%.0f%%", getSuccessBonus(currentPrestige) * 100),
                ""
            )));
        
        // Next prestige preview
        if (currentPrestige < maxPrestige) {
            inv.setItem(22, createItem(Material.EXPERIENCE_BOTTLE, 
                "§a§l⬆ Next Prestige Preview",
                Arrays.asList(
                    "",
                    "§7After prestige you'll have:",
                    "§7Earnings Bonus: §a+" + String.format("%.0f%%", getEarningsMultiplier(currentPrestige + 1) * 100 - 100),
                    "§7Growth Speed: §a+" + String.format("%.0f%%", getGrowthSpeedMultiplier(currentPrestige + 1) * 100 - 100),
                    "§7Quality Bonus: §a+" + String.format("%.0f%%", getQualityMultiplier(currentPrestige + 1) * 100 - 100),
                    "§7Trade Success: §a+" + String.format("%.0f%%", getSuccessBonus(currentPrestige + 1) * 100),
                    ""
                )));
        } else {
            inv.setItem(22, createItem(Material.DIAMOND_BLOCK, 
                "§6§l✦ MAX PRESTIGE ✦",
                Arrays.asList(
                    "",
                    "§7You've reached the maximum",
                    "§7prestige level!",
                    "",
                    "§6You are a true BudLord!"
                )));
        }
        
        // Cost display
        inv.setItem(24, createItem(Material.EMERALD_BLOCK, 
            "§e§l💵 Prestige Cost",
            Arrays.asList(
                "",
                "§7Required: §e$" + String.format("%,.0f", prestigeCost),
                "§7Your Balance: " + (currentBalance >= prestigeCost ? "§a" : "§c") + 
                    "$" + String.format("%,.0f", currentBalance),
                "",
                currentBalance >= prestigeCost ? "§a✓ You can afford this!" : "§c✗ Not enough money"
            )));
        
        // Prestige button
        Material buttonMat = canPrestige ? Material.END_CRYSTAL : Material.BARRIER;
        String buttonName = canPrestige ? "§a§l✓ PRESTIGE NOW" : "§c§l✗ Cannot Prestige";
        List<String> buttonLore = canPrestige 
            ? Arrays.asList(
                "",
                "§c⚠ WARNING! ⚠",
                "§7This will reset your:",
                "§7• Balance",
                "§7• Rank progress",
                "",
                "§7You will KEEP:",
                "§7• Created strains",
                "§7• Statistics",
                "§7• Achievements",
                "",
                "§a▶ Click to prestige!"
            )
            : Arrays.asList(
                "",
                currentPrestige >= maxPrestige 
                    ? "§7You've reached max prestige!"
                    : "§7You need more money to prestige.",
                ""
            );
        
        inv.setItem(31, createItem(buttonMat, buttonName, buttonLore));
        
        // Info panel
        inv.setItem(40, createItem(Material.BOOK, 
            "§e§l? What is Prestige?",
            Arrays.asList(
                "",
                "§7Prestige resets your progress",
                "§7in exchange for permanent",
                "§7gameplay bonuses!",
                "",
                "§7Each prestige level grants:",
                "§a• +10% Earnings",
                "§a• +5% Growth Speed",
                "§a• +8% Quality Bonus",
                "§a• +2% Trade Success",
                "",
                "§7Max Prestige Level: §6" + maxPrestige
            )));
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.2f);
    }

    /**
     * Handles clicking the prestige button.
     */
    public boolean handlePrestigeClick(Player player, int slot) {
        if (slot != 31) return false;
        
        PlayerStats stats = statsManager.getStats(player);
        int currentPrestige = stats.getPrestigeLevel();
        double currentBalance = economyManager.getBalance(player);
        double prestigeCost = getPrestigeCost(currentPrestige);
        
        if (currentPrestige >= maxPrestige) {
            player.sendMessage("§cYou've already reached the maximum prestige level!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return true;
        }
        
        if (currentBalance < prestigeCost) {
            player.sendMessage("§cYou need §e$" + String.format("%,.0f", prestigeCost) + " §cto prestige!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return true;
        }
        
        // Perform prestige
        performPrestige(player, stats);
        return true;
    }

    private void performPrestige(Player player, PlayerStats stats) {
        int newPrestigeLevel = stats.getPrestigeLevel() + 1;
        
        // Reset balance
        economyManager.setBalance(player, 0);
        
        // Increment prestige
        stats.incrementPrestige();
        
        // Award skill points for prestige (3 points per prestige level)
        int skillPointsAwarded = 3;
        if (plugin.getSkillManager() != null) {
            plugin.getSkillManager().addSkillPoints(player.getUniqueId(), skillPointsAwarded);
        }
        
        // Close inventory
        player.closeInventory();
        
        // Sync achievements with stats
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().syncWithStats(player);
        }
        
        // Save stats
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().saveStats();
        }
        
        // Epic celebration effects!
        playPrestigeCelebration(player, newPrestigeLevel);
        
        // Send messages - simplified format without box alignment issues
        player.sendMessage("");
        player.sendMessage("§5§l═════════════════════════════════════");
        player.sendMessage("§d§l    ✦ PRESTIGE COMPLETE! ✦");
        player.sendMessage("§5§l═════════════════════════════════════");
        player.sendMessage("");
        player.sendMessage("§7You are now Prestige " + getPrestigeDisplay(newPrestigeLevel));
        player.sendMessage("");
        player.sendMessage("§7New Bonuses:");
        player.sendMessage("§a  • Earnings: §f+" + String.format("%.0f%%", getEarningsMultiplier(newPrestigeLevel) * 100 - 100));
        player.sendMessage("§a  • Growth Speed: §f+" + String.format("%.0f%%", getGrowthSpeedMultiplier(newPrestigeLevel) * 100 - 100));
        player.sendMessage("§a  • Quality: §f+" + String.format("%.0f%%", getQualityMultiplier(newPrestigeLevel) * 100 - 100));
        player.sendMessage("§a  • Trade Success: §f+" + String.format("%.0f%%", getSuccessBonus(newPrestigeLevel) * 100));
        player.sendMessage("");
        player.sendMessage("§a  • Skill Points: §f+" + skillPointsAwarded + " §7(Use /skills to unlock skills!)");
        player.sendMessage("");
        player.sendMessage("§5§l═════════════════════════════════════");
        player.sendMessage("");
        
        // Broadcast to server
        Bukkit.broadcastMessage("§5§l✦ §d" + player.getName() + " §7has reached §5Prestige " + 
            getPrestigeDisplay(newPrestigeLevel) + "§7!");
    }

    private void playPrestigeCelebration(Player player, int prestigeLevel) {
        // Sound effects
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);
        
        // Particles
        player.spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 
            100, 0.5, 1, 0.5, 0.2);
        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 
            50, 1, 1, 1, 0.1);
        player.spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 0.5, 0), 
            30, 0.5, 0.5, 0.5, 0.05);
        
        // Delayed effects
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
            player.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 2, 0), 
                50, 1, 0.5, 1, 0.1);
        }, 20L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.2f);
        }, 40L);
    }

    // ===== BONUS CALCULATIONS =====

    public double getPrestigeCost(int currentPrestige) {
        // Exponential cost increase
        return basePrestigeCost * Math.pow(2, currentPrestige);
    }

    public double getEarningsMultiplier(int prestigeLevel) {
        return 1.0 + (prestigeLevel * earningsBonusPerLevel);
    }

    public double getGrowthSpeedMultiplier(int prestigeLevel) {
        return 1.0 + (prestigeLevel * growthSpeedBonusPerLevel);
    }

    public double getQualityMultiplier(int prestigeLevel) {
        return 1.0 + (prestigeLevel * qualityBonusPerLevel);
    }

    public double getSuccessBonus(int prestigeLevel) {
        return prestigeLevel * successBonusPerLevel;
    }

    public String getPrestigeDisplay(int level) {
        if (level <= 0) return "§7None";
        
        String color = switch (level) {
            case 1 -> "§f";
            case 2 -> "§e";
            case 3 -> "§6";
            case 4 -> "§a";
            case 5 -> "§9";
            case 6 -> "§5";
            case 7 -> "§d";
            case 8 -> "§c";
            case 9 -> "§4";
            case 10 -> "§6§l";
            default -> "§f";
        };
        
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < Math.min(level, 10); i++) {
            stars.append("✦");
        }
        
        return color + "P" + level + " " + stars;
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
}
