package com.budlords;

import com.budlords.commands.*;
import com.budlords.data.DataManager;
import com.budlords.economy.EconomyManager;
import com.budlords.farming.FarmingManager;
import com.budlords.gui.MarketShopGUI;
import com.budlords.gui.RollingShopGUI;
import com.budlords.joint.JointRollingManager;
import com.budlords.listeners.FarmingListener;
import com.budlords.listeners.ItemDropListener;
import com.budlords.listeners.NPCListener;
import com.budlords.listeners.PlayerListener;
import com.budlords.npc.NPCManager;
import com.budlords.packaging.DroppedBudTracker;
import com.budlords.packaging.PackagingManager;
import com.budlords.progression.RankManager;
import com.budlords.quality.QualityItemManager;
import com.budlords.strain.StrainManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public class BudLords extends JavaPlugin {

    private DataManager dataManager;
    private EconomyManager economyManager;
    private StrainManager strainManager;
    private FarmingManager farmingManager;
    private NPCManager npcManager;
    private PackagingManager packagingManager;
    private RankManager rankManager;
    private QualityItemManager qualityItemManager;
    private MarketShopGUI marketShopGUI;
    private RollingShopGUI rollingShopGUI;
    private JointRollingManager jointRollingManager;
    private DroppedBudTracker droppedBudTracker;

    @Override
    public void onEnable() {
        try {
            // Save default configs
            saveDefaultConfig();
            
            // Initialize managers in order of dependency
            this.dataManager = new DataManager(this);
            this.strainManager = new StrainManager(this, dataManager);
            this.economyManager = new EconomyManager(this, dataManager);
            this.rankManager = new RankManager(this, dataManager);
            this.packagingManager = new PackagingManager(this, strainManager);
            this.farmingManager = new FarmingManager(this, dataManager, strainManager);
            this.npcManager = new NPCManager(this, economyManager, strainManager, rankManager, packagingManager);
            this.qualityItemManager = new QualityItemManager(this);
            this.marketShopGUI = new MarketShopGUI(this, economyManager, qualityItemManager);
            this.rollingShopGUI = new RollingShopGUI(this, economyManager);
            this.jointRollingManager = new JointRollingManager(this, strainManager);
            this.droppedBudTracker = new DroppedBudTracker();
            
            // Register commands
            registerCommands();
            
            // Register listeners
            registerListeners();
            
            // Start autosave task
            startAutosaveTask();
            
            getLogger().info("BudLords has been enabled successfully!");
            getLogger().info("Loaded " + strainManager.getStrainCount() + " strains.");
            getLogger().info("Loaded " + farmingManager.getPlantCount() + " active plants.");
            getLogger().info("★ Star Quality System enabled!");
            getLogger().info("✦ Drag-and-Drop Packaging enabled!");
            getLogger().info("✦ Joint Rolling Minigame enabled!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable BudLords!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (dataManager != null) {
                dataManager.saveAll();
                getLogger().info("All data saved successfully.");
            }
            if (farmingManager != null) {
                farmingManager.shutdown();
            }
            getLogger().info("BudLords has been disabled.");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error during shutdown", e);
        }
    }

    private void registerCommands() {
        BalanceCommand balanceCommand = new BalanceCommand(economyManager, rankManager);
        AddMoneyCommand addMoneyCommand = new AddMoneyCommand(economyManager);
        PayCommand payCommand = new PayCommand(economyManager);
        StrainCreatorCommand strainCreatorCommand = new StrainCreatorCommand(this, strainManager);
        SpawnMarketCommand spawnMarketCommand = new SpawnMarketCommand(npcManager);
        SpawnBlackMarketCommand spawnBlackMarketCommand = new SpawnBlackMarketCommand(npcManager);
        PackageCommand packageCommand = new PackageCommand(packagingManager);
        BudLordsCommand budLordsCommand = new BudLordsCommand(this);

        Objects.requireNonNull(getCommand("bal")).setExecutor(balanceCommand);
        Objects.requireNonNull(getCommand("bal")).setTabCompleter(balanceCommand);
        Objects.requireNonNull(getCommand("addmoney")).setExecutor(addMoneyCommand);
        Objects.requireNonNull(getCommand("addmoney")).setTabCompleter(addMoneyCommand);
        Objects.requireNonNull(getCommand("pay")).setExecutor(payCommand);
        Objects.requireNonNull(getCommand("pay")).setTabCompleter(payCommand);
        Objects.requireNonNull(getCommand("straincreator")).setExecutor(strainCreatorCommand);
        Objects.requireNonNull(getCommand("straincreator")).setTabCompleter(strainCreatorCommand);
        Objects.requireNonNull(getCommand("spawnmarket")).setExecutor(spawnMarketCommand);
        Objects.requireNonNull(getCommand("spawnmarket")).setTabCompleter(spawnMarketCommand);
        Objects.requireNonNull(getCommand("spawnblackmarket")).setExecutor(spawnBlackMarketCommand);
        Objects.requireNonNull(getCommand("spawnblackmarket")).setTabCompleter(spawnBlackMarketCommand);
        Objects.requireNonNull(getCommand("package")).setExecutor(packageCommand);
        Objects.requireNonNull(getCommand("package")).setTabCompleter(packageCommand);
        Objects.requireNonNull(getCommand("budlords")).setExecutor(budLordsCommand);
        Objects.requireNonNull(getCommand("budlords")).setTabCompleter(budLordsCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new FarmingListener(this, farmingManager, strainManager), this);
        getServer().getPluginManager().registerEvents(new NPCListener(npcManager, economyManager, rankManager, packagingManager, marketShopGUI, strainManager), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, dataManager), this);
        getServer().getPluginManager().registerEvents(new ItemDropListener(this, strainManager, packagingManager, droppedBudTracker, jointRollingManager), this);
    }

    private void startAutosaveTask() {
        int intervalTicks = getConfig().getInt("autosave-interval-seconds", 300) * 20;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                dataManager.saveAll();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Autosave failed", e);
            }
        }, intervalTicks, intervalTicks);
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public StrainManager getStrainManager() {
        return strainManager;
    }

    public FarmingManager getFarmingManager() {
        return farmingManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public PackagingManager getPackagingManager() {
        return packagingManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }
    
    public QualityItemManager getQualityItemManager() {
        return qualityItemManager;
    }
    
    public RollingShopGUI getRollingShopGUI() {
        return rollingShopGUI;
    }
    
    public JointRollingManager getJointRollingManager() {
        return jointRollingManager;
    }
    
    public DroppedBudTracker getDroppedBudTracker() {
        return droppedBudTracker;
    }
}
