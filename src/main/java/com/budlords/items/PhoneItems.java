package com.budlords.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating Phone items.
 * The phone allows players to view buyer profiles and relationships.
 */
public class PhoneItems {

    private PhoneItems() {
        // Utility class
    }

    /**
     * Creates a Phone item.
     */
    public static ItemStack createPhone(int amount) {
        ItemStack phone = new ItemStack(Material.ECHO_SHARD, amount);
        ItemMeta meta = phone.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§b§l📱 Dealer Phone");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Your essential business tool!");
            lore.add("");
            lore.add("§e§lUsage:");
            lore.add("§7• Right-click on a buyer to view");
            lore.add("§7  their profile and relationship");
            lore.add("§7• Right-click air to see all buyers");
            lore.add("");
            lore.add("§8Type: dealer_phone");
            meta.setLore(lore);
            phone.setItemMeta(meta);
        }
        
        return phone;
    }

    /**
     * Checks if an item is a Phone.
     */
    public static boolean isPhone(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.equals("§8Type: dealer_phone")) return true;
        }
        return false;
    }
}
