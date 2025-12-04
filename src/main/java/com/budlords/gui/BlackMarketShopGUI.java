package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Shop GUI for BlackMarket Joe where he SELLS items to players:
 * - Seeds of various strains (★1-5)
 * - Illegal items and special materials
 * 
 * This is different from regular Market Joe - BlackMarket Joe sells TO players,
 * not buys FROM them (though he also buys packaged products when player holds them).
 */
public class BlackMarketShopGUI implements InventoryHolder, Listener {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final StrainManager strainManager;

    // Base prices for seeds (multiplied by star rating and rarity)
    private static final double SEED_BASE_PRICE = 100.0;
    
    // Special item prices
    private static final double FERTILIZER_BASE_PRICE = 50.0;
    private static final double GROW_LAMP_BASE_PRICE = 150.0;

    public BlackMarketShopGUI(BudLords plugin, EconomyManager economyManager, StrainManager strainManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.strainManager = strainManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Using deprecated Inventory title API for Bukkit/Spigot compatibility
    @SuppressWarnings("deprecation")
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, 54, "§5§l☠ BlackMarket Joe's Shop");
        updateInventory(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 0.8f);
    }

    private void updateInventory(Inventory inv, Player player) {
        inv.clear();

        // Border - Dark purple theme
        ItemStack borderPurple = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null);
        ItemStack borderBlack = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);

        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderPurple : borderBlack);
            inv.setItem(45 + i, i % 2 == 0 ? borderPurple : borderBlack);
        }
        // Side borders
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, borderBlack);
            inv.setItem(i + 8, borderBlack);
        }

        // Header
        double balance = economyManager.getBalance(player);
        ItemStack header = createItem(Material.WITHER_SKELETON_SKULL,
            "§5§l☠ BlackMarket Joe's Shop",
            Arrays.asList(
                "§8\"Quality goods... no questions asked.\"",
                "",
                "§7Your balance: §e" + economyManager.formatMoney(balance),
                "",
                "§7Click an item to purchase",
                "§5§oPremium prices for premium products"
            ));
        inv.setItem(4, header);

        // Get available strains
        Collection<Strain> allStrains = strainManager.getAllStrains();
        List<Strain> strainList = new ArrayList<>(allStrains);
        
        // Sort by rarity for display
        strainList.sort((a, b) -> a.getRarity().ordinal() - b.getRarity().ordinal());

        // ====== SEEDS SECTION ======
        
        // Section header
        inv.setItem(10, createItem(Material.WHEAT_SEEDS, "§a§l🌱 Seeds for Sale",
            Arrays.asList("", "§7Premium seeds from unknown sources", "§7Plant and grow your operation!")));

        // Display up to 7 strains with star selection
        int seedSlot = 11;
        int maxSeeds = Math.min(strainList.size(), 7);
        
        for (int i = 0; i < maxSeeds && seedSlot < 18; i++) {
            Strain strain = strainList.get(i);
            StarRating rating = getRecommendedRating(strain);
            double price = calculateSeedPrice(strain, rating);
            
            inv.setItem(seedSlot, createShopItem(
                Material.WHEAT_SEEDS,
                strain.getRarity().getColorCode() + strain.getName() + " Seed " + rating.getDisplay(),
                price,
                Arrays.asList(
                    "§7Strain: §f" + strain.getName(),
                    "§7Rarity: " + strain.getRarity().getDisplayName(),
                    "§7Quality: " + rating.getDisplay(),
                    "",
                    "§7Potency: §e" + strain.getPotency() + "%",
                    "§7Yield: §e" + strain.getYield() + " buds",
                    "",
                    "§7Price: §e" + economyManager.formatMoney(price),
                    "",
                    canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money"
                ),
                "seed_" + strain.getId() + "_" + rating.getStars()
            ));
            seedSlot++;
        }

        // ====== SPECIAL ITEMS SECTION ======
        
        // Section header
        inv.setItem(19, createItem(Material.DRAGON_BREATH, "§5§l⚗ Special Items",
            Arrays.asList("", "§7Rare items not found elsewhere", "§7Boost your operation!")));

        // Fertilizer (★3-5 only - premium versions)
        for (int star = 3; star <= 5; star++) {
            StarRating rating = StarRating.fromValue(star);
            double price = calculateFertilizerPrice(star);
            inv.setItem(20 + star - 3, createShopItem(
                Material.BONE_MEAL,
                rating.getColorCode() + "Premium Fertilizer " + rating.getDisplay(),
                price,
                Arrays.asList(
                    "§7Quality: " + rating.getDisplay(),
                    "",
                    "§7Nutrient Boost: §a+" + String.format("%.0f%%", (double) (star * 20)),
                    "§7Duration: §e" + (star * 2) + " growth cycles",
                    "",
                    "§5§oBlack market special formula",
                    "",
                    "§7Price: §e" + economyManager.formatMoney(price),
                    "",
                    canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money"
                ),
                "fertilizer_" + star
            ));
        }

        // Grow Lamps (★3-5 only - premium versions)
        for (int star = 3; star <= 5; star++) {
            StarRating rating = StarRating.fromValue(star);
            double price = calculateGrowLampPrice(star);
            inv.setItem(28 + star - 3, createShopItem(
                Material.SEA_LANTERN,
                rating.getColorCode() + "Elite Grow Lamp " + rating.getDisplay(),
                price,
                Arrays.asList(
                    "§7Quality: " + rating.getDisplay(),
                    "",
                    "§7Light Output: §e" + String.format("%.0f%%", (double) (60 + star * 10)),
                    "§7Quality Bonus: §a+" + String.format("%.0f%%", (double) (star * 5)),
                    "",
                    "§5§oImported from... somewhere",
                    "",
                    "§7Price: §e" + economyManager.formatMoney(price),
                    "",
                    canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money"
                ),
                "grow_lamp_" + star
            ));
        }

        // ====== RARE SEEDS SECTION (if available) ======
        
        // Find rare and legendary strains
        List<Strain> rareStrains = strainList.stream()
            .filter(s -> s.getRarity() == Strain.Rarity.RARE || s.getRarity() == Strain.Rarity.LEGENDARY)
            .toList();
        
        if (!rareStrains.isEmpty()) {
            inv.setItem(37, createItem(Material.NETHER_STAR, "§6§l⭐ Rare Collection",
                Arrays.asList("", "§7Exclusive rare strain seeds", "§7Limited availability!")));
            
            int rareSlot = 38;
            for (int i = 0; i < Math.min(rareStrains.size(), 4) && rareSlot < 42; i++) {
                Strain strain = rareStrains.get(i);
                StarRating rating = StarRating.FOUR_STAR; // Rare seeds come at 4★ minimum
                double price = calculateSeedPrice(strain, rating) * 1.5; // Premium markup
                
                inv.setItem(rareSlot, createShopItem(
                    Material.WHEAT_SEEDS,
                    "§6✦ " + strain.getName() + " Seed " + rating.getDisplay(),
                    price,
                    Arrays.asList(
                        "§6§l✦ RARE COLLECTION ✦",
                        "",
                        "§7Strain: §f" + strain.getName(),
                        "§7Rarity: " + strain.getRarity().getDisplayName(),
                        "§7Quality: " + rating.getDisplay(),
                        "",
                        "§7Potency: §e" + strain.getPotency() + "%",
                        "§7Yield: §e" + strain.getYield() + " buds",
                        "",
                        "§7Price: §e" + economyManager.formatMoney(price),
                        "",
                        canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money"
                    ),
                    "rare_seed_" + strain.getId() + "_" + rating.getStars()
                ));
                rareSlot++;
            }
        }

        // Info panel
        inv.setItem(43, createItem(Material.BOOK, "§5§lBlackMarket Info",
            Arrays.asList(
                "",
                "§7• Seeds sold here are §5premium",
                "§7• Higher prices, better quality",
                "§7• Special items not found elsewhere",
                "",
                "§7To sell products:",
                "§7Close this menu, hold packaged",
                "§7buds and right-click me!"
            )));

        // Close button
        inv.setItem(49, createItem(Material.BARRIER, "§c§l✗ Close Shop",
            Arrays.asList("", "§7Click to close")));
    }

    private StarRating getRecommendedRating(Strain strain) {
        // Better rarity strains get better default seed quality
        return switch (strain.getRarity()) {
            case COMMON -> StarRating.TWO_STAR;
            case UNCOMMON -> StarRating.THREE_STAR;
            case RARE -> StarRating.FOUR_STAR;
            case LEGENDARY -> StarRating.FIVE_STAR;
        };
    }

    private double calculateSeedPrice(Strain strain, StarRating rating) {
        double basePrice = SEED_BASE_PRICE;
        
        // Rarity multiplier
        basePrice *= switch (strain.getRarity()) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.5;
            case RARE -> 2.5;
            case LEGENDARY -> 5.0;
        };
        
        // Star rating multiplier
        basePrice *= Math.pow(1.6, rating.getStars() - 1);
        
        // Potency affects price
        basePrice *= (1.0 + (strain.getPotency() / 200.0));
        
        return basePrice;
    }

    private double calculateFertilizerPrice(int starRating) {
        return FERTILIZER_BASE_PRICE * Math.pow(2.0, starRating - 1);
    }

    private double calculateGrowLampPrice(int starRating) {
        return GROW_LAMP_BASE_PRICE * Math.pow(2.0, starRating - 1);
    }

    private boolean canAfford(Player player, double price) {
        return economyManager.getBalance(player) >= price;
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

    private ItemStack createShopItem(Material material, String name, double price, List<String> lore, String itemId) {
        ItemStack item = createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> currentLore = meta.getLore();
            if (currentLore != null) {
                currentLore.add("§8ID: " + itemId);
                currentLore.add("§8Price: " + price);
                meta.setLore(currentLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlackMarketShopGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        // Close button
        if (slot == 49) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 0.5f, 0.8f);
            return;
        }

        // Check if it's a shop item
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasLore()) return;

        List<String> lore = meta.getLore();
        if (lore == null) return;

        String itemId = null;
        double price = 0;

        for (String line : lore) {
            if (line.startsWith("§8ID: ")) {
                itemId = line.substring(6);
            } else if (line.startsWith("§8Price: ")) {
                try {
                    price = Double.parseDouble(line.substring(9));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Failed to parse price from shop item lore: " + line);
                    return;
                }
            }
        }

        if (itemId == null || price <= 0) return;

        // Process purchase
        if (!canAfford(player, price)) {
            player.sendMessage("§cYou don't have enough money! You need " + economyManager.formatMoney(price));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }

        // Create and give item
        ItemStack purchasedItem = null;
        String itemName = "";

        // Parse item ID to determine what to give
        if (itemId.startsWith("seed_") || itemId.startsWith("rare_seed_")) {
            // Parse: seed_strainid_stars or rare_seed_strainid_stars
            // Note: strain IDs can contain underscores, so we need to find the last underscore
            // which separates the star rating from the strain ID
            try {
                String strainId;
                int starRating;
                
                if (itemId.startsWith("rare_seed_")) {
                    // rare_seed_strainid_stars - remove "rare_seed_" prefix
                    String remainder = itemId.substring(10); // Remove "rare_seed_"
                    int lastUnderscore = remainder.lastIndexOf('_');
                    if (lastUnderscore > 0) {
                        strainId = remainder.substring(0, lastUnderscore);
                        starRating = Integer.parseInt(remainder.substring(lastUnderscore + 1));
                    } else {
                        return;
                    }
                } else {
                    // seed_strainid_stars - remove "seed_" prefix
                    String remainder = itemId.substring(5); // Remove "seed_"
                    int lastUnderscore = remainder.lastIndexOf('_');
                    if (lastUnderscore > 0) {
                        strainId = remainder.substring(0, lastUnderscore);
                        starRating = Integer.parseInt(remainder.substring(lastUnderscore + 1));
                    } else {
                        return;
                    }
                }
                
                Strain strain = strainManager.getStrain(strainId);
                if (strain != null) {
                    StarRating rating = StarRating.fromValue(starRating);
                    purchasedItem = strainManager.createSeedItem(strain, 1, rating);
                    itemName = strain.getName() + " Seed " + rating.getDisplay();
                } else {
                    player.sendMessage("§cError: Strain not found!");
                    return;
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Failed to parse star rating from item ID: " + itemId + " - " + e.getMessage());
                return;
            }
        } else if (itemId.startsWith("fertilizer_")) {
            int star = Integer.parseInt(itemId.substring(11));
            StarRating rating = StarRating.fromValue(star);
            purchasedItem = plugin.getQualityItemManager().createFertilizer(rating, 1);
            itemName = "Premium Fertilizer " + rating.getDisplay();
        } else if (itemId.startsWith("grow_lamp_")) {
            int star = Integer.parseInt(itemId.substring(10));
            StarRating rating = StarRating.fromValue(star);
            purchasedItem = plugin.getQualityItemManager().createLamp(rating, 1);
            itemName = "Elite Grow Lamp " + rating.getDisplay();
        }

        if (purchasedItem == null) return;

        // Deduct money and give item
        economyManager.removeBalance(player, price);
        player.getInventory().addItem(purchasedItem);

        player.sendMessage("§5Purchased §f" + itemName + " §5for §e" + economyManager.formatMoney(price) + "§5!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f);

        // Refresh the inventory
        updateInventory(event.getInventory(), player);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
