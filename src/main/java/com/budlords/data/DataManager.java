package com.budlords.data;

import com.budlords.BudLords;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DataManager {

    private final BudLords plugin;
    
    private File strainsFile;
    private File playersFile;
    private File plantsFile;
    
    private FileConfiguration strainsConfig;
    private FileConfiguration playersConfig;
    private FileConfiguration plantsConfig;

    public DataManager(BudLords plugin) {
        this.plugin = plugin;
        setupFiles();
        loadConfigs();
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        strainsFile = new File(plugin.getDataFolder(), "strains.yml");
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        plantsFile = new File(plugin.getDataFolder(), "plants.yml");

        createFileIfNotExists(strainsFile);
        createFileIfNotExists(playersFile);
        createFileIfNotExists(plantsFile);
    }

    private void createFileIfNotExists(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create file: " + file.getName(), e);
            }
        }
    }

    private void loadConfigs() {
        strainsConfig = YamlConfiguration.loadConfiguration(strainsFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        plantsConfig = YamlConfiguration.loadConfiguration(plantsFile);
    }

    public void saveAll() {
        saveStrains();
        savePlayers();
        savePlants();
    }

    public CompletableFuture<Void> saveAllAsync() {
        return CompletableFuture.runAsync(() -> {
            saveStrains();
            savePlayers();
            savePlants();
        });
    }

    public void saveStrains() {
        try {
            strainsConfig.save(strainsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save strains.yml", e);
        }
    }

    public void savePlayers() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save players.yml", e);
        }
    }

    public void savePlants() {
        try {
            plantsConfig.save(plantsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save plants.yml", e);
        }
    }

    public FileConfiguration getStrainsConfig() {
        return strainsConfig;
    }

    public FileConfiguration getPlayersConfig() {
        return playersConfig;
    }

    public FileConfiguration getPlantsConfig() {
        return plantsConfig;
    }

    public void reloadStrains() {
        strainsConfig = YamlConfiguration.loadConfiguration(strainsFile);
    }

    public void reloadPlayers() {
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    public void reloadPlants() {
        plantsConfig = YamlConfiguration.loadConfiguration(plantsFile);
    }

    public void reloadAll() {
        loadConfigs();
    }
}
