package com.budlords.economy;

import com.budlords.BudLords;
import com.budlords.data.DataManager;
import com.budlords.stats.PlayerStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class EconomyManager {

    private final BudLords plugin;
    private final DataManager dataManager;
    private final Map<UUID, Double> balances;
    private final Map<UUID, Double> totalEarnings;

    public EconomyManager(BudLords plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.balances = new ConcurrentHashMap<>();
        this.totalEarnings = new ConcurrentHashMap<>();
        loadBalances();
    }

    private void loadBalances() {
        FileConfiguration config = dataManager.getPlayersConfig();
        
        if (config.getConfigurationSection("players") == null) {
            return;
        }

        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                double balance = config.getDouble("players." + uuidStr + ".balance", 0.0);
                double earnings = config.getDouble("players." + uuidStr + ".total-earnings", 0.0);
                balances.put(uuid, balance);
                totalEarnings.put(uuid, earnings);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in players.yml: " + uuidStr);
            }
        }
    }

    public void saveBalances() {
        FileConfiguration config = dataManager.getPlayersConfig();
        
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            String uuid = entry.getKey().toString();
            config.set("players." + uuid + ".balance", entry.getValue());
            config.set("players." + uuid + ".total-earnings", totalEarnings.getOrDefault(entry.getKey(), 0.0));
        }
        
        dataManager.savePlayers();
    }

    public double getBalance(UUID playerUuid) {
        return balances.getOrDefault(playerUuid, 0.0);
    }

    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    public void setBalance(UUID playerUuid, double amount) {
        balances.put(playerUuid, Math.max(0, amount));
    }

    public void setBalance(Player player, double amount) {
        setBalance(player.getUniqueId(), amount);
    }

    public void addBalance(UUID playerUuid, double amount) {
        double current = getBalance(playerUuid);
        setBalance(playerUuid, current + amount);
    }

    public void addBalance(Player player, double amount) {
        addBalance(player.getUniqueId(), amount);
    }
    
    /**
     * Adds balance with prestige bonus applied.
     * Use this for earnings from sales to apply prestige multipliers.
     */
    public void addBalanceWithPrestigeBonus(Player player, double baseAmount) {
        double bonusAmount = applyPrestigeEarningsBonus(player, baseAmount);
        addBalance(player.getUniqueId(), bonusAmount);
    }
    
    /**
     * Applies the prestige earnings bonus to a base amount.
     * @return The amount after applying prestige multiplier
     */
    public double applyPrestigeEarningsBonus(Player player, double baseAmount) {
        if (plugin.getStatsManager() == null || plugin.getPrestigeManager() == null) {
            return baseAmount;
        }
        
        PlayerStats stats = plugin.getStatsManager().getStats(player);
        int prestigeLevel = stats.getPrestigeLevel();
        
        if (prestigeLevel <= 0) {
            return baseAmount;
        }
        
        double multiplier = plugin.getPrestigeManager().getEarningsMultiplier(prestigeLevel);
        return baseAmount * multiplier;
    }

    public boolean removeBalance(UUID playerUuid, double amount) {
        double current = getBalance(playerUuid);
        if (current < amount) {
            return false;
        }
        setBalance(playerUuid, current - amount);
        return true;
    }

    public boolean removeBalance(Player player, double amount) {
        return removeBalance(player.getUniqueId(), amount);
    }

    public boolean hasBalance(UUID playerUuid, double amount) {
        return getBalance(playerUuid) >= amount;
    }

    public boolean hasBalance(Player player, double amount) {
        return hasBalance(player.getUniqueId(), amount);
    }
    
    /**
     * Deposits money into a player's account (alias for addBalance).
     */
    public void deposit(Player player, double amount) {
        addBalance(player, amount);
    }
    
    /**
     * Deposits money into a player's account (alias for addBalance).
     */
    public void deposit(Player player, int amount) {
        addBalance(player, (double) amount);
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        if (!hasBalance(from, amount)) {
            return false;
        }
        removeBalance(from, amount);
        addBalance(to, amount);
        return true;
    }

    public boolean transfer(Player from, Player to, double amount) {
        return transfer(from.getUniqueId(), to.getUniqueId(), amount);
    }

    public void recordEarnings(UUID playerUuid, double amount) {
        double current = totalEarnings.getOrDefault(playerUuid, 0.0);
        totalEarnings.put(playerUuid, current + amount);
    }

    public void recordEarnings(Player player, double amount) {
        recordEarnings(player.getUniqueId(), amount);
    }

    public double getTotalEarnings(UUID playerUuid) {
        return totalEarnings.getOrDefault(playerUuid, 0.0);
    }

    public double getTotalEarnings(Player player) {
        return getTotalEarnings(player.getUniqueId());
    }

    public String formatMoney(double amount) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        return symbol + String.format("%.2f", amount);
    }

    public void initializePlayer(UUID playerUuid) {
        if (!balances.containsKey(playerUuid)) {
            double startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 0.0);
            balances.put(playerUuid, startingBalance);
            totalEarnings.put(playerUuid, 0.0);
        }
    }

    public void initializePlayer(Player player) {
        initializePlayer(player.getUniqueId());
    }
}
