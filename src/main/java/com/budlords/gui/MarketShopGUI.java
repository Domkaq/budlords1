package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.diseases.PlantDisease;
import com.budlords.economy.EconomyManager;
import com.budlords.items.PhoneItems;
import com.budlords.quality.QualityItemManager;
import com.budlords.quality.StarRating;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shop GUI for Market Joe where players can buy farming equipment:
 * - Growing Pots (★1-5)
 * - Watering Cans (★1-5)
 * - Harvest Scissors (★1-5)
 * - Disease Cures
 */
public class MarketShopGUI implements InventoryHolder, Listener {

    private final BudLords plugin;
    private final EconomyManager economyManager;
    private final QualityItemManager qualityItemManager;

    // Base prices for items (multiplied by star rating)
    private static final double POT_BASE_PRICE = 50.0;
    private static final double WATERING_CAN_BASE_PRICE = 30.0;
    private static final double SCISSORS_BASE_PRICE = 75.0;
    private static final double SEEDBAG_BASE_PRICE = 40.0;
    private static final double PHONE_PRICE = 150.0;

    public MarketShopGUI(BudLords plugin, EconomyManager economyManager, QualityItemManager qualityItemManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.qualityItemManager = qualityItemManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Using deprecated Inventory title API for Bukkit/Spigot compatibility
    // Paper servers can replace with Adventure API's title(Component) method
    @SuppressWarnings("deprecation")
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, 54, "§a§l🛒 Market Joe's Shop");
        updateInventory(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 1.0f);
    }

    private void updateInventory(Inventory inv, Player player) {
        inv.clear();

        // Border
        ItemStack borderGreen = createItem(Material.GREEN_STAINED_GLASS_PANE, " ", null);
        ItemStack borderLime = createItem(Material.LIME_STAINED_GLASS_PANE, " ", null);

        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderGreen : borderLime);
            inv.setItem(45 + i, i % 2 == 0 ? borderGreen : borderLime);
        }
        // Side borders
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, borderGreen);
            inv.setItem(i + 8, borderGreen);
        }

        // Header
        double balance = economyManager.getBalance(player);
        ItemStack header = createItem(Material.EMERALD,
            "§a§l🛒 Market Joe's Shop",
            Arrays.asList(
                "§7Welcome to the market!",
                "",
                "§7Your balance: §e" + economyManager.formatMoney(balance),
                "",
                "§7Click an item to purchase"
            ));
        inv.setItem(4, header);

        // Category labels
        inv.setItem(10, createItem(Material.FLOWER_POT, "§6§l🌱 Growing Pots",
            Arrays.asList("", "§7Essential for growing plants", "§7Higher ★ = Faster growth")));
        
        inv.setItem(19, createItem(Material.BUCKET, "§b§l💧 Watering Cans",
            Arrays.asList("", "§7Water your plants", "§7Higher ★ = More efficient")));
        
        inv.setItem(28, createItem(Material.SHEARS, "§e§l✂ Harvest Scissors",
            Arrays.asList("", "§7Better harvests", "§7Higher ★ = Better yields")));
        
        inv.setItem(37, createItem(Material.BUNDLE, "§d§l🎒 Seed Bags",
            Arrays.asList("", "§7Store seeds safely", "§7Higher ★ = More capacity")));

        // Growing Pots (★1-5)
        for (int star = 1; star <= 5; star++) {
            StarRating rating = StarRating.fromValue(star);
            double price = calculatePrice(POT_BASE_PRICE, star);
            inv.setItem(11 + star - 1, createShopItem(
                Material.FLOWER_POT,
                rating.getColorCode() + "Growing Pot " + rating.getDisplay(),
                price,
                Arrays.asList(
                    "§7Quality: " + rating.getDisplay(),
                    "",
                    "§7Growth Speed: §a" + String.format("%.0f%%", rating.getGrowthSpeedMultiplier() * 100),
                    "§7Quality Bonus: §a" + String.format("%.0f%%", (rating.getQualityMultiplier() - 1) * 100),
                    "",
                    "§7Price: §e" + economyManager.formatMoney(price),
                    "",
                    canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money"
                ),
                "pot_" + star
            ));
        }

        // Watering Cans (★1-5)
        for (int star = 1; star <= 5; star++) {
            StarRating rating = StarRating.fromValue(star);
            double price = calculatePrice(WATERING_CAN_BASE_PRICE, star);
            inv.setItem(20 + star - 1, createShopItem(
                Material.BUCKET,
                rating.getColorCode() + "Watering Can " + rating.getDisplay(),
                price,
                Arrays.asList(
                    "§7Quality: " + rating.getDisplay(),
                    "",
                    "§7Water Efficiency: §a" + String.format("%.0f%%", (double) ((star * 15) + 70)),
                    "§7Capacity: §e" + (star * 5) + " uses",
                    "",
                    "§7Price: §e" + economyManager.formatMoney(price),
                    "",
                    canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money"
                ),
                "watering_can_" + star
            ));
        }

        // Harvest Scissors (★1-5)
        for (int star = 1; star <= 5; star++) {
            StarRating rating = StarRating.fromValue(star);
            double price = calculatePrice(SCISSORS_BASE_PRICE, star);
            inv.setItem(29 + star - 1, createShopItem(
                Material.SHEARS,
                rating.getColorCode() + "Harvest Scissors " + rating.getDisplay(),
                price,
                Arrays.asList(
                    "§7Quality: " + rating.getDisplay(),
                    "",
                    "§7Yield Bonus: §a+" + String.format("%.0f%%", (double) ((star - 1) * 10)),
                    "§7Quality Upgrade: §a" + String.format("%.0f%%", (double) (star * 4)) + " chance",
                    "§7Rare Drop: §a" + String.format("%.0f%%", (double) (star * 2)) + " chance",
                    "",
                    "§7Price: §e" + economyManager.formatMoney(price),
                    "",
                    canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money"
                ),
                "scissors_" + star
            ));
        }
        
        // Seed Bags (★1-5) - NEW!
        int[] seedBagCapacities = {9, 18, 27, 36, 54};
        double seedBagBasePrice = 40.0;
        for (int star = 1; star <= 5; star++) {
            StarRating rating = StarRating.fromValue(star);
            double price = calculatePrice(seedBagBasePrice, star);
            int capacity = seedBagCapacities[star - 1];
            inv.setItem(38 + star - 1, createShopItem(
                Material.BUNDLE,
                rating.getColorCode() + "Seed Bag " + rating.getDisplay(),
                price,
                Arrays.asList(
                    "§7Quality: " + rating.getDisplay(),
                    "",
                    "§7Capacity: §e" + capacity + " slots",
                    "§7Special: §dOnly stores seeds!",
                    "",
                    "§7Price: §e" + economyManager.formatMoney(price),
                    "",
                    canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money"
                ),
                "seedbag_" + star
            ));
        }

        // Info panel
        inv.setItem(40, createItem(Material.BOOK, "§e§lShopping Tips",
            Arrays.asList(
                "",
                "§7• Higher ★ items cost more",
                "§7• Better equipment = better buds",
                "§7• Invest in quality for profit!",
                "",
                "§7To sell products:",
                "§7Hold packaged buds and",
                "§7right-click me!"
            )));
        
        // Phone - special item for viewing buyer profiles
        inv.setItem(44, createShopItem(Material.ECHO_SHARD, 
            "§b§l📱 Dealer Phone",
            PHONE_PRICE,
            Arrays.asList(
                "",
                "§7Your essential business tool!",
                "",
                "§e§lUsage:",
                "§7• Right-click on buyers to view",
                "§7  their profile & your reputation",
                "§7• Right-click air to see contacts",
                "",
                "§7Price: §e" + economyManager.formatMoney(PHONE_PRICE),
                "",
                canAfford(player, PHONE_PRICE) ? "§a▶ Click to buy" : "§c✗ Not enough money"
            ),
            "phone"
        ));
        
        // Rolling Shop button
        inv.setItem(43, createShopItem(Material.STICK, 
            "§6§l✦ Rolling & Packaging Shop",
            0,
            Arrays.asList(
                "",
                "§7Buy rolling supplies:",
                "§f• Rolling Paper",
                "§6• Tobacco",
                "§a• Grinders",
                "",
                "§7Buy packaging supplies:",
                "§6• 1g, 3g, 5g, 10g Packs",
                "",
                "§a▶ Click to open"
            ),
            "rolling_shop"
        ));
        
        // Disease Cures Shop button
        inv.setItem(37, createShopItem(Material.POTION, 
            "§c§l✦ Disease Cures",
            0,
            Arrays.asList(
                "",
                "§7Cure plant diseases:",
                "§5• Fungicide",
                "§3• Antibacterial Spray",
                "§8• Pesticide",
                "§6• Nutrient Flush",
                "§a• Neem Oil",
                "§d• Healing Salve",
                "",
                "§a▶ Click to open"
            ),
            "cure_shop"
        ));

        // Close button
        inv.setItem(49, createItem(Material.BARRIER, "§c§l✗ Close Shop",
            Arrays.asList("", "§7Click to close")));
    }

    private double calculatePrice(double basePrice, int starRating) {
        // Price increases exponentially with star rating
        return basePrice * Math.pow(1.8, starRating - 1);
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
            // Store item ID in lore for identification
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
        if (!(event.getInventory().getHolder() instanceof MarketShopGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        // Close button
        if (slot == 49) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        // Check if it's a shop item (has price in lore)
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
        
        // Handle rolling shop button
        if (itemId.equals("rolling_shop")) {
            player.closeInventory();
            plugin.getRollingShopGUI().open(player);
            return;
        }
        
        // Handle cure shop button
        if (itemId.equals("cure_shop")) {
            player.closeInventory();
            openCureShopGUI(player);
            return;
        }

        if (price <= 0) return;

        // Process purchase
        if (!canAfford(player, price)) {
            player.sendMessage("§cYou don't have enough money! You need " + economyManager.formatMoney(price));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }

        // Determine item type and star rating
        ItemStack purchasedItem = null;
        String itemName = "";

        if (itemId.startsWith("pot_")) {
            try {
                int star = Integer.parseInt(itemId.substring(4));
                StarRating rating = StarRating.fromValue(star);
                purchasedItem = qualityItemManager.createPot(rating, 1);
                itemName = "Growing Pot " + rating.getDisplay();
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Failed to parse pot rating from item ID: " + itemId);
                return;
            }
        } else if (itemId.startsWith("watering_can_")) {
            try {
                int star = Integer.parseInt(itemId.substring(13));
                StarRating rating = StarRating.fromValue(star);
                purchasedItem = qualityItemManager.createWateringCan(rating, 1);
                itemName = "Watering Can " + rating.getDisplay();
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Failed to parse watering can rating from item ID: " + itemId);
                return;
            }
        } else if (itemId.startsWith("scissors_")) {
            try {
                int star = Integer.parseInt(itemId.substring(9));
                StarRating rating = StarRating.fromValue(star);
                purchasedItem = qualityItemManager.createScissors(rating, 1);
                itemName = "Harvest Scissors " + rating.getDisplay();
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Failed to parse scissors rating from item ID: " + itemId);
                return;
            }
        } else if (itemId.startsWith("seedbag_")) {
            try {
                int star = Integer.parseInt(itemId.substring(8));
                StarRating rating = StarRating.fromValue(star);
                purchasedItem = com.budlords.quality.SeedBag.createSeedBagItem(rating);
                itemName = "Seed Bag " + rating.getDisplay();
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Failed to parse seed bag rating from item ID: " + itemId);
                return;
            }
        } else if (itemId.startsWith("cure_")) {
            // Handle cure purchases
            String cureName = itemId.substring(5);
            try {
                PlantDisease.Cure cure = PlantDisease.Cure.valueOf(cureName);
                purchasedItem = createCureItem(cure);
                itemName = cure.getDisplayName();
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to find cure: " + cureName);
                return;
            }
        } else if (itemId.equals("phone")) {
            // Handle phone purchase
            purchasedItem = PhoneItems.createPhone(1);
            itemName = "Dealer Phone";
        }

        if (purchasedItem == null) return;

        // Deduct money and give item
        economyManager.removeBalance(player, price);
        player.getInventory().addItem(purchasedItem);

        player.sendMessage("§aPurchased §f" + itemName + " §afor §e" + economyManager.formatMoney(price) + "§a!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

        // Refresh the inventory to update balance display
        // Check what kind of shop we're in based on title
        String title = event.getView().getTitle();
        if (title.contains("Disease Cures")) {
            openCureShopGUI(player);
        } else {
            updateInventory(event.getInventory(), player);
        }
    }
    
    /**
     * Creates a cure item that can be used on infected plants.
     */
    private ItemStack createCureItem(PlantDisease.Cure cure) {
        ItemStack item = new ItemStack(cure.getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(cure.getColoredDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7" + cure.getDescription());
            lore.add("");
            lore.add("§7Effectiveness: §a" + String.format("%.0f%%", cure.getEffectiveness() * 100));
            lore.add("");
            lore.add("§7Right-click on an infected plant");
            lore.add("§7to apply this cure!");
            lore.add("");
            lore.add("§8Type: cure");
            lore.add("§8Cure: " + cure.name());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Opens the disease cure shop GUI.
     */
    @SuppressWarnings("deprecation")
    private void openCureShopGUI(Player player) {
        Inventory inv = Bukkit.createInventory(this, 54, "§c§l✦ Disease Cures Shop");
        
        // Border
        ItemStack borderRed = createItem(Material.RED_STAINED_GLASS_PANE, " ", null);
        ItemStack borderPink = createItem(Material.PINK_STAINED_GLASS_PANE, " ", null);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderRed : borderPink);
            inv.setItem(45 + i, i % 2 == 0 ? borderRed : borderPink);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, borderRed);
            inv.setItem(i + 8, borderRed);
        }

        // Header
        double balance = economyManager.getBalance(player);
        inv.setItem(4, createItem(Material.POTION,
            "§c§l✦ Disease Cures Shop",
            Arrays.asList(
                "§7Protect your plants from diseases!",
                "",
                "§7Your balance: §e" + economyManager.formatMoney(balance),
                "",
                "§7Click a cure to purchase"
            )));

        // Add each cure type
        int slot = 10;
        for (PlantDisease.Cure cure : PlantDisease.Cure.values()) {
            // Skip Golden Elixir - too rare for regular shop
            if (cure == PlantDisease.Cure.GOLDEN_ELIXIR) continue;
            
            double price = cure.getBaseCost();
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7" + cure.getDescription());
            lore.add("");
            lore.add("§7Effectiveness: §a" + String.format("%.0f%%", cure.getEffectiveness() * 100));
            lore.add("");
            lore.add("§7Best for treating:");
            lore.add("§7" + getCureTargetInfo(cure));
            lore.add("");
            lore.add("§7Price: §e" + economyManager.formatMoney(price));
            lore.add("");
            lore.add(canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money");
            
            inv.setItem(slot, createShopItem(
                cure.getMaterial(),
                cure.getColoredDisplay(),
                price,
                lore,
                "cure_" + cure.name()
            ));
            
            slot++;
            // Skip border slots
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot++;
        }
        
        // Info section
        inv.setItem(31, createItem(Material.BOOK, "§e§lHow to Use Cures",
            Arrays.asList(
                "",
                "§7When a plant gets infected:",
                "§71. Check the disease type",
                "§72. Buy the right cure",
                "§73. Right-click the infected plant",
                "§7   with the cure item!",
                "",
                "§7Different cures work best",
                "§7against different diseases."
            )));

        // Back button
        inv.setItem(49, createItem(Material.ARROW, "§e§l← Back to Main Shop",
            Arrays.asList("", "§7Click to go back")));
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 1.0f);
    }
    
    private String getCureTargetInfo(PlantDisease.Cure cure) {
        return switch (cure) {
            case FUNGICIDE -> "Root Rot, Powdery Mildew, Botrytis";
            case ANTIBACTERIAL_SPRAY -> "Leaf Blight, Bacterial infections";
            case PESTICIDE -> "Spider Mites, Aphids, Thrips";
            case NUTRIENT_FLUSH -> "Nutrient Burn, Heat Stress, Light Burn";
            case NEEM_OIL -> "Multiple pest types (general use)";
            case HEALING_SALVE -> "General plant health (moderate effectiveness)";
            case GOLDEN_ELIXIR -> "Mystical diseases (Zombie Fungus, Crystal Virus)";
        };
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
