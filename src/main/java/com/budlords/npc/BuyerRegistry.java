package com.budlords.npc;

import com.budlords.BudLords;
import com.budlords.economy.CustomerType;
import com.budlords.strain.Strain;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Central registry for all individual buyers in the game.
 * Manages buyer creation, persistence, and lookup.
 * 
 * Provides professional paginated access to buyer lists and comprehensive tracking.
 */
public class BuyerRegistry {
    
    private final BudLords plugin;
    private final Map<UUID, IndividualBuyer> buyers;
    private final File buyersFile;
    private FileConfiguration buyersConfig;
    
    // Name generation lists for creating diverse, realistic buyers
    private static final String[] FIRST_NAMES = {
        "Marcus", "Tony", "Jake", "Derek", "Chris", "Mike", "Steve", "Johnny",
        "Alex", "Ryan", "Brad", "Kevin", "Tyler", "Josh", "Sean", "Matt",
        "Lucas", "Danny", "Eric", "Nick", "Sam", "Kyle", "Brian", "Justin",
        "Carlos", "Rico", "Diego", "Pablo", "Marco", "Luis",
        "Chen", "Wei", "Jin", "Kai", "Raj", "Arjun",
        "Andre", "Malik", "Jamal", "Tyrone", "DeShawn",
        "Dimitri", "Ivan", "Vlad", "Boris", "Alexei"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
        "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez",
        "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
        "Lee", "Thompson", "White", "Harris", "Clark", "Lewis", "Robinson",
        "Walker", "Hall", "Allen", "Young", "King", "Wright", "Scott", "Green",
        "Adams", "Baker", "Nelson", "Carter", "Mitchell", "Roberts", "Turner"
    };
    
    private static final String[] NICKNAMES = {
        "Big", "Lil", "Fast", "Smooth", "Lucky", "Crazy", "Silent", "Cool",
        "Mad", "Wild", "Slick", "Sharp", "Wise", "Happy", "Chill", "Boss"
    };
    
    public BuyerRegistry(BudLords plugin) {
        this.plugin = plugin;
        this.buyers = new ConcurrentHashMap<>();
        this.buyersFile = new File(plugin.getDataFolder(), "buyers.yml");
        loadBuyers();
        
        // Initialize fixed NPCs if they don't exist
        initializeFixedNPCs();
        
        // Don't generate fake buyers - only real dynamic buyers (villagers) and fixed NPCs
        plugin.getLogger().info("Buyer registry initialized with " + buyers.size() + " buyers (including fixed NPCs)");
    }
    
    /**
     * Initializes fixed NPCs (Market Joe, BlackMarket Joe) in the registry.
     * These are permanent buyers that should always exist.
     */
    private void initializeFixedNPCs() {
        // Fixed UUIDs for permanent NPCs to ensure they persist
        UUID marketJoeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID blackMarketJoeId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        
        // Check if Market Joe exists, if not create him
        if (!buyers.containsKey(marketJoeId)) {
            IndividualBuyer marketJoe = new IndividualBuyer(marketJoeId, "Market Joe", CustomerType.CASUAL_USER);
            buyers.put(marketJoeId, marketJoe);
            plugin.getLogger().info("Initialized Market Joe in buyer registry");
        }
        
        // Check if BlackMarket Joe exists, if not create him
        if (!buyers.containsKey(blackMarketJoeId)) {
            IndividualBuyer blackMarketJoe = new IndividualBuyer(blackMarketJoeId, "BlackMarket Joe", CustomerType.VIP_CLIENT);
            buyers.put(blackMarketJoeId, blackMarketJoe);
            plugin.getLogger().info("Initialized BlackMarket Joe in buyer registry");
        }
        
        // Save after initialization
        saveBuyers();
    }
    
    /**
     * Gets Market Joe buyer instance (fixed NPC).
     */
    public IndividualBuyer getMarketJoe() {
        UUID marketJoeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        return buyers.get(marketJoeId);
    }
    
    /**
     * Gets BlackMarket Joe buyer instance (fixed NPC).
     */
    public IndividualBuyer getBlackMarketJoe() {
        UUID blackMarketJoeId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        return buyers.get(blackMarketJoeId);
    }
    
    /**
     * Loads all buyers from storage.
     */
    private void loadBuyers() {
        if (!buyersFile.exists()) {
            try {
                buyersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create buyers.yml: " + e.getMessage());
                return;
            }
        }
        
        buyersConfig = YamlConfiguration.loadConfiguration(buyersFile);
        ConfigurationSection buyersSection = buyersConfig.getConfigurationSection("buyers");
        
        if (buyersSection == null) {
            return;
        }
        
        for (String key : buyersSection.getKeys(false)) {
            try {
                ConfigurationSection buyerSection = buyersSection.getConfigurationSection(key);
                if (buyerSection == null) continue;
                
                UUID id = UUID.fromString(buyerSection.getString("id"));
                String name = buyerSection.getString("name");
                CustomerType personality = CustomerType.valueOf(buyerSection.getString("personality"));
                
                IndividualBuyer buyer = new IndividualBuyer(id, name, personality);
                
                // Load purchase history
                ConfigurationSection historySection = buyerSection.getConfigurationSection("purchase-history");
                if (historySection != null) {
                    for (String strainId : historySection.getKeys(false)) {
                        int amount = historySection.getInt(strainId);
                        double totalPrice = buyerSection.getDouble("total-spent", 0.0);
                        // Restore purchases (simplified - just adds to history)
                        for (int i = 0; i < amount; i++) {
                            buyer.recordPurchase(strainId, 1, totalPrice / amount);
                        }
                    }
                }
                
                buyers.put(id, buyer);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load buyer: " + key + " - " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + buyers.size() + " individual buyers");
    }
    
    /**
     * Saves all buyers to storage.
     */
    public void saveBuyers() {
        buyersConfig.set("buyers", null); // Clear existing
        
        for (IndividualBuyer buyer : buyers.values()) {
            String path = "buyers." + buyer.getId().toString();
            buyersConfig.set(path + ".id", buyer.getId().toString());
            buyersConfig.set(path + ".name", buyer.getName());
            buyersConfig.set(path + ".personality", buyer.getPersonality().name());
            buyersConfig.set(path + ".total-purchases", buyer.getTotalPurchases());
            buyersConfig.set(path + ".total-spent", buyer.getTotalMoneySpent());
            buyersConfig.set(path + ".first-met", buyer.getFirstMetTimestamp());
            buyersConfig.set(path + ".last-seen", buyer.getLastSeenTimestamp());
            
            // Save purchase history
            Map<String, Integer> history = buyer.getPurchaseHistory();
            for (Map.Entry<String, Integer> entry : history.entrySet()) {
                buyersConfig.set(path + ".purchase-history." + entry.getKey(), entry.getValue());
            }
        }
        
        try {
            buyersConfig.save(buyersFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save buyers.yml: " + e.getMessage());
        }
    }
    
    /**
     * Generates initial diverse buyer population.
     */
    private void generateInitialBuyers(int count) {
        CustomerType[] types = CustomerType.values();
        Set<String> usedNames = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            String name = generateUniqueName(usedNames);
            CustomerType personality = types[ThreadLocalRandom.current().nextInt(types.length)];
            
            IndividualBuyer buyer = new IndividualBuyer(UUID.randomUUID(), name, personality);
            buyers.put(buyer.getId(), buyer);
            usedNames.add(name);
        }
        
        saveBuyers();
        plugin.getLogger().info("Generated " + count + " initial buyers");
    }
    
    /**
     * Generates a unique buyer name.
     */
    private String generateUniqueName(Set<String> usedNames) {
        String name;
        int attempts = 0;
        
        do {
            if (attempts > 0 && ThreadLocalRandom.current().nextBoolean()) {
                // Use nickname format: "Nickname FirstName"
                String nickname = NICKNAMES[ThreadLocalRandom.current().nextInt(NICKNAMES.length)];
                String firstName = FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
                name = nickname + " " + firstName;
            } else {
                // Use regular format: "FirstName LastName"
                String firstName = FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
                String lastName = LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)];
                name = firstName + " " + lastName;
            }
            attempts++;
        } while (usedNames.contains(name) && attempts < 100);
        
        return name;
    }
    
    /**
     * Gets a buyer by ID.
     */
    public IndividualBuyer getBuyer(UUID id) {
        return buyers.get(id);
    }
    
    /**
     * Gets a random buyer (for encounters).
     */
    public IndividualBuyer getRandomBuyer() {
        if (buyers.isEmpty()) return null;
        List<IndividualBuyer> list = new ArrayList<>(buyers.values());
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
    
    /**
     * Gets all buyers.
     */
    public Collection<IndividualBuyer> getAllBuyers() {
        return new ArrayList<>(buyers.values());
    }
    
    /**
     * Gets buyers sorted by total purchases (best customers first).
     */
    public List<IndividualBuyer> getBuyersSortedByPurchases() {
        return buyers.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTotalPurchases(), a.getTotalPurchases()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets buyers sorted by money spent (highest value customers first).
     */
    public List<IndividualBuyer> getBuyersSortedByValue() {
        return buyers.values().stream()
            .sorted((a, b) -> Double.compare(b.getTotalMoneySpent(), a.getTotalMoneySpent()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets buyers sorted by last seen (most recent first).
     */
    public List<IndividualBuyer> getBuyersSortedByRecency() {
        return buyers.values().stream()
            .sorted((a, b) -> Long.compare(b.getLastSeenTimestamp(), a.getLastSeenTimestamp()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets buyers by personality type.
     */
    public List<IndividualBuyer> getBuyersByPersonality(CustomerType personality) {
        return buyers.values().stream()
            .filter(b -> b.getPersonality() == personality)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets buyers who prefer a specific rarity.
     */
    public List<IndividualBuyer> getBuyersByFavoriteRarity(Strain.Rarity rarity) {
        return buyers.values().stream()
            .filter(b -> b.getFavoriteRarity() == rarity)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets paginated buyer list (for GUI display).
     */
    public List<IndividualBuyer> getBuyersPage(int page, int pageSize, BuyerSortType sortType) {
        List<IndividualBuyer> sorted = switch (sortType) {
            case PURCHASES -> getBuyersSortedByPurchases();
            case VALUE -> getBuyersSortedByValue();
            case RECENCY -> getBuyersSortedByRecency();
            case NAME -> buyers.values().stream()
                .sorted(Comparator.comparing(IndividualBuyer::getName))
                .collect(Collectors.toList());
        };
        
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, sorted.size());
        
        if (startIndex >= sorted.size()) {
            return new ArrayList<>();
        }
        
        return sorted.subList(startIndex, endIndex);
    }
    
    /**
     * Gets total number of pages for pagination.
     */
    public int getTotalPages(int pageSize) {
        return (int) Math.ceil((double) buyers.size() / pageSize);
    }
    
    /**
     * Adds a new buyer to the registry.
     */
    public IndividualBuyer createBuyer(String name, CustomerType personality) {
        IndividualBuyer buyer = new IndividualBuyer(UUID.randomUUID(), name, personality);
        buyers.put(buyer.getId(), buyer);
        saveBuyers();
        return buyer;
    }

    /**
     * Adds an existing buyer to the registry.
     */
    public void addBuyer(IndividualBuyer buyer) {
        if (buyer != null) {
            buyers.put(buyer.getId(), buyer);
            saveBuyers();
        }
    }
    
    /**
     * Records a purchase for a buyer.
     */
    public void recordPurchase(UUID buyerId, String strainId, int amount, double price) {
        IndividualBuyer buyer = buyers.get(buyerId);
        if (buyer != null) {
            buyer.recordPurchase(strainId, amount, price);
            saveBuyers();
        }
    }
    
    /**
     * Gets statistics about the buyer registry.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_buyers", buyers.size());
        stats.put("total_purchases", buyers.values().stream()
            .mapToInt(IndividualBuyer::getTotalPurchases).sum());
        stats.put("total_money", buyers.values().stream()
            .mapToDouble(IndividualBuyer::getTotalMoneySpent).sum());
        stats.put("most_purchases", getBuyersSortedByPurchases().isEmpty() ? 
            null : getBuyersSortedByPurchases().get(0).getName());
        stats.put("highest_value", getBuyersSortedByValue().isEmpty() ? 
            null : getBuyersSortedByValue().get(0).getName());
        return stats;
    }
    
    public enum BuyerSortType {
        PURCHASES,  // Sort by total purchases
        VALUE,      // Sort by money spent
        RECENCY,    // Sort by last seen
        NAME        // Sort alphabetically
    }
}
