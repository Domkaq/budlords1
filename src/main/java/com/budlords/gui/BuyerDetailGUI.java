package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.npc.IndividualBuyer;
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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detailed buyer profile GUI showing comprehensive information about a specific buyer.
 * 
 * Shows:
 * - Buyer info (name, personality, backstory)
 * - Relationship status and bonuses
 * - Purchase history with top products
 * - Favorite strains and preferences
 * - Special requests and dialogue
 * - Statistics and insights
 */
public class BuyerDetailGUI implements InventoryHolder, Listener {
    
    private final BudLords plugin;
    private final StrainManager strainManager;
    
    // Track which buyer each player is viewing
    private final Map<UUID, UUID> viewingSessions;
    
    public BuyerDetailGUI(BudLords plugin, StrainManager strainManager) {
        this.plugin = plugin;
        this.strainManager = strainManager;
        this.viewingSessions = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the detailed buyer profile for a player.
     */
    @SuppressWarnings("deprecation")
    public void open(Player player, IndividualBuyer buyer) {
        viewingSessions.put(player.getUniqueId(), buyer.getId());
        
        String title = buyer.getPersonality().getColorCode() + "§l" + buyer.getName();
        Inventory inv = Bukkit.createInventory(this, 54, title);
        updateInventory(inv, player, buyer);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f);
    }
    
    private void updateInventory(Inventory inv, Player player, IndividualBuyer buyer) {
        inv.clear();
        
        // Border
        ItemStack borderGold = createItem(Material.YELLOW_STAINED_GLASS_PANE, " ", null);
        ItemStack borderOrange = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ", null);
        
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderGold : borderOrange);
            inv.setItem(45 + i, i % 2 == 0 ? borderGold : borderOrange);
        }
        
        // Buyer icon
        ItemStack icon = createItem(buyer.getHeadMaterial(),
            buyer.getPersonality().getColorCode() + "§l" + buyer.getName(),
            Arrays.asList(
                "",
                "§7Type: " + buyer.getPersonality().getDisplayName(),
                "§7Status: " + buyer.getRelationshipSummary(),
                ""
            ));
        inv.setItem(4, icon);
        
        // Backstory
        ItemStack backstory = createItem(Material.BOOK,
            "§6§l📖 Backstory",
            Arrays.asList(
                "",
                "§7" + buyer.getBackstory(),
                ""
            ));
        inv.setItem(10, backstory);
        
        // Personality traits
        List<String> traitLore = new ArrayList<>();
        traitLore.add("");
        traitLore.add("§7Personality: " + buyer.getPersonality().getDisplayName());
        traitLore.add("§7Current Mood: " + getMoodDisplay(buyer.getCurrentMood()));
        traitLore.add("");
        if (buyer.isPrefersQuality()) {
            traitLore.add("§e✦ Prefers High Quality (4-5★)");
        }
        if (buyer.isPrefersBulk()) {
            traitLore.add("§e✦ Prefers Bulk Orders (10g+)");
        }
        traitLore.add("§e✦ Prefers " + getRarityDisplay(buyer.getFavoriteRarity()) + " §7strains");
        traitLore.add("");
        
        ItemStack traits = createItem(Material.ENCHANTED_BOOK, "§d§l✦ Personality Traits", traitLore);
        inv.setItem(11, traits);
        
        // Special request
        ItemStack request = createItem(Material.PAPER,
            "§e§l💬 Special Request",
            Arrays.asList(
                "",
                "§7\"" + buyer.getSpecialRequest() + "\"",
                ""
            ));
        inv.setItem(12, request);
        
        // Statistics
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        Date firstMet = new Date(buyer.getFirstMetTimestamp());
        Date lastSeen = new Date(buyer.getLastSeenTimestamp());
        long daysSince = (System.currentTimeMillis() - buyer.getLastSeenTimestamp()) / (1000 * 60 * 60 * 24);
        
        ItemStack stats = createItem(Material.GOLD_INGOT,
            "§6§l📊 Statistics",
            Arrays.asList(
                "",
                "§7Total Purchases: §e" + buyer.getTotalPurchases(),
                "§7Total Spent: §a$" + String.format("%.2f", buyer.getTotalMoneySpent()),
                "§7Loyalty Bonus: §6+" + String.format("%.0f%%", (buyer.getLoyaltyBonus() - 1.0) * 100),
                "",
                "§7First Met: §e" + dateFormat.format(firstMet),
                "§7Last Seen: §e" + (daysSince == 0 ? "Today" : daysSince + " days ago"),
                "",
                "§7Average per transaction:",
                "§7 $" + String.format("%.2f", buyer.getTotalPurchases() > 0 ? 
                    buyer.getTotalMoneySpent() / buyer.getTotalPurchases() : 0),
                ""
            ));
        inv.setItem(13, stats);
        
        // Purchase history - top 5 products
        Map<String, Integer> history = buyer.getPurchaseHistory();
        List<Map.Entry<String, Integer>> topProducts = history.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .collect(Collectors.toList());
        
        ItemStack historyItem = createItem(Material.CHEST,
            "§b§l📦 Purchase History",
            Arrays.asList(
                "",
                "§7Top Products Purchased:",
                ""
            ));
        
        List<String> historyLore = new ArrayList<>(historyItem.getItemMeta().getLore());
        for (int i = 0; i < topProducts.size(); i++) {
            Map.Entry<String, Integer> entry = topProducts.get(i);
            Strain strain = strainManager.getStrain(entry.getKey());
            String strainName = strain != null ? strain.getName() : entry.getKey();
            historyLore.add("§e" + (i + 1) + ". §a" + strainName + " §7- §e" + entry.getValue() + "g");
        }
        
        if (topProducts.isEmpty()) {
            historyLore.add("§7No purchases yet");
        }
        historyLore.add("");
        
        ItemMeta historyMeta = historyItem.getItemMeta();
        historyMeta.setLore(historyLore);
        historyItem.setItemMeta(historyMeta);
        inv.setItem(14, historyItem);
        
        // Favorites
        List<String> favorites = buyer.getFavoriteStrains();
        List<String> favLore = new ArrayList<>();
        favLore.add("");
        favLore.add("§7This buyer's favorite products:");
        favLore.add("");
        
        for (String strainId : favorites) {
            Strain strain = strainManager.getStrain(strainId);
            if (strain != null) {
                favLore.add("§a❤ " + strain.getName());
            }
        }
        
        if (favorites.isEmpty()) {
            favLore.add("§7No favorites yet");
            favLore.add("§7§oSell to them to discover!");
        }
        favLore.add("");
        favLore.add("§7Selling favorites gives §a+15% §7bonus!");
        favLore.add("");
        
        ItemStack favItem = createItem(Material.EMERALD, "§a§l❤ Favorites", favLore);
        inv.setItem(15, favItem);
        
        // Memory dialogue
        List<String> memories = buyer.getMemoryDialogue();
        List<String> memLore = new ArrayList<>();
        memLore.add("");
        memLore.add("§7Recent conversations:");
        memLore.add("");
        
        for (String memory : memories) {
            memLore.add("§e\"§7" + memory + "§e\"");
        }
        
        if (memories.isEmpty()) {
            memLore.add("§7No memories yet");
        }
        memLore.add("");
        
        ItemStack memItem = createItem(Material.WRITABLE_BOOK, "§6§l💭 Memories", memLore);
        inv.setItem(16, memItem);
        
        // Price calculation info
        ItemStack priceInfo = createItem(Material.DIAMOND,
            "§b§l💰 Price Multipliers",
            Arrays.asList(
                "",
                "§7This buyer pays:",
                "",
                "§7Base: " + buyer.getPersonality().getColorCode() + buyer.getPersonality().getDisplayName(),
                "§7Loyalty: §6+" + String.format("%.0f%%", (buyer.getLoyaltyBonus() - 1.0) * 100),
                "§7Favorites: §a+15%",
                "§7Preferred Rarity: §e+10%",
                "§7Quality Preference: §d+20% §7(4-5★)",
                "§7Bulk Preference: §6+15% §7(10g+)",
                "§7Loyal Mood: §a+10%",
                "",
                "§7§oStack bonuses for best prices!",
                ""
            ));
        inv.setItem(22, priceInfo);
        
        // Greeting
        ItemStack greeting = createItem(Material.NAME_TAG,
            "§e§l💬 Greeting",
            Arrays.asList(
                "",
                buyer.getGreeting(),
                ""
            ));
        inv.setItem(31, greeting);
        
        // Back button
        ItemStack back = createItem(Material.ARROW,
            "§e◄ Back to Registry",
            Arrays.asList("§7Return to buyer list"));
        inv.setItem(45, back);
        
        // Close button
        ItemStack close = createItem(Material.BARRIER,
            "§c✕ Close",
            Arrays.asList("§7Exit buyer profile"));
        inv.setItem(49, close);
    }
    
    private String getMoodDisplay(String mood) {
        return switch (mood) {
            case "loyal" -> "§a§l♥ LOYAL";
            case "satisfied" -> "§a§l😊 Happy";
            case "missed_you" -> "§e§l🤝 Missed You";
            case "new" -> "§7§l👤 New";
            default -> "§f§l😐 Neutral";
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
        if (!(event.getInventory().getHolder() instanceof BuyerDetailGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        
        // Back button
        if (clicked.getType() == Material.ARROW) {
            player.closeInventory();
            // Open buyer list (would need reference to BuyerListGUI)
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        // Close button
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            return;
        }
    }
    
    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, 54, "§6§lBuyer Profile");
    }
}
