package com.budlords.strain;

import com.budlords.BudLords;
import com.budlords.data.DataManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StrainManager {

    private final BudLords plugin;
    private final DataManager dataManager;
    private final Map<String, Strain> strains;

    public StrainManager(BudLords plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.strains = new ConcurrentHashMap<>();
        loadStrains();
        ensureDefaultStrains();
    }

    private void loadStrains() {
        FileConfiguration config = dataManager.getStrainsConfig();
        ConfigurationSection strainsSection = config.getConfigurationSection("strains");
        
        if (strainsSection == null) {
            return;
        }

        for (String id : strainsSection.getKeys(false)) {
            try {
                ConfigurationSection strainSection = strainsSection.getConfigurationSection(id);
                if (strainSection == null) continue;

                String name = strainSection.getString("name", id);
                String rarityStr = strainSection.getString("rarity", "COMMON");
                Strain.Rarity rarity = Strain.Rarity.valueOf(rarityStr.toUpperCase());
                int potency = strainSection.getInt("potency", 50);
                int yield = strainSection.getInt("yield", 3);
                int packagingQuality = strainSection.getInt("packaging-quality", 50);
                String materialStr = strainSection.getString("icon", "GREEN_DYE");
                Material iconMaterial = Material.valueOf(materialStr.toUpperCase());

                Strain strain = new Strain(id, name, rarity, potency, yield, packagingQuality);
                strain.setIconMaterial(iconMaterial);
                strains.put(id, strain);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load strain: " + id);
            }
        }
    }

    private void ensureDefaultStrains() {
        if (strains.isEmpty()) {
            registerStrain(new Strain("og_kush", "OG Kush", Strain.Rarity.COMMON, 40, 3, 50));
            registerStrain(new Strain("purple_haze", "Purple Haze", Strain.Rarity.UNCOMMON, 60, 4, 65));
            registerStrain(new Strain("white_widow", "White Widow", Strain.Rarity.RARE, 75, 5, 80));
            registerStrain(new Strain("northern_lights", "Northern Lights", Strain.Rarity.LEGENDARY, 95, 7, 95));
            saveStrains();
        }
    }

    public void registerStrain(Strain strain) {
        strains.put(strain.getId(), strain);
        saveStrainToConfig(strain);
    }

    private void saveStrainToConfig(Strain strain) {
        FileConfiguration config = dataManager.getStrainsConfig();
        String path = "strains." + strain.getId();
        
        config.set(path + ".name", strain.getName());
        config.set(path + ".rarity", strain.getRarity().name());
        config.set(path + ".potency", strain.getPotency());
        config.set(path + ".yield", strain.getYield());
        config.set(path + ".packaging-quality", strain.getPackagingQuality());
        config.set(path + ".icon", strain.getIconMaterial().name());
    }

    public void saveStrains() {
        for (Strain strain : strains.values()) {
            saveStrainToConfig(strain);
        }
        dataManager.saveStrains();
    }

    public Strain getStrain(String id) {
        return strains.get(id);
    }

    public Collection<Strain> getAllStrains() {
        return Collections.unmodifiableCollection(strains.values());
    }

    public int getStrainCount() {
        return strains.size();
    }

    public boolean hasStrain(String id) {
        return strains.containsKey(id);
    }

    public void removeStrain(String id) {
        strains.remove(id);
        dataManager.getStrainsConfig().set("strains." + id, null);
        dataManager.saveStrains();
    }

    public ItemStack createSeedItem(Strain strain, int amount) {
        return createSeedItem(strain, amount, com.budlords.quality.StarRating.ONE_STAR);
    }
    
    public ItemStack createSeedItem(Strain strain, int amount, com.budlords.quality.StarRating starRating) {
        ItemStack seed = new ItemStack(Material.WHEAT_SEEDS, amount);
        ItemMeta meta = seed.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(starRating.getColorCode() + strain.getName() + " Seed " + starRating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Strain: §f" + strain.getName());
            lore.add("§7Quality: " + starRating.getDisplay());
            lore.add("§7Rarity: " + strain.getRarity().getDisplayName());
            lore.add("§7Potency: §e" + strain.getPotency() + "%");
            lore.add("§7Yield: §e" + strain.getYield() + " buds");
            lore.add("");
            lore.add("§7Plant in a Growing Pot!");
            lore.add("");
            lore.add("§8ID: " + strain.getId());
            lore.add("§8Rating: " + starRating.getStars());
            meta.setLore(lore);
            seed.setItemMeta(meta);
        }
        
        return seed;
    }

    public ItemStack createBudItem(Strain strain, int amount) {
        return createBudItem(strain, amount, com.budlords.quality.StarRating.ONE_STAR);
    }
    
    public ItemStack createBudItem(Strain strain, int amount, com.budlords.quality.StarRating starRating) {
        ItemStack bud = new ItemStack(Material.GREEN_DYE, amount);
        ItemMeta meta = bud.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(starRating.getColorCode() + strain.getName() + " Bud " + starRating.getDisplay());
            List<String> lore = new ArrayList<>();
            lore.add("§7Strain: §f" + strain.getName());
            lore.add("§7Quality: " + starRating.getDisplay());
            lore.add("§7Rarity: " + strain.getRarity().getDisplayName());
            lore.add("§7Potency: §e" + strain.getPotency() + "%");
            lore.add("§7Packaging Quality: §e" + strain.getPackagingQuality() + "%");
            lore.add("");
            lore.add("§7Use §f/package §7to package for sale");
            lore.add("");
            lore.add("§8ID: " + strain.getId());
            lore.add("§8Rating: " + starRating.getStars());
            meta.setLore(lore);
            bud.setItemMeta(meta);
        }
        
        return bud;
    }

    public com.budlords.quality.StarRating getSeedStarRating(ItemStack item) {
        return getStarRatingFromItem(item);
    }

    public com.budlords.quality.StarRating getBudStarRating(ItemStack item) {
        return getStarRatingFromItem(item);
    }

    private com.budlords.quality.StarRating getStarRatingFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8Rating: ")) {
                try {
                    int rating = Integer.parseInt(line.substring(10).trim());
                    return com.budlords.quality.StarRating.fromValue(rating);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public String getStrainIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
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

    public boolean isSeedItem(ItemStack item) {
        if (item == null || item.getType() != Material.WHEAT_SEEDS) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Seed");
    }

    public boolean isBudItem(ItemStack item) {
        if (item == null || item.getType() != Material.GREEN_DYE) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Bud");
    }

    public String generateUniqueId(String baseName) {
        String baseId = baseName.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
        String id = baseId;
        int counter = 1;
        while (strains.containsKey(id)) {
            id = baseId + "_" + counter;
            counter++;
        }
        return id;
    }

    /**
     * Gets the strain ID from a seed item.
     */
    public String getStrainIdFromSeed(ItemStack item) {
        if (!isSeedItem(item)) return null;
        return getStrainIdFromItem(item);
    }

    /**
     * Gets the star rating from a seed item.
     */
    public com.budlords.quality.StarRating getSeedRating(ItemStack item) {
        if (!isSeedItem(item)) return null;
        return getSeedStarRating(item);
    }
}
