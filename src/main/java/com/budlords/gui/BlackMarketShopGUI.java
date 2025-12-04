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
import java.util.stream.Collectors;

/**
 * Shop GUI for BlackMarket Joe where he SELLS items to players:
 * - Seeds of various strains (★1-5)
 * - Illegal items and special materials
 * 
 * Features category pages for better organization:
 * - Seeds Shop
 * - Special Items
 * - Grow Lamps
 * - Rare Collection
 */
public class BlackMarketShopGUI implements InventoryHolder, Listener {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final StrainManager strainManager;
    
    // Track current page for each player
    private final Map<UUID, ShopCategory> playerCategories = new HashMap<>();

    // Base prices for seeds (multiplied by star rating and rarity)
    private static final double SEED_BASE_PRICE = 100.0;
    
    // Special item prices
    private static final double FERTILIZER_BASE_PRICE = 50.0;
    private static final double GROW_LAMP_BASE_PRICE = 150.0;
    
    // Shop categories
    public enum ShopCategory {
        MAIN("§5§l☠ BlackMarket Joe"),
        SEEDS("§5§l☠ Seeds Shop"),
        SPECIAL("§5§l☠ Special Items"),
        LAMPS("§5§l☠ Grow Lamps"),
        RARE("§5§l☠ Rare Collection");
        
        private final String displayName;
        
        ShopCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    public BlackMarketShopGUI(BudLords plugin, EconomyManager economyManager, StrainManager strainManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.strainManager = strainManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Using deprecated Inventory title API for Bukkit/Spigot compatibility
    @SuppressWarnings("deprecation")
    public void open(Player player) {
        openCategory(player, ShopCategory.MAIN);
    }
    
    @SuppressWarnings("deprecation")
    public void openCategory(Player player, ShopCategory category) {
        playerCategories.put(player.getUniqueId(), category);
        String title = category == ShopCategory.MAIN ? "§5§l☠ BlackMarket Joe's Shop" : category.getDisplayName();
        Inventory inv = Bukkit.createInventory(this, 54, title);
        updateInventory(inv, player, category);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 0.8f);
    }

    private void updateInventory(Inventory inv, Player player, ShopCategory category) {
        inv.clear();

        // Border - Dark purple theme with better spacing
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
                "§7Browse categories below"
            ));
        inv.setItem(4, header);

        switch (category) {
            case MAIN -> setupMainPage(inv, player);
            case SEEDS -> setupSeedsPage(inv, player);
            case SPECIAL -> setupSpecialPage(inv, player);
            case LAMPS -> setupLampsPage(inv, player);
            case RARE -> setupRarePage(inv, player);
        }

        // Close button (always at same position)
        inv.setItem(49, createItem(Material.BARRIER, "§c§l✗ Close Shop",
            Arrays.asList("", "§7Click to close")));
            
        // Back button for sub-pages
        if (category != ShopCategory.MAIN) {
            inv.setItem(45, createItem(Material.ARROW, "§7§l◀ Back to Main",
                Arrays.asList("", "§7Return to category selection")));
        }
    }
    
    private void setupMainPage(Inventory inv, Player player) {
        // Category selection menu - centered and well-aligned
        
        // Row 2: Categories
        inv.setItem(20, createCategoryItem(Material.WHEAT_SEEDS, "§a§l🌱 Seeds Shop",
            Arrays.asList(
                "",
                "§7Browse seeds of various strains",
                "§7Different rarities available!",
                "",
                "§a▶ Click to browse"
            ), "cat_seeds"));
            
        inv.setItem(22, createCategoryItem(Material.DRAGON_BREATH, "§5§l⚗ Special Items",
            Arrays.asList(
                "",
                "§7Premium fertilizers and",
                "§7other special supplies!",
                "",
                "§5▶ Click to browse"
            ), "cat_special"));
            
        inv.setItem(24, createCategoryItem(Material.SEA_LANTERN, "§e§l💡 Grow Lamps",
            Arrays.asList(
                "",
                "§7Elite grow lamps for",
                "§7maximum plant quality!",
                "",
                "§e▶ Click to browse"
            ), "cat_lamps"));
        
        // Row 3: Rare Collection
        inv.setItem(31, createCategoryItem(Material.NETHER_STAR, "§6§l⭐ Rare Collection",
            Arrays.asList(
                "",
                "§7Exclusive rare and legendary",
                "§7strain seeds!",
                "",
                "§6Limited availability!",
                "",
                "§6▶ Click to browse"
            ), "cat_rare"));
        
        // Info panel
        inv.setItem(40, createItem(Material.BOOK, "§5§lBlackMarket Info",
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
    }
    
    private void setupSeedsPage(Inventory inv, Player player) {
        // Get available strains and sort by rarity
        Collection<Strain> allStrains = strainManager.getAllStrains();
        List<Strain> strainList = new ArrayList<>(allStrains);
        strainList.sort((a, b) -> a.getRarity().ordinal() - b.getRarity().ordinal());
        
        // Filter out rare/legendary for this page (they go in rare collection)
        List<Strain> normalStrains = strainList.stream()
            .filter(s -> s.getRarity() == Strain.Rarity.COMMON || s.getRarity() == Strain.Rarity.UNCOMMON)
            .toList();
        
        // Display seeds in rows (slots 10-16, 19-25, 28-34)
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int slotIndex = 0;
        
        for (Strain strain : normalStrains) {
            if (slotIndex >= slots.length) break;
            
            StarRating rating = getRecommendedRating(strain);
            double price = calculateSeedPrice(strain, rating);
            
            inv.setItem(slots[slotIndex], createShopItem(
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
            slotIndex++;
        }
    }
    
    private void setupSpecialPage(Inventory inv, Player player) {
        // Fertilizers in first row (slots 11-15)
        for (int star = 1; star <= 5; star++) {
            StarRating rating = StarRating.fromValue(star);
            double price = calculateFertilizerPrice(star);
            inv.setItem(10 + star, createShopItem(
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
        
        // Info section
        inv.setItem(22, createItem(Material.BOOK, "§5§lFertilizer Info",
            Arrays.asList(
                "",
                "§7Fertilizers boost plant nutrients",
                "§7and improve final bud quality!",
                "",
                "§7Higher ★ = Better results"
            )));
    }
    
    private void setupLampsPage(Inventory inv, Player player) {
        // Grow Lamps (slots 11-15)
        for (int star = 1; star <= 5; star++) {
            StarRating rating = StarRating.fromValue(star);
            double price = calculateGrowLampPrice(star);
            inv.setItem(10 + star, createShopItem(
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
        
        // Info section
        inv.setItem(22, createItem(Material.BOOK, "§e§lLamp Info",
            Arrays.asList(
                "",
                "§7Grow lamps provide light and",
                "§7improve plant growth speed!",
                "",
                "§7Place near pots for best effect"
            )));
    }
    
    private void setupRarePage(Inventory inv, Player player) {
        // Find rare and legendary strains
        Collection<Strain> allStrains = strainManager.getAllStrains();
        List<Strain> rareStrains = allStrains.stream()
            .filter(s -> s.getRarity() == Strain.Rarity.RARE || s.getRarity() == Strain.Rarity.LEGENDARY)
            .toList();
        
        // Display rare seeds (slots 11-15, 20-24)
        int[] slots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
        int slotIndex = 0;
        
        for (Strain strain : rareStrains) {
            if (slotIndex >= slots.length) break;
            
            StarRating rating = StarRating.FOUR_STAR; // Rare seeds at 4★ minimum
            if (strain.getRarity() == Strain.Rarity.LEGENDARY) {
                rating = StarRating.FIVE_STAR;
            }
            double price = calculateSeedPrice(strain, rating) * 1.5; // Premium markup
            
            inv.setItem(slots[slotIndex], createShopItem(
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
            slotIndex++;
        }
        
        if (rareStrains.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, "§cNo Rare Seeds Available",
                Arrays.asList("", "§7Check back later!", "§7Rare strains come and go...")));
        }
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

    private ItemStack createCategoryItem(Material material, String name, List<String> lore, String categoryId) {
        ItemStack item = createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> currentLore = meta.getLore();
            if (currentLore != null) {
                currentLore.add("§8ID: " + categoryId);
                meta.setLore(currentLore);
            }
            item.setItemMeta(meta);
        }
        return item;
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
        
        // Back button
        if (slot == 45) {
            ShopCategory current = playerCategories.getOrDefault(player.getUniqueId(), ShopCategory.MAIN);
            if (current != ShopCategory.MAIN) {
                openCategory(player, ShopCategory.MAIN);
            }
            return;
        }

        // Check if it's a shop item or category
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

        if (itemId == null) return;
        
        // Handle category navigation
        if (itemId.startsWith("cat_")) {
            String categoryName = itemId.substring(4).toUpperCase();
            try {
                ShopCategory category = ShopCategory.valueOf(categoryName);
                openCategory(player, category);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown category: " + categoryName);
            }
            return;
        }
        
        // Below is purchase handling - needs valid price
        if (price <= 0) return;

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
            try {
                int star = Integer.parseInt(itemId.substring(11));
                StarRating rating = StarRating.fromValue(star);
                purchasedItem = plugin.getQualityItemManager().createFertilizer(rating, 1);
                itemName = "Premium Fertilizer " + rating.getDisplay();
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Failed to parse fertilizer rating from item ID: " + itemId + " - " + e.getMessage());
                return;
            }
        } else if (itemId.startsWith("grow_lamp_")) {
            try {
                int star = Integer.parseInt(itemId.substring(10));
                StarRating rating = StarRating.fromValue(star);
                purchasedItem = plugin.getQualityItemManager().createLamp(rating, 1);
                itemName = "Elite Grow Lamp " + rating.getDisplay();
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Failed to parse grow lamp rating from item ID: " + itemId + " - " + e.getMessage());
                return;
            }
        }

        if (purchasedItem == null) return;

        // Deduct money and give item
        economyManager.removeBalance(player, price);
        player.getInventory().addItem(purchasedItem);

        player.sendMessage("§5Purchased §f" + itemName + " §5for §e" + economyManager.formatMoney(price) + "§5!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f);

        // Refresh the inventory with current category
        ShopCategory currentCategory = playerCategories.getOrDefault(player.getUniqueId(), ShopCategory.MAIN);
        updateInventory(event.getInventory(), player, currentCategory);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
