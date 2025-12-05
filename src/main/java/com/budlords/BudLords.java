package com.budlords;

import com.budlords.achievements.AchievementManager;
import com.budlords.challenges.ChallengeManager;
import com.budlords.collections.CollectionManager;
import com.budlords.commands.*;
import com.budlords.crossbreed.CrossbreedManager;
import com.budlords.data.DataManager;
import com.budlords.diseases.DiseaseManager;
import com.budlords.economy.EconomyManager;
import com.budlords.events.RandomEventManager;
import com.budlords.farming.AmbientEffectsManager;
import com.budlords.farming.FarmingManager;
import com.budlords.gui.BlackMarketShopGUI;
import com.budlords.gui.MarketShopGUI;
import com.budlords.gui.MobSaleGUI;
import com.budlords.gui.RollingShopGUI;
import com.budlords.joint.JointRollingManager;
import com.budlords.listeners.FarmingListener;
import com.budlords.listeners.GUIListener;
import com.budlords.listeners.ItemDropListener;
import com.budlords.listeners.NPCListener;
import com.budlords.listeners.PlayerListener;
import com.budlords.npc.NPCManager;
import com.budlords.packaging.DroppedBudTracker;
import com.budlords.packaging.PackagingManager;
import com.budlords.prestige.PrestigeManager;
import com.budlords.progression.RankManager;
import com.budlords.quality.QualityItemManager;
import com.budlords.seasons.SeasonManager;
import com.budlords.skills.SkillManager;
import com.budlords.stats.StatsManager;
import com.budlords.strain.StrainManager;
import com.budlords.weather.WeatherManager;
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
    private BlackMarketShopGUI blackMarketShopGUI;
    private MobSaleGUI mobSaleGUI;
    private RollingShopGUI rollingShopGUI;
    private JointRollingManager jointRollingManager;
    private DroppedBudTracker droppedBudTracker;
    
    // New features
    private StatsManager statsManager;
    private PrestigeManager prestigeManager;
    private ChallengeManager challengeManager;
    private RandomEventManager randomEventManager;
    private CrossbreedManager crossbreedManager;
    private AmbientEffectsManager ambientEffectsManager;
    private com.budlords.joint.JointEffectsManager jointEffectsManager;
    private com.budlords.effects.StrainEffectsManager strainEffectsManager;
    
    // v2.0.0 New Feature Managers
    private SeasonManager seasonManager;
    private WeatherManager weatherManager;
    private DiseaseManager diseaseManager;
    private AchievementManager achievementManager;
    private SkillManager skillManager;
    private CollectionManager collectionManager;

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
            this.blackMarketShopGUI = new BlackMarketShopGUI(this, economyManager, strainManager);
            this.mobSaleGUI = new MobSaleGUI(this, economyManager, packagingManager, strainManager);
            this.rollingShopGUI = new RollingShopGUI(this, economyManager);
            this.jointRollingManager = new JointRollingManager(this, strainManager);
            this.droppedBudTracker = new DroppedBudTracker();
            
            // Initialize new feature managers
            this.statsManager = new StatsManager(this);
            this.prestigeManager = new PrestigeManager(this, economyManager, statsManager);
            this.challengeManager = new ChallengeManager(this, economyManager, statsManager);
            this.randomEventManager = new RandomEventManager(this, farmingManager);
            this.crossbreedManager = new CrossbreedManager(this, strainManager, economyManager, statsManager);
            this.ambientEffectsManager = new AmbientEffectsManager(this, farmingManager, strainManager);
            this.jointEffectsManager = new com.budlords.joint.JointEffectsManager(this);
            this.strainEffectsManager = new com.budlords.effects.StrainEffectsManager(this);
            
            // v2.0.0 New Feature Managers
            this.seasonManager = new SeasonManager(this);
            this.weatherManager = new WeatherManager(this, farmingManager);
            this.diseaseManager = new DiseaseManager(this, farmingManager);
            this.achievementManager = new AchievementManager(this, economyManager, statsManager);
            this.skillManager = new SkillManager(this);
            this.collectionManager = new CollectionManager(this, strainManager);
            
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
            getLogger().info("✦ Joint Smoking Effects enabled!");
            getLogger().info("✦ Prestige System enabled!");
            getLogger().info("✦ Daily Challenges enabled!");
            getLogger().info("✦ Random Events enabled!");
            getLogger().info("✦ Crossbreeding Lab enabled!");
            getLogger().info("✦ Enhanced Ambient Effects enabled!");
            getLogger().info("✦ Strain Special Effects System enabled with 160+ unique effects!");
            getLogger().info("");
            getLogger().info("§6§l=== BudLords v3.0.0 MAJOR UPDATE ===");
            getLogger().info("");
            getLogger().info("§a✦ BUG FIXES:");
            getLogger().info("  • Fixed grinder menu alignment in Rolling Shop");
            getLogger().info("  • Grinder can no longer be placed on ground");
            getLogger().info("  • Lamps now buff ALL nearby plants in radius!");
            getLogger().info("  • Admin strains now have UNLIMITED effects!");
            getLogger().info("  • Fixed rank and prestige system integration");
            getLogger().info("");
            getLogger().info("§a✦ NEW CONTENT:");
            getLogger().info("  • 50+ New Strain Effects added! (160+ total)");
            getLogger().info("  • 15+ New Strains with unique effect combos!");
            getLogger().info("  • New effect categories: Combat, Nature, Mystical");
            getLogger().info("  • New legendary transformations and abilities!");
            getLogger().info("");
            getLogger().info("§a✦ GAMEPLAY IMPROVEMENTS:");
            getLogger().info("  • Lamp radius based on star quality (★1-5 blocks)");
            getLogger().info("  • Better multiplayer support");
            getLogger().info("  • Enhanced visual feedback");
            getLogger().info("");
            getLogger().info("§6§l=====================================");
            
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
            if (statsManager != null) {
                statsManager.saveStats();
            }
            if (farmingManager != null) {
                farmingManager.shutdown();
            }
            if (challengeManager != null) {
                challengeManager.shutdown();
            }
            if (randomEventManager != null) {
                randomEventManager.shutdown();
            }
            if (ambientEffectsManager != null) {
                ambientEffectsManager.shutdown();
            }
            // v2.0.0 shutdown
            if (seasonManager != null) {
                seasonManager.shutdown();
            }
            if (weatherManager != null) {
                weatherManager.shutdown();
            }
            if (diseaseManager != null) {
                diseaseManager.shutdown();
            }
            if (achievementManager != null) {
                achievementManager.saveAchievements();
            }
            if (skillManager != null) {
                skillManager.saveSkills();
            }
            if (collectionManager != null) {
                collectionManager.saveCollections();
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
        
        // New feature commands
        StatsCommand statsCommand = new StatsCommand(statsManager);
        PrestigeCommand prestigeCommand = new PrestigeCommand(prestigeManager);
        ChallengesCommand challengesCommand = new ChallengesCommand(challengeManager);
        CrossbreedCommand crossbreedCommand = new CrossbreedCommand(crossbreedManager);
        LeaderboardCommand leaderboardCommand = new LeaderboardCommand(statsManager);
        
        // v2.0.0 New Commands
        SeasonCommand seasonCommand = new SeasonCommand(this);
        AchievementsCommand achievementsCommand = new AchievementsCommand(this);
        SkillsCommand skillsCommand = new SkillsCommand(this);
        CollectionCommand collectionCommand = new CollectionCommand(this);
        DebugCommand debugCommand = new DebugCommand(this);

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
        
        // Register new commands
        Objects.requireNonNull(getCommand("stats")).setExecutor(statsCommand);
        Objects.requireNonNull(getCommand("stats")).setTabCompleter(statsCommand);
        Objects.requireNonNull(getCommand("prestige")).setExecutor(prestigeCommand);
        Objects.requireNonNull(getCommand("prestige")).setTabCompleter(prestigeCommand);
        Objects.requireNonNull(getCommand("challenges")).setExecutor(challengesCommand);
        Objects.requireNonNull(getCommand("challenges")).setTabCompleter(challengesCommand);
        Objects.requireNonNull(getCommand("crossbreed")).setExecutor(crossbreedCommand);
        Objects.requireNonNull(getCommand("crossbreed")).setTabCompleter(crossbreedCommand);
        Objects.requireNonNull(getCommand("leaderboard")).setExecutor(leaderboardCommand);
        Objects.requireNonNull(getCommand("leaderboard")).setTabCompleter(leaderboardCommand);
        
        // Register v2.0.0 commands
        Objects.requireNonNull(getCommand("season")).setExecutor(seasonCommand);
        Objects.requireNonNull(getCommand("season")).setTabCompleter(seasonCommand);
        Objects.requireNonNull(getCommand("achievements")).setExecutor(achievementsCommand);
        Objects.requireNonNull(getCommand("achievements")).setTabCompleter(achievementsCommand);
        Objects.requireNonNull(getCommand("skills")).setExecutor(skillsCommand);
        Objects.requireNonNull(getCommand("skills")).setTabCompleter(skillsCommand);
        Objects.requireNonNull(getCommand("collection")).setExecutor(collectionCommand);
        Objects.requireNonNull(getCommand("collection")).setTabCompleter(collectionCommand);
        
        // v3.0.0 - Daily rewards command
        DailyCommand dailyCommand = new DailyCommand(this, economyManager, statsManager);
        Objects.requireNonNull(getCommand("daily")).setExecutor(dailyCommand);
        
        // Register admin debug command
        Objects.requireNonNull(getCommand("debug")).setExecutor(debugCommand);
        Objects.requireNonNull(getCommand("debug")).setTabCompleter(debugCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new FarmingListener(this, farmingManager, strainManager), this);
        getServer().getPluginManager().registerEvents(new NPCListener(this, npcManager, economyManager, rankManager, packagingManager, marketShopGUI, blackMarketShopGUI, mobSaleGUI, strainManager), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, dataManager), this);
        getServer().getPluginManager().registerEvents(new ItemDropListener(this, strainManager, packagingManager, droppedBudTracker, jointRollingManager), this);
        
        // Register GUI listener for new features
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
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
    
    public BlackMarketShopGUI getBlackMarketShopGUI() {
        return blackMarketShopGUI;
    }
    
    public MobSaleGUI getMobSaleGUI() {
        return mobSaleGUI;
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
    
    // New feature getters
    public StatsManager getStatsManager() {
        return statsManager;
    }
    
    public PrestigeManager getPrestigeManager() {
        return prestigeManager;
    }
    
    public ChallengeManager getChallengeManager() {
        return challengeManager;
    }
    
    public RandomEventManager getRandomEventManager() {
        return randomEventManager;
    }
    
    public CrossbreedManager getCrossbreedManager() {
        return crossbreedManager;
    }
    
    public AmbientEffectsManager getAmbientEffectsManager() {
        return ambientEffectsManager;
    }
    
    public com.budlords.joint.JointEffectsManager getJointEffectsManager() {
        return jointEffectsManager;
    }
    
    public com.budlords.effects.StrainEffectsManager getStrainEffectsManager() {
        return strainEffectsManager;
    }
    
    // v2.0.0 New Feature Getters
    public SeasonManager getSeasonManager() {
        return seasonManager;
    }
    
    public WeatherManager getWeatherManager() {
        return weatherManager;
    }
    
    public DiseaseManager getDiseaseManager() {
        return diseaseManager;
    }
    
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }
    
    public SkillManager getSkillManager() {
        return skillManager;
    }
    
    public CollectionManager getCollectionManager() {
        return collectionManager;
    }
}
