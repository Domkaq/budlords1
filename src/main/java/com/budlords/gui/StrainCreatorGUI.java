package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    // Using deprecated createInventory(InventoryHolder, int, String) for Bukkit/Spigot compatibility
    // Paper servers can replace with Adventure API's Component-based version
    @SuppressWarnings("deprecation")
    public void open(Player player) {
        StrainBuilder builder = new StrainBuilder();
        activeBuilders.put(player.getUniqueId(), builder);
        
        Inventory inv = Bukkit.createInventory(this, 54, "§2§lStrain Creator");
        updateInventory(inv, builder);
        player.openInventory(inv);
    }

    private void updateInventory(Inventory inv, StrainBuilder builder) {
        inv.clear();

        // Border
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }

        // Name display/edit
        ItemStack nameItem = createItem(Material.NAME_TAG, 
            "§e§lStrain Name: §f" + builder.name,
            Arrays.asList("§7Click to rename", "§7Current: §f" + builder.name));
        inv.setItem(13, nameItem);

        // Rarity selector
        ItemStack rarityItem = createItem(getRarityMaterial(builder.rarity),
            "§e§lRarity: " + builder.rarity.getDisplayName(),
            Arrays.asList("§7Click to cycle rarity", "§7Current: " + builder.rarity.getDisplayName()));
        inv.setItem(20, rarityItem);

        // Potency controls
        inv.setItem(21, createItem(Material.RED_DYE, "§c- Potency", List.of("§7Decrease potency")));
        inv.setItem(22, createItem(Material.BLAZE_POWDER, 
            "§6§lPotency: §e" + builder.potency + "%",
            List.of("§7THC strength", "§7Range: 1-100")));
        inv.setItem(23, createItem(Material.LIME_DYE, "§a+ Potency", List.of("§7Increase potency")));

        // Yield controls
        inv.setItem(29, createItem(Material.RED_DYE, "§c- Yield", List.of("§7Decrease yield")));
        inv.setItem(30, createItem(Material.WHEAT_SEEDS, 
            "§6§lYield: §e" + builder.yield + " buds",
            List.of("§7Number of buds when harvested", "§7Range: 1-20")));
        inv.setItem(31, createItem(Material.LIME_DYE, "§a+ Yield", List.of("§7Increase yield")));

        // Quality controls
        inv.setItem(37, createItem(Material.RED_DYE, "§c- Quality", List.of("§7Decrease packaging quality")));
        inv.setItem(38, createItem(Material.DIAMOND, 
            "§6§lPackaging Quality: §e" + builder.packagingQuality + "%",
            List.of("§7Affects final sale value", "§7Range: 1-100")));
        inv.setItem(39, createItem(Material.LIME_DYE, "§a+ Quality", List.of("§7Increase packaging quality")));

        // Icon selector area (slots 24-26, 33-35, 42-44)
        ItemStack iconLabel = createItem(Material.PAINTING, "§e§lDrag Icon Here", 
            List.of("§7Place an item to use as icon", "§7Current: " + builder.iconMaterial.name()));
        inv.setItem(25, iconLabel);
        
        // Current icon display
        inv.setItem(34, createItem(builder.iconMaterial, "§aCurrent Icon", 
            List.of("§7" + builder.iconMaterial.name())));

        // Save button
        ItemStack saveBtn = createItem(Material.EMERALD_BLOCK, "§a§lSAVE STRAIN",
            Arrays.asList(
                "§7Click to save and register",
                "",
                "§7Name: §f" + builder.name,
                "§7Rarity: " + builder.rarity.getDisplayName(),
                "§7Potency: §e" + builder.potency + "%",
                "§7Yield: §e" + builder.yield,
                "§7Quality: §e" + builder.packagingQuality + "%"
            ));
        inv.setItem(49, saveBtn);

        // Cancel button
        inv.setItem(45, createItem(Material.BARRIER, "§c§lCANCEL", List.of("§7Close without saving")));
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
        
        // Allow placing items in icon area (slots 24-26, 33-35, 42-44)
        if (slot == 34 && event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
            builder.iconMaterial = event.getCursor().getType();
            event.setCancelled(true);
            updateInventory(event.getInventory(), builder);
            return;
        }

        event.setCancelled(true);

        switch (slot) {
            case 13 -> { // Name
                player.closeInventory();
                player.sendMessage("§eType the new strain name in chat:");
                builder.awaitingName = true;
                
                // Re-open after delay with chat listener
                plugin.getServer().getPluginManager().registerEvents(new ChatListener(plugin, player, builder, this), plugin);
            }
            case 20 -> { // Rarity
                builder.rarity = builder.rarity.next();
                updateInventory(event.getInventory(), builder);
            }
            case 21 -> { // Potency -
                builder.potency = Math.max(1, builder.potency - 5);
                updateInventory(event.getInventory(), builder);
            }
            case 23 -> { // Potency +
                builder.potency = Math.min(100, builder.potency + 5);
                updateInventory(event.getInventory(), builder);
            }
            case 29 -> { // Yield -
                builder.yield = Math.max(1, builder.yield - 1);
                updateInventory(event.getInventory(), builder);
            }
            case 31 -> { // Yield +
                builder.yield = Math.min(20, builder.yield + 1);
                updateInventory(event.getInventory(), builder);
            }
            case 37 -> { // Quality -
                builder.packagingQuality = Math.max(1, builder.packagingQuality - 5);
                updateInventory(event.getInventory(), builder);
            }
            case 39 -> { // Quality +
                builder.packagingQuality = Math.min(100, builder.packagingQuality + 5);
                updateInventory(event.getInventory(), builder);
            }
            case 45 -> { // Cancel
                player.closeInventory();
                player.sendMessage("§cStrain creation cancelled.");
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
        
        // Give player some seeds
        ItemStack seeds = strainManager.createSeedItem(strain, 5);
        player.getInventory().addItem(seeds);
        
        player.closeInventory();
        player.sendMessage("§a§l✓ Strain Created Successfully!");
        player.sendMessage("§7Name: §f" + strain.getName());
        player.sendMessage("§7ID: §f" + strain.getId());
        player.sendMessage("§7You received §e5 seeds§7 to get started!");
    }

    // Using deprecated createInventory for Bukkit/Spigot compatibility
    @SuppressWarnings("deprecation")
    public void reopenForPlayer(Player player) {
        StrainBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder != null) {
            Inventory inv = Bukkit.createInventory(this, 54, "§2§lStrain Creator");
            updateInventory(inv, builder);
            player.openInventory(inv);
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
        boolean awaitingName = false;
    }

    public Map<UUID, StrainBuilder> getActiveBuilders() {
        return activeBuilders;
    }
}
