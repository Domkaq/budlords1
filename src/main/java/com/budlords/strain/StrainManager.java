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
            purpleHaze.setIconMaterial(Material.PURPLE_DYE);
            // Apply purple visual theme - true to the real Purple Haze strain
            StrainVisualConfig purpleHazeVisual = new StrainVisualConfig();
            purpleHazeVisual.applyTheme(StrainVisualConfig.VisualTheme.PURPLE_HAZE);
            purpleHaze.setVisualConfig(purpleHazeVisual);
            registerStrain(purpleHaze);
            
            Strain girlScoutCookies = new Strain("girl_scout_cookies", "Girl Scout Cookies", Strain.Rarity.UNCOMMON, 55, 4, 60);
            girlScoutCookies.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MUNCHIES, 3));
            girlScoutCookies.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.LUCKY_CHARM, 2));
            registerStrain(girlScoutCookies);
            
            Strain gorillaGlue = new Strain("gorilla_glue", "Gorilla Glue", Strain.Rarity.UNCOMMON, 65, 3, 70);
            gorillaGlue.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TANK_MODE, 2));
            gorillaGlue.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SLOW_MO, 2));
            registerStrain(gorillaGlue);
            
            Strain jackHerer = new Strain("jack_herer", "Jack Herer", Strain.Rarity.UNCOMMON, 60, 4, 65);
            jackHerer.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.EAGLE_SIGHT, 3));
            jackHerer.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GREEN_THUMB, 2));
            registerStrain(jackHerer);
            
            Strain granddaddyPurple = new Strain("granddaddy_purple", "Granddaddy Purple", Strain.Rarity.UNCOMMON, 58, 4, 68);
            granddaddyPurple.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MEDITATION, 3));
            granddaddyPurple.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BUBBLE_AURA, 2));
            granddaddyPurple.setIconMaterial(Material.PURPLE_DYE);
            // Apply purple visual theme - Granddaddy Purple is known for its deep purple buds
            StrainVisualConfig gdpVisual = new StrainVisualConfig();
            gdpVisual.applyTheme(StrainVisualConfig.VisualTheme.PURPLE_HAZE);
            // Customize to be darker purple than Purple Haze
            gdpVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(75, 0, 130)); // Indigo/deep purple
            gdpVisual.setBudType(StrainVisualConfig.BudType.NORMAL);
            gdpVisual.setBudMaterial(Material.PURPLE_CONCRETE);
            granddaddyPurple.setVisualConfig(gdpVisual);
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
            
            // ===== V3.0.0 NEW STRAINS - 15+ ADDITIONAL STRAINS =====
            
            // NEW COMMON STRAINS
            Strain lemonHaze = new Strain("lemon_haze", "Lemon Haze", Strain.Rarity.COMMON, 42, 4, 48);
            lemonHaze.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GOLDEN_GLOW, 2));
            lemonHaze.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SPARKLE_STEP, 1));
            registerStrain(lemonHaze);
            
            Strain akBuddy = new Strain("ak_buddy", "AK Buddy", Strain.Rarity.COMMON, 48, 3, 52);
            akBuddy.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BUNNY_HOP, 2));
            akBuddy.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PET_MAGNET, 1));
            registerStrain(akBuddy);
            
            // NEW UNCOMMON STRAINS
            Strain tropicalThunder = new Strain("tropical_thunder", "Tropical Thunder", Strain.Rarity.UNCOMMON, 58, 4, 62);
            tropicalThunder.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.STORM_RIDER, 3));
            tropicalThunder.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.RAIN_DANCER, 2));
            tropicalThunder.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SONIC_BOOM, 2));
            registerStrain(tropicalThunder);
            
            Strain crystalKush = new Strain("crystal_kush", "Crystal Kush", Strain.Rarity.UNCOMMON, 62, 4, 68);
            crystalKush.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CRYSTALLINE_BODY, 3));
            crystalKush.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.FROST_AURA, 2));
            registerStrain(crystalKush);
            
            Strain midnightShadow = new Strain("midnight_shadow", "Midnight Shadow", Strain.Rarity.UNCOMMON, 55, 5, 60);
            midnightShadow.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SHADOW_STRIKE, 3));
            midnightShadow.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.NINJA_MODE, 2));
            registerStrain(midnightShadow);
            
            Strain goldenHarvest = new Strain("golden_harvest", "Golden Harvest", Strain.Rarity.UNCOMMON, 52, 6, 65);
            goldenHarvest.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.HARVEST_MOON, 3));
            goldenHarvest.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SEED_MULTIPLIER, 3));
            goldenHarvest.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.LUCKY_DROPS, 2));
            registerStrain(goldenHarvest);
            
            // NEW RARE STRAINS
            Strain plasmaFire = new Strain("plasma_fire", "Plasma Fire", Strain.Rarity.RARE, 78, 5, 82);
            plasmaFire.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PLASMA_AURA, 4));
            plasmaFire.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.LIGHTNING_FIST, 3));
            plasmaFire.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.FIRE_TRAIL, 3));
            registerStrain(plasmaFire);
            
            Strain quantumLeap = new Strain("quantum_leap", "Quantum Leap", Strain.Rarity.RARE, 75, 4, 78);
            quantumLeap.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.QUANTUM_STATE, 4));
            quantumLeap.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PHASE_SHIFT, 3));
            quantumLeap.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TIME_WARP, 3));
            registerStrain(quantumLeap);
            
            Strain bloodMoon = new Strain("blood_moon", "Blood Moon", Strain.Rarity.RARE, 82, 5, 80);
            bloodMoon.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.WEREWOLF_FORM, 4));
            bloodMoon.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BLOOD_TRAIL, 3));
            bloodMoon.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.LIFE_DRAIN, 3));
            registerStrain(bloodMoon);
            
            Strain spiritWalker = new Strain("spirit_walker", "Spirit Walker", Strain.Rarity.RARE, 74, 5, 76);
            spiritWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ANCESTOR_SPIRITS, 4));
            spiritWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GHOST_FRIEND, 3));
            spiritWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ETHER_SIGHT, 3));
            registerStrain(spiritWalker);
            
            Strain merchantsDream = new Strain("merchants_dream", "Merchant's Dream", Strain.Rarity.RARE, 70, 6, 85);
            merchantsDream.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MERCHANT_BLESSING, 4));
            merchantsDream.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.QUALITY_BOOST, 3));
            merchantsDream.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TREASURE_HUNTER, 3));
            registerStrain(merchantsDream);
            
            // NEW LEGENDARY STRAINS
            Strain titanicFury = new Strain("titanic_fury", "Titanic Fury", Strain.Rarity.LEGENDARY, 94, 8, 94);
            titanicFury.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TITAN_FORM, 5));
            titanicFury.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CRITICAL_FURY, 4));
            titanicFury.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BERSERKER, 4));
            titanicFury.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.VOLCANIC_HEAT, 4));
            registerStrain(titanicFury);
            
            Strain singularityPrime = new Strain("singularity_prime", "Singularity Prime", Strain.Rarity.LEGENDARY, 97, 6, 97);
            singularityPrime.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SINGULARITY, 5));
            singularityPrime.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GRAVITY_WELL, 5));
            singularityPrime.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.VOID_EYES, 4));
            singularityPrime.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DIMENSIONAL_RIFT, 4));
            registerStrain(singularityPrime);
            
            Strain cosmicRebirth = new Strain("cosmic_rebirth_strain", "Cosmic Rebirth", Strain.Rarity.LEGENDARY, 99, 9, 99);
            cosmicRebirth.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.COSMIC_REBIRTH, 5));
            cosmicRebirth.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PHOENIX_REBIRTH, 5));
            cosmicRebirth.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CELESTIAL_BEING, 5));
            cosmicRebirth.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GODMODE_AURA, 4));
            registerStrain(cosmicRebirth);
            
            Strain entropyMaster = new Strain("entropy_master_strain", "Entropy Master", Strain.Rarity.LEGENDARY, 95, 7, 95);
            entropyMaster.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ENTROPY_MASTER, 5));
            entropyMaster.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.KARMA_BALANCE, 4));
            entropyMaster.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ORACLE_VISION, 4));
            entropyMaster.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PARTY_MODE, 3));
            registerStrain(entropyMaster);
            
            Strain multiverseEcho = new Strain("multiverse_echo_strain", "Multiverse Echo", Strain.Rarity.LEGENDARY, 96, 8, 96);
            multiverseEcho.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MULTIVERSE_ECHO, 5));
            multiverseEcho.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MIRROR_IMAGE, 4));
            multiverseEcho.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.QUANTUM_STATE, 4));
            multiverseEcho.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TELEKINETIC_LIFT, 4));
            registerStrain(multiverseEcho);
            
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
        // Use different seed materials based on strain rarity for visual variety
        Material seedMaterial = getSeedMaterialForRarity(strain.getRarity());
        ItemStack seed = new ItemStack(seedMaterial, amount);
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
    
    /**
     * Gets a seed material based on strain rarity for visual variety in shops.
     * Public so it can be used by shop GUIs for consistent seed appearance.
     */
    public static Material getSeedMaterialForRarity(Strain.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Material.WHEAT_SEEDS;      // Regular wheat seeds for common
            case UNCOMMON -> Material.BEETROOT_SEEDS; // Beetroot seeds for uncommon
            case RARE -> Material.MELON_SEEDS;        // Melon seeds for rare
            case LEGENDARY -> Material.PUMPKIN_SEEDS; // Pumpkin seeds for legendary
        };
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
        if (item == null) return false;
        // Check for all seed types used by different rarity levels
        Material type = item.getType();
        if (type != Material.WHEAT_SEEDS && 
            type != Material.BEETROOT_SEEDS && 
            type != Material.MELON_SEEDS && 
            type != Material.PUMPKIN_SEEDS) {
            return false;
        }
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
