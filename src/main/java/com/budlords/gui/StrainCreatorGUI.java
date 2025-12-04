package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StrainCreatorGUI implements InventoryHolder, Listener {

    private final BudLords plugin;
    private final StrainManager strainManager;
    private final Map<UUID, StrainBuilder> activeBuilders;

    public StrainCreatorGUI(BudLords plugin, StrainManager strainManager) {
        this.plugin = plugin;
        this.strainManager = strainManager;
        this.activeBuilders = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @SuppressWarnings("deprecation")
    public void open(Player player) {
        StrainBuilder builder = new StrainBuilder();
        activeBuilders.put(player.getUniqueId(), builder);
        
        Inventory inv = Bukkit.createInventory(this, 54, "§2§l✿ Strain Creator ✿");
        updateInventory(inv, builder);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
    }

    private void updateInventory(Inventory inv, StrainBuilder builder) {
        inv.clear();

        // Modern gradient border
        ItemStack borderDark = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        ItemStack borderGreen = createItem(Material.GREEN_STAINED_GLASS_PANE, " ", null);
        ItemStack borderLime = createItem(Material.LIME_STAINED_GLASS_PANE, " ", null);
        
        // Top border with gradient
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderGreen : borderLime);
        }
        // Bottom border with gradient
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, i % 2 == 0 ? borderGreen : borderLime);
        }
        // Side borders
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, borderDark);
            inv.setItem(i + 8, borderDark);
        }

        // Header info
        ItemStack header = createItem(Material.OAK_SIGN, 
            "§a§l✿ Create Your Strain ✿",
            Arrays.asList(
                "§7Design a unique cannabis strain!",
                "",
                "§7Click items to adjust settings",
                "§7Save when you're satisfied"
            ));
        inv.setItem(4, header);

        // Name display/edit - Modern style
        ItemStack nameItem = createItem(Material.NAME_TAG, 
            "§e§l✎ Name: §f" + builder.name,
            Arrays.asList(
                "",
                "§7Current: §f" + builder.name,
                "",
                "§a▶ Click to rename",
                "§7You'll type the new name in chat",
                "§7and return here automatically!"
            ));
        inv.setItem(13, nameItem);

        // Rarity selector with visual indicator
        ItemStack rarityItem = createItem(getRarityMaterial(builder.rarity),
            "§e§l◆ Rarity: " + builder.rarity.getDisplayName(),
            Arrays.asList(
                "",
                getRarityDescription(builder.rarity),
                "",
                "§a▶ Click to cycle rarity",
                "§7" + getRarityOrder(builder.rarity)
            ));
        inv.setItem(20, rarityItem);

        // Seed star rating selector
        ItemStack starItem = createItem(Material.NETHER_STAR,
            "§e§l★ Seed Quality: " + builder.seedStarRating.getDisplay(),
            Arrays.asList(
                "",
                "§7Affects growth and final quality",
                "§7Better seeds = Better buds!",
                "",
                "§a▶ Click to cycle star rating"
            ));
        inv.setItem(24, starItem);

        // Potency controls - Enhanced
        inv.setItem(21, createDecreaseButton("Potency"));
        inv.setItem(22, createItem(Material.BLAZE_POWDER, 
            "§6§l⚗ Potency: §e" + builder.potency + "%",
            Arrays.asList(
                "",
                "§7THC strength of the strain",
                "§7Affects sale value and effects",
                "",
                createProgressBar(builder.potency),
                "§7Range: 1-100"
            )));
        inv.setItem(23, createIncreaseButton("Potency"));

        // Yield controls - Enhanced
        inv.setItem(29, createDecreaseButton("Yield"));
        inv.setItem(30, createItem(Material.WHEAT_SEEDS, 
            "§6§l🌿 Yield: §e" + builder.yield + " buds",
            Arrays.asList(
                "",
                "§7Buds harvested per plant",
                "§7More buds = More profit!",
                "",
                createYieldDisplay(builder.yield),
                "§7Range: 1-20"
            )));
        inv.setItem(31, createIncreaseButton("Yield"));

        // Quality controls - Enhanced
        inv.setItem(37, createDecreaseButton("Quality"));
        inv.setItem(38, createItem(Material.DIAMOND, 
            "§6§l💎 Packaging Quality: §e" + builder.packagingQuality + "%",
            Arrays.asList(
                "",
                "§7Quality of packaged product",
                "§7Affects final sale value",
                "",
                createProgressBar(builder.packagingQuality),
                "§7Range: 1-100"
            )));
        inv.setItem(39, createIncreaseButton("Quality"));

        // Icon selector area
        inv.setItem(25, createItem(Material.PAINTING, "§e§l🎨 Custom Icon", 
            Arrays.asList(
                "",
                "§7Drag an item here to set icon",
                "§7Current: §f" + builder.iconMaterial.name()
            )));
        
        // Current icon display
        inv.setItem(34, createItem(builder.iconMaterial, "§a✓ Current Icon", 
            Arrays.asList("§7" + builder.iconMaterial.name(), "", "§7Drop item here to change")));

        // Preview area
        ItemStack preview = createItem(builder.iconMaterial,
            builder.rarity.getDisplayName() + " " + builder.name,
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━",
                "§7Potency: §e" + builder.potency + "%",
                "§7Yield: §e" + builder.yield + " buds",
                "§7Quality: §e" + builder.packagingQuality + "%",
                "§7Seed: " + builder.seedStarRating.getDisplay(),
                "§8━━━━━━━━━━━━━━━━━━━━",
                "",
                "§7This is a preview of your strain!"
            ));
        inv.setItem(43, preview);

        // Save button - Modern with animation hint
        ItemStack saveBtn = createItem(Material.EMERALD_BLOCK, "§a§l✓ SAVE & CREATE",
            Arrays.asList(
                "",
                "§7━━━━ Strain Summary ━━━━",
                "§7Name: §f" + builder.name,
                "§7Rarity: " + builder.rarity.getDisplayName(),
                "§7Potency: §e" + builder.potency + "%",
                "§7Yield: §e" + builder.yield + " buds",
                "§7Quality: §e" + builder.packagingQuality + "%",
                "§7Seed: " + builder.seedStarRating.getDisplay(),
                "",
                "§a▶ Click to create strain!",
                "§7You'll receive 5 seeds"
            ));
        inv.setItem(49, saveBtn);

        // Cancel button
        inv.setItem(45, createItem(Material.BARRIER, "§c§l✗ CANCEL", 
            Arrays.asList("", "§7Close without saving", "§7Progress will be lost!")));
    }
    
    private String createProgressBar(int value) {
        StringBuilder bar = new StringBuilder("§8[");
        int filled = value / 10;
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }
    
    private String createYieldDisplay(int yield) {
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < Math.min(yield, 10); i++) {
            display.append("§a✿");
        }
        if (yield > 10) {
            display.append(" §7+").append(yield - 10);
        }
        return display.toString();
    }
    
    private ItemStack createDecreaseButton(String type) {
        return createItem(Material.RED_CONCRETE, "§c§l◀ -5 " + type, 
            Arrays.asList("", "§7Click to decrease", "§7Shift-click: -10"));
    }
    
    private ItemStack createIncreaseButton(String type) {
        return createItem(Material.LIME_CONCRETE, "§a§l+5 " + type + " ▶", 
            Arrays.asList("", "§7Click to increase", "§7Shift-click: +10"));
    }
    
    private String getRarityDescription(Strain.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> "§7Basic strain, easy to sell";
            case UNCOMMON -> "§aSlightly better quality";
            case RARE -> "§9High demand, better prices";
            case LEGENDARY -> "§6Premium quality, max value!";
        };
    }
    
    private String getRarityOrder(Strain.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> "Common → Uncommon → Rare → Legendary";
            case UNCOMMON -> "Common ← Uncommon → Rare → Legendary";
            case RARE -> "Common ← Uncommon ← Rare → Legendary";
            case LEGENDARY -> "Common ← Uncommon ← Rare ← Legendary";
        };
    }

    private Material getRarityMaterial(Strain.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Material.COAL;
            case UNCOMMON -> Material.IRON_INGOT;
            case RARE -> Material.GOLD_INGOT;
            case LEGENDARY -> Material.DIAMOND;
        };
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StrainCreatorGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        StrainBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) return;

        int slot = event.getRawSlot();
        boolean shift = event.isShiftClick();
        int amount = shift ? 10 : 5;
        
        // Allow clicking in player inventory to pick up items for drag
        if (slot >= 54) {
            // Player inventory slot - allow normal interaction for picking up items
            // But prevent shift-clicking into the GUI
            if (shift) {
                event.setCancelled(true);
            }
            return;
        }
        
        // Allow placing items in icon area (slot 34 or 25)
        if ((slot == 34 || slot == 25) && event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
            builder.iconMaterial = event.getCursor().getType();
            event.setCancelled(true);
            updateInventory(event.getInventory(), builder);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.3f, 1.5f);
            player.sendMessage("§aIcon set to: §f" + builder.iconMaterial.name());
            return;
        }

        event.setCancelled(true);

        switch (slot) {
            case 13 -> { // Name
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§a§l✎ §eType the new strain name in chat:");
                player.sendMessage("§7(The name will be set and you'll return to the GUI)");
                player.sendMessage("");
                builder.awaitingName = true;
                
                // Register chat listener
                plugin.getServer().getPluginManager().registerEvents(new ChatListener(plugin, player, builder, this), plugin);
            }
            case 20 -> { // Rarity
                builder.rarity = builder.rarity.next();
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 1.0f + (builder.rarity.ordinal() * 0.2f));
            }
            case 24 -> { // Seed star rating
                int currentStars = builder.seedStarRating.getStars();
                builder.seedStarRating = StarRating.fromValue((currentStars % 5) + 1);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.4f, 0.8f + (builder.seedStarRating.getStars() * 0.15f));
            }
            case 21 -> { // Potency -
                builder.potency = Math.max(1, builder.potency - amount);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.8f);
            }
            case 23 -> { // Potency +
                builder.potency = Math.min(100, builder.potency + amount);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 1.2f);
            }
            case 29 -> { // Yield -
                builder.yield = Math.max(1, builder.yield - (shift ? 2 : 1));
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.8f);
            }
            case 31 -> { // Yield +
                builder.yield = Math.min(20, builder.yield + (shift ? 2 : 1));
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 1.2f);
            }
            case 37 -> { // Quality -
                builder.packagingQuality = Math.max(1, builder.packagingQuality - amount);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.8f);
            }
            case 39 -> { // Quality +
                builder.packagingQuality = Math.min(100, builder.packagingQuality + amount);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 1.2f);
            }
            case 45 -> { // Cancel
                player.closeInventory();
                player.sendMessage("§c✗ Strain creation cancelled.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
            case 49 -> { // Save
                saveStrain(player, builder);
            }
        }
    }

    private void saveStrain(Player player, StrainBuilder builder) {
        String id = strainManager.generateUniqueId(builder.name);
        
        Strain strain = new Strain(
            id,
            builder.name,
            builder.rarity,
            builder.potency,
            builder.yield,
            builder.packagingQuality
        );
        strain.setIconMaterial(builder.iconMaterial);
        
        strainManager.registerStrain(strain);
        strainManager.saveStrains();
        
        // Give player seeds with selected star rating
        ItemStack seeds = strainManager.createSeedItem(strain, 5, builder.seedStarRating);
        player.getInventory().addItem(seeds);
        
        player.closeInventory();
        
        // Success effects
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        
        player.sendMessage("");
        player.sendMessage("§a§l✓ Strain Created Successfully!");
        player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7Name: §f" + strain.getName());
        player.sendMessage("§7ID: §8" + strain.getId());
        player.sendMessage("§7Rarity: " + strain.getRarity().getDisplayName());
        player.sendMessage("§7Seeds: " + builder.seedStarRating.getDisplay());
        player.sendMessage("");
        player.sendMessage("§eYou received §a5 seeds §eto get started!");
        player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
    }

    @SuppressWarnings("deprecation")
    public void reopenForPlayer(Player player) {
        StrainBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder != null) {
            Inventory inv = Bukkit.createInventory(this, 54, "§2§l✿ Strain Creator ✿");
            updateInventory(inv, builder);
            player.openInventory(inv);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, 1.0f);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StrainCreatorGUI)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        
        StrainBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder != null && !builder.awaitingName) {
            activeBuilders.remove(player.getUniqueId());
        }
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public static class StrainBuilder {
        String name = "New Strain";
        Strain.Rarity rarity = Strain.Rarity.COMMON;
        int potency = 50;
        int yield = 3;
        int packagingQuality = 50;
        Material iconMaterial = Material.GREEN_DYE;
        StarRating seedStarRating = StarRating.ONE_STAR;
        boolean awaitingName = false;
    }

    public Map<UUID, StrainBuilder> getActiveBuilders() {
        return activeBuilders;
    }
}
