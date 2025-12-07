package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.npc.BuyerRegistry;
import com.budlords.npc.IndividualBuyer;
import com.budlords.strain.Strain;
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

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Professional paginated GUI for viewing all buyers in the registry.
 * Shows buyer cards with stats, favorites, and purchase history.
 * 
 * Features:
 * - Pagination support for large buyer lists
 * - Multiple sort options (purchases, value, recency, name)
 * - Click to view detailed buyer profile
 * - Visual indicators for relationship levels
 * - Search/filter capabilities
 */
public class BuyerListGUI implements InventoryHolder, Listener {
    
    private final BudLords plugin;
    private final BuyerRegistry buyerRegistry;
    private final BuyerDetailGUI detailGUI;
    
    // Track current page and sort for each player
    private final Map<UUID, Integer> currentPage;
    private final Map<UUID, BuyerRegistry.BuyerSortType> currentSort;
    
    private static final int BUYERS_PER_PAGE = 28; // 4 rows of 7 buyers
    
    public BuyerListGUI(BudLords plugin, BuyerRegistry buyerRegistry, BuyerDetailGUI detailGUI) {
        this.plugin = plugin;
        this.buyerRegistry = buyerRegistry;
        this.detailGUI = detailGUI;
        this.currentPage = new HashMap<>();
        this.currentSort = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the buyer list GUI for a player.
     */
    @SuppressWarnings("deprecation")
    public void open(Player player) {
        currentPage.putIfAbsent(player.getUniqueId(), 0);
        currentSort.putIfAbsent(player.getUniqueId(), BuyerRegistry.BuyerSortType.PURCHASES);
        
        Inventory inv = Bukkit.createInventory(this, 54, "§6§l📋 Buyer Registry");
        updateInventory(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
    }
    
    private void updateInventory(Inventory inv, Player player) {
        inv.clear();
        
        UUID playerId = player.getUniqueId();
        int page = currentPage.getOrDefault(playerId, 0);
        BuyerRegistry.BuyerSortType sort = currentSort.getOrDefault(playerId, BuyerRegistry.BuyerSortType.PURCHASES);
        
        // Get buyers for current page
        List<IndividualBuyer> buyers = buyerRegistry.getBuyersPage(page, BUYERS_PER_PAGE, sort);
        int totalPages = buyerRegistry.getTotalPages(BUYERS_PER_PAGE);
        
        // Header info
        ItemStack header = createItem(Material.WRITABLE_BOOK, 
            "§6§l📋 Buyer Registry",
            Arrays.asList(
                "§7Total Buyers: §e" + buyerRegistry.getAllBuyers().size(),
                "§7Page: §e" + (page + 1) + " §7/ §e" + totalPages,
                "",
                "§7Sort: " + getSortDisplay(sort),
                "",
                "§7Click buyers to view details"
            ));
        inv.setItem(4, header);
        
        // Sort buttons
        inv.setItem(0, createSortButton(Material.DIAMOND, "§b★ By Value", 
            "Sort by total money spent", BuyerRegistry.BuyerSortType.VALUE));
        inv.setItem(1, createSortButton(Material.CHEST, "§a★ By Purchases", 
            "Sort by number of transactions", BuyerRegistry.BuyerSortType.PURCHASES));
        inv.setItem(2, createSortButton(Material.CLOCK, "§e★ By Recency", 
            "Sort by last interaction", BuyerRegistry.BuyerSortType.RECENCY));
        inv.setItem(3, createSortButton(Material.NAME_TAG, "§f★ By Name", 
            "Sort alphabetically", BuyerRegistry.BuyerSortType.NAME));
        
        // Statistics
        Map<String, Object> stats = buyerRegistry.getStatistics();
        ItemStack statsItem = createItem(Material.PAPER,
            "§6§l📊 Statistics",
            Arrays.asList(
                "§7Total Buyers: §e" + stats.get("total_buyers"),
                "§7Total Transactions: §e" + stats.get("total_purchases"),
                "§7Total Revenue: §e$" + String.format("%.2f", stats.get("total_money")),
                "",
                "§7Best Customer: §a" + (stats.get("most_purchases") != null ? stats.get("most_purchases") : "None"),
                "§7Highest Value: §6" + (stats.get("highest_value") != null ? stats.get("highest_value") : "None")
            ));
        inv.setItem(8, statsItem);
        
        // Border
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, border);
        }
        
        // Buyer cards
        int slot = 18;
        for (IndividualBuyer buyer : buyers) {
            if (slot >= 45) break; // Don't overflow into control row
            
            ItemStack buyerCard = createBuyerCard(buyer);
            inv.setItem(slot, buyerCard);
            slot++;
        }
        
        // Navigation buttons
        if (page > 0) {
            ItemStack prev = createItem(Material.ARROW, 
                "§e◄ Previous Page", 
                Arrays.asList("§7Go to page " + page));
            inv.setItem(45, prev);
        }
        
        if (page < totalPages - 1) {
            ItemStack next = createItem(Material.ARROW, 
                "§eNext Page ►", 
                Arrays.asList("§7Go to page " + (page + 2)));
            inv.setItem(53, next);
        }
        
        // Analytics button
        ItemStack analytics = createItem(Material.SPYGLASS,
            "§d§l📊 Analytics Dashboard",
            Arrays.asList("§7View insights and recommendations", "§a▶ Click to open"));
        inv.setItem(48, analytics);
        
        // Close button
        ItemStack close = createItem(Material.BARRIER, 
            "§c✕ Close", 
            Arrays.asList("§7Return to phone"));
        inv.setItem(49, close);
    }
    
    private ItemStack createBuyerCard(IndividualBuyer buyer) {
        Material icon = buyer.getHeadMaterial();
        String name = buyer.getPersonality().getColorCode() + "§l" + buyer.getName();
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Type: " + buyer.getPersonality().getDisplayName());
        lore.add("§7Status: " + buyer.getRelationshipSummary());
        lore.add("");
        lore.add("§7Purchases: §e" + buyer.getTotalPurchases());
        lore.add("§7Total Spent: §a$" + String.format("%.2f", buyer.getTotalMoneySpent()));
        lore.add("§7Loyalty Bonus: §6+" + String.format("%.0f%%", (buyer.getLoyaltyBonus() - 1.0) * 100));
        lore.add("");
        
        // Show favorite strain if available
        if (!buyer.getFavoriteStrains().isEmpty()) {
            lore.add("§7Favorite: §a" + buyer.getFavoriteStrains().get(0));
        }
        
        // Show preferred rarity
        lore.add("§7Prefers: " + getRarityDisplay(buyer.getFavoriteRarity()));
        
        // Show last seen
        long daysSince = (System.currentTimeMillis() - buyer.getLastSeenTimestamp()) / (1000 * 60 * 60 * 24);
        if (daysSince == 0) {
            lore.add("§7Last seen: §aToday");
        } else if (daysSince == 1) {
            lore.add("§7Last seen: §eYesterday");
        } else {
            lore.add("§7Last seen: §7" + daysSince + " days ago");
        }
        
        lore.add("");
        lore.add("§e▶ Click to view profile");
        lore.add("§8ID: " + buyer.getId().toString());
        
        return createItem(icon, name, lore);
    }
    
    private ItemStack createSortButton(Material material, String name, String desc, BuyerRegistry.BuyerSortType type) {
        return createItem(material, name, 
            Arrays.asList("§7" + desc, "", "§e▶ Click to sort", "§8Sort: " + type.name()));
    }
    
    private String getSortDisplay(BuyerRegistry.BuyerSortType sort) {
        return switch (sort) {
            case PURCHASES -> "§a★ Number of Purchases";
            case VALUE -> "§b★ Money Spent";
            case RECENCY -> "§e★ Last Interaction";
            case NAME -> "§f★ Alphabetical";
        };
    }
    
    private String getRarityDisplay(Strain.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> "§7Common";
            case UNCOMMON -> "§aUncommon";
            case RARE -> "§9Rare";
            case LEGENDARY -> "§6Legendary";
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
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BuyerListGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        
        List<String> lore = meta.getLore();
        if (lore == null) return;
        
        UUID playerId = player.getUniqueId();
        
        // Check for navigation
        if (clicked.getType() == Material.ARROW) {
            if (meta.getDisplayName().contains("Previous")) {
                currentPage.put(playerId, currentPage.getOrDefault(playerId, 0) - 1);
            } else if (meta.getDisplayName().contains("Next")) {
                currentPage.put(playerId, currentPage.getOrDefault(playerId, 0) + 1);
            }
            updateInventory(event.getInventory(), player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        // Check for analytics
        if (clicked.getType() == Material.SPYGLASS) {
            player.closeInventory();
            plugin.getBuyerAnalyticsGUI().open(player);
            return;
        }
        
        // Check for close
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            return;
        }
        
        // Check for sort buttons
        for (String line : lore) {
            if (line.startsWith("§8Sort: ")) {
                String sortName = line.substring(8);
                try {
                    BuyerRegistry.BuyerSortType sortType = BuyerRegistry.BuyerSortType.valueOf(sortName);
                    currentSort.put(playerId, sortType);
                    currentPage.put(playerId, 0); // Reset to first page
                    updateInventory(event.getInventory(), player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                } catch (IllegalArgumentException e) {
                    // Not a sort button
                }
                return;
            }
        }
        
        // Check for buyer card click
        for (String line : lore) {
            if (line.startsWith("§8ID: ")) {
                String idStr = line.substring(6);
                try {
                    UUID buyerId = UUID.fromString(idStr);
                    IndividualBuyer buyer = buyerRegistry.getBuyer(buyerId);
                    if (buyer != null) {
                        player.closeInventory();
                        detailGUI.open(player, buyer);
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cError loading buyer profile.");
                }
                return;
            }
        }
    }
    
    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, 54, "§6§l📋 Buyer Registry");
    }
}
