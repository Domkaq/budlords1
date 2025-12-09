package com.budlords.npc;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Manages buyer requests - a quest-like system where buyers post specific
 * purchase requests that players can fulfill for bonus rewards.
 * 
 * Features:
 * - Dynamic request generation based on buyer personality
 * - Time-limited requests with urgency levels
 * - Bonus payments for fulfillment
 * - Reputation penalties for expired requests
 * - Recommendation system for best requests to fulfill
 */
public class BuyerRequestManager {
    
    private final BudLords plugin;
    private final BuyerRegistry buyerRegistry;
    private final Map<UUID, BuyerRequest> activeRequests; // requestId -> request
    private final Map<UUID, List<UUID>> buyerRequests; // buyerId -> list of requestIds
    private final Map<UUID, org.bukkit.boss.BossBar> requestBossBars; // requestId -> bossbar
    
    // Request generation settings
    private static final int MAX_ACTIVE_REQUESTS = 10;
    private static final int REQUEST_GENERATION_INTERVAL = 6000; // 5 minutes in ticks
    private static final double REQUEST_GENERATION_CHANCE = 0.3; // 30% chance per cycle
    private static final double HIGH_VALUE_THRESHOLD = 5000.0; // Show bossbar for requests > $5000
    
    public BuyerRequestManager(BudLords plugin, BuyerRegistry buyerRegistry) {
        this.plugin = plugin;
        this.buyerRegistry = buyerRegistry;
        this.activeRequests = new ConcurrentHashMap<>();
        this.buyerRequests = new ConcurrentHashMap<>();
        this.requestBossBars = new ConcurrentHashMap<>();
        
        startRequestGenerationTask();
        startExpirationCheckTask();
        startBossBarUpdateTask();
    }
    
    /**
     * Starts the background task that generates new requests.
     */
    private void startRequestGenerationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeRequests.size() < MAX_ACTIVE_REQUESTS) {
                    if (ThreadLocalRandom.current().nextDouble() < REQUEST_GENERATION_CHANCE) {
                        generateRandomRequest();
                    }
                }
            }
        }.runTaskTimer(plugin, REQUEST_GENERATION_INTERVAL, REQUEST_GENERATION_INTERVAL);
    }
    
    /**
     * Starts the background task that checks for expired requests.
     */
    private void startExpirationCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<UUID> expiredIds = new ArrayList<>();
                
                for (BuyerRequest request : activeRequests.values()) {
                    if (request.checkExpiration()) {
                        expiredIds.add(request.getRequestId());
                        
                        // Notify online players about expiration
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage("§c✗ Request Expired: §7" + request.getBuyerName() + 
                                             "'s request has expired!");
                        }
                    }
                }
                
                // Remove expired requests
                for (UUID id : expiredIds) {
                    BuyerRequest request = activeRequests.remove(id);
                    if (request != null) {
                        List<UUID> buyerReqs = buyerRequests.get(request.getBuyerId());
                        if (buyerReqs != null) {
                            buyerReqs.remove(id);
                        }
                        
                        // Remove bossbar if exists
                        org.bukkit.boss.BossBar bossBar = requestBossBars.remove(id);
                        if (bossBar != null) {
                            bossBar.removeAll();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1200, 1200); // Every minute
    }
    
    /**
     * Starts the bossbar update task for high-value requests.
     */
    private void startBossBarUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, BuyerRequest> entry : activeRequests.entrySet()) {
                    BuyerRequest request = entry.getValue();
                    
                    // Only show bossbar for high-value requests
                    if (request.getBonusPayment() >= HIGH_VALUE_THRESHOLD) {
                        org.bukkit.boss.BossBar bossBar = requestBossBars.get(entry.getKey());
                        
                        if (bossBar == null) {
                            // Create new bossbar
                            bossBar = Bukkit.createBossBar(
                                "§6§l🔥 HIGH VALUE REQUEST §f" + request.getBuyerName() + " §7(+§a$" + String.format("%.0f", request.getBonusPayment()) + "§7)",
                                org.bukkit.boss.BarColor.YELLOW,
                                org.bukkit.boss.BarStyle.SEGMENTED_10
                            );
                            bossBar.setVisible(true);
                            requestBossBars.put(entry.getKey(), bossBar);
                            
                            // Add all online players
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                bossBar.addPlayer(player);
                            }
                        }
                        
                        // Update progress based on time remaining
                        long timeRemaining = request.getExpirationTime() - System.currentTimeMillis();
                        long totalDuration = request.getDurationHours() * 3600000L;
                        double progress = Math.max(0.0, Math.min(1.0, (double) timeRemaining / totalDuration));
                        bossBar.setProgress(progress);
                        
                        // Change color based on urgency
                        if (progress < 0.2) {
                            bossBar.setColor(org.bukkit.boss.BarColor.RED);
                        } else if (progress < 0.5) {
                            bossBar.setColor(org.bukkit.boss.BarColor.YELLOW);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 40, 40); // Every 2 seconds
    }
    
    /**
     * Generates a random request from a random buyer.
     */
    private void generateRandomRequest() {
        Collection<IndividualBuyer> buyers = buyerRegistry.getAllBuyers();
        if (buyers.isEmpty()) return;
        
        // IMPORTANT: Only generate requests from buyers who have made at least one purchase
        // This ensures only REAL, interacted-with buyers generate requests
        List<IndividualBuyer> eligibleBuyers = buyers.stream()
            .filter(b -> b.getTotalPurchases() > 0)
            .collect(Collectors.toList());
            
        if (eligibleBuyers.isEmpty()) {
            // No buyers with purchase history yet - don't generate fake requests
            return;
        }
        
        // Select a random buyer from eligible ones
        IndividualBuyer buyer = eligibleBuyers.get(ThreadLocalRandom.current().nextInt(eligibleBuyers.size()));
        
        // Generate request based on buyer personality
        BuyerRequest request = generateRequestForBuyer(buyer);
        
        if (request != null) {
            activeRequests.put(request.getRequestId(), request);
            buyerRequests.computeIfAbsent(buyer.getId(), k -> new ArrayList<>()).add(request.getRequestId());
            
            // Notify all online players about this request
            // (Since this buyer has purchase history, they're a real buyer)
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage("");
                player.sendMessage("§6§l📋 NEW REQUEST!");
                player.sendMessage("§e" + buyer.getName() + "§7: " + request.getRequestMessage());
                player.sendMessage("§7Bonus: §a+$" + String.format("%.2f", request.getBonusPayment()));
                player.sendMessage("§7Expires in: §e" + request.getHoursRemaining() + " hours");
                
                if (request.getBonusPayment() >= HIGH_VALUE_THRESHOLD) {
                    player.sendMessage("§6§l🔥 HIGH VALUE REQUEST - Check your bossbar!");
                }
                
                player.sendMessage("");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.5f);
            }
        }
    }
    
    /**
     * Generates a request appropriate for a buyer's personality.
     */
    private BuyerRequest generateRequestForBuyer(IndividualBuyer buyer) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        BuyerRequest.RequestType type;
        String strainId = null;
        Strain.Rarity rarity = buyer.getFavoriteRarity();
        StarRating quality = StarRating.fromValue(4); // Default 4★
        int quantity = 10;
        double bonusPayment = 500.0;
        long durationHours = 24;
        
        // Generate request based on personality
        switch (buyer.getPersonality()) {
            case CONNOISSEUR:
                type = BuyerRequest.RequestType.ANY_HIGH_QUALITY;
                quality = StarRating.fromValue(5);
                quantity = random.nextInt(5, 15);
                bonusPayment = 800.0;
                durationHours = 48;
                break;
                
            case BULK_BUYER:
                type = BuyerRequest.RequestType.BULK_ORDER;
                quantity = random.nextInt(30, 60);
                bonusPayment = 1200.0;
                durationHours = 72;
                break;
                
            case COLLECTOR:
                type = random.nextBoolean() ? 
                    BuyerRequest.RequestType.RARE_STRAIN : 
                    BuyerRequest.RequestType.COLLECTOR_QUEST;
                rarity = Strain.Rarity.LEGENDARY;
                quantity = 5;
                bonusPayment = 1500.0;
                durationHours = 96;
                break;
                
            case VIP_CLIENT:
                type = BuyerRequest.RequestType.ANY_HIGH_QUALITY;
                quality = StarRating.fromValue(5);
                quantity = random.nextInt(10, 20);
                bonusPayment = 2000.0;
                durationHours = 48;
                break;
                
            case PARTY_ANIMAL:
                type = BuyerRequest.RequestType.BULK_ORDER;
                quantity = random.nextInt(20, 40);
                bonusPayment = 600.0;
                durationHours = 24;
                break;
                
            default:
                // For other personalities, use specific strain if they have favorites
                if (!buyer.getFavoriteStrains().isEmpty()) {
                    type = BuyerRequest.RequestType.SPECIFIC_STRAIN;
                    strainId = buyer.getFavoriteStrains().get(0);
                    quantity = random.nextInt(10, 25);
                    bonusPayment = 700.0;
                    durationHours = 36;
                } else {
                    type = BuyerRequest.RequestType.BULK_ORDER;
                    quantity = random.nextInt(15, 30);
                    bonusPayment = 500.0;
                    durationHours = 24;
                }
        }
        
        return new BuyerRequest(buyer.getId(), buyer.getName(), type, 
                              strainId, rarity, quality, quantity, 
                              bonusPayment, durationHours);
    }
    
    /**
     * Gets all active requests.
     */
    public List<BuyerRequest> getActiveRequests() {
        return activeRequests.values().stream()
            .filter(r -> !r.isFulfilled() && !r.isExpired())
            .sorted((a, b) -> Long.compare(a.getExpirationTime(), b.getExpirationTime()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets requests for a specific buyer.
     */
    public List<BuyerRequest> getRequestsForBuyer(UUID buyerId) {
        List<UUID> requestIds = buyerRequests.get(buyerId);
        if (requestIds == null) return new ArrayList<>();
        
        return requestIds.stream()
            .map(activeRequests::get)
            .filter(Objects::nonNull)
            .filter(r -> !r.isFulfilled() && !r.isExpired())
            .collect(Collectors.toList());
    }
    
    /**
     * Gets requests sorted by urgency.
     */
    public List<BuyerRequest> getUrgentRequests() {
        return activeRequests.values().stream()
            .filter(r -> !r.isFulfilled() && !r.isExpired())
            .filter(r -> r.getHoursRemaining() <= 6)
            .sorted((a, b) -> Long.compare(a.getHoursRemaining(), b.getHoursRemaining()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets requests with highest bonus payments.
     */
    public List<BuyerRequest> getHighValueRequests() {
        return activeRequests.values().stream()
            .filter(r -> !r.isFulfilled() && !r.isExpired())
            .sorted((a, b) -> Double.compare(b.getBonusPayment(), a.getBonusPayment()))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * Attempts to fulfill a request with the given sale.
     */
    public BuyerRequest checkAndFulfillRequest(UUID buyerId, String strainId, 
                                              Strain.Rarity rarity, StarRating quality, 
                                              int quantity) {
        List<BuyerRequest> buyerReqs = getRequestsForBuyer(buyerId);
        
        for (BuyerRequest request : buyerReqs) {
            if (request.matches(strainId, rarity, quality, quantity)) {
                request.fulfill();
                return request;
            }
        }
        
        return null;
    }
    
    /**
     * Gets statistics about the request system.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<BuyerRequest> active = getActiveRequests();
        stats.put("total_active", active.size());
        stats.put("urgent", getUrgentRequests().size());
        stats.put("high_value", getHighValueRequests().size());
        
        double totalBonuses = active.stream()
            .mapToDouble(BuyerRequest::getBonusPayment)
            .sum();
        stats.put("total_potential_bonuses", totalBonuses);
        
        return stats;
    }
    
    /**
     * Manually creates a request for testing or admin purposes.
     */
    public BuyerRequest createCustomRequest(UUID buyerId, BuyerRequest.RequestType type,
                                          String strainId, Strain.Rarity rarity,
                                          StarRating quality, int quantity,
                                          double bonus, long hours) {
        IndividualBuyer buyer = buyerRegistry.getBuyer(buyerId);
        if (buyer == null) return null;
        
        BuyerRequest request = new BuyerRequest(buyerId, buyer.getName(), type,
                                                strainId, rarity, quality, quantity,
                                                bonus, hours);
        
        activeRequests.put(request.getRequestId(), request);
        buyerRequests.computeIfAbsent(buyerId, k -> new ArrayList<>()).add(request.getRequestId());
        
        return request;
    }
    
    /**
     * Cleans up on shutdown.
     */
    public void shutdown() {
        // Remove all bossbars
        for (org.bukkit.boss.BossBar bossBar : requestBossBars.values()) {
            bossBar.removeAll();
        }
        requestBossBars.clear();
        activeRequests.clear();
        buyerRequests.clear();
    }
}
