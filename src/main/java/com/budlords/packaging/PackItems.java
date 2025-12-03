package com.budlords.packaging;

import com.budlords.quality.StarRating;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating pack items used in the drag-and-drop packaging system.
 * Pack items are dropped onto bud quantities to create packaged products.
 */
public class PackItems {

    private PackItems() {
        // Utility class
    }

    /**
     * Creates a pack item for the specified weight.
     */
    public static ItemStack createPack(int grams, int amount) {
        ItemStack pack = new ItemStack(Material.BROWN_DYE, amount);
        ItemMeta meta = pack.getItemMeta();
        
        if (meta != null) {
            String weightName = switch (grams) {
                case 1 -> "1g";
                case 3 -> "3g";
                case 5 -> "5g";
                case 10 -> "10g";
                default -> grams + "g";
            };
            
            meta.setDisplayName("§6§l✦ " + weightName + " Pack");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7A packaging bag for §e" + weightName + " §7of buds");
            lore.add("");
            lore.add("§e§lHow to use:");
            lore.add("§71. Drop §a" + grams + " buds §7on the ground");
            lore.add("§72. Drop this §6pack §7on the buds");
            lore.add("§73. Pick up your packaged product!");
            lore.add("");
            lore.add(getMultiplierText(grams));
            lore.add("");
            lore.add("§8Type: pack");
            lore.add("§8Grams: " + grams);
            meta.setLore(lore);
            pack.setItemMeta(meta);
        }
        
        return pack;
    }

    private static String getMultiplierText(int grams) {
        double mult = switch (grams) {
            case 1 -> 1.0;
            case 3 -> 1.25;
            case 5 -> 1.5;
            case 10 -> 2.0;
            default -> 1.0;
        };
        return "§7Value Multiplier: §a×" + String.format("%.2f", mult);
    }

    /**
     * Checks if an item is a BudLords pack.
     */
    public static boolean isPack(ItemStack item) {
        if (item == null || item.getType() != Material.BROWN_DYE) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.equals("§8Type: pack")) return true;
        }
        return false;
    }

    /**
     * Gets the gram weight from a pack item.
     */
    public static int getPackGrams(ItemStack item) {
        if (!isPack(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return 0;
        
        List<String> lore = meta.getLore();
        if (lore == null) return 0;
        
        for (String line : lore) {
            if (line.startsWith("§8Grams: ")) {
                try {
                    return Integer.parseInt(line.substring(9).trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * Creates all pack variants (1g, 3g, 5g, 10g).
     */
    public static List<ItemStack> createAllPacks(int amountEach) {
        List<ItemStack> packs = new ArrayList<>();
        packs.add(createPack(1, amountEach));
        packs.add(createPack(3, amountEach));
        packs.add(createPack(5, amountEach));
        packs.add(createPack(10, amountEach));
        return packs;
    }
}
