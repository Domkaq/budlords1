package com.budlords.collections;

import com.budlords.BudLords;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the strain collection book system in BudLords v2.0.0.
 * Players can collect and display their discovered strains.
 */
public class CollectionManager implements InventoryHolder {

    private final BudLords plugin;
    private final StrainManager strainManager;
    
    // Player collections: UUID -> Set of collected strain IDs
    private final Map<UUID, Set<String>> playerCollections;
    
    // Collection statistics: UUID -> CollectionStats
    private final Map<UUID, CollectionStats> collectionStats;
    
    // Data file
    private File collectionsFile;
    private FileConfiguration collectionsConfig;

    public CollectionManager(BudLords plugin, StrainManager strainManager) {
        this.plugin = plugin;
        this.strainManager = strainManager;
        this.playerCollections = new ConcurrentHashMap<>();
        this.collectionStats = new ConcurrentHashMap<>();
        
        loadCollections();
        
        plugin.getLogger().info("✦ Collection Book System initialized");
    }

    private void loadCollections() {
        collectionsFile = new File(plugin.getDataFolder(), "collections.yml");
        if (!collectionsFile.exists()) {
            try {
                collectionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create collections file: " + e.getMessage());
            }
        }
        collectionsConfig = YamlConfiguration.loadConfiguration(collectionsFile);
        
        ConfigurationSection playersSection = collectionsConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    
                    // Load collected strains
                    List<String> collected = playersSection.getStringList(uuidStr + ".strains");
                    playerCollections.put(uuid, new HashSet<>(collected));
                    
                    // Load stats
                    ConfigurationSection statsSection = playersSection.getConfigurationSection(uuidStr + ".stats");
                    if (statsSection != null) {
                        CollectionStats stats = new CollectionStats();
                        stats.commonCollected = statsSection.getInt("common", 0);
                        stats.uncommonCollected = statsSection.getInt("uncommon", 0);
                        stats.rareCollected = statsSection.getInt("rare", 0);
                        stats.legendaryCollected = statsSection.getInt("legendary", 0);
                        stats.totalHarvestsPerStrain = new HashMap<>();
                        
                        ConfigurationSection harvestsSection = statsSection.getConfigurationSection("harvests");
                        if (harvestsSection != null) {
                            for (String strainId : harvestsSection.getKeys(false)) {
                                stats.totalHarvestsPerStrain.put(strainId, harvestsSection.getInt(strainId));
                            }
                        }
                        
                        collectionStats.put(uuid, stats);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load collection for " + uuidStr);
                }
            }
        }
    }

    public void saveCollections() {
        collectionsConfig.set("players", null);
        
        for (UUID uuid : playerCollections.keySet()) {
            String path = "players." + uuid.toString();
            
            // Save collected strains
            Set<String> strains = playerCollections.get(uuid);
            collectionsConfig.set(path + ".strains", new ArrayList<>(strains));
            
            // Save stats
            CollectionStats stats = collectionStats.get(uuid);
            if (stats != null) {
                collectionsConfig.set(path + ".stats.common", stats.commonCollected);
                collectionsConfig.set(path + ".stats.uncommon", stats.uncommonCollected);
                collectionsConfig.set(path + ".stats.rare", stats.rareCollected);
                collectionsConfig.set(path + ".stats.legendary", stats.legendaryCollected);
                
                for (Map.Entry<String, Integer> entry : stats.totalHarvestsPerStrain.entrySet()) {
                    collectionsConfig.set(path + ".stats.harvests." + entry.getKey(), entry.getValue());
                }
            }
        }
        
        try {
            collectionsConfig.save(collectionsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save collections: " + e.getMessage());
        }
    }

    /**
     * Adds a strain to a player's collection when they harvest it.
     */
    public void addToCollection(Player player, String strainId) {
        UUID uuid = player.getUniqueId();
        Set<String> collection = playerCollections.computeIfAbsent(uuid, k -> new HashSet<>());
        CollectionStats stats = collectionStats.computeIfAbsent(uuid, k -> new CollectionStats());
        
        // Track harvest count
        stats.totalHarvestsPerStrain.merge(strainId, 1, Integer::sum);
        
        // Check if new discovery
        boolean isNewDiscovery = !collection.contains(strainId);
        
        if (isNewDiscovery) {
            collection.add(strainId);
            
            // Update rarity stats
            Strain strain = strainManager.getStrain(strainId);
            if (strain != null) {
                switch (strain.getRarity()) {
                    case COMMON -> stats.commonCollected++;
                    case UNCOMMON -> stats.uncommonCollected++;
                    case RARE -> stats.rareCollected++;
                    case LEGENDARY -> stats.legendaryCollected++;
                }
                
                // Send discovery notification
                sendDiscoveryNotification(player, strain);
            }
            
            // Check for collection milestones
            checkMilestones(player, collection.size());
        }
    }

    private void sendDiscoveryNotification(Player player, Strain strain) {
        player.sendMessage("");
        player.sendMessage("§8§l═══════════════════════════════════════════");
        player.sendMessage("§d§l   📖 NEW STRAIN DISCOVERED! 📖");
        player.sendMessage("");
        player.sendMessage("   " + strain.getRarity().getColorCode() + strain.getName());
        player.sendMessage("   §7Rarity: " + strain.getRarity().getDisplayName());
        player.sendMessage("");
        player.sendMessage("   §7Added to your Collection Book!");
        player.sendMessage("§8§l═══════════════════════════════════════════");
        player.sendMessage("");
        
        // Sound based on rarity
        Sound sound = switch (strain.getRarity()) {
            case LEGENDARY -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case RARE -> Sound.ENTITY_PLAYER_LEVELUP;
            case UNCOMMON -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            default -> Sound.BLOCK_NOTE_BLOCK_CHIME;
        };
        
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        
        // Particles based on rarity
        Particle particle = switch (strain.getRarity()) {
            case LEGENDARY -> Particle.TOTEM;
            case RARE -> Particle.END_ROD;
            case UNCOMMON -> Particle.VILLAGER_HAPPY;
            default -> Particle.COMPOSTER;
        };
        
        int count = switch (strain.getRarity()) {
            case LEGENDARY -> 50;
            case RARE -> 30;
            case UNCOMMON -> 20;
            default -> 10;
        };
        
        player.spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.5, 0.5, 0.5, 0.05);
    }

    private void checkMilestones(Player player, int collectionSize) {
        // Milestone rewards
        int[] milestones = {5, 10, 25, 50, 100};
        double[] rewards = {500, 1500, 5000, 15000, 50000};
        
        for (int i = 0; i < milestones.length; i++) {
            if (collectionSize == milestones[i]) {
                player.sendMessage("");
                player.sendMessage("§6§l🏆 COLLECTION MILESTONE: " + milestones[i] + " Strains!");
                player.sendMessage("§7Reward: §e$" + String.format("%,.0f", rewards[i]));
                player.sendMessage("");
                
                if (plugin.getEconomyManager() != null) {
                    plugin.getEconomyManager().addBalance(player, rewards[i]);
                }
                
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 100, 0.5, 1, 0.5, 0.1);
                break;
            }
        }
    }

    /**
     * Opens the collection book GUI for a player.
     */
    @SuppressWarnings("deprecation")
    public void openCollectionGUI(Player player, int page) {
        UUID uuid = player.getUniqueId();
        Set<String> collection = playerCollections.getOrDefault(uuid, new HashSet<>());
        CollectionStats stats = collectionStats.getOrDefault(uuid, new CollectionStats());
        
        Inventory inv = Bukkit.createInventory(this, 54, "§d§l📖 Collection Book - Page " + (page + 1));
        
        // Border
        ItemStack border = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
        
        // Collection stats header
        int totalStrains = strainManager.getStrainCount();
        int collectedCount = collection.size();
        double completionPercent = totalStrains > 0 ? (collectedCount * 100.0 / totalStrains) : 0;
        
        inv.setItem(4, createItem(Material.BOOK, 
            "§d§lYour Collection",
            Arrays.asList(
                "",
                "§7Collected: §a" + collectedCount + "§7/" + totalStrains + 
                    " §8(" + String.format("%.1f", completionPercent) + "%)",
                "",
                "§7By Rarity:",
                "§7 • Common: §f" + stats.commonCollected,
                "§7 • Uncommon: §a" + stats.uncommonCollected,
                "§7 • Rare: §9" + stats.rareCollected,
                "§7 • Legendary: §6" + stats.legendaryCollected,
                "",
                "§7Progress: " + createProgressBar(completionPercent / 100.0)
            )));
        
        // Display strains
        List<Strain> allStrains = new ArrayList<>(strainManager.getAllStrains());
        allStrains.sort((a, b) -> {
            // Sort by: collected first, then by rarity
            boolean aCollected = collection.contains(a.getId());
            boolean bCollected = collection.contains(b.getId());
            
            if (aCollected != bCollected) {
                return aCollected ? -1 : 1;
            }
            return a.getRarity().compareTo(b.getRarity());
        });
        
        int itemsPerPage = 28; // 7 columns x 4 rows
        int startIndex = page * itemsPerPage;
        int slot = 10;
        
        for (int i = startIndex; i < Math.min(startIndex + itemsPerPage, allStrains.size()); i++) {
            // Skip border slots
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 45) break;
            
            Strain strain = allStrains.get(i);
            boolean collected = collection.contains(strain.getId());
            
            Material material;
            String name;
            List<String> lore = new ArrayList<>();
            
            if (collected) {
                material = strain.getIconMaterial();
                name = strain.getRarity().getColorCode() + strain.getName();
                
                lore.add("");
                lore.add("§7Rarity: " + strain.getRarity().getDisplayName());
                lore.add("§7Potency: §e" + strain.getPotency() + "%");
                lore.add("§7Yield: §e" + strain.getYield() + " buds");
                lore.add("");
                
                int harvests = stats.totalHarvestsPerStrain.getOrDefault(strain.getId(), 0);
                lore.add("§7Times Harvested: §a" + harvests);
                
                // Effects
                if (!strain.getEffects().isEmpty()) {
                    lore.add("");
                    lore.add("§d§lSpecial Effects:");
                    strain.getEffectsLore().forEach(l -> lore.add("  " + l));
                }
                
                lore.add("");
                lore.add("§a✓ Collected!");
            } else {
                material = Material.GRAY_DYE;
                name = "§8???";
                
                lore.add("");
                lore.add("§7Rarity: " + strain.getRarity().getDisplayName());
                lore.add("");
                lore.add("§cNot Discovered");
                lore.add("§7Harvest this strain to discover it!");
            }
            
            inv.setItem(slot++, createItem(material, name, lore));
        }
        
        // Navigation
        int totalPages = (allStrains.size() + itemsPerPage - 1) / itemsPerPage;
        
        if (page > 0) {
            inv.setItem(47, createItem(Material.ARROW, "§e← Previous Page", 
                Collections.singletonList("§7Page " + page + "/" + totalPages)));
        }
        
        if (page < totalPages - 1) {
            inv.setItem(51, createItem(Material.ARROW, "§eNext Page →", 
                Collections.singletonList("§7Page " + (page + 2) + "/" + totalPages)));
        }
        
        // Filter options
        inv.setItem(48, createItem(Material.HOPPER, "§eFilter Options", 
            Arrays.asList("", "§7Click to filter by rarity", "§7(Coming soon!)")));
        
        // Search
        inv.setItem(50, createItem(Material.COMPASS, "§eSearch Strains", 
            Arrays.asList("", "§7Click to search by name", "§7(Coming soon!)")));
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
    }

    /**
     * Gets the collection completion percentage for a player.
     */
    public double getCompletionPercent(UUID uuid) {
        Set<String> collection = playerCollections.get(uuid);
        if (collection == null) return 0;
        
        int totalStrains = strainManager.getStrainCount();
        return totalStrains > 0 ? (collection.size() * 100.0 / totalStrains) : 0;
    }

    /**
     * Checks if a player has collected a specific strain.
     */
    public boolean hasCollected(UUID uuid, String strainId) {
        Set<String> collection = playerCollections.get(uuid);
        return collection != null && collection.contains(strainId);
    }

    /**
     * Gets the number of collected strains for a player.
     */
    public int getCollectedCount(UUID uuid) {
        Set<String> collection = playerCollections.get(uuid);
        return collection != null ? collection.size() : 0;
    }

    private String createProgressBar(double progress) {
        StringBuilder bar = new StringBuilder("§8[");
        int filled = (int) (progress * 20);
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                bar.append("§d█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    /**
     * Statistics for a player's collection.
     */
    public static class CollectionStats {
        public int commonCollected;
        public int uncommonCollected;
        public int rareCollected;
        public int legendaryCollected;
        public Map<String, Integer> totalHarvestsPerStrain = new HashMap<>();
    }
}
