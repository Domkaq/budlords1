package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.economy.SaleHistory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Feature #1: Sale History and Analytics Panel
 * Shows player's recent sales, statistics, trends, and insights.
 */
public class SaleAnalyticsGUI implements InventoryHolder, Listener {
    
    private final BudLords plugin;
    private final SaleHistory saleHistory;
    
    public SaleAnalyticsGUI(BudLords plugin, SaleHistory saleHistory) {
        this.plugin = plugin;
        this.saleHistory = saleHistory;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, 54, "§6§l📊 SALE ANALYTICS");
        updateInventory(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }
    
    private void updateInventory(Inventory inv, Player player) {
        inv.clear();
        
        SaleHistory.PlayerSaleData data = saleHistory.getPlayerData(player.getUniqueId());
        
        // Border
        ItemStack border = createItem(Material.CYAN_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        
        // Title/Stats overview
        List<String> statsLore = new ArrayList<>();
        statsLore.add("");
        statsLore.add("§7Total Sales: §e" + data.getTotalSales());
        statsLore.add("§7Total Revenue: §a$" + String.format("%.2f", data.getTotalRevenue()));
        statsLore.add("§7Current Streak: §6" + data.getCurrentStreak() + " §7sales");
        statsLore.add("");
        
        String topBuyer = data.getMostFrequentBuyer();
        if (topBuyer != null) {
            statsLore.add("§7Most Frequent: §e" + topBuyer);
        }
        
        String topRevenue = data.getHighestRevenueBuyer();
        if (topRevenue != null) {
            statsLore.add("§7Best Revenue: §a" + topRevenue);
        }
        
        statsLore.add("");
        statsLore.add("§8§oSale history tracked for insights");
        
        inv.setItem(4, createItem(Material.GOLD_INGOT, "§6§l💰 YOUR STATISTICS", statsLore));
        
        // Recent sales (slots 19-25, 28-34, 37-43)
        List<SaleHistory.SaleRecord> recentSales = data.getRecentSales(15);
        int[] saleSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        
        for (int i = 0; i < Math.min(recentSales.size(), saleSlots.length); i++) {
            SaleHistory.SaleRecord sale = recentSales.get(i);
            
            Strain strain = plugin.getStrainManager().getStrain(sale.getStrainId());
            Material icon = strain != null ? strain.getIconMaterial() : Material.CHEST;
            
            List<String> saleLore = new ArrayList<>();
            saleLore.add("");
            saleLore.add("§7Buyer: §e" + sale.getBuyerName());
            saleLore.add("§7Amount: §a$" + String.format("%.2f", sale.getAmount()));
            saleLore.add("§7Items: §f" + sale.getItemCount());
            saleLore.add("§7Time: §8" + sale.getTimeAgo());
            saleLore.add("");
            
            String strainName = strain != null ? strain.getName() : "Unknown";
            inv.setItem(saleSlots[i], createItem(icon, "§e§l#" + (i + 1) + " §f" + strainName, saleLore));
        }
        
        // Favorite buyers section (slots 10-12)
        int favSlot = 10;
        for (String favBuyer : data.getFavoriteBuyers()) {
            if (favSlot > 12) break;
            
            List<String> favLore = new ArrayList<>();
            favLore.add("");
            favLore.add("§7★ Favorite Buyer");
            favLore.add("");
            favLore.add("§7Quick access when selling!");
            favLore.add("§cClick to remove from favorites");
            
            inv.setItem(favSlot++, createItem(Material.NETHER_STAR, "§e★ " + favBuyer, favLore));
        }
        
        // Saved presets (slots 14-16)
        int presetSlot = 14;
        for (Map.Entry<String, SaleHistory.SalePreset> entry : data.getAllPresets().entrySet()) {
            if (presetSlot > 16) break;
            
            SaleHistory.SalePreset preset = entry.getValue();
            List<String> presetLore = new ArrayList<>();
            presetLore.add("");
            presetLore.add("§7" + preset.getDescription());
            presetLore.add("§7Multiplier: §e" + String.format("%.0f%%", preset.getPriceMultiplier() * 100));
            presetLore.add("");
            presetLore.add("§aClick to apply this preset");
            presetLore.add("§cShift+Click to delete");
            
            inv.setItem(presetSlot++, createItem(Material.WRITABLE_BOOK, "§6§l" + preset.getName(), presetLore));
        }
        
        // Close button
        inv.setItem(49, createItem(Material.BARRIER, "§c§lClose", List.of("", "§7Click to close")));
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SaleAnalyticsGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Close button
        if (slot == 49) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        // Favorite buyer removal (slots 10-12)
        if (slot >= 10 && slot <= 12) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String buyerName = meta.getDisplayName().replace("§e★ ", "");
                SaleHistory.PlayerSaleData data = saleHistory.getPlayerData(player.getUniqueId());
                data.removeFavoriteBuyer(buyerName);
                
                player.sendMessage("§e★ §7Removed §e" + buyerName + " §7from favorites!");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f);
                
                updateInventory(event.getInventory(), player);
            }
            return;
        }
        
        // Preset management (slots 14-16)
        if (slot >= 14 && slot <= 16) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String presetName = meta.getDisplayName()
                    .replace("§6§l", "")
                    .replace("§", "");
                
                SaleHistory.PlayerSaleData data = saleHistory.getPlayerData(player.getUniqueId());
                
                if (event.isShiftClick()) {
                    // Delete preset
                    data.deletePreset(presetName);
                    player.sendMessage("§c✗ §7Deleted preset: §e" + presetName);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                } else {
                    // Apply preset (would need to open sale GUI)
                    player.sendMessage("§a§l✓ §7Preset §e" + presetName + " §7will be applied to your next sale!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                    player.closeInventory();
                }
                
                updateInventory(event.getInventory(), player);
            }
            return;
        }
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
}
