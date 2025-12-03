package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
import com.budlords.joint.JointItems;
import com.budlords.packaging.PackItems;
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

import java.util.Arrays;
import java.util.List;

/**
 * Shop GUI for purchasing joint rolling supplies and packaging materials.
 * Extends the Market Joe's shop with additional categories.
 */
public class RollingShopGUI implements InventoryHolder, Listener {

    private final BudLords plugin;
    private final EconomyManager economyManager;

    // Prices
    private static final double ROLLING_PAPER_PRICE = 5.0;
    private static final double TOBACCO_PRICE = 10.0;
    private static final double GRINDER_BASE_PRICE = 100.0;
    private static final double PACK_1G_PRICE = 2.0;
    private static final double PACK_3G_PRICE = 5.0;
    private static final double PACK_5G_PRICE = 8.0;
    private static final double PACK_10G_PRICE = 15.0;

    public RollingShopGUI(BudLords plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Using deprecated Inventory title API for Bukkit/Spigot compatibility
    // Paper servers can replace with Adventure API's title(Component) method
    @SuppressWarnings("deprecation")
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, 54, "§6§l✦ Rolling & Packaging Shop");
        updateInventory(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 1.2f);
    }

    private void updateInventory(Inventory inv, Player player) {
        inv.clear();

        // Border
        ItemStack borderOrange = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ", null);
        ItemStack borderBrown = createItem(Material.BROWN_STAINED_GLASS_PANE, " ", null);

        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderOrange : borderBrown);
            inv.setItem(45 + i, i % 2 == 0 ? borderOrange : borderBrown);
        }
        // Side borders
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, borderBrown);
            inv.setItem(i + 8, borderBrown);
        }

        // Header
        double balance = economyManager.getBalance(player);
        ItemStack header = createItem(Material.STICK,
            "§6§l✦ Rolling & Packaging Shop",
            Arrays.asList(
                "§7Everything you need for",
                "§7rolling joints and packaging buds!",
                "",
                "§7Your balance: §e" + economyManager.formatMoney(balance),
                "",
                "§7Click an item to purchase"
            ));
        inv.setItem(4, header);

        // ====== ROLLING SUPPLIES SECTION ======
        
        // Section header
        inv.setItem(10, createItem(Material.PAPER, "§f§l✦ Rolling Supplies",
            Arrays.asList("", "§7Items for rolling joints", "§7Essential for the minigame!")));

        // Rolling Paper (x5)
        inv.setItem(11, createShopItem(
            Material.PAPER,
            "§f✦ Rolling Paper §7(x5)",
            ROLLING_PAPER_PRICE,
            Arrays.asList(
                "§7Essential for rolling joints",
                "",
                "§7Amount: §e5 papers",
                "",
                "§7Price: §e" + economyManager.formatMoney(ROLLING_PAPER_PRICE),
                "",
                canAfford(player, ROLLING_PAPER_PRICE) ? "§a▶ Click to buy" : "§c✗ Not enough money"
            ),
            "rolling_paper"
        ));

        // Tobacco (x5)
        inv.setItem(12, createShopItem(
            Material.DRIED_KELP,
            "§6✦ Tobacco §7(x5)",
            TOBACCO_PRICE,
            Arrays.asList(
                "§7Used in the joint mixture",
                "",
                "§7Amount: §e5 tobacco",
                "",
                "§7Price: §e" + economyManager.formatMoney(TOBACCO_PRICE),
                "",
                canAfford(player, TOBACCO_PRICE) ? "§a▶ Click to buy" : "§c✗ Not enough money"
            ),
            "tobacco"
        ));

        // Grinders (★1-5)
        for (int star = 1; star <= 5; star++) {
            StarRating rating = StarRating.fromValue(star);
            double price = calculateGrinderPrice(star);
            inv.setItem(13 + star - 1, createShopItem(
                Material.CAULDRON,
                rating.getColorCode() + "✦ Grinder " + rating.getDisplay(),
                price,
                Arrays.asList(
                    "§7Quality: " + rating.getDisplay(),
                    "",
                    "§7Grind Speed: §a" + (60 + star * 10) + "%",
                    "§7Quality Bonus: §a+" + (star * 3) + "%",
                    "",
                    "§7Grind buds before rolling!",
                    "",
                    "§7Price: §e" + economyManager.formatMoney(price),
                    "",
                    canAfford(player, price) ? "§a▶ Click to buy" : "§c✗ Not enough money"
                ),
                "grinder_" + star
            ));
        }

        // ====== PACKAGING SECTION ======
        
        // Section header
        inv.setItem(28, createItem(Material.BROWN_DYE, "§6§l✦ Packaging Supplies",
            Arrays.asList("", "§7Packs for drag-and-drop packaging", "§7Drop on buds to package!")));

        // 1g Pack (x10)
        inv.setItem(29, createShopItem(
            Material.BROWN_DYE,
            "§6✦ 1g Pack §7(x10)",
            PACK_1G_PRICE,
            Arrays.asList(
                "§7Pack for 1 gram of buds",
                "",
                "§7Value Multiplier: §a×1.00",
                "§7Amount: §e10 packs",
                "",
                "§7Drop on buds to package!",
                "",
                "§7Price: §e" + economyManager.formatMoney(PACK_1G_PRICE),
                "",
                canAfford(player, PACK_1G_PRICE) ? "§a▶ Click to buy" : "§c✗ Not enough money"
            ),
            "pack_1g"
        ));

        // 3g Pack (x10)
        inv.setItem(30, createShopItem(
            Material.BROWN_DYE,
            "§6✦ 3g Pack §7(x10)",
            PACK_3G_PRICE,
            Arrays.asList(
                "§7Pack for 3 grams of buds",
                "",
                "§7Value Multiplier: §a×1.25",
                "§7Amount: §e10 packs",
                "",
                "§7Drop on buds to package!",
                "",
                "§7Price: §e" + economyManager.formatMoney(PACK_3G_PRICE),
                "",
                canAfford(player, PACK_3G_PRICE) ? "§a▶ Click to buy" : "§c✗ Not enough money"
            ),
            "pack_3g"
        ));

        // 5g Pack (x10)
        inv.setItem(31, createShopItem(
            Material.BROWN_DYE,
            "§6✦ 5g Pack §7(x10)",
            PACK_5G_PRICE,
            Arrays.asList(
                "§7Pack for 5 grams of buds",
                "",
                "§7Value Multiplier: §a×1.50",
                "§7Amount: §e10 packs",
                "",
                "§7Drop on buds to package!",
                "",
                "§7Price: §e" + economyManager.formatMoney(PACK_5G_PRICE),
                "",
                canAfford(player, PACK_5G_PRICE) ? "§a▶ Click to buy" : "§c✗ Not enough money"
            ),
            "pack_5g"
        ));

        // 10g Pack (x10)
        inv.setItem(32, createShopItem(
            Material.BROWN_DYE,
            "§6✦ 10g Pack §7(x10)",
            PACK_10G_PRICE,
            Arrays.asList(
                "§7Pack for 10 grams of buds",
                "",
                "§7Value Multiplier: §a×2.00",
                "§7Amount: §e10 packs",
                "",
                "§7Drop on buds to package!",
                "",
                "§7Price: §e" + economyManager.formatMoney(PACK_10G_PRICE),
                "",
                canAfford(player, PACK_10G_PRICE) ? "§a▶ Click to buy" : "§c✗ Not enough money"
            ),
            "pack_10g"
        ));

        // ====== INFO SECTION ======
        
        // How to roll joints
        inv.setItem(19, createItem(Material.BOOK, "§e§lHow to Roll Joints",
            Arrays.asList(
                "",
                "§71. Buy a §fGrinder §7and §fTobacco",
                "§72. Grind buds using the grinder",
                "§73. Right-click with grinded bud",
                "§74. Complete the 4-stage minigame!",
                "",
                "§7Stages:",
                "§f• Paper Pull §7- Timing game",
                "§6• Tobacco Roll §7- Click rapidly",
                "§a• Grinding §7- Follow the pattern",
                "§e• Final Roll §7- Perfect the roll"
            )));

        // How to package
        inv.setItem(37, createItem(Material.BOOK, "§6§lHow to Package",
            Arrays.asList(
                "",
                "§71. Harvest some buds",
                "§72. §cDrop §7the buds on the ground",
                "§73. §cDrop §7a pack on the buds",
                "§74. Pick up your packaged product!",
                "",
                "§7Alternatively:",
                "§7Use §f/package <amount> §7command",
                "",
                "§7Bigger packs = Better value!"
            )));

        // Close button
        inv.setItem(49, createItem(Material.BARRIER, "§c§l✗ Close Shop",
            Arrays.asList("", "§7Click to close")));
    }

    private double calculateGrinderPrice(int starRating) {
        return GRINDER_BASE_PRICE * Math.pow(1.7, starRating - 1);
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
        if (!(event.getInventory().getHolder() instanceof RollingShopGUI)) return;
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

        switch (itemId) {
            case "rolling_paper" -> {
                purchasedItem = JointItems.createRollingPaper(5);
                itemName = "5x Rolling Paper";
            }
            case "tobacco" -> {
                purchasedItem = JointItems.createTobacco(5);
                itemName = "5x Tobacco";
            }
            case "pack_1g" -> {
                purchasedItem = PackItems.createPack(1, 10);
                itemName = "10x 1g Pack";
            }
            case "pack_3g" -> {
                purchasedItem = PackItems.createPack(3, 10);
                itemName = "10x 3g Pack";
            }
            case "pack_5g" -> {
                purchasedItem = PackItems.createPack(5, 10);
                itemName = "10x 5g Pack";
            }
            case "pack_10g" -> {
                purchasedItem = PackItems.createPack(10, 10);
                itemName = "10x 10g Pack";
            }
            default -> {
                if (itemId.startsWith("grinder_")) {
                    int star = Integer.parseInt(itemId.substring(8));
                    StarRating rating = StarRating.fromValue(star);
                    purchasedItem = JointItems.createGrinder(rating, 1);
                    itemName = "Grinder " + rating.getDisplay();
                }
            }
        }

        if (purchasedItem == null) return;

        // Deduct money and give item
        economyManager.removeBalance(player, price);
        player.getInventory().addItem(purchasedItem);

        player.sendMessage("§aPurchased §f" + itemName + " §afor §e" + economyManager.formatMoney(price) + "§a!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

        // Refresh the inventory
        updateInventory(event.getInventory(), player);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
