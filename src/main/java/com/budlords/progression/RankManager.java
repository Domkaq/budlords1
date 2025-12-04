package com.budlords.progression;

import com.budlords.BudLords;
import com.budlords.data.DataManager;
import com.budlords.economy.EconomyManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class RankManager {

    private final BudLords plugin;
    private final DataManager dataManager;
    private final List<Rank> ranks;

    public RankManager(BudLords plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.ranks = new ArrayList<>();
        loadRanks();
    }

    private void loadRanks() {
        // Default ranks if not configured
        ranks.add(new Rank("Novice", 0, 0.7, new HashSet<>()));
        ranks.add(new Rank("Dealer", 1000, 0.75, Set.of("og_kush", "purple_haze")));
        ranks.add(new Rank("Supplier", 5000, 0.8, Set.of("og_kush", "purple_haze", "white_widow")));
        ranks.add(new Rank("Distributor", 15000, 0.85, Set.of("og_kush", "purple_haze", "white_widow")));
        ranks.add(new Rank("Kingpin", 50000, 0.9, Set.of("og_kush", "purple_haze", "white_widow", "northern_lights")));
        ranks.add(new Rank("Cartel Boss", 150000, 0.95, Set.of("og_kush", "purple_haze", "white_widow", "northern_lights")));
        ranks.add(new Rank("BudLord", 500000, 1.0, Set.of("og_kush", "purple_haze", "white_widow", "northern_lights")));

        // Load custom ranks from config
        ConfigurationSection ranksSection = plugin.getConfig().getConfigurationSection("ranks");
        if (ranksSection != null) {
            ranks.clear();
            for (String key : ranksSection.getKeys(false)) {
                ConfigurationSection rankSection = ranksSection.getConfigurationSection(key);
                if (rankSection != null) {
                    String name = rankSection.getString("name", key);
                    double required = rankSection.getDouble("required-earnings", 0);
                    double successChance = rankSection.getDouble("success-chance-bonus", 0.7);
                    List<String> strainsList = rankSection.getStringList("unlocked-strains");
                    ranks.add(new Rank(name, required, successChance, new HashSet<>(strainsList)));
                }
            }
        }

        // Sort by required earnings
        ranks.sort(Comparator.comparingDouble(Rank::requiredEarnings));
    }

    public Rank getRank(Player player) {
        // Get live earnings from EconomyManager instead of config file
        EconomyManager economyManager = plugin.getEconomyManager();
        double earnings = economyManager != null ? economyManager.getTotalEarnings(player) : 
                dataManager.getPlayersConfig().getDouble("players." + player.getUniqueId() + ".total-earnings", 0);
        return getRankForEarnings(earnings);
    }

    public Rank getRankForEarnings(double earnings) {
        Rank currentRank = ranks.get(0);
        for (Rank rank : ranks) {
            if (earnings >= rank.requiredEarnings()) {
                currentRank = rank;
            } else {
                break;
            }
        }
        return currentRank;
    }

    public Rank getNextRank(Player player) {
        // Get live earnings from EconomyManager instead of config file
        EconomyManager economyManager = plugin.getEconomyManager();
        double earnings = economyManager != null ? economyManager.getTotalEarnings(player) : 
                dataManager.getPlayersConfig().getDouble("players." + player.getUniqueId() + ".total-earnings", 0);
        
        for (Rank rank : ranks) {
            if (earnings < rank.requiredEarnings()) {
                return rank;
            }
        }
        return null; // Already at max rank
    }

    public double getProgressToNextRank(Player player) {
        // Get live earnings from EconomyManager instead of config file
        EconomyManager economyManager = plugin.getEconomyManager();
        double earnings = economyManager != null ? economyManager.getTotalEarnings(player) : 
                dataManager.getPlayersConfig().getDouble("players." + player.getUniqueId() + ".total-earnings", 0);
        
        Rank current = getRankForEarnings(earnings);
        Rank next = null;
        
        for (Rank rank : ranks) {
            if (rank.requiredEarnings() > current.requiredEarnings()) {
                next = rank;
                break;
            }
        }
        
        if (next == null) {
            return 1.0; // Max rank
        }
        
        double rangeStart = current.requiredEarnings();
        double rangeEnd = next.requiredEarnings();
        double progress = (earnings - rangeStart) / (rangeEnd - rangeStart);
        
        return Math.max(0, Math.min(1, progress));
    }

    public boolean canAccessStrain(Player player, String strainId) {
        Rank rank = getRank(player);
        if (rank.unlockedStrains().isEmpty()) {
            return true; // All strains accessible
        }
        return rank.unlockedStrains().contains(strainId);
    }

    public List<Rank> getAllRanks() {
        return Collections.unmodifiableList(ranks);
    }

    public String getRankDisplayName(Player player) {
        Rank rank = getRank(player);
        return getRankColor(rank) + rank.name();
    }

    public String getRankColor(Rank rank) {
        int index = ranks.indexOf(rank);
        return switch (index) {
            case 0 -> "§7";      // Gray - Novice
            case 1 -> "§a";      // Green - Dealer
            case 2 -> "§e";      // Yellow - Supplier
            case 3 -> "§6";      // Gold - Distributor
            case 4 -> "§c";      // Red - Kingpin
            case 5 -> "§5";      // Purple - Cartel Boss
            case 6 -> "§d§l";    // Light Purple Bold - BudLord
            default -> "§f";
        };
    }

    public record Rank(String name, double requiredEarnings, double successChanceBonus, Set<String> unlockedStrains) {
    }
}
