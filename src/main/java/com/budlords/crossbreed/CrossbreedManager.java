package com.budlords.crossbreed;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
import com.budlords.quality.StarRating;
import com.budlords.stats.PlayerStats;
import com.budlords.stats.StatsManager;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the strain crossbreeding system.
 * Players can combine two strains to create new hybrid strains.
 */
public class CrossbreedManager implements InventoryHolder {

    private final BudLords plugin;
    private final StrainManager strainManager;
    private final EconomyManager economyManager;
    private final StatsManager statsManager;
    
    // Active crossbreeding sessions
    private final Map<UUID, CrossbreedSession> activeSessions;
    
    // Crossbreeding costs
    private static final double BASE_CROSSBREED_COST = 500.0;
    private static final double RARE_MULTIPLIER = 2.0;
    private static final double LEGENDARY_MULTIPLIER = 5.0;

    public CrossbreedManager(BudLords plugin, StrainManager strainManager, 
                             EconomyManager economyManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.strainManager = strainManager;
        this.economyManager = economyManager;
        this.statsManager = statsManager;
        this.activeSessions = new HashMap<>();
    }

    /**
     * Opens the crossbreeding GUI for a player.
     */
    @SuppressWarnings("deprecation")
    public void openCrossbreedGUI(Player player) {
        CrossbreedSession session = activeSessions.computeIfAbsent(
            player.getUniqueId(), k -> new CrossbreedSession());
        
        Inventory inv = Bukkit.createInventory(this, 45, "§d§l✿ Strain Crossbreeding Lab ✿");
        updateCrossbreedGUI(inv, session);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 1.2f);
    }

    private void updateCrossbreedGUI(Inventory inv, CrossbreedSession session) {
        inv.clear();
        
        // Border
        ItemStack borderPink = createItem(Material.PINK_STAINED_GLASS_PANE, " ", null);
        ItemStack borderMagenta = createItem(Material.MAGENTA_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderPink : borderMagenta);
            inv.setItem(36 + i, i % 2 == 0 ? borderPink : borderMagenta);
        }
        for (int i = 9; i < 36; i += 9) {
            inv.setItem(i, borderPink);
            inv.setItem(i + 8, borderPink);
        }
        
        // Header
        inv.setItem(4, createItem(Material.FLOWER_POT, 
            "§d§l✿ Crossbreeding Lab ✿",
            Arrays.asList(
                "",
                "§7Combine two strains to create",
                "§7a new hybrid with mixed traits!",
                "",
                "§7Drop seeds in the slots to begin."
            )));
        
        // Parent strain 1 slot
        if (session.strain1 != null) {
            Strain strain = strainManager.getStrain(session.strain1);
            if (strain != null) {
                inv.setItem(20, createStrainItem(strain, session.strain1Rating, "§a§lParent 1"));
            }
        } else {
            inv.setItem(20, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 
                "§7§lParent 1",
                Arrays.asList(
                    "",
                    "§7Drop seeds here!",
                    "§7This strain's traits will be",
                    "§7inherited by the hybrid."
                )));
        }
        
        // Plus symbol
        inv.setItem(21, createItem(Material.END_ROD, "§e§l+", null));
        
        // Parent strain 2 slot
        if (session.strain2 != null) {
            Strain strain = strainManager.getStrain(session.strain2);
            if (strain != null) {
                inv.setItem(22, createStrainItem(strain, session.strain2Rating, "§b§lParent 2"));
            }
        } else {
            inv.setItem(22, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 
                "§7§lParent 2",
                Arrays.asList(
                    "",
                    "§7Drop seeds here!",
                    "§7This strain's traits will be",
                    "§7inherited by the hybrid."
                )));
        }
        
        // Equals symbol
        inv.setItem(23, createItem(Material.END_ROD, "§e§l=", null));
        
        // Result preview
        if (session.strain1 != null && session.strain2 != null) {
            CrossbreedResult preview = calculateCrossbreed(session);
            inv.setItem(24, createPreviewItem(preview));
        } else {
            inv.setItem(24, createItem(Material.GRAY_STAINED_GLASS_PANE, 
                "§7§l???",
                Arrays.asList(
                    "",
                    "§7Select both parents",
                    "§7to see preview!"
                )));
        }
        
        // Cost display
        double cost = calculateCost(session);
        inv.setItem(31, createItem(Material.EMERALD, 
            "§e§l💰 Crossbreed Cost",
            Arrays.asList(
                "",
                "§7Cost: §e$" + String.format("%,.0f", cost),
                "",
                "§7Higher rarity parents",
                "§7increase the cost!"
            )));
        
        // Crossbreed button
        boolean canCrossbreed = session.strain1 != null && session.strain2 != null;
        Material buttonMat = canCrossbreed ? Material.BREWING_STAND : Material.BARRIER;
        String buttonName = canCrossbreed ? "§a§l✓ CROSSBREED!" : "§c§l✗ Select Both Parents";
        inv.setItem(40, createItem(buttonMat, buttonName,
            canCrossbreed 
                ? Arrays.asList(
                    "",
                    "§7Click to create a new",
                    "§7hybrid strain!",
                    "",
                    "§7Cost: §e$" + String.format("%,.0f", cost)
                )
                : Arrays.asList(
                    "",
                    "§7You need to select",
                    "§7both parent strains first!"
                )));
        
        // Clear button
        inv.setItem(36, createItem(Material.BARRIER, "§c§lClear Selection",
            Arrays.asList("", "§7Remove both parents", "§7and start over.")));
        
        // Info panel
        inv.setItem(44, createItem(Material.BOOK, 
            "§e§l? How Crossbreeding Works",
            Arrays.asList(
                "",
                "§7• Hybrid inherits traits from both",
                "§7• Potency: Average + random bonus",
                "§7• Yield: Average of both parents",
                "§7• Rarity: Based on parent rarities",
                "§7• Quality: Average of seed ratings",
                "",
                "§7There's a small chance for",
                "§6rare mutations §7to occur!"
            )));
    }

    private ItemStack createStrainItem(Strain strain, StarRating rating, String title) {
        Material icon = strain.getIconMaterial();
        return createItem(icon, title + ": " + strain.getName(), Arrays.asList(
            "",
            "§7Rarity: " + strain.getRarity().getDisplayName(),
            "§7Potency: §e" + strain.getPotency() + "%",
            "§7Yield: §e" + strain.getYield() + " buds",
            "§7Quality: " + (rating != null ? rating.getDisplay() : "§7N/A"),
            ""
        ));
    }

    private ItemStack createPreviewItem(CrossbreedResult preview) {
        return createItem(Material.NETHER_STAR, 
            "§d§lHybrid Preview: " + preview.name,
            Arrays.asList(
                "",
                "§7Rarity: " + preview.rarity.getDisplayName(),
                "§7Potency: §e~" + preview.potency + "%",
                "§7Yield: §e~" + preview.yield + " buds",
                "§7Quality: ~" + preview.seedRating.getDisplay(),
                "",
                preview.hasMutation 
                    ? "§6✦ Mutation Detected!" 
                    : "§7No mutation detected",
                ""
            ));
    }

    /**
     * Handles dropping a seed into the crossbreed GUI.
     */
    public boolean handleSeedDrop(Player player, ItemStack item, int slot) {
        CrossbreedSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return false;
        
        // Check if it's a seed
        String strainId = strainManager.getStrainIdFromSeed(item);
        if (strainId == null) {
            player.sendMessage("§cYou can only use seeds for crossbreeding!");
            return false;
        }
        
        StarRating rating = strainManager.getSeedRating(item);
        
        // Determine which slot was clicked
        if (slot == 20) {
            session.strain1 = strainId;
            session.strain1Rating = rating;
            player.sendMessage("§aSet Parent 1: §e" + strainManager.getStrain(strainId).getName());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
        } else if (slot == 22) {
            session.strain2 = strainId;
            session.strain2Rating = rating;
            player.sendMessage("§aSet Parent 2: §e" + strainManager.getStrain(strainId).getName());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
        } else {
            return false;
        }
        
        // Update GUI
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv.getHolder() instanceof CrossbreedManager) {
            updateCrossbreedGUI(inv, session);
        }
        
        return true;
    }

    /**
     * Handles clicking the crossbreed button.
     */
    public boolean handleCrossbreedClick(Player player, int slot) {
        CrossbreedSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return false;
        
        // Clear button
        if (slot == 36) {
            session.strain1 = null;
            session.strain2 = null;
            session.strain1Rating = null;
            session.strain2Rating = null;
            player.sendMessage("§7Cleared selection.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
            
            Inventory inv = player.getOpenInventory().getTopInventory();
            if (inv.getHolder() instanceof CrossbreedManager) {
                updateCrossbreedGUI(inv, session);
            }
            return true;
        }
        
        // Crossbreed button
        if (slot == 40) {
            if (session.strain1 == null || session.strain2 == null) {
                player.sendMessage("§cSelect both parent strains first!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return true;
            }
            
            double cost = calculateCost(session);
            if (economyManager.getBalance(player) < cost) {
                player.sendMessage("§cYou need §e$" + String.format("%,.0f", cost) + " §cto crossbreed!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return true;
            }
            
            // Perform crossbreeding
            performCrossbreed(player, session, cost);
            return true;
        }
        
        return false;
    }

    private void performCrossbreed(Player player, CrossbreedSession session, double cost) {
        // Deduct cost
        economyManager.removeBalance(player, cost);
        
        // Calculate result
        CrossbreedResult result = calculateCrossbreed(session);
        
        // Create new strain
        String newStrainId = strainManager.generateUniqueId(result.name);
        Strain newStrain = new Strain(
            newStrainId,
            result.name,
            result.rarity,
            result.potency,
            result.yield,
            (int) (result.seedRating.getStars() * 20) // Convert to quality percentage
        );
        
        // Set icon based on parents
        Strain parent1 = strainManager.getStrain(session.strain1);
        Strain parent2 = strainManager.getStrain(session.strain2);
        newStrain.setIconMaterial(
            ThreadLocalRandom.current().nextBoolean() 
                ? parent1.getIconMaterial() 
                : parent2.getIconMaterial()
        );
        
        // Register the new strain
        strainManager.registerStrain(newStrain);
        strainManager.saveStrains();
        
        // Give seeds to player
        ItemStack seeds = strainManager.createSeedItem(newStrain, 3, result.seedRating);
        player.getInventory().addItem(seeds);
        
        // Update stats
        PlayerStats stats = statsManager.getStats(player);
        stats.incrementCrossbreeds();
        stats.incrementStrainsCreated();
        if (result.rarity == Strain.Rarity.LEGENDARY) {
            stats.incrementLegendaryStrains();
        }
        
        // Clear session
        session.strain1 = null;
        session.strain2 = null;
        session.strain1Rating = null;
        session.strain2Rating = null;
        
        // Close inventory
        player.closeInventory();
        
        // Epic celebration!
        playCrossbreedCelebration(player, result);
        
        // Messages
        player.sendMessage("");
        player.sendMessage("§d§l╔══════════════════════════════════╗");
        player.sendMessage("§d§l║  §5§l✿ CROSSBREED SUCCESS! ✿  §d§l      ║");
        player.sendMessage("§d§l╠══════════════════════════════════╣");
        player.sendMessage("§d§l║ §7Created: §f" + result.name);
        player.sendMessage("§d§l║ §7Rarity: " + result.rarity.getDisplayName());
        player.sendMessage("§d§l║ §7Potency: §e" + result.potency + "%");
        player.sendMessage("§d§l║ §7Yield: §e" + result.yield + " buds");
        player.sendMessage("§d§l║ §7Seed Quality: " + result.seedRating.getDisplay());
        if (result.hasMutation) {
            player.sendMessage("§d§l║ §6✦ MUTATION BONUS APPLIED!");
        }
        player.sendMessage("§d§l╚══════════════════════════════════╝");
        player.sendMessage("§eYou received §a3 seeds §eof your new strain!");
        player.sendMessage("");
    }

    private void playCrossbreedCelebration(Player player, CrossbreedResult result) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        
        player.spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 
            30, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 
            10, 0.3, 0.3, 0.3, 0);
        
        if (result.hasMutation) {
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 
                50, 0.5, 0.5, 0.5, 0.2);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);
        }
        
        if (result.rarity == Strain.Rarity.LEGENDARY) {
            Bukkit.broadcastMessage("§6§l✿ §d" + player.getName() + " §7created a §6LEGENDARY §7strain: §d" + result.name + "§7!");
        }
    }

    private CrossbreedResult calculateCrossbreed(CrossbreedSession session) {
        Strain parent1 = strainManager.getStrain(session.strain1);
        Strain parent2 = strainManager.getStrain(session.strain2);
        
        if (parent1 == null || parent2 == null) {
            return new CrossbreedResult("Unknown", Strain.Rarity.COMMON, 50, 3, StarRating.ONE_STAR, false);
        }
        
        // Generate hybrid name
        String hybridName = generateHybridName(parent1.getName(), parent2.getName());
        
        // Calculate average potency with random bonus
        int avgPotency = (parent1.getPotency() + parent2.getPotency()) / 2;
        int potencyVariation = ThreadLocalRandom.current().nextInt(-10, 16);
        int finalPotency = Math.max(10, Math.min(100, avgPotency + potencyVariation));
        
        // Calculate average yield
        int avgYield = (parent1.getYield() + parent2.getYield()) / 2;
        int yieldVariation = ThreadLocalRandom.current().nextInt(-1, 3);
        int finalYield = Math.max(1, Math.min(20, avgYield + yieldVariation));
        
        // Calculate rarity based on parents
        Strain.Rarity finalRarity = calculateHybridRarity(parent1.getRarity(), parent2.getRarity());
        
        // Calculate seed rating
        int rating1 = session.strain1Rating != null ? session.strain1Rating.getStars() : 1;
        int rating2 = session.strain2Rating != null ? session.strain2Rating.getStars() : 1;
        int avgRating = (rating1 + rating2 + 1) / 2; // Slight bonus
        StarRating finalRating = StarRating.fromValue(avgRating);
        
        // Check for mutation (5% chance, higher with legendary parents)
        double mutationChance = 0.05;
        if (parent1.getRarity() == Strain.Rarity.LEGENDARY || parent2.getRarity() == Strain.Rarity.LEGENDARY) {
            mutationChance = 0.15;
        }
        boolean hasMutation = ThreadLocalRandom.current().nextDouble() < mutationChance;
        
        if (hasMutation) {
            // Mutation bonus!
            finalPotency = Math.min(100, finalPotency + 15);
            finalYield = Math.min(20, finalYield + 2);
            finalRating = StarRating.fromValue(Math.min(5, finalRating.getStars() + 1));
            if (finalRarity != Strain.Rarity.LEGENDARY && ThreadLocalRandom.current().nextDouble() < 0.3) {
                finalRarity = Strain.Rarity.values()[Math.min(3, finalRarity.ordinal() + 1)];
            }
        }
        
        return new CrossbreedResult(hybridName, finalRarity, finalPotency, finalYield, finalRating, hasMutation);
    }

    private String generateHybridName(String name1, String name2) {
        // Take parts from each name
        String part1 = name1.length() > 4 ? name1.substring(0, name1.length() / 2) : name1;
        String part2 = name2.length() > 4 ? name2.substring(name2.length() / 2) : name2;
        
        // Clean up
        String hybrid = part1.trim() + part2.trim();
        
        // Add occasional suffixes
        String[] suffixes = {"", " X", " Hybrid", " Kush", " Haze", " OG"};
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            hybrid += suffixes[ThreadLocalRandom.current().nextInt(suffixes.length)];
        }
        
        return hybrid;
    }

    private Strain.Rarity calculateHybridRarity(Strain.Rarity r1, Strain.Rarity r2) {
        // Average of ordinals, with chance for upgrade
        int avgOrdinal = (r1.ordinal() + r2.ordinal()) / 2;
        
        // Small chance to upgrade rarity
        if (ThreadLocalRandom.current().nextDouble() < 0.20) {
            avgOrdinal = Math.min(3, avgOrdinal + 1);
        }
        
        return Strain.Rarity.values()[avgOrdinal];
    }

    private double calculateCost(CrossbreedSession session) {
        double cost = BASE_CROSSBREED_COST;
        
        if (session.strain1 != null) {
            Strain s1 = strainManager.getStrain(session.strain1);
            if (s1 != null) {
                cost *= getRarityMultiplier(s1.getRarity());
            }
        }
        
        if (session.strain2 != null) {
            Strain s2 = strainManager.getStrain(session.strain2);
            if (s2 != null) {
                cost *= getRarityMultiplier(s2.getRarity());
            }
        }
        
        return cost;
    }

    private double getRarityMultiplier(Strain.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.5;
            case RARE -> RARE_MULTIPLIER;
            case LEGENDARY -> LEGENDARY_MULTIPLIER;
        };
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    /**
     * Session data for crossbreeding.
     */
    private static class CrossbreedSession {
        String strain1;
        String strain2;
        StarRating strain1Rating;
        StarRating strain2Rating;
    }

    /**
     * Result of a crossbreed calculation.
     */
    private record CrossbreedResult(
        String name,
        Strain.Rarity rarity,
        int potency,
        int yield,
        StarRating seedRating,
        boolean hasMutation
    ) {}
}
