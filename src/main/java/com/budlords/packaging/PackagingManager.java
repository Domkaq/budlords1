package com.budlords.packaging;

import com.budlords.BudLords;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackagingManager {

    private final BudLords plugin;
    private final StrainManager strainManager;
    private final Map<Integer, Double> weightMultipliers;

    public PackagingManager(BudLords plugin, StrainManager strainManager) {
        this.plugin = plugin;
        this.strainManager = strainManager;
        this.weightMultipliers = new HashMap<>();
        loadMultipliers();
    }

    private void loadMultipliers() {
        weightMultipliers.put(1, plugin.getConfig().getDouble("packaging.multipliers.1g", 1.0));
        weightMultipliers.put(3, plugin.getConfig().getDouble("packaging.multipliers.3g", 1.25));
        weightMultipliers.put(5, plugin.getConfig().getDouble("packaging.multipliers.5g", 1.5));
        weightMultipliers.put(10, plugin.getConfig().getDouble("packaging.multipliers.10g", 2.0));
    }

    public boolean packageBuds(Player player, int grams) {
        PackagedProduct.WeightType weightType = PackagedProduct.WeightType.fromGrams(grams);
        if (weightType == null) {
            player.sendMessage("§cInvalid weight! Use 1, 3, 5, or 10 grams.");
            return false;
        }

        // Find bud items in inventory
        ItemStack budItem = null;
        int budSlot = -1;
        String strainId = null;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (strainManager.isBudItem(item)) {
                String id = strainManager.getStrainIdFromItem(item);
                if (id != null && item.getAmount() >= grams) {
                    budItem = item;
                    budSlot = i;
                    strainId = id;
                    break;
                }
            }
        }

        if (budItem == null || strainId == null) {
            player.sendMessage("§cYou need at least " + grams + " buds of the same strain to package!");
            return false;
        }

        Strain strain = strainManager.getStrain(strainId);
        if (strain == null) {
            player.sendMessage("§cStrain not found!");
            return false;
        }

        // Remove buds from inventory
        if (budItem.getAmount() == grams) {
            player.getInventory().setItem(budSlot, null);
        } else {
            budItem.setAmount(budItem.getAmount() - grams);
        }

        // Create packaged product
        ItemStack packagedItem = createPackagedItem(strain, weightType);
        
        // Give to player
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(packagedItem);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> 
                player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
        }

        player.sendMessage("§aPackaged " + grams + "g of " + strain.getName() + "!");
        return true;
    }

    public ItemStack createPackagedItem(Strain strain, PackagedProduct.WeightType weight) {
        double multiplier = weightMultipliers.getOrDefault(weight.getGrams(), weight.getMultiplier());
        double value = strain.getBaseValue() * multiplier;

        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§a" + strain.getName() + " - " + weight.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Strain: §f" + strain.getName());
            lore.add("§7Rarity: " + strain.getRarity().getDisplayName());
            lore.add("§7Weight: §e" + weight.getDisplayName());
            lore.add("§7Potency: §e" + strain.getPotency() + "%");
            lore.add("");
            lore.add("§7Base Value: §a$" + String.format("%.2f", value));
            lore.add("");
            lore.add("§7Sell to traders for profit!");
            lore.add("");
            lore.add("§8ID: " + strain.getId());
            lore.add("§8Weight: " + weight.getGrams());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isPackagedProduct(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.startsWith("§8Weight: ")) {
                return true;
            }
        }
        return false;
    }

    public String getStrainIdFromPackage(ItemStack item) {
        return strainManager.getStrainIdFromItem(item);
    }

    /**
     * Alias for getStrainIdFromPackage for compatibility.
     */
    public String getStrainId(ItemStack item) {
        return getStrainIdFromPackage(item);
    }

    public int getWeightFromPackage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return 0;
        
        List<String> lore = meta.getLore();
        if (lore == null) return 0;
        
        for (String line : lore) {
            if (line.startsWith("§8Weight: ")) {
                try {
                    return Integer.parseInt(line.substring(10));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * Alias for getWeightFromPackage for compatibility.
     */
    public int getPackageSize(ItemStack item) {
        return getWeightFromPackage(item);
    }

    /**
     * Gets the star rating from a packaged item.
     * Note: Star ratings are stored on the original buds, not packages.
     * For packages, we need to extract the rating from the bud item metadata if stored.
     * Returns null if no rating is found.
     */
    public com.budlords.quality.StarRating getStarRatingFromPackage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        // Try to find a star rating in the lore
        for (String line : lore) {
            if (line.contains("★")) {
                // Count the filled stars in the line
                long stars = line.chars().filter(ch -> ch == '★').count();
                if (stars > 0 && stars <= 6) {
                    return com.budlords.quality.StarRating.fromValue((int) stars);
                }
            }
        }
        
        // Default to ONE_STAR if not found
        return com.budlords.quality.StarRating.ONE_STAR;
    }

    public double getValueFromPackage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return 0;
        
        List<String> lore = meta.getLore();
        if (lore == null) return 0;
        
        for (String line : lore) {
            if (line.startsWith("§7Base Value: §a$")) {
                try {
                    return Double.parseDouble(line.substring(17));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
