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
            // Create default strains with effects and UNIQUE VISUAL APPEARANCES
            
            // === COMMON STRAINS ===
            Strain ogKush = new Strain("og_kush", "OG Kush", Strain.Rarity.COMMON, 40, 3, 50);
            ogKush.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MUNCHIES, 2));
            // OG Kush - Classic green with earthy tones
            StrainVisualConfig ogKushVisual = new StrainVisualConfig();
            ogKushVisual.applyTheme(StrainVisualConfig.VisualTheme.CLASSIC);
            ogKushVisual.setLeafMaterial(Material.OAK_LEAVES);
            ogKushVisual.setBudMaterial(Material.LIME_CONCRETE);
            ogKushVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(34, 139, 34));
            ogKush.setVisualConfig(ogKushVisual);
            registerStrain(ogKush);
            
            Strain sourDiesel = new Strain("sour_diesel", "Sour Diesel", Strain.Rarity.COMMON, 35, 4, 45);
            sourDiesel.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SPEED_DEMON, 2));
            // Sour Diesel - Yellow-green fuel-like appearance
            StrainVisualConfig sourDieselVisual = new StrainVisualConfig();
            sourDieselVisual.setLeafMaterial(Material.BIRCH_LEAVES);
            sourDieselVisual.setBudMaterial(Material.YELLOW_CONCRETE);
            sourDieselVisual.setBudType(StrainVisualConfig.BudType.GOLD);
            sourDieselVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(173, 255, 47));
            sourDieselVisual.setAmbientParticle(org.bukkit.Particle.SMOKE_NORMAL);
            sourDiesel.setIconMaterial(Material.YELLOW_DYE);
            sourDiesel.setVisualConfig(sourDieselVisual);
            registerStrain(sourDiesel);
            
            Strain blueDream = new Strain("blue_dream", "Blue Dream", Strain.Rarity.COMMON, 45, 3, 55);
            blueDream.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DREAM_STATE, 2));
            // Blue Dream - Blue-tinted dreamy appearance
            StrainVisualConfig blueDreamVisual = new StrainVisualConfig();
            blueDreamVisual.setLeafMaterial(Material.PRISMARINE);
            blueDreamVisual.setBudMaterial(Material.LIGHT_BLUE_CONCRETE);
            blueDreamVisual.setBudType(StrainVisualConfig.BudType.ICE);
            blueDreamVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(100, 149, 237));
            blueDreamVisual.setAmbientParticle(org.bukkit.Particle.WATER_WAKE);
            blueDreamVisual.setGlowing(true);
            blueDreamVisual.setGlowColor(org.bukkit.Color.AQUA);
            blueDream.setIconMaterial(Material.LIGHT_BLUE_DYE);
            blueDream.setVisualConfig(blueDreamVisual);
            registerStrain(blueDream);
            
            Strain greenCrack = new Strain("green_crack", "Green Crack", Strain.Rarity.COMMON, 50, 3, 50);
            greenCrack.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BUNNY_HOP, 2));
            // Green Crack - Bright vibrant green with energy vibes
            StrainVisualConfig greenCrackVisual = new StrainVisualConfig();
            greenCrackVisual.setLeafMaterial(Material.JUNGLE_LEAVES);
            greenCrackVisual.setBudMaterial(Material.LIME_WOOL);
            greenCrackVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(50, 205, 50));
            greenCrackVisual.setAmbientParticle(org.bukkit.Particle.FIREWORKS_SPARK);
            greenCrackVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.BOUNCE);
            greenCrack.setVisualConfig(greenCrackVisual);
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
            // Girl Scout Cookies - Brown/cookie-like with sweet appearance
            StrainVisualConfig gscVisual = new StrainVisualConfig();
            gscVisual.setLeafMaterial(Material.DARK_OAK_LEAVES);
            gscVisual.setBudMaterial(Material.BROWN_MUSHROOM_BLOCK);
            gscVisual.setBudType(StrainVisualConfig.BudType.MUSHROOM);
            gscVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(139, 90, 43));
            gscVisual.setAmbientParticle(org.bukkit.Particle.WAX_ON);
            girlScoutCookies.setIconMaterial(Material.BROWN_DYE);
            girlScoutCookies.setVisualConfig(gscVisual);
            registerStrain(girlScoutCookies);
            
            Strain gorillaGlue = new Strain("gorilla_glue", "Gorilla Glue", Strain.Rarity.UNCOMMON, 65, 3, 70);
            gorillaGlue.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TANK_MODE, 2));
            gorillaGlue.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SLOW_MO, 2));
            // Gorilla Glue - Sticky, resinous, dark green with amber
            StrainVisualConfig ggVisual = new StrainVisualConfig();
            ggVisual.setLeafMaterial(Material.MOSS_BLOCK);
            ggVisual.setBudMaterial(Material.HONEY_BLOCK);
            ggVisual.setBudType(StrainVisualConfig.BudType.HONEY);
            ggVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(0, 100, 0));
            ggVisual.setAmbientParticle(org.bukkit.Particle.DRIPPING_HONEY);
            ggVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.AGGRESSIVE);
            gorillaGlue.setIconMaterial(Material.ORANGE_DYE);
            gorillaGlue.setVisualConfig(ggVisual);
            registerStrain(gorillaGlue);
            
            Strain jackHerer = new Strain("jack_herer", "Jack Herer", Strain.Rarity.UNCOMMON, 60, 4, 65);
            jackHerer.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.EAGLE_SIGHT, 3));
            jackHerer.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GREEN_THUMB, 2));
            // Jack Herer - Classic sativa, tall bright green
            StrainVisualConfig jhVisual = new StrainVisualConfig();
            jhVisual.setLeafMaterial(Material.FERN);
            jhVisual.setBudMaterial(Material.GREEN_WOOL);
            jhVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(124, 252, 0));
            jhVisual.setHeightScale(1.5);
            jhVisual.setAmbientParticle(org.bukkit.Particle.COMPOSTER);
            jackHerer.setVisualConfig(jhVisual);
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
            gdpVisual.setLeafMaterial(Material.PURPLE_WOOL);
            granddaddyPurple.setVisualConfig(gdpVisual);
            registerStrain(granddaddyPurple);
            
            // === RARE STRAINS ===
            Strain whiteWidow = new Strain("white_widow", "White Widow", Strain.Rarity.RARE, 75, 5, 80);
            whiteWidow.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SPEED_DEMON, 3));
            whiteWidow.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.THIRD_EYE, 3));
            // White Widow - Frosty white crystals, spider web patterns
            StrainVisualConfig wwVisual = new StrainVisualConfig();
            wwVisual.applyTheme(StrainVisualConfig.VisualTheme.CRYSTAL_CLEAR);
            wwVisual.setLeafMaterial(Material.WHITE_WOOL);
            wwVisual.setBudMaterial(Material.SNOW_BLOCK);
            wwVisual.setBudType(StrainVisualConfig.BudType.CRYSTAL);
            wwVisual.setAmbientParticle(org.bukkit.Particle.SNOWFLAKE);
            wwVisual.setGlowing(true);
            wwVisual.setGlowColor(org.bukkit.Color.WHITE);
            whiteWidow.setIconMaterial(Material.WHITE_DYE);
            whiteWidow.setVisualConfig(wwVisual);
            registerStrain(whiteWidow);
            
            Strain wedding_cake = new Strain("wedding_cake", "Wedding Cake", Strain.Rarity.RARE, 72, 5, 78);
            wedding_cake.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CONFETTI, 3));
            wedding_cake.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.HEART_TRAIL, 3));
            wedding_cake.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DISCO_FEVER, 2));
            // Wedding Cake - Pink/white with heart decorations
            StrainVisualConfig wcVisual = new StrainVisualConfig();
            wcVisual.applyTheme(StrainVisualConfig.VisualTheme.CHERRY_BLOSSOM);
            wcVisual.setLeafMaterial(Material.PINK_WOOL);
            wcVisual.setBudMaterial(Material.PINK_GLAZED_TERRACOTTA);
            wcVisual.setBudType(StrainVisualConfig.BudType.HEART);
            wcVisual.setAmbientParticle(org.bukkit.Particle.HEART);
            wedding_cake.setIconMaterial(Material.PINK_DYE);
            wedding_cake.setVisualConfig(wcVisual);
            registerStrain(wedding_cake);
            
            Strain amnesia = new Strain("amnesia", "Amnesia Haze", Strain.Rarity.RARE, 78, 4, 82);
            amnesia.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DRUNK_VISION, 4));
            amnesia.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MATRIX_VISION, 3));
            // Amnesia Haze - Hazy, foggy, disorienting appearance
            StrainVisualConfig amnesiaVisual = new StrainVisualConfig();
            amnesiaVisual.setLeafMaterial(Material.GRAY_WOOL);
            amnesiaVisual.setBudMaterial(Material.LIGHT_GRAY_CONCRETE);
            amnesiaVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(169, 169, 169));
            amnesiaVisual.setAmbientParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE);
            amnesiaVisual.setGlowing(true);
            amnesiaVisual.setGlowColor(org.bukkit.Color.GRAY);
            amnesiaVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.WAVE);
            amnesia.setIconMaterial(Material.GRAY_DYE);
            amnesia.setVisualConfig(amnesiaVisual);
            registerStrain(amnesia);
            
            Strain skywalkerOG = new Strain("skywalker_og", "Skywalker OG", Strain.Rarity.RARE, 76, 5, 80);
            skywalkerOG.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MOON_GRAVITY, 4));
            skywalkerOG.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ASTRAL_PROJECTION, 3));
            // Skywalker OG - Space/star theme, floating appearance
            StrainVisualConfig swVisual = new StrainVisualConfig();
            swVisual.applyTheme(StrainVisualConfig.VisualTheme.VOID_WALKER);
            swVisual.setLeafMaterial(Material.END_STONE_BRICKS);
            swVisual.setBudMaterial(Material.END_ROD);
            swVisual.setBudType(StrainVisualConfig.BudType.END);
            swVisual.setAmbientParticle(org.bukkit.Particle.END_ROD);
            swVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.FLOAT);
            swVisual.setGlowing(true);
            swVisual.setGlowColor(org.bukkit.Color.fromRGB(200, 200, 255));
            skywalkerOG.setIconMaterial(Material.CYAN_DYE);
            skywalkerOG.setVisualConfig(swVisual);
            registerStrain(skywalkerOG);
            
            Strain trainwreck = new Strain("trainwreck", "Trainwreck", Strain.Rarity.RARE, 80, 4, 75);
            trainwreck.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ROCKET_BOOST, 4));
            trainwreck.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BERSERKER, 3));
            // Trainwreck - Explosive, chaotic, fiery
            StrainVisualConfig twVisual = new StrainVisualConfig();
            twVisual.applyTheme(StrainVisualConfig.VisualTheme.FIRE_OG);
            twVisual.setLeafMaterial(Material.RED_WOOL);
            twVisual.setBudMaterial(Material.TNT);
            twVisual.setBudType(StrainVisualConfig.BudType.CREEPER);
            twVisual.setAmbientParticle(org.bukkit.Particle.EXPLOSION_LARGE);
            twVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.SHAKE);
            trainwreck.setIconMaterial(Material.RED_DYE);
            trainwreck.setVisualConfig(twVisual);
            registerStrain(trainwreck);
            
            // === LEGENDARY STRAINS ===
            Strain northernLights = new Strain("northern_lights", "Northern Lights", Strain.Rarity.LEGENDARY, 95, 7, 95);
            northernLights.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.AURORA_BOREALIS, 5));
            northernLights.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CELESTIAL_BEING, 4));
            northernLights.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ENLIGHTENMENT, 4));
            // Northern Lights - Aurora/rainbow glow, ice crystals
            StrainVisualConfig nlVisual = new StrainVisualConfig();
            nlVisual.applyTheme(StrainVisualConfig.VisualTheme.RAINBOW);
            nlVisual.setLeafMaterial(Material.BLUE_ICE);
            nlVisual.setBudMaterial(Material.SEA_LANTERN);
            nlVisual.setBudType(StrainVisualConfig.BudType.BEACON);
            nlVisual.setAmbientParticle(org.bukkit.Particle.END_ROD);
            nlVisual.setGlowing(true);
            nlVisual.setGlowColor(org.bukkit.Color.fromRGB(0, 255, 127));
            nlVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.PULSE);
            northernLights.setIconMaterial(Material.LIGHT_BLUE_DYE);
            northernLights.setVisualConfig(nlVisual);
            registerStrain(northernLights);
            
            Strain pineappleExpress = new Strain("pineapple_express", "Pineapple Express", Strain.Rarity.LEGENDARY, 90, 6, 92);
            pineappleExpress.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.FIRE_TRAIL, 5));
            pineappleExpress.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SPEED_DEMON, 4));
            pineappleExpress.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MIDAS_TOUCH, 4));
            // Pineapple Express - Tropical yellow/gold, pineapple-like
            StrainVisualConfig peVisual = new StrainVisualConfig();
            peVisual.applyTheme(StrainVisualConfig.VisualTheme.GOLDEN_LEAF);
            peVisual.setLeafMaterial(Material.YELLOW_WOOL);
            peVisual.setBudMaterial(Material.GOLD_BLOCK);
            peVisual.setBudType(StrainVisualConfig.BudType.GOLD);
            peVisual.setAmbientParticle(org.bukkit.Particle.TOTEM);
            peVisual.setGlowing(true);
            peVisual.setGlowColor(org.bukkit.Color.YELLOW);
            pineappleExpress.setIconMaterial(Material.YELLOW_DYE);
            pineappleExpress.setVisualConfig(peVisual);
            registerStrain(pineappleExpress);
            
            Strain godsFavorite = new Strain("gods_favorite", "God's Favorite", Strain.Rarity.LEGENDARY, 98, 8, 98);
            godsFavorite.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PHOENIX_REBIRTH, 5));
            godsFavorite.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ANGEL_WINGS, 5));
            godsFavorite.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ENLIGHTENMENT, 5));
            // God's Favorite - Divine, heavenly, beacon of light
            StrainVisualConfig gfVisual = new StrainVisualConfig();
            gfVisual.setLeafMaterial(Material.WHITE_WOOL);
            gfVisual.setBudMaterial(Material.BEACON);
            gfVisual.setBudType(StrainVisualConfig.BudType.BEACON);
            gfVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(255, 255, 240));
            gfVisual.setAmbientParticle(org.bukkit.Particle.END_ROD);
            gfVisual.setGlowing(true);
            gfVisual.setGlowColor(org.bukkit.Color.fromRGB(255, 215, 0));
            gfVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.FLOAT);
            gfVisual.setBudScale(1.5);
            godsFavorite.setIconMaterial(Material.GOLD_INGOT);
            godsFavorite.setVisualConfig(gfVisual);
            registerStrain(godsFavorite);
            
            Strain voidWalker = new Strain("void_walker_strain", "Void Walker", Strain.Rarity.LEGENDARY, 92, 6, 94);
            voidWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.VOID_WALKER, 5));
            voidWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SHADOW_WALKER, 4));
            voidWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.REALITY_BENDER, 4));
            // Void Walker - Dark void, end dimension
            StrainVisualConfig vwVisual = new StrainVisualConfig();
            vwVisual.applyTheme(StrainVisualConfig.VisualTheme.VOID_WALKER);
            vwVisual.setLeafMaterial(Material.BLACK_CONCRETE);
            vwVisual.setBudMaterial(Material.CRYING_OBSIDIAN);
            vwVisual.setBudType(StrainVisualConfig.BudType.END);
            vwVisual.setAmbientParticle(org.bukkit.Particle.REVERSE_PORTAL);
            vwVisual.setGlowing(true);
            vwVisual.setGlowColor(org.bukkit.Color.fromRGB(75, 0, 130));
            voidWalker.setIconMaterial(Material.BLACK_DYE);
            voidWalker.setVisualConfig(vwVisual);
            registerStrain(voidWalker);
            
            Strain dragonBreath = new Strain("dragon_breath_strain", "Dragon's Breath", Strain.Rarity.LEGENDARY, 96, 7, 96);
            dragonBreath.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DRAGON_BREATH, 5));
            dragonBreath.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.FIRE_TRAIL, 5));
            dragonBreath.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GHOST_RIDER, 4));
            // Dragon's Breath - Dragon head buds, fiery purple
            StrainVisualConfig dbVisual = new StrainVisualConfig();
            dbVisual.applyTheme(StrainVisualConfig.VisualTheme.DRAGON_BREATH);
            dbVisual.setLeafMaterial(Material.PURPLE_CONCRETE);
            dbVisual.setBudMaterial(Material.DRAGON_HEAD);
            dbVisual.setBudType(StrainVisualConfig.BudType.DRAGON);
            dbVisual.setAmbientParticle(org.bukkit.Particle.DRAGON_BREATH);
            dbVisual.setGlowing(true);
            dbVisual.setGlowColor(org.bukkit.Color.PURPLE);
            dragonBreath.setIconMaterial(Material.MAGENTA_DYE);
            dragonBreath.setVisualConfig(dbVisual);
            registerStrain(dragonBreath);
            
            // ===== V3.0.0 NEW STRAINS - 15+ ADDITIONAL STRAINS WITH UNIQUE VISUALS =====
            
            // NEW COMMON STRAINS
            Strain lemonHaze = new Strain("lemon_haze", "Lemon Haze", Strain.Rarity.COMMON, 42, 4, 48);
            lemonHaze.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GOLDEN_GLOW, 2));
            lemonHaze.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SPARKLE_STEP, 1));
            // Lemon Haze - Bright yellow citrus appearance
            StrainVisualConfig lhVisual = new StrainVisualConfig();
            lhVisual.setLeafMaterial(Material.YELLOW_WOOL);
            lhVisual.setBudMaterial(Material.YELLOW_CONCRETE);
            lhVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(255, 255, 0));
            lhVisual.setAmbientParticle(org.bukkit.Particle.WAX_ON);
            lemonHaze.setIconMaterial(Material.YELLOW_DYE);
            lemonHaze.setVisualConfig(lhVisual);
            registerStrain(lemonHaze);
            
            Strain akBuddy = new Strain("ak_buddy", "AK Buddy", Strain.Rarity.COMMON, 48, 3, 52);
            akBuddy.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BUNNY_HOP, 2));
            akBuddy.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PET_MAGNET, 1));
            // AK Buddy - Military green with strong appearance
            StrainVisualConfig akVisual = new StrainVisualConfig();
            akVisual.setLeafMaterial(Material.DARK_OAK_LEAVES);
            akVisual.setBudMaterial(Material.GREEN_TERRACOTTA);
            akVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(85, 107, 47));
            akVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.AGGRESSIVE);
            akBuddy.setVisualConfig(akVisual);
            registerStrain(akBuddy);
            
            // NEW UNCOMMON STRAINS
            Strain tropicalThunder = new Strain("tropical_thunder", "Tropical Thunder", Strain.Rarity.UNCOMMON, 58, 4, 62);
            tropicalThunder.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.STORM_RIDER, 3));
            tropicalThunder.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.RAIN_DANCER, 2));
            tropicalThunder.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SONIC_BOOM, 2));
            // Tropical Thunder - Storm and lightning theme
            StrainVisualConfig ttVisual = new StrainVisualConfig();
            ttVisual.setLeafMaterial(Material.CYAN_WOOL);
            ttVisual.setBudMaterial(Material.LIGHTNING_ROD);
            ttVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(0, 191, 255));
            ttVisual.setAmbientParticle(org.bukkit.Particle.ELECTRIC_SPARK);
            ttVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.SHAKE);
            tropicalThunder.setIconMaterial(Material.CYAN_DYE);
            tropicalThunder.setVisualConfig(ttVisual);
            registerStrain(tropicalThunder);
            
            Strain crystalKush = new Strain("crystal_kush", "Crystal Kush", Strain.Rarity.UNCOMMON, 62, 4, 68);
            crystalKush.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CRYSTALLINE_BODY, 3));
            crystalKush.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.FROST_AURA, 2));
            // Crystal Kush - Crystalline, frosty appearance
            StrainVisualConfig ckVisual = new StrainVisualConfig();
            ckVisual.applyTheme(StrainVisualConfig.VisualTheme.CRYSTAL_CLEAR);
            ckVisual.setLeafMaterial(Material.AMETHYST_BLOCK);
            ckVisual.setBudMaterial(Material.AMETHYST_CLUSTER);
            ckVisual.setBudType(StrainVisualConfig.BudType.AMETHYST);
            ckVisual.setAmbientParticle(org.bukkit.Particle.END_ROD);
            ckVisual.setGlowing(true);
            ckVisual.setGlowColor(org.bukkit.Color.fromRGB(155, 89, 182));
            crystalKush.setIconMaterial(Material.AMETHYST_SHARD);
            crystalKush.setVisualConfig(ckVisual);
            registerStrain(crystalKush);
            
            Strain midnightShadow = new Strain("midnight_shadow", "Midnight Shadow", Strain.Rarity.UNCOMMON, 55, 5, 60);
            midnightShadow.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SHADOW_STRIKE, 3));
            midnightShadow.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.NINJA_MODE, 2));
            // Midnight Shadow - Dark, stealthy appearance
            StrainVisualConfig msVisual = new StrainVisualConfig();
            msVisual.setLeafMaterial(Material.BLACK_WOOL);
            msVisual.setBudMaterial(Material.COAL_BLOCK);
            msVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(25, 25, 25));
            msVisual.setAmbientParticle(org.bukkit.Particle.SQUID_INK);
            msVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.FROZEN);
            midnightShadow.setIconMaterial(Material.BLACK_DYE);
            midnightShadow.setVisualConfig(msVisual);
            registerStrain(midnightShadow);
            
            Strain goldenHarvest = new Strain("golden_harvest", "Golden Harvest", Strain.Rarity.UNCOMMON, 52, 6, 65);
            goldenHarvest.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.HARVEST_MOON, 3));
            goldenHarvest.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SEED_MULTIPLIER, 3));
            goldenHarvest.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.LUCKY_DROPS, 2));
            // Golden Harvest - Rich golden wheat appearance
            StrainVisualConfig ghVisual = new StrainVisualConfig();
            ghVisual.applyTheme(StrainVisualConfig.VisualTheme.GOLDEN_LEAF);
            ghVisual.setLeafMaterial(Material.HAY_BLOCK);
            ghVisual.setBudMaterial(Material.GOLD_NUGGET);
            ghVisual.setBudType(StrainVisualConfig.BudType.GOLD);
            ghVisual.setAmbientParticle(org.bukkit.Particle.WAX_ON);
            goldenHarvest.setIconMaterial(Material.WHEAT);
            goldenHarvest.setVisualConfig(ghVisual);
            registerStrain(goldenHarvest);
            
            // NEW RARE STRAINS
            Strain plasmaFire = new Strain("plasma_fire", "Plasma Fire", Strain.Rarity.RARE, 78, 5, 82);
            plasmaFire.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PLASMA_AURA, 4));
            plasmaFire.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.LIGHTNING_FIST, 3));
            plasmaFire.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.FIRE_TRAIL, 3));
            // Plasma Fire - Electric blue/purple plasma flames
            StrainVisualConfig pfVisual = new StrainVisualConfig();
            pfVisual.setLeafMaterial(Material.BLUE_WOOL);
            pfVisual.setBudMaterial(Material.SOUL_LANTERN);
            pfVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(147, 112, 219));
            pfVisual.setAmbientParticle(org.bukkit.Particle.SOUL_FIRE_FLAME);
            pfVisual.setGlowing(true);
            pfVisual.setGlowColor(org.bukkit.Color.fromRGB(0, 191, 255));
            pfVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.PULSE);
            plasmaFire.setIconMaterial(Material.BLUE_DYE);
            plasmaFire.setVisualConfig(pfVisual);
            registerStrain(plasmaFire);
            
            Strain quantumLeap = new Strain("quantum_leap", "Quantum Leap", Strain.Rarity.RARE, 75, 4, 78);
            quantumLeap.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.QUANTUM_STATE, 4));
            quantumLeap.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PHASE_SHIFT, 3));
            quantumLeap.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TIME_WARP, 3));
            // Quantum Leap - Glitchy, phasing, sci-fi appearance
            StrainVisualConfig qlVisual = new StrainVisualConfig();
            qlVisual.setLeafMaterial(Material.WARPED_WART_BLOCK);
            qlVisual.setBudMaterial(Material.RESPAWN_ANCHOR);
            qlVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(0, 255, 255));
            qlVisual.setAmbientParticle(org.bukkit.Particle.PORTAL);
            qlVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.SPIN);
            qlVisual.setGlowing(true);
            qlVisual.setGlowColor(org.bukkit.Color.AQUA);
            quantumLeap.setIconMaterial(Material.ENDER_PEARL);
            quantumLeap.setVisualConfig(qlVisual);
            registerStrain(quantumLeap);
            
            Strain bloodMoon = new Strain("blood_moon", "Blood Moon", Strain.Rarity.RARE, 82, 5, 80);
            bloodMoon.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.WEREWOLF_FORM, 4));
            bloodMoon.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BLOOD_TRAIL, 3));
            bloodMoon.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.LIFE_DRAIN, 3));
            // Blood Moon - WEREWOLF/WOLF SKULL theme! Dark red blood with wolf skull buds
            StrainVisualConfig bmVisual = new StrainVisualConfig();
            bmVisual.applyTheme(StrainVisualConfig.VisualTheme.WEREWOLF);
            bmVisual.setLeafMaterial(Material.RED_WOOL);
            bmVisual.setBudMaterial(Material.WITHER_SKELETON_SKULL); // Wolf-like skull
            bmVisual.setBudType(StrainVisualConfig.BudType.SKULL);
            bmVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(139, 0, 0)); // Dark red
            bmVisual.setAmbientParticle(org.bukkit.Particle.DAMAGE_INDICATOR); // Blood particles
            bmVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.AGGRESSIVE);
            bmVisual.setGlowing(true);
            bmVisual.setGlowColor(org.bukkit.Color.fromRGB(139, 0, 0));
            bloodMoon.setIconMaterial(Material.RED_DYE);
            bloodMoon.setVisualConfig(bmVisual);
            registerStrain(bloodMoon);
            
            Strain spiritWalker = new Strain("spirit_walker", "Spirit Walker", Strain.Rarity.RARE, 74, 5, 76);
            spiritWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ANCESTOR_SPIRITS, 4));
            spiritWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GHOST_FRIEND, 3));
            spiritWalker.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ETHER_SIGHT, 3));
            // Spirit Walker - Ethereal, ghostly, spiritual appearance
            StrainVisualConfig spwVisual = new StrainVisualConfig();
            spwVisual.setLeafMaterial(Material.LIGHT_GRAY_WOOL);
            spwVisual.setBudMaterial(Material.SKELETON_SKULL);
            spwVisual.setBudType(StrainVisualConfig.BudType.SKULL);
            spwVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(192, 192, 192));
            spwVisual.setAmbientParticle(org.bukkit.Particle.SOUL);
            spwVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.FLOAT);
            spwVisual.setGlowing(true);
            spwVisual.setGlowColor(org.bukkit.Color.fromRGB(200, 200, 255));
            spiritWalker.setIconMaterial(Material.SOUL_LANTERN);
            spiritWalker.setVisualConfig(spwVisual);
            registerStrain(spiritWalker);
            
            Strain merchantsDream = new Strain("merchants_dream", "Merchant's Dream", Strain.Rarity.RARE, 70, 6, 85);
            merchantsDream.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MERCHANT_BLESSING, 4));
            merchantsDream.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.QUALITY_BOOST, 3));
            merchantsDream.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TREASURE_HUNTER, 3));
            // Merchant's Dream - Rich, luxurious emerald appearance
            StrainVisualConfig mdVisual = new StrainVisualConfig();
            mdVisual.setLeafMaterial(Material.EMERALD_BLOCK);
            mdVisual.setBudMaterial(Material.EMERALD_ORE);
            mdVisual.setBudType(StrainVisualConfig.BudType.EMERALD);
            mdVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(0, 168, 107));
            mdVisual.setAmbientParticle(org.bukkit.Particle.VILLAGER_HAPPY);
            mdVisual.setGlowing(true);
            mdVisual.setGlowColor(org.bukkit.Color.GREEN);
            merchantsDream.setIconMaterial(Material.EMERALD);
            merchantsDream.setVisualConfig(mdVisual);
            registerStrain(merchantsDream);
            
            // NEW LEGENDARY STRAINS
            Strain titanicFury = new Strain("titanic_fury", "Titanic Fury", Strain.Rarity.LEGENDARY, 94, 8, 94);
            titanicFury.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TITAN_FORM, 5));
            titanicFury.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CRITICAL_FURY, 4));
            titanicFury.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.BERSERKER, 4));
            titanicFury.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.VOLCANIC_HEAT, 4));
            // Titanic Fury - Massive, volcanic, rage appearance
            StrainVisualConfig tfVisual = new StrainVisualConfig();
            tfVisual.applyTheme(StrainVisualConfig.VisualTheme.FIRE_OG);
            tfVisual.setLeafMaterial(Material.MAGMA_BLOCK);
            tfVisual.setBudMaterial(Material.MAGMA_BLOCK);
            tfVisual.setBudType(StrainVisualConfig.BudType.VOLCANO);
            tfVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(255, 69, 0));
            tfVisual.setAmbientParticle(org.bukkit.Particle.LAVA);
            tfVisual.setGlowing(true);
            tfVisual.setGlowColor(org.bukkit.Color.fromRGB(255, 140, 0));
            tfVisual.setBudScale(2.0); // MASSIVE!
            titanicFury.setIconMaterial(Material.NETHERITE_INGOT);
            titanicFury.setVisualConfig(tfVisual);
            registerStrain(titanicFury);
            
            Strain singularityPrime = new Strain("singularity_prime", "Singularity Prime", Strain.Rarity.LEGENDARY, 97, 6, 97);
            singularityPrime.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.SINGULARITY, 5));
            singularityPrime.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GRAVITY_WELL, 5));
            singularityPrime.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.VOID_EYES, 4));
            singularityPrime.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.DIMENSIONAL_RIFT, 4));
            // Singularity Prime - Black hole, cosmic void appearance
            StrainVisualConfig spVisual = new StrainVisualConfig();
            spVisual.applyTheme(StrainVisualConfig.VisualTheme.VOID_WALKER);
            spVisual.setLeafMaterial(Material.OBSIDIAN);
            spVisual.setBudMaterial(Material.END_PORTAL_FRAME);
            spVisual.setBudType(StrainVisualConfig.BudType.VOID);
            spVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(0, 0, 0));
            spVisual.setAmbientParticle(org.bukkit.Particle.REVERSE_PORTAL);
            spVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.SPIN);
            spVisual.setGlowing(true);
            spVisual.setGlowColor(org.bukkit.Color.fromRGB(48, 25, 52));
            singularityPrime.setIconMaterial(Material.ENDER_EYE);
            singularityPrime.setVisualConfig(spVisual);
            registerStrain(singularityPrime);
            
            Strain cosmicRebirth = new Strain("cosmic_rebirth_strain", "Cosmic Rebirth", Strain.Rarity.LEGENDARY, 99, 9, 99);
            cosmicRebirth.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.COSMIC_REBIRTH, 5));
            cosmicRebirth.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PHOENIX_REBIRTH, 5));
            cosmicRebirth.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.CELESTIAL_BEING, 5));
            cosmicRebirth.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.GODMODE_AURA, 4));
            // Cosmic Rebirth - Phoenix/galaxy rebirth theme
            StrainVisualConfig crVisual = new StrainVisualConfig();
            crVisual.applyTheme(StrainVisualConfig.VisualTheme.RAINBOW);
            crVisual.setLeafMaterial(Material.NETHER_STAR);
            crVisual.setBudMaterial(Material.NETHER_STAR);
            crVisual.setBudType(StrainVisualConfig.BudType.STAR);
            crVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(255, 215, 0));
            crVisual.setAmbientParticle(org.bukkit.Particle.END_ROD);
            crVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.PULSE);
            crVisual.setGlowing(true);
            crVisual.setGlowColor(org.bukkit.Color.fromRGB(255, 223, 0));
            crVisual.setBudScale(1.8);
            cosmicRebirth.setIconMaterial(Material.NETHER_STAR);
            cosmicRebirth.setVisualConfig(crVisual);
            registerStrain(cosmicRebirth);
            
            Strain entropyMaster = new Strain("entropy_master_strain", "Entropy Master", Strain.Rarity.LEGENDARY, 95, 7, 95);
            entropyMaster.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ENTROPY_MASTER, 5));
            entropyMaster.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.KARMA_BALANCE, 4));
            entropyMaster.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.ORACLE_VISION, 4));
            entropyMaster.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.PARTY_MODE, 3));
            // Entropy Master - Chaos/disorder appearance with swirling patterns
            StrainVisualConfig emVisual = new StrainVisualConfig();
            emVisual.setLeafMaterial(Material.MYCELIUM);
            emVisual.setBudMaterial(Material.SCULK_CATALYST);
            emVisual.setBudType(StrainVisualConfig.BudType.SCULK);
            emVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(75, 0, 130));
            emVisual.setAmbientParticle(org.bukkit.Particle.SCULK_SOUL);
            emVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.WAVE);
            emVisual.setGlowing(true);
            emVisual.setGlowColor(org.bukkit.Color.fromRGB(0, 128, 128));
            entropyMaster.setIconMaterial(Material.SCULK_SENSOR);
            entropyMaster.setVisualConfig(emVisual);
            registerStrain(entropyMaster);
            
            Strain multiverseEcho = new Strain("multiverse_echo_strain", "Multiverse Echo", Strain.Rarity.LEGENDARY, 96, 8, 96);
            multiverseEcho.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MULTIVERSE_ECHO, 5));
            multiverseEcho.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.MIRROR_IMAGE, 4));
            multiverseEcho.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.QUANTUM_STATE, 4));
            multiverseEcho.addEffect(new com.budlords.effects.StrainEffect(com.budlords.effects.StrainEffectType.TELEKINETIC_LIFT, 4));
            // Multiverse Echo - Mirror/reflection dimensional appearance
            StrainVisualConfig meVisual = new StrainVisualConfig();
            meVisual.setLeafMaterial(Material.TINTED_GLASS);
            meVisual.setBudMaterial(Material.LODESTONE);
            meVisual.setBudType(StrainVisualConfig.BudType.MIRROR);
            meVisual.setLeafColorPrimary(org.bukkit.Color.fromRGB(192, 192, 192));
            meVisual.setAmbientParticle(org.bukkit.Particle.ASH);
            meVisual.setAnimationStyle(StrainVisualConfig.AnimationStyle.FLOAT);
            meVisual.setGlowing(true);
            meVisual.setGlowColor(org.bukkit.Color.SILVER);
            meVisual.setHeightScale(1.3);
            multiverseEcho.setIconMaterial(Material.SPYGLASS);
            multiverseEcho.setVisualConfig(meVisual);
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
        // Use strain's icon material if it's a dye (for colored buds like purple strains)
        // Otherwise default to GREEN_DYE
        Material budMaterial = strain.getIconMaterial();
        if (budMaterial == null || !budMaterial.name().endsWith("_DYE")) {
            budMaterial = Material.GREEN_DYE;
        }
        
        ItemStack bud = new ItemStack(budMaterial, amount);
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
        if (item == null) return false;
        // Accept any dye material for buds (for colored strains like purple)
        Material type = item.getType();
        if (!type.name().endsWith("_DYE")) return false;
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
