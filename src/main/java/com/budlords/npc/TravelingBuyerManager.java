package com.budlords.npc;

import com.budlords.BudLords;
import com.budlords.economy.CustomerType;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages traveling buyers who spawn randomly in villages,
 * appear in bossbars, and offer premium deals before disappearing.
 */
public class TravelingBuyerManager {
    
    private final BudLords plugin;
    private final BuyerRegistry buyerRegistry;
    private final NamespacedKey travelingBuyerKey;
    
    private final Map<UUID, TravelingBuyer> activeTravelers; // entityId -> TravelingBuyer
    private final Map<UUID, BossBar> activeBossBars; // entityId -> BossBar
    private BukkitTask spawnTask;
    private BukkitTask cleanupTask;
    
    // Predefined traveling buyer names and personalities
    private static final TravelingBuyerTemplate[] TEMPLATES = {
        new TravelingBuyerTemplate("Big Tony", "§6§l💰 VIP BUYER", CustomerType.VIP_CLIENT, BarColor.YELLOW, 1.5),
        new TravelingBuyerTemplate("The Connoisseur", "§d§l🏆 ELITE COLLECTOR", CustomerType.CONNOISSEUR, BarColor.PURPLE, 1.8),
        new TravelingBuyerTemplate("Fast Eddie", "§a§l⚡ QUICK DEALER", CustomerType.CASUAL_USER, BarColor.GREEN, 1.3),
        new TravelingBuyerTemplate("Mr. Premium", "§6§l✦ PREMIUM BUYER", CustomerType.VIP_CLIENT, BarColor.YELLOW, 1.6),
        new TravelingBuyerTemplate("The Collector", "§b§l📦 BULK BUYER", CustomerType.BULK_PURCHASER, BarColor.BLUE, 1.4),
        new TravelingBuyerTemplate("Lucky Lou", "§e§l🍀 FORTUNE BUYER", CustomerType.CASUAL_USER, BarColor.YELLOW, 1.7),
        new TravelingBuyerTemplate("High Roller", "§c§l💎 HIGH STAKES", CustomerType.VIP_CLIENT, BarColor.RED, 2.0),
        new TravelingBuyerTemplate("Night Owl", "§5§l🦉 NIGHT TRADER", CustomerType.URBAN_DEALER, BarColor.PURPLE, 1.5)
    };
    
    // Spawn settings
    private static final int SPAWN_CHECK_INTERVAL = 6000; // 5 minutes
    private static final double SPAWN_CHANCE = 0.15; // 15% per check
    private static final int MAX_ACTIVE_TRAVELERS = 3;
    private static final long TRAVELER_LIFETIME_MS = 600000; // 10 minutes
    private static final double BOSSBAR_RANGE = 50.0; // Show bossbar within 50 blocks
    
    public TravelingBuyerManager(BudLords plugin, BuyerRegistry buyerRegistry) {
        this.plugin = plugin;
        this.buyerRegistry = buyerRegistry;
        this.travelingBuyerKey = new NamespacedKey(plugin, "traveling_buyer");
        this.activeTravelers = new ConcurrentHashMap<>();
        this.activeBossBars = new ConcurrentHashMap<>();
        
        startSpawnTask();
        startCleanupTask();
        startBossBarUpdateTask();
    }
    
    /**
     * Starts the spawn task for traveling buyers.
     */
    private void startSpawnTask() {
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeTravelers.size() >= MAX_ACTIVE_TRAVELERS) {
                    return;
                }
                
                if (ThreadLocalRandom.current().nextDouble() < SPAWN_CHANCE) {
                    spawnRandomTraveler();
                }
            }
        }.runTaskTimer(plugin, SPAWN_CHECK_INTERVAL, SPAWN_CHECK_INTERVAL);
    }
    
    /**
     * Starts cleanup task to remove expired travelers.
     */
    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                List<UUID> toRemove = new ArrayList<>();
                
                for (Map.Entry<UUID, TravelingBuyer> entry : activeTravelers.entrySet()) {
                    TravelingBuyer traveler = entry.getValue();
                    if (now > traveler.despawnTime || traveler.entity.isDead()) {
                        toRemove.add(entry.getKey());
                    }
                }
                
                toRemove.forEach(TravelingBuyerManager.this::removeTraveler);
            }
        }.runTaskTimer(plugin, 600, 600); // Every 30 seconds
    }
    
    /**
     * Starts task to update bossbars based on player proximity.
     */
    private void startBossBarUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, TravelingBuyer> entry : activeTravelers.entrySet()) {
                    TravelingBuyer traveler = entry.getValue();
                    BossBar bossBar = activeBossBars.get(entry.getKey());
                    
                    if (bossBar == null || traveler.entity.isDead()) {
                        continue;
                    }
                    
                    Location travelerLoc = traveler.entity.getLocation();
                    
                    // Update progress (time remaining)
                    long remaining = traveler.despawnTime - System.currentTimeMillis();
                    double progress = Math.max(0.0, Math.min(1.0, (double) remaining / TRAVELER_LIFETIME_MS));
                    bossBar.setProgress(progress);
                    
                    // Add/remove players based on distance
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(travelerLoc.getWorld())) {
                            double distance = player.getLocation().distance(travelerLoc);
                            
                            if (distance <= BOSSBAR_RANGE) {
                                if (!bossBar.getPlayers().contains(player)) {
                                    bossBar.addPlayer(player);
                                    // Send notification
                                    player.sendMessage("§a§l⚡ " + traveler.template.title);
                                    player.sendMessage("§7" + traveler.template.name + " §7has arrived nearby!");
                                    player.sendMessage("§7Hurry! They'll leave in §e" + (remaining / 60000) + " minutes§7!");
                                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.2f);
                                }
                            } else {
                                if (bossBar.getPlayers().contains(player)) {
                                    bossBar.removePlayer(player);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 40, 40); // Every 2 seconds
    }
    
    /**
     * Spawns a random traveling buyer.
     */
    private void spawnRandomTraveler() {
        // Find a random online player to spawn near
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty()) {
            return;
        }
        
        Player targetPlayer = onlinePlayers.get(ThreadLocalRandom.current().nextInt(onlinePlayers.size()));
        Location spawnLoc = findVillageSpawnLocation(targetPlayer.getLocation());
        
        if (spawnLoc == null) {
            return; // No valid spawn location found
        }
        
        // Select random template
        TravelingBuyerTemplate template = TEMPLATES[ThreadLocalRandom.current().nextInt(TEMPLATES.length)];
        
        // Spawn villager
        Villager villager = (Villager) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.VILLAGER);
        villager.setCustomName(template.title + " " + template.name);
        villager.setCustomNameVisible(true);
        villager.setProfession(Villager.Profession.NONE);
        villager.setAI(true);
        villager.setInvulnerable(true);
        
        // Mark as traveling buyer
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        pdc.set(travelingBuyerKey, PersistentDataType.STRING, "true");
        
        // Create buyer profile
        IndividualBuyer buyer = new IndividualBuyer(UUID.randomUUID(), template.name, template.personality);
        buyerRegistry.addBuyer(buyer);
        
        // Store buyer ID on entity
        NamespacedKey buyerIdKey = new NamespacedKey(plugin, "buyer_id");
        pdc.set(buyerIdKey, PersistentDataType.STRING, buyer.getId().toString());
        
        // Create traveling buyer record
        long despawnTime = System.currentTimeMillis() + TRAVELER_LIFETIME_MS;
        TravelingBuyer traveler = new TravelingBuyer(villager, template, buyer, despawnTime);
        activeTravelers.put(villager.getUniqueId(), traveler);
        
        // Create bossbar
        BossBar bossBar = Bukkit.createBossBar(
            template.title + " §f" + template.name + " §7(+§6" + String.format("%.0f%%", (template.priceBonus - 1.0) * 100) + " §7bonus)",
            template.barColor,
            BarStyle.SOLID
        );
        bossBar.setVisible(true);
        activeBossBars.put(villager.getUniqueId(), bossBar);
        
        // Spawn particles
        spawnLoc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, spawnLoc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        
        plugin.getLogger().info("Spawned traveling buyer: " + template.name + " at " + spawnLoc);
    }
    
    /**
     * Finds a suitable spawn location near a player (preferably in a village).
     */
    private Location findVillageSpawnLocation(Location near) {
        World world = near.getWorld();
        if (world == null) return null;
        
        // Try to find a safe spawn location within 100 blocks
        for (int attempts = 0; attempts < 10; attempts++) {
            int offsetX = ThreadLocalRandom.current().nextInt(-100, 101);
            int offsetZ = ThreadLocalRandom.current().nextInt(-100, 101);
            
            Location testLoc = near.clone().add(offsetX, 0, offsetZ);
            testLoc.setY(world.getHighestBlockYAt(testLoc));
            
            // Check if location is safe
            if (testLoc.getBlock().getType() == Material.AIR && 
                testLoc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                return testLoc;
            }
        }
        
        return null; // Failed to find location
    }
    
    /**
     * Removes a traveling buyer and cleans up.
     */
    private void removeTraveler(UUID entityId) {
        TravelingBuyer traveler = activeTravelers.remove(entityId);
        if (traveler != null) {
            // Remove bossbar
            BossBar bossBar = activeBossBars.remove(entityId);
            if (bossBar != null) {
                bossBar.removeAll();
            }
            
            // Notify nearby players
            Location loc = traveler.entity.getLocation();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(loc.getWorld()) && 
                    player.getLocation().distance(loc) <= BOSSBAR_RANGE) {
                    player.sendMessage("§c§l✗ " + traveler.template.name + " §7has left the area.");
                }
            }
            
            // Remove entity
            traveler.entity.remove();
            
            // Spawn departure particles
            loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        }
    }
    
    /**
     * Checks if an entity is a traveling buyer.
     */
    public boolean isTravelingBuyer(org.bukkit.entity.Entity entity) {
        if (entity == null) return false;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(travelingBuyerKey, PersistentDataType.STRING);
    }
    
    /**
     * Gets the price bonus for a traveling buyer.
     */
    public double getPriceBonus(org.bukkit.entity.Entity entity) {
        TravelingBuyer traveler = activeTravelers.get(entity.getUniqueId());
        return traveler != null ? traveler.template.priceBonus : 1.0;
    }
    
    /**
     * Shuts down the manager.
     */
    public void shutdown() {
        if (spawnTask != null) spawnTask.cancel();
        if (cleanupTask != null) cleanupTask.cancel();
        
        activeBossBars.values().forEach(BossBar::removeAll);
        activeTravelers.values().forEach(t -> t.entity.remove());
        
        activeTravelers.clear();
        activeBossBars.clear();
    }
    
    // Inner classes
    
    private static class TravelingBuyerTemplate {
        final String name;
        final String title;
        final CustomerType personality;
        final BarColor barColor;
        final double priceBonus;
        
        TravelingBuyerTemplate(String name, String title, CustomerType personality, BarColor barColor, double priceBonus) {
            this.name = name;
            this.title = title;
            this.personality = personality;
            this.barColor = barColor;
            this.priceBonus = priceBonus;
        }
    }
    
    private static class TravelingBuyer {
        final Villager entity;
        final TravelingBuyerTemplate template;
        final IndividualBuyer buyer;
        final long despawnTime;
        
        TravelingBuyer(Villager entity, TravelingBuyerTemplate template, IndividualBuyer buyer, long despawnTime) {
            this.entity = entity;
            this.template = template;
            this.buyer = buyer;
            this.despawnTime = despawnTime;
        }
    }
}
