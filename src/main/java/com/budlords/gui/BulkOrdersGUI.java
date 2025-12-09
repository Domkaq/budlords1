package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.economy.BulkOrderManager;
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

/**
 * GUI for viewing and managing bulk orders.
 */
public class BulkOrdersGUI implements InventoryHolder, Listener {
    
    private final BudLords plugin;
    private final BulkOrderManager bulkOrderManager;
    
    public BulkOrdersGUI(BudLords plugin, BulkOrderManager bulkOrderManager) {
        this.plugin = plugin;
        this.bulkOrderManager = bulkOrderManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the bulk orders GUI for a player.
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, 54, "§6§l📦 BULK ORDERS §6§l📦");
        updateInventory(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }
    
    /**
     * Updates the inventory display.
     */
    private void updateInventory(Inventory inv, Player player) {
        inv.clear();
        
        // Border
        ItemStack border = createItem(Material.YELLOW_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
        
        // Header
        inv.setItem(4, createItem(Material.FILLED_MAP,
            "§6§l📦 BULK ORDERS",
            List.of(
                "",
                "§7Special large-quantity orders",
                "§7from buyers with bonus rewards!",
                "",
                "§eClick an order to view details"
            )));
        
        // Get player's active order
        BulkOrderManager.BulkOrder order = bulkOrderManager.getActiveOrder(player.getUniqueId());
        
        if (order == null || order.isExpired()) {
            // No active order - show generation button
            inv.setItem(22, createItem(Material.PAPER,
                "§e§lGenerate New Order",
                List.of(
                    "",
                    "§7Click to request a new",
                    "§7bulk order from buyers!",
                    "",
                    "§7Orders refresh every §e30 minutes",
                    "",
                    "§a▶ Click to generate"
                )));
        } else {
            // Show active order
            Strain strain = plugin.getStrainManager().getStrain(order.strainId);
            Material icon = strain != null ? strain.getIconMaterial() : Material.SHORT_GRASS;
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Buyer: §e" + order.buyerName);
            lore.add("§7Strain: §a" + order.strainName);
            lore.add("§7Quantity: §e" + order.quantity + " units");
            lore.add("");
            lore.add("§7Bonus: §6+" + String.format("%.0f%%", (order.priceMultiplier - 1.0) * 100));
            lore.add("§7Tier: " + order.tier.displayName);
            lore.add("");
            lore.add("§7Time Remaining: §e" + order.getTimeRemainingText());
            lore.add("");
            lore.add("§8Fulfill this order by selling");
            lore.add("§8the required strain to any buyer!");
            
            inv.setItem(22, createItem(icon, "§6§lACTIVE ORDER", lore));
            
            // Tips
            inv.setItem(40, createItem(Material.BOOK,
                "§e§l💡 Bulk Order Tips",
                List.of(
                    "",
                    "§7• Bulk orders give §6bonus payments",
                    "§7• Higher tiers = better bonuses",
                    "§7• Orders expire after time limit",
                    "§7• Sell to ANY buyer to complete",
                    "",
                    "§7Order tiers:",
                    "§7  Small: §e15-25% bonus",
                    "§7  Medium: §e25-40% bonus",
                    "§7  Large: §e40-60% bonus",
                    "§7  Massive: §e60-100% bonus",
                    "§7  Legendary: §e100-200% bonus!"
                )));
        }
        
        // Close button
        inv.setItem(49, createItem(Material.BARRIER,
            "§c§lClose",
            List.of("", "§7Click to close")));
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BulkOrdersGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        
        // Close button
        if (slot == 49) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        // Generate order button
        if (slot == 22) {
            BulkOrderManager.BulkOrder currentOrder = bulkOrderManager.getActiveOrder(player.getUniqueId());
            
            if (currentOrder != null && !currentOrder.isExpired()) {
                player.sendMessage("§cYou already have an active bulk order!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            // Try to generate new order
            BulkOrderManager.BulkOrder newOrder = bulkOrderManager.generateOrder(player.getUniqueId());
            
            if (newOrder == null) {
                player.sendMessage("§cCouldn't generate order. Try again later!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            // Success!
            player.sendMessage("");
            player.sendMessage("§6§l✦ NEW BULK ORDER GENERATED! ✦");
            player.sendMessage("§7Buyer: §e" + newOrder.buyerName);
            player.sendMessage("§7Wants: §a" + newOrder.quantity + "x " + newOrder.strainName);
            player.sendMessage("§7Bonus: §6+" + String.format("%.0f%%", (newOrder.priceMultiplier - 1.0) * 100));
            player.sendMessage("§7Time: §e" + newOrder.getTimeRemainingText());
            player.sendMessage("");
            
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
            
            // Refresh GUI
            updateInventory(event.getInventory(), player);
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
