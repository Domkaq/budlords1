package com.budlords.npc;

import com.budlords.BudLords;
import com.budlords.economy.CustomerType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Special limited-time buyer events that spawn premium buyers with unique bonuses.
 * 
 * Features:
 * - Celebrity buyers with massive bonuses
 * - Flash sales with time pressure
 * - Premium buyers who pay 2-3x normal prices
 * - Exclusive requests from special buyers
 * - Random events that create excitement
 */
public class SpecialBuyerEvent {
    
    private final BudLords plugin;
    private final BuyerRegistry registry;
    
    private SpecialBuyer currentEvent;
    private long eventEndTime;
    
    public enum EventType {
        CELEBRITY_VISIT("🌟 Celebrity Visit", 2.5, 3600000), // 1 hour, 2.5x bonus
        HIGH_ROLLER("💎 High Roller", 2.0, 1800000), // 30 min, 2.0x bonus
        BULK_BUYER_CONVENTION("📦 Bulk Convention", 1.8, 7200000), // 2 hours, 1.8x bonus
        CONNOISSEUR_SHOWCASE("🏆 Connoisseur Event", 2.2, 3600000), // 1 hour, 2.2x bonus
        FLASH_SALE("⚡ Flash Sale", 3.0, 600000); // 10 min, 3.0x bonus!
        
        private final String displayName;
        private final double priceMultiplier;
        private final long durationMs;
        
        EventType(String displayName, double priceMultiplier, long durationMs) {
            this.displayName = displayName;
            this.priceMultiplier = priceMultiplier;
            this.durationMs = durationMs;
        }
        
        public String getDisplayName() { return displayName; }
        public double getPriceMultiplier() { return priceMultiplier; }
        public long getDurationMs() { return durationMs; }
    }
    
    public SpecialBuyerEvent(BudLords plugin, BuyerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        startEventGenerationTask();
    }
    
    /**
     * Starts background task to generate random events.
     */
    private void startEventGenerationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentEvent == null || System.currentTimeMillis() > eventEndTime) {
                    // 5% chance per check to generate event (every 10 minutes)
                    if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                        generateRandomEvent();
                    }
                }
            }
        }.runTaskTimer(plugin, 12000, 12000); // Every 10 minutes
    }
    
    /**
     * Generates a random special event.
     */
    private void generateRandomEvent() {
        EventType[] types = EventType.values();
        EventType type = types[ThreadLocalRandom.current().nextInt(types.length)];
        
        currentEvent = new SpecialBuyer(type);
        eventEndTime = System.currentTimeMillis() + type.getDurationMs();
        
        // Announce to all online players
        String announcement = "§6§l═══════════════════════════════";
        String title = "§e§l" + type.getDisplayName().toUpperCase();
        String description = getEventDescription(type);
        String multiplier = "§a§lPrice Bonus: §6" + String.format("%.0f%%", (type.getPriceMultiplier() - 1.0) * 100) + " extra!";
        String duration = "§7Duration: §e" + (type.getDurationMs() / 60000) + " minutes";
        String footer = "§6§l═══════════════════════════════";
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("");
            player.sendMessage(announcement);
            player.sendMessage(title);
            player.sendMessage("");
            player.sendMessage(description);
            player.sendMessage(multiplier);
            player.sendMessage(duration);
            player.sendMessage("");
            player.sendMessage("§e" + currentEvent.getName() + " §7is waiting!");
            player.sendMessage(footer);
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);
        }
    }
    
    private String getEventDescription(EventType type) {
        return switch (type) {
            case CELEBRITY_VISIT -> "§7A famous buyer is in town and paying premium prices!";
            case HIGH_ROLLER -> "§7A wealthy buyer seeks high-quality products!";
            case BULK_BUYER_CONVENTION -> "§7Bulk buyers are gathering, bring large quantities!";
            case CONNOISSEUR_SHOWCASE -> "§7Elite connoisseurs want only the finest strains!";
            case FLASH_SALE -> "§7§lURGENT: Premium buyer needs products NOW!";
        };
    }
    
    /**
     * Gets the current active special buyer.
     */
    public SpecialBuyer getCurrentEvent() {
        if (currentEvent != null && System.currentTimeMillis() > eventEndTime) {
            currentEvent = null;
        }
        return currentEvent;
    }
    
    /**
     * Checks if an event is currently active.
     */
    public boolean isEventActive() {
        return getCurrentEvent() != null;
    }
    
    /**
     * Gets time remaining in current event (ms).
     */
    public long getTimeRemaining() {
        if (!isEventActive()) return 0;
        return Math.max(0, eventEndTime - System.currentTimeMillis());
    }
    
    /**
     * Gets time remaining as formatted string.
     */
    public String getTimeRemainingFormatted() {
        long ms = getTimeRemaining();
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    /**
     * Represents a special buyer for an event.
     */
    public class SpecialBuyer {
        private final EventType type;
        private final String name;
        private final double priceMultiplier;
        
        public SpecialBuyer(EventType type) {
            this.type = type;
            this.priceMultiplier = type.getPriceMultiplier();
            this.name = generateSpecialName(type);
        }
        
        private String generateSpecialName(EventType type) {
            return switch (type) {
                case CELEBRITY_VISIT -> "§6★ " + getRandomCelebrityName() + " §6★";
                case HIGH_ROLLER -> "§5💎 " + getRandomWealthyName();
                case BULK_BUYER_CONVENTION -> "§e📦 " + getRandomBusinessName();
                case CONNOISSEUR_SHOWCASE -> "§d🏆 " + getRandomEliteName();
                case FLASH_SALE -> "§c⚡ " + getRandomUrgentName();
            };
        }
        
        private String getRandomCelebrityName() {
            String[] names = {"Dr. Greenthumb", "Snoop D-O-Double-G", "Willie Nelson", 
                            "Tommy Chong", "Cheech Marin", "Bob Marley Jr."};
            return names[ThreadLocalRandom.current().nextInt(names.length)];
        }
        
        private String getRandomWealthyName() {
            String[] names = {"Mr. Moneybags", "Baron von Rich", "Lady Luxe", 
                            "Count Cash", "Duke Diamond"};
            return names[ThreadLocalRandom.current().nextInt(names.length)];
        }
        
        private String getRandomBusinessName() {
            String[] names = {"Wholesale Wally", "Bulk Bob", "Corporate Carl", 
                            "Distribution Dan"};
            return names[ThreadLocalRandom.current().nextInt(names.length)];
        }
        
        private String getRandomEliteName() {
            String[] names = {"Sommelier Steve", "Critic Charles", "Judge Judy", 
                            "Expert Elaine"};
            return names[ThreadLocalRandom.current().nextInt(names.length)];
        }
        
        private String getRandomUrgentName() {
            String[] names = {"Speedy Sam", "Rush Roger", "Quick Quinn", 
                            "Hasty Harry"};
            return names[ThreadLocalRandom.current().nextInt(names.length)];
        }
        
        public EventType getType() { return type; }
        public String getName() { return name; }
        public double getPriceMultiplier() { return priceMultiplier; }
    }
    
    /**
     * Shuts down the event system.
     */
    public void shutdown() {
        currentEvent = null;
    }
}
