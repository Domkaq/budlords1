package com.budlords.joint;

import com.budlords.quality.StarRating;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating joint-related items:
 * - Rolling Paper
 * - Tobacco
 * - Grinder
 * - Joint (final product)
 */
public class JointItems {

    private JointItems() {
        // Utility class
    }

    // ====== ROLLING PAPER ======
    
    public static ItemStack createRollingPaper(int amount) {
        ItemStack paper = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = paper.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§f✦ Rolling Paper");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Essential for rolling joints");
            lore.add("");
            lore.add("§7Use with grinded bud to roll!");
            lore.add("");
            lore.add("§8Type: rolling_paper");
            meta.setLore(lore);
            paper.setItemMeta(meta);
        }
        
        return paper;
    }
    
    public static boolean isRollingPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.equals("§8Type: rolling_paper")) return true;
        }
        return false;
    }
    
    // ====== TOBACCO ======
    
    public static ItemStack createTobacco(int amount) {
        ItemStack tobacco = new ItemStack(Material.DRIED_KELP, amount);
        ItemMeta meta = tobacco.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6✦ Tobacco");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Used for mixing in joints");
            lore.add("");
            lore.add("§7Combine with grinded ganja");
            lore.add("§7for the perfect blend!");
            lore.add("");
            lore.add("§8Type: tobacco");
            meta.setLore(lore);
            tobacco.setItemMeta(meta);
        }
        
        return tobacco;
    }
    
    public static boolean isTobacco(ItemStack item) {
        if (item == null || item.getType() != Material.DRIED_KELP) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.equals("§8Type: tobacco")) return true;
        }
        return false;
    }
    
    // ====== GRINDER ======
    
    public static ItemStack createGrinder(StarRating rating, int amount) {
        ItemStack grinder = new ItemStack(Material.CAULDRON, amount);
        ItemMeta meta = grinder.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(rating.getColorCode() + "✦ Grinder " + rating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Quality: " + rating.getDisplay());
            lore.add("");
            lore.add("§7Used to grind cannabis buds");
            lore.add("");
            lore.add("§7Grind Speed: §a" + (60 + rating.getStars() * 10) + "%");
            lore.add("§7Quality Bonus: §a+" + (rating.getStars() * 3) + "%");
            lore.add("");
            lore.add("§7Right-click with buds to grind!");
            lore.add("");
            lore.add("§8Type: grinder");
            lore.add("§8Rating: " + rating.getStars());
            meta.setLore(lore);
            grinder.setItemMeta(meta);
        }
        
        return grinder;
    }
    
    public static boolean isGrinder(ItemStack item) {
        if (item == null || item.getType() != Material.CAULDRON) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.equals("§8Type: grinder")) return true;
        }
        return false;
    }
    
    public static StarRating getGrinderRating(ItemStack item) {
        if (!isGrinder(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8Rating: ")) {
                try {
                    int rating = Integer.parseInt(line.substring(10).trim());
                    return StarRating.fromValue(rating);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    // ====== GRINDED BUD ======
    
    public static ItemStack createGrindedBud(String strainId, String strainName, StarRating rating, int amount) {
        ItemStack grinded = new ItemStack(Material.LIME_DYE, amount);
        ItemMeta meta = grinded.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(rating.getColorCode() + "✦ Grinded " + strainName + " " + rating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Quality: " + rating.getDisplay());
            lore.add("§7Strain: §f" + strainName);
            lore.add("");
            lore.add("§7Ready to be rolled!");
            lore.add("§7Mix with tobacco and paper");
            lore.add("");
            lore.add("§8Type: grinded_bud");
            lore.add("§8ID: " + strainId);
            lore.add("§8Rating: " + rating.getStars());
            meta.setLore(lore);
            grinded.setItemMeta(meta);
        }
        
        return grinded;
    }
    
    public static boolean isGrindedBud(ItemStack item) {
        if (item == null || item.getType() != Material.LIME_DYE) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.equals("§8Type: grinded_bud")) return true;
        }
        return false;
    }
    
    public static String getGrindedBudStrainId(ItemStack item) {
        if (!isGrindedBud(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8ID: ")) {
                return line.substring(6);
            }
        }
        return null;
    }
    
    public static StarRating getGrindedBudRating(ItemStack item) {
        if (!isGrindedBud(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8Rating: ")) {
                try {
                    int rating = Integer.parseInt(line.substring(10).trim());
                    return StarRating.fromValue(rating);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    // ====== JOINT (FINAL PRODUCT) ======
    
    public static ItemStack createJoint(String strainId, String strainName, StarRating rating, int potency, int amount) {
        ItemStack joint = new ItemStack(Material.STICK, amount);
        ItemMeta meta = joint.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(rating.getColorCode() + "§l✦ " + strainName + " Joint " + rating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━");
            lore.add("§7Quality: " + rating.getDisplay());
            lore.add("§7Strain: §f" + strainName);
            lore.add("§7Potency: §e" + potency + "%");
            lore.add("§8━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§a✦ Hand-rolled with care");
            lore.add("");
            lore.add("§7Sell to NPCs or use for effects!");
            lore.add("");
            lore.add("§8Type: joint");
            lore.add("§8ID: " + strainId);
            lore.add("§8Rating: " + rating.getStars());
            lore.add("§8Potency: " + potency);
            meta.setLore(lore);
            joint.setItemMeta(meta);
        }
        
        return joint;
    }
    
    public static boolean isJoint(ItemStack item) {
        if (item == null || item.getType() != Material.STICK) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.equals("§8Type: joint")) return true;
        }
        return false;
    }
    
    public static String getJointStrainId(ItemStack item) {
        if (!isJoint(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8ID: ")) {
                return line.substring(6);
            }
        }
        return null;
    }
    
    public static StarRating getJointRating(ItemStack item) {
        if (!isJoint(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8Rating: ")) {
                try {
                    int rating = Integer.parseInt(line.substring(10).trim());
                    return StarRating.fromValue(rating);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public static int getJointPotency(ItemStack item) {
        if (!isJoint(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return 0;
        
        List<String> lore = meta.getLore();
        if (lore == null) return 0;
        
        for (String line : lore) {
            if (line.startsWith("§8Potency: ")) {
                try {
                    return Integer.parseInt(line.substring(11).trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
