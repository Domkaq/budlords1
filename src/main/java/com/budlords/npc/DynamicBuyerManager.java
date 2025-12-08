package com.budlords.npc;

import com.budlords.BudLords;
import com.budlords.economy.CustomerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages dynamic buyer profiles for spawned mobs.
 * Handles generation of buyer personalities, demand indicators, and name tags.
 */
public class DynamicBuyerManager {
    
    private final BudLords plugin;
    private final BuyerRegistry buyerRegistry;
    private final NamespacedKey buyerIdKey;
    private final NamespacedKey demandKey;
    private final NamespacedKey lastDemandUpdateKey;
    
    // Track entities with demand indicators (UUID -> last update time)
    private final Map<UUID, Long> demandIndicators;
    
    // Configurable values loaded from config
    private double buyerGenerationChance = 0.3; // 30% by default
    private long demandCooldownMs = 300000; // 5 minutes default
    
    public DynamicBuyerManager(BudLords plugin, BuyerRegistry buyerRegistry) {
        this.plugin = plugin;
        this.buyerRegistry = buyerRegistry;
        this.buyerIdKey = new NamespacedKey(plugin, "buyer_id");
        this.demandKey = new NamespacedKey(plugin, "has_demand");
        this.lastDemandUpdateKey = new NamespacedKey(plugin, "last_demand_update");
        this.demandIndicators = new ConcurrentHashMap<>();
        
        // Load config values
        loadConfig();
        
        // Start demand indicator task
        startDemandIndicatorTask();
    }
    
    /**
     * Loads configuration values.
     */
    private void loadConfig() {
        buyerGenerationChance = plugin.getConfig().getDouble("trading.buyer-generation-chance", 0.3);
        int cooldownMinutes = plugin.getConfig().getInt("trading.buyer-demand-cooldown-minutes", 5);
        demandCooldownMs = cooldownMinutes * 60 * 1000L; // Convert minutes to milliseconds
    }
    
    /**
     * Checks if an entity type is allowed to become a buyer based on config.
     */
    public boolean canEntityBeBuyer(Entity entity) {
        if (entity == null) return false;
        
        EntityType type = entity.getType();
        String configPath = "trading.allowed-mobs." + type.name().toLowerCase();
        return plugin.getConfig().getBoolean(configPath, false);
    }
    
    /**
     * Gets or creates a buyer profile for an entity.
     * Returns null if entity cannot be a buyer or generation fails.
     */
    public IndividualBuyer getOrCreateBuyer(Entity entity) {
        if (!canEntityBeBuyer(entity)) {
            return null;
        }
        
        if (!(entity instanceof LivingEntity)) {
            return null;
        }
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        
        // Check if entity already has a buyer ID
        if (pdc.has(buyerIdKey, PersistentDataType.STRING)) {
            String buyerIdStr = pdc.get(buyerIdKey, PersistentDataType.STRING);
            try {
                UUID buyerId = UUID.fromString(buyerIdStr);
                IndividualBuyer buyer = buyerRegistry.getBuyer(buyerId);
                if (buyer != null) {
                    return buyer;
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID, generate new buyer
            }
        }
        
        // Generate new buyer with chance
        if (ThreadLocalRandom.current().nextDouble() >= buyerGenerationChance) {
            return null; // Failed generation chance
        }
        
        // Create new buyer
        CustomerType personality = getRandomPersonalityForEntity(entity.getType());
        Set<String> usedNames = new HashSet<>();
        // Collect used names from all buyers to ensure uniqueness
        buyerRegistry.getAllBuyers().forEach(b -> usedNames.add(b.getName()));
        
        String name = generateNameForEntity(entity.getType(), usedNames);
        IndividualBuyer buyer = new IndividualBuyer(UUID.randomUUID(), name, personality);
        
        // Register buyer
        buyerRegistry.addBuyer(buyer);
        
        // Store buyer ID on entity
        pdc.set(buyerIdKey, PersistentDataType.STRING, buyer.getId().toString());
        
        // Initialize demand
        updateDemand(entity, true);
        
        // Set custom name tag
        updateNameTag(entity, buyer);
        
        plugin.getLogger().info("Generated buyer profile: " + name + " (" + personality + ") for " + entity.getType());
        
        return buyer;
    }
    
    /**
     * Gets the buyer associated with an entity, if any.
     */
    public IndividualBuyer getBuyer(Entity entity) {
        if (entity == null) return null;
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        
        if (!pdc.has(buyerIdKey, PersistentDataType.STRING)) {
            return null;
        }
        
        String buyerIdStr = pdc.get(buyerIdKey, PersistentDataType.STRING);
        try {
            UUID buyerId = UUID.fromString(buyerIdStr);
            return buyerRegistry.getBuyer(buyerId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Updates the demand status for an entity.
     */
    public void updateDemand(Entity entity, boolean hasDemand) {
        if (!(entity instanceof LivingEntity)) return;
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(demandKey, PersistentDataType.BYTE, hasDemand ? (byte) 1 : (byte) 0);
        pdc.set(lastDemandUpdateKey, PersistentDataType.LONG, System.currentTimeMillis());
        
        if (hasDemand) {
            demandIndicators.put(entity.getUniqueId(), System.currentTimeMillis());
        } else {
            demandIndicators.remove(entity.getUniqueId());
        }
    }
    
    /**
     * Checks if an entity currently has demand.
     */
    public boolean hasDemand(Entity entity) {
        if (entity == null) return false;
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        
        if (!pdc.has(demandKey, PersistentDataType.BYTE)) {
            return false;
        }
        
        // Check if demand has expired
        long lastUpdate = pdc.getOrDefault(lastDemandUpdateKey, PersistentDataType.LONG, 0L);
        long elapsed = System.currentTimeMillis() - lastUpdate;
        
        if (elapsed > demandCooldownMs) {
            // Demand expired, randomly refresh
            boolean newDemand = ThreadLocalRandom.current().nextDouble() < 0.5; // 50% chance
            updateDemand(entity, newDemand);
            return newDemand;
        }
        
        return pdc.get(demandKey, PersistentDataType.BYTE) == 1;
    }
    
    /**
     * Updates the name tag for a buyer entity with enhanced visual indicators.
     * Shows: name, demand status, reputation level, and purchase satisfaction.
     * Uses deprecated setCustomName() for Bukkit/Spigot compatibility.
     * Paper servers can replace with Adventure API's customName(Component) method.
     */
    @SuppressWarnings("deprecation")
    private void updateNameTag(Entity entity, IndividualBuyer buyer) {
        if (!(entity instanceof LivingEntity livingEntity)) return;
        
        StringBuilder displayName = new StringBuilder();
        
        // Add demand indicator if buyer wants to buy
        if (hasDemand(entity)) {
            displayName.append("§a§l💰 "); // Money bag emoji = wants to buy
        }
        
        // Add reputation level stars
        int totalPurchases = buyer.getTotalPurchases();
        if (totalPurchases >= 50) {
            displayName.append("§5⭐⭐⭐ "); // VIP customer (3 stars)
        } else if (totalPurchases >= 25) {
            displayName.append("§d⭐⭐ "); // Premium customer (2 stars)
        } else if (totalPurchases >= 10) {
            displayName.append("§6⭐ "); // Regular customer (1 star)
        }
        
        // Add buyer name with personality color
        String nameColor = getPersonalityColor(buyer.getPersonality());
        displayName.append(nameColor).append(buyer.getName());
        
        // Add mood indicator based on recent interactions
        String mood = buyer.getCurrentMood();
        if (mood != null) {
            switch (mood) {
                case "happy" -> displayName.append(" §a😊"); // Happy face
                case "satisfied" -> displayName.append(" §e✓"); // Check mark
                case "neutral" -> { } // No indicator
                case "disappointed" -> displayName.append(" §7😐"); // Neutral face
                case "angry" -> displayName.append(" §c😠"); // Angry face
            }
        }
        
        livingEntity.setCustomName(displayName.toString());
        livingEntity.setCustomNameVisible(true);
    }
    
    /**
     * Gets the color code for a buyer's personality type.
     */
    private String getPersonalityColor(CustomerType personality) {
        return switch (personality) {
            case CONNOISSEUR -> "§b"; // Aqua - refined
            case VIP_CLIENT -> "§d"; // Light purple - exclusive
            case BULK_BUYER -> "§6"; // Gold - business
            case MYSTERY_BUYER -> "§5"; // Dark purple - mysterious
            case COLLECTOR -> "§3"; // Dark aqua - rare
            case MEDICAL_USER -> "§a"; // Green - health
            case PARTY_ANIMAL -> "§c"; // Red - energetic
            case RUSH_CUSTOMER -> "§e"; // Yellow - urgent
            case SKEPTIC -> "§7"; // Gray - cautious
            default -> "§f"; // White - default
        };
    }
    
    /**
     * Generates an appropriate personality for an entity type.
     */
    private CustomerType getRandomPersonalityForEntity(EntityType type) {
        // Different mob types have different personality distributions
        return switch (type) {
            case VILLAGER -> {
                // Villagers tend to be more conservative/skeptical
                CustomerType[] types = {
                    CustomerType.SKEPTIC, CustomerType.MEDICAL_USER, 
                    CustomerType.BULK_BUYER, CustomerType.CONNOISSEUR
                };
                yield types[ThreadLocalRandom.current().nextInt(types.length)];
            }
            case WANDERING_TRADER -> {
                // Wandering traders are diverse
                CustomerType[] types = {
                    CustomerType.MYSTERY_BUYER, CustomerType.COLLECTOR,
                    CustomerType.VIP_CLIENT, CustomerType.CONNOISSEUR
                };
                yield types[ThreadLocalRandom.current().nextInt(types.length)];
            }
            case PIGLIN -> {
                // Piglins love gold and partying
                CustomerType[] types = {
                    CustomerType.PARTY_ANIMAL, CustomerType.RUSH_CUSTOMER,
                    CustomerType.BULK_BUYER
                };
                yield types[ThreadLocalRandom.current().nextInt(types.length)];
            }
            case WITCH -> {
                // Witches are mysterious and use for potions
                CustomerType[] types = {
                    CustomerType.MYSTERY_BUYER, CustomerType.MEDICAL_USER,
                    CustomerType.COLLECTOR
                };
                yield types[ThreadLocalRandom.current().nextInt(types.length)];
            }
            default -> {
                // Random for other types
                CustomerType[] allTypes = CustomerType.values();
                yield allTypes[ThreadLocalRandom.current().nextInt(allTypes.length)];
            }
        };
    }
    
    /**
     * Generates a name appropriate for an entity type.
     */
    private String generateNameForEntity(EntityType type, Set<String> usedNames) {
        String[] prefixes = switch (type) {
            case VILLAGER -> new String[]{"Trader", "Merchant", "Farmer", "Shopkeeper"};
            case WANDERING_TRADER -> new String[]{"Nomad", "Wanderer", "Traveler", "Drifter"};
            case PIGLIN -> new String[]{"Boss", "Chief", "Captain", "Lord"};
            case WITCH -> new String[]{"Mystic", "Sage", "Oracle", "Witch"};
            case ZOMBIE_VILLAGER -> new String[]{"Undead", "Zombie", "Ghoul"};
            case PILLAGER -> new String[]{"Raider", "Bandit", "Outlaw"};
            default -> new String[]{"Strange", "Mysterious", "Unknown"};
        };
        
        String[] names = {
            "Marcus", "Tony", "Jake", "Chris", "Mike", "Alex", "Sam", "Kevin",
            "Luna", "Aria", "Nova", "Zara", "Rex", "Max", "Duke", "Ace"
        };
        
        String name;
        int attempts = 0;
        do {
            String prefix = prefixes[ThreadLocalRandom.current().nextInt(prefixes.length)];
            String baseName = names[ThreadLocalRandom.current().nextInt(names.length)];
            name = prefix + " " + baseName;
            attempts++;
        } while (usedNames.contains(name) && attempts < 50);
        
        return name;
    }
    
    /**
     * Starts the task that shows demand indicators above buyers.
     */
    private void startDemandIndicatorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID entityId : new HashSet<>(demandIndicators.keySet())) {
                    Entity entity = Bukkit.getEntity(entityId);
                    
                    if (entity == null || !entity.isValid()) {
                        demandIndicators.remove(entityId);
                        continue;
                    }
                    
                    if (!hasDemand(entity)) {
                        demandIndicators.remove(entityId);
                        continue;
                    }
                    
                    // Show demand indicator (exclamation mark particles)
                    showDemandIndicator(entity);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second
    }
    
    /**
     * Shows enhanced visual demand indicator above an entity.
     * Multiple animations and particles to make it very noticeable.
     */
    private void showDemandIndicator(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return;
        
        Location loc = livingEntity.getEyeLocation().add(0, 0.8, 0);
        World world = entity.getWorld();
        
        // Animated circle of happy particles (buyer wants to buy!)
        double time = System.currentTimeMillis() / 1000.0;
        double radius = 0.5;
        for (int i = 0; i < 8; i++) {
            double angle = (time + i * 0.785) % (Math.PI * 2); // 8 particles rotating
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(Particle.VILLAGER_HAPPY, 
                loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
        
        // Money/emerald sparkles above head
        world.spawnParticle(Particle.END_ROD, 
            loc.clone().add(0, 0.3, 0), 2, 0.1, 0.1, 0.1, 0.02);
            
        // Green sparkles (ready to buy)
        world.spawnParticle(Particle.COMPOSTER, 
            loc, 1, 0.15, 0.15, 0.15, 0);
    }
    
    /**
     * Called when a player successfully sells to a buyer entity.
     * Updates demand status, mood, and shows visual feedback.
     */
    public void onSuccessfulSale(Entity entity, double saleValue, int qualityRating) {
        // After a sale, remove demand temporarily
        updateDemand(entity, false);
        
        // Update buyer's last seen timestamp and mood
        IndividualBuyer buyer = getBuyer(entity);
        if (buyer != null) {
            buyer.updateLastSeen();
            
            // Set mood based on sale quality
            if (qualityRating >= 5) {
                buyer.setCurrentMood("happy"); // 5-star = very happy
            } else if (qualityRating >= 3) {
                buyer.setCurrentMood("satisfied"); // 3-4 star = satisfied
            } else {
                buyer.setCurrentMood("neutral"); // 1-2 star = neutral
            }
            
            // Update name tag to reflect new mood
            updateNameTag(entity, buyer);
            
            buyerRegistry.saveBuyers();
            
            // Show visual feedback for successful purchase
            showPurchaseFeedback(entity, qualityRating);
        }
    }
    
    /**
     * Legacy method for backward compatibility.
     */
    public void onSuccessfulSale(Entity entity) {
        onSuccessfulSale(entity, 0, 3); // Default to satisfied
    }
    
    /**
     * Shows visual feedback after a purchase based on quality.
     */
    private void showPurchaseFeedback(Entity entity, int qualityRating) {
        if (!(entity instanceof LivingEntity)) return;
        
        Location loc = entity.getLocation().add(0, 2, 0);
        World world = entity.getWorld();
        
        if (qualityRating >= 5) {
            // Excellent quality - hearts and sparkles
            world.spawnParticle(Particle.HEART, loc, 5, 0.5, 0.3, 0.5, 0);
            world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 10, 0.5, 0.5, 0.5, 0);
            world.spawnParticle(Particle.END_ROD, loc, 8, 0.3, 0.3, 0.3, 0.05);
        } else if (qualityRating >= 3) {
            // Good quality - happy particles
            world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 6, 0.4, 0.4, 0.4, 0);
            world.spawnParticle(Particle.COMPOSTER, loc, 3, 0.3, 0.3, 0.3, 0);
        } else {
            // Low quality - neutral particles
            world.spawnParticle(Particle.SMOKE_NORMAL, loc, 3, 0.2, 0.2, 0.2, 0.01);
        }
    }
    
    /**
     * Updates buyer's reputation status and shows visual indicator.
     */
    public void updateReputationStatus(Entity entity, boolean increased) {
        IndividualBuyer buyer = getBuyer(entity);
        if (buyer == null) return;
        
        // Update name tag immediately
        updateNameTag(entity, buyer);
        
        // Show reputation change particles
        showReputationChangeParticles(entity, increased);
    }
    
    /**
     * Shows particles indicating reputation change.
     */
    private void showReputationChangeParticles(Entity entity, boolean increased) {
        if (!(entity instanceof LivingEntity)) return;
        
        Location loc = entity.getLocation().add(0, 2.5, 0);
        World world = entity.getWorld();
        
        if (increased) {
            // Reputation increased - green upward particles
            for (int i = 0; i < 5; i++) {
                world.spawnParticle(Particle.COMPOSTER, 
                    loc.clone().add(0, i * 0.2, 0), 1, 0.1, 0, 0.1, 0);
            }
            world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 3, 0.2, 0.2, 0.2, 0);
        } else {
            // Reputation decreased - red downward particles
            for (int i = 0; i < 5; i++) {
                world.spawnParticle(Particle.SMOKE_NORMAL, 
                    loc.clone().add(0, -i * 0.2, 0), 1, 0.1, 0, 0.1, 0);
            }
            world.spawnParticle(Particle.VILLAGER_ANGRY, loc, 3, 0.2, 0.2, 0.2, 0);
        }
    }
    
    /**
     * Gets all entities with active buyer profiles in a world.
     */
    public List<Entity> getActiveBuyers() {
        List<Entity> buyers = new ArrayList<>();
        
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (getBuyer(entity) != null) {
                    buyers.add(entity);
                }
            }
        }
        
        return buyers;
    }
    
    /**
     * Cleanup on plugin disable.
     */
    public void shutdown() {
        demandIndicators.clear();
    }
}
