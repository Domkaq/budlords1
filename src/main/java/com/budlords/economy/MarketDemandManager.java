package com.budlords.economy;

import com.budlords.BudLords;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dynamic market system for BudLords v3.0.0.
 * Prices fluctuate based on supply/demand and market events.
 */
public class MarketDemandManager {

    private final BudLords plugin;
    
    // Current market demand multipliers per strain (1.0 = normal)
    private final Map<String, Double> strainDemand;
    
    // Market event tracking
    private String currentMarketEvent;
    private long eventEndTime;
    private double eventMultiplier;
    
    // Constants for demand fluctuation
    private static final double MIN_DEMAND = 0.5;      // 50% of base price
    private static final double MAX_DEMAND = 2.0;      // 200% of base price
    private static final double FLUCTUATION_RATE = 0.05; // 5% change per update
    
    // Update interval (in ticks - 20 ticks = 1 second)
    private static final long DEMAND_UPDATE_INTERVAL = 12000L; // 10 minutes
    private static final long EVENT_CHECK_INTERVAL = 36000L;   // 30 minutes
    
    private BukkitTask demandUpdateTask;
    private BukkitTask eventCheckTask;
    
    // Market events with their multipliers
    private static final String[] MARKET_EVENTS = {
        "BUYER_RUSH",        // +30% all prices - "A wave of buyers has hit the market!"
        "POLICE_CRACKDOWN",  // -20% all prices - "Police activity is high, buyers are cautious..."
        "FESTIVAL_SEASON",   // +50% prices - "Festival season brings high demand!"
        "SUPPLY_SHORTAGE",   // +40% rare strains - "Supply shortage affects rare strains!"
        "MARKET_CRASH",      // -30% all prices - "Market oversupply, prices dropping!"
        "PREMIUM_DEMAND",    // +25% legendary - "Connoisseurs seeking premium product!"
        "NORMAL"             // No change - "Market conditions are stable."
    };

    public MarketDemandManager(BudLords plugin) {
        this.plugin = plugin;
        this.strainDemand = new ConcurrentHashMap<>();
        this.currentMarketEvent = "NORMAL";
        this.eventEndTime = 0;
        this.eventMultiplier = 1.0;
        
        loadDemandData();
        startDemandUpdateTask();
        startEventCheckTask();
    }
    
    private void loadDemandData() {
        FileConfiguration config = plugin.getDataManager().getPlayersConfig();
        ConfigurationSection demandSection = config.getConfigurationSection("market-demand");
        
        if (demandSection != null) {
            for (String strainId : demandSection.getKeys(false)) {
                double demand = demandSection.getDouble(strainId, 1.0);
                strainDemand.put(strainId, Math.max(MIN_DEMAND, Math.min(MAX_DEMAND, demand)));
            }
            
            currentMarketEvent = config.getString("market-event.type", "NORMAL");
            eventEndTime = config.getLong("market-event.end-time", 0);
            eventMultiplier = config.getDouble("market-event.multiplier", 1.0);
        }
        
        // Check if event has expired
        if (System.currentTimeMillis() > eventEndTime) {
            currentMarketEvent = "NORMAL";
            eventMultiplier = 1.0;
        }
    }
    
    public void saveDemandData() {
        FileConfiguration config = plugin.getDataManager().getPlayersConfig();
        
        for (Map.Entry<String, Double> entry : strainDemand.entrySet()) {
            config.set("market-demand." + entry.getKey(), entry.getValue());
        }
        
        config.set("market-event.type", currentMarketEvent);
        config.set("market-event.end-time", eventEndTime);
        config.set("market-event.multiplier", eventMultiplier);
        
        plugin.getDataManager().savePlayers();
    }
    
    private void startDemandUpdateTask() {
        // Initialize strains at startup rather than in the task
        initializeStrainDemand();
        
        demandUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Randomly fluctuate demand for each strain
            for (String strainId : strainDemand.keySet()) {
                double current = strainDemand.get(strainId);
                double change = (ThreadLocalRandom.current().nextDouble() - 0.5) * FLUCTUATION_RATE * 2;
                double newDemand = Math.max(MIN_DEMAND, Math.min(MAX_DEMAND, current + change));
                strainDemand.put(strainId, newDemand);
            }
        }, DEMAND_UPDATE_INTERVAL, DEMAND_UPDATE_INTERVAL);
    }
    
    /**
     * Initializes demand values for all strains that don't have tracked demand.
     * Called once at startup.
     */
    private void initializeStrainDemand() {
        if (plugin.getStrainManager() != null) {
            for (var strain : plugin.getStrainManager().getAllStrains()) {
                if (!strainDemand.containsKey(strain.getId())) {
                    // Initialize with slight random variance around 1.0
                    double initial = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2;
                    strainDemand.put(strain.getId(), initial);
                }
            }
        }
    }
    
    private void startEventCheckTask() {
        eventCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Check if current event has expired
            if (System.currentTimeMillis() > eventEndTime && !currentMarketEvent.equals("NORMAL")) {
                currentMarketEvent = "NORMAL";
                eventMultiplier = 1.0;
                broadcastMarketNews("§e[Market News] §7Market conditions have returned to normal.");
            }
            
            // Random chance to trigger a new event (10% chance per check)
            if (currentMarketEvent.equals("NORMAL") && ThreadLocalRandom.current().nextDouble() < 0.10) {
                triggerRandomEvent();
            }
        }, EVENT_CHECK_INTERVAL, EVENT_CHECK_INTERVAL);
    }
    
    private void triggerRandomEvent() {
        int eventIndex = ThreadLocalRandom.current().nextInt(MARKET_EVENTS.length - 1); // Exclude NORMAL
        currentMarketEvent = MARKET_EVENTS[eventIndex];
        
        // Event duration: 15-45 minutes
        long duration = (15 + ThreadLocalRandom.current().nextInt(30)) * 60 * 1000L;
        eventEndTime = System.currentTimeMillis() + duration;
        
        // Set multiplier based on event type
        switch (currentMarketEvent) {
            case "BUYER_RUSH" -> {
                eventMultiplier = 1.30;
                broadcastMarketNews("§a[Market News] §e⬆ BUYER RUSH! §7A wave of buyers has hit the market! Prices are up!");
            }
            case "POLICE_CRACKDOWN" -> {
                eventMultiplier = 0.80;
                broadcastMarketNews("§c[Market News] §e⬇ POLICE CRACKDOWN! §7Buyers are cautious. Prices have dropped.");
            }
            case "FESTIVAL_SEASON" -> {
                eventMultiplier = 1.50;
                broadcastMarketNews("§d[Market News] §e⬆⬆ FESTIVAL SEASON! §7High demand! Prices are soaring!");
            }
            case "SUPPLY_SHORTAGE" -> {
                eventMultiplier = 1.40;
                broadcastMarketNews("§6[Market News] §e⬆ SUPPLY SHORTAGE! §7Rare strains are in high demand!");
            }
            case "MARKET_CRASH" -> {
                eventMultiplier = 0.70;
                broadcastMarketNews("§c[Market News] §c⬇⬇ MARKET CRASH! §7Oversupply has crashed prices!");
            }
            case "PREMIUM_DEMAND" -> {
                eventMultiplier = 1.25;
                broadcastMarketNews("§5[Market News] §e⬆ PREMIUM DEMAND! §7Connoisseurs are paying top dollar for quality!");
            }
            default -> eventMultiplier = 1.0;
        }
        
        saveDemandData();
    }
    
    private void broadcastMarketNews(String message) {
        // Only send to players with permission or who haven't opted out
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("budlords.market.news"))
            .forEach(player -> player.sendMessage(message));
        plugin.getLogger().info("[Market] " + currentMarketEvent + " event triggered");
    }
    
    /**
     * Gets the demand multiplier for a specific strain.
     * Combines base strain demand with current market event.
     */
    public double getDemandMultiplier(String strainId) {
        double baseDemand = strainDemand.getOrDefault(strainId, 1.0);
        return baseDemand * eventMultiplier;
    }
    
    /**
     * Gets the current market event.
     */
    public String getCurrentMarketEvent() {
        return currentMarketEvent;
    }
    
    /**
     * Gets the event-specific multiplier.
     */
    public double getEventMultiplier() {
        return eventMultiplier;
    }
    
    /**
     * Gets the time remaining for the current event in minutes.
     */
    public long getEventTimeRemainingMinutes() {
        if (eventEndTime <= 0 || currentMarketEvent.equals("NORMAL")) {
            return 0;
        }
        long remaining = eventEndTime - System.currentTimeMillis();
        return Math.max(0, remaining / 60000);
    }
    
    /**
     * Records a sale for demand calculation.
     * Selling reduces demand slightly over time.
     */
    public void recordSale(String strainId, int amount) {
        double current = strainDemand.getOrDefault(strainId, 1.0);
        // Each sale slightly decreases demand (market saturation)
        double decrease = 0.01 * amount;
        strainDemand.put(strainId, Math.max(MIN_DEMAND, current - decrease));
    }
    
    /**
     * Gets the formatted market status for display.
     */
    public String getMarketStatusDisplay() {
        StringBuilder status = new StringBuilder();
        status.append("§6§l═══ Market Status ═══\n");
        status.append("§7Current Event: ");
        
        switch (currentMarketEvent) {
            case "BUYER_RUSH" -> status.append("§a⬆ Buyer Rush (+30%)");
            case "POLICE_CRACKDOWN" -> status.append("§c⬇ Police Crackdown (-20%)");
            case "FESTIVAL_SEASON" -> status.append("§d⬆⬆ Festival Season (+50%)");
            case "SUPPLY_SHORTAGE" -> status.append("§6⬆ Supply Shortage (+40%)");
            case "MARKET_CRASH" -> status.append("§c⬇⬇ Market Crash (-30%)");
            case "PREMIUM_DEMAND" -> status.append("§5⬆ Premium Demand (+25%)");
            default -> status.append("§7Stable");
        }
        
        if (!currentMarketEvent.equals("NORMAL")) {
            status.append("\n§7Time Remaining: §e").append(getEventTimeRemainingMinutes()).append(" minutes");
        }
        
        return status.toString();
    }
    
    public void shutdown() {
        if (demandUpdateTask != null) {
            demandUpdateTask.cancel();
        }
        if (eventCheckTask != null) {
            eventCheckTask.cancel();
        }
        saveDemandData();
    }
}
