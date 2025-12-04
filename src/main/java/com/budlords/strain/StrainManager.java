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
                
                // Load effects
                String effectsData = strainSection.getString("effects", "");

                Strain strain = new Strain(id, name, rarity, potency, yield, packagingQuality);
                strain.setIconMaterial(iconMaterial);
                strain.deserializeEffects(effectsData);
                strains.put(id, strain);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load strain: " + id);
            }
        }
    }

    private void ensureDefaultStrains() {
        if (strains.isEmpty()) {
            // Create default strains with effects
            
            // === COMMON STRAINS ===
            Strain ogKush = new Strain("og_kush", "OG Kush", Strain.Rarity.COMMON, 40, 3, 50);
            ogKush.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MUNCHIES, 2));
            registerStrain(ogKush);
            
            Strain sourDiesel = new Strain("sour_diesel", "Sour Diesel", Strain.Rarity.COMMON, 35, 4, 45);
            sourDiesel.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SPEED_DEMON, 2));
            registerStrain(sourDiesel);
            
            Strain blueDream = new Strain("blue_dream", "Blue Dream", Strain.Rarity.COMMON, 45, 3, 55);
            blueDream.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DREAM_STATE, 2));
            registerStrain(blueDream);
            
            Strain greenCrack = new Strain("green_crack", "Green Crack", Strain.Rarity.COMMON, 50, 3, 50);
            greenCrack.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BUNNY_HOP, 2));
            registerStrain(greenCrack);
            
            // === UNCOMMON STRAINS ===
            Strain purpleHaze = new Strain("purple_haze", "Purple Haze", Strain.Rarity.UNCOMMON, 60, 4, 65);
            purpleHaze.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.RAINBOW_AURA, 3));
            purpleHaze.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DRUNK_VISION, 2));
            registerStrain(purpleHaze);
            
            Strain girlScoutCookies = new Strain("girl_scout_cookies", "Girl Scout Cookies", Strain.Rarity.UNCOMMON, 55, 4, 60);
            girlScoutCookies.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MUNCHIES, 3));
            girlScoutCookies.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.LUCKY_CHARM, 2));
            registerStrain(girlScoutCookies);
            
            Strain gorillGlue = new Strain("gorilla_glue", "Gorilla Glue", Strain.Rarity.UNCOMMON, 65, 3, 70);
            gorillGlue.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TANK_MODE, 2));
            gorillGlue.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SLOW_MO, 2));
            registerStrain(gorillGlue);
            
            Strain jackHerer = new Strain("jack_herer", "Jack Herer", Strain.Rarity.UNCOMMON, 60, 4, 65);
            jackHerer.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.EAGLE_SIGHT, 3));
            jackHerer.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GREEN_THUMB, 2));
            registerStrain(jackHerer);
            
            Strain granddaddyPurple = new Strain("granddaddy_purple", "Granddaddy Purple", Strain.Rarity.UNCOMMON, 58, 4, 68);
            granddaddyPurple.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MEDITATION, 3));
            granddaddyPurple.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BUBBLE_AURA, 2));
            registerStrain(granddaddyPurple);
            
            // === RARE STRAINS ===
            Strain whiteWidow = new Strain("white_widow", "White Widow", Strain.Rarity.RARE, 75, 5, 80);
            whiteWidow.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SPEED_DEMON, 3));
            whiteWidow.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.THIRD_EYE, 3));
            registerStrain(whiteWidow);
            
            Strain wedding_cake = new Strain("wedding_cake", "Wedding Cake", Strain.Rarity.RARE, 72, 5, 78);
            wedding_cake.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CONFETTI, 3));
            wedding_cake.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.HEART_TRAIL, 3));
            wedding_cake.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DISCO_FEVER, 2));
            registerStrain(wedding_cake);
            
            Strain amnesia = new Strain("amnesia", "Amnesia Haze", Strain.Rarity.RARE, 78, 4, 82);
            amnesia.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DRUNK_VISION, 4));
            amnesia.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MATRIX_VISION, 3));
            registerStrain(amnesia);
            
            Strain skywalkerOG = new Strain("skywalker_og", "Skywalker OG", Strain.Rarity.RARE, 76, 5, 80);
            skywalkerOG.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MOON_GRAVITY, 4));
            skywalkerOG.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ASTRAL_PROJECTION, 3));
            registerStrain(skywalkerOG);
            
            Strain trainwreck = new Strain("trainwreck", "Trainwreck", Strain.Rarity.RARE, 80, 4, 75);
            trainwreck.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ROCKET_BOOST, 4));
            trainwreck.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BERSERKER, 3));
            registerStrain(trainwreck);
            
            // === LEGENDARY STRAINS ===
            Strain northernLights = new Strain("northern_lights", "Northern Lights", Strain.Rarity.LEGENDARY, 95, 7, 95);
            northernLights.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.AURORA_BOREALIS, 5));
            northernLights.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CELESTIAL_BEING, 4));
            northernLights.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ENLIGHTENMENT, 4));
            registerStrain(northernLights);
            
            Strain pineappleExpress = new Strain("pineapple_express", "Pineapple Express", Strain.Rarity.LEGENDARY, 90, 6, 92);
            pineappleExpress.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.FIRE_TRAIL, 5));
            pineappleExpress.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SPEED_DEMON, 4));
            pineappleExpress.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MIDAS_TOUCH, 4));
            registerStrain(pineappleExpress);
            
            Strain godsFavorite = new Strain("gods_favorite", "God's Favorite", Strain.Rarity.LEGENDARY, 98, 8, 98);
            godsFavorite.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PHOENIX_REBIRTH, 5));
            godsFavorite.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ANGEL_WINGS, 5));
            godsFavorite.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ENLIGHTENMENT, 5));
            registerStrain(godsFavorite);
            
            Strain voidWalker = new Strain("void_walker_strain", "Void Walker", Strain.Rarity.LEGENDARY, 92, 6, 94);
            voidWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.VOID_WALKER, 5));
            voidWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SHADOW_WALKER, 4));
            voidWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.REALITY_BENDER, 4));
            registerStrain(voidWalker);
            
            Strain dragonBreath = new Strain("dragon_breath_strain", "Dragon's Breath", Strain.Rarity.LEGENDARY, 96, 7, 96);
            dragonBreath.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DRAGON_BREATH, 5));
            dragonBreath.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.FIRE_TRAIL, 5));
            dragonBreath.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GHOST_RIDER, 4));
            registerStrain(dragonBreath);
            
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
        config.set(path + ".effects", strain.serializeEffects());
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
            
            // Add effects to lore
            if (!strain.getEffects().isEmpty()) {
                lore.add("");
                lore.add("§d§lSpecial Effects:");
                for (com.budlords.effects.StrainEffect effect : strain.getEffects()) {
                    lore.add("  " + effect.getLoreDisplay());
                }
            }
            
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
            
            // Add effects to lore
            if (!strain.getEffects().isEmpty()) {
                lore.add("");
                lore.add("§d§lSpecial Effects:");
                for (com.budlords.effects.StrainEffect effect : strain.getEffects()) {
                    lore.add("  " + effect.getLoreDisplay());
                }
            }
            
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
