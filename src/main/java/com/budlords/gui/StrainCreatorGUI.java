package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.effects.StrainEffect;
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
    private EffectSelectorGUI effectSelectorGUI;

    public StrainCreatorGUI(BudLords plugin, StrainManager strainManager) {
        this.plugin = plugin;
        this.strainManager = strainManager;
        this.activeBuilders = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Initialize effect selector
        this.effectSelectorGUI = new EffectSelectorGUI(plugin, this);
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

        // Header info (centered at top)
        ItemStack header = createItem(Material.OAK_SIGN, 
            "§a§l✿ Create Your Strain ✿",
            Arrays.asList(
                "§7Design a unique cannabis strain!",
                "",
                "§7Click items to adjust settings",
                "§7Save when you're satisfied"
            ));
        inv.setItem(4, header);

        // ===== ROW 1 (slots 10-16): Name and Rarity =====
        
        // Name display/edit - Left side
        ItemStack nameItem = createItem(Material.NAME_TAG, 
            "§e§l✎ Name: §f" + builder.name,
            Arrays.asList(
                "",
                "§7Current: §f" + builder.name,
                "",
                "§a▶ Click to rename",
                "§7Type in chat and return"
            ));
        inv.setItem(11, nameItem);
        
        // Separator
        inv.setItem(13, borderDark);

        // Rarity selector - Right side
        ItemStack rarityItem = createItem(getRarityMaterial(builder.rarity),
            "§e§l◆ Rarity: " + builder.rarity.getDisplayName(),
            Arrays.asList(
                "",
                getRarityDescription(builder.rarity),
                "",
                "§a▶ Click to cycle rarity"
            ));
        inv.setItem(15, rarityItem);

        // ===== ROW 2 (slots 19-25): Potency and Seed Quality =====
        
        // Potency controls - Left centered
        inv.setItem(19, createDecreaseButton("Potency"));
        inv.setItem(20, createItem(Material.BLAZE_POWDER, 
            "§6§l⚗ Potency: §e" + builder.potency + "%",
            Arrays.asList(
                "",
                "§7THC strength of the strain",
                "§7Affects sale value",
                "",
                createProgressBar(builder.potency),
                "§7Range: 1-100"
            )));
        inv.setItem(21, createIncreaseButton("Potency"));
        
        // Separator
        inv.setItem(22, borderDark);
        
        // Seed star rating - Right centered
        ItemStack starItem = createItem(Material.NETHER_STAR,
            "§e§l★ Seed Quality: " + builder.seedStarRating.getDisplay(),
            Arrays.asList(
                "",
                "§7Affects growth and final quality",
                "§7Better seeds = Better buds!",
                "",
                "§a▶ Click to cycle rating"
            ));
        inv.setItem(24, starItem);

        // ===== ROW 3 (slots 28-34): Yield and Icon =====
        
        // Yield controls - Left centered
        inv.setItem(28, createDecreaseButton("Yield"));
        inv.setItem(29, createItem(Material.WHEAT_SEEDS, 
            "§6§l🌿 Yield: §e" + builder.yield + " buds",
            Arrays.asList(
                "",
                "§7Buds harvested per plant",
                "§7More buds = More profit!",
                "",
                createYieldDisplay(builder.yield),
                "§7Range: 1-20"
            )));
        inv.setItem(30, createIncreaseButton("Yield"));
        
        // Separator
        inv.setItem(31, borderDark);
        
        // Icon display - Right side
        inv.setItem(33, createItem(Material.PAINTING, "§e§l🎨 Set Icon", 
            Arrays.asList(
                "",
                "§7Drag an item here to set",
                "§7Click with item on cursor"
            )));
        inv.setItem(34, createItem(builder.iconMaterial, "§a✓ Current: §f" + formatMaterialName(builder.iconMaterial), 
            Arrays.asList("", "§7This is your strain's icon")));

        // ===== ROW 4 (slots 37-43): Quality and Effects =====
        
        // Quality controls - Left centered
        inv.setItem(37, createDecreaseButton("Quality"));
        inv.setItem(38, createItem(Material.DIAMOND, 
            "§6§l💎 Quality: §e" + builder.packagingQuality + "%",
            Arrays.asList(
                "",
                "§7Quality of packaged product",
                "§7Affects final sale value",
                "",
                createProgressBar(builder.packagingQuality),
                "§7Range: 1-100"
            )));
        inv.setItem(39, createIncreaseButton("Quality"));
        
        // EFFECTS BUTTON - Center
        List<String> effectsLore = new ArrayList<>();
        effectsLore.add("");
        effectsLore.add("§7Add special visual and gameplay");
        effectsLore.add("§7effects to your strain!");
        effectsLore.add("");
        effectsLore.add("§7Selected: §e" + builder.effects.size() + " effects");
        effectsLore.add("§6Admin tool - §e§lUNLIMITED EFFECTS!");
        if (!builder.effects.isEmpty()) {
            effectsLore.add("");
            // Only show first 10 effects to prevent lore overflow
            int displayCount = Math.min(builder.effects.size(), 10);
            for (int i = 0; i < displayCount; i++) {
                effectsLore.add("  " + builder.effects.get(i).getCompactDisplay());
            }
            if (builder.effects.size() > 10) {
                effectsLore.add("  §7... and " + (builder.effects.size() - 10) + " more");
            }
        }
        effectsLore.add("");
        effectsLore.add("§a▶ Click to select effects!");
        
        inv.setItem(41, createItem(Material.BEACON, 
            "§d§l✦ Special Effects §7(" + builder.effects.size() + ")",
            effectsLore));

        // Preview area - Right side
        List<String> previewLore = new ArrayList<>();
        previewLore.add("§8━━━━━━━━━━━━━━━━━");
        previewLore.add("§7Potency: §e" + builder.potency + "%");
        previewLore.add("§7Yield: §e" + builder.yield + " buds");
        previewLore.add("§7Quality: §e" + builder.packagingQuality + "%");
        previewLore.add("§7Seed: " + builder.seedStarRating.getDisplay());
        if (!builder.effects.isEmpty()) {
            previewLore.add("§7Effects: §d" + builder.effects.size());
        }
        previewLore.add("§8━━━━━━━━━━━━━━━━━");
        previewLore.add("");
        previewLore.add("§a✓ Preview of your strain");
        
        ItemStack preview = createItem(builder.iconMaterial,
            builder.rarity.getDisplayName() + " " + builder.name,
            previewLore);
        inv.setItem(43, preview);

        // ===== BOTTOM ROW: Cancel and Save =====
        
        // Cancel button - Left
        inv.setItem(46, createItem(Material.BARRIER, "§c§l✗ CANCEL", 
            Arrays.asList("", "§7Close without saving", "§cProgress will be lost!")));
        
        // Save button - Center-right
        List<String> saveLore = new ArrayList<>();
        saveLore.add("");
        saveLore.add("§7━━ Strain Summary ━━");
        saveLore.add("§7Name: §f" + builder.name);
        saveLore.add("§7Rarity: " + builder.rarity.getDisplayName());
        saveLore.add("§7Potency: §e" + builder.potency + "%");
        saveLore.add("§7Yield: §e" + builder.yield + " buds");
        saveLore.add("§7Quality: §e" + builder.packagingQuality + "%");
        saveLore.add("§7Seed: " + builder.seedStarRating.getDisplay());
        if (!builder.effects.isEmpty()) {
            saveLore.add("§7Effects: §d" + builder.effects.size());
        }
        saveLore.add("");
        saveLore.add("§a▶ Click to create!");
        saveLore.add("§7You'll receive 5 seeds");
        
        ItemStack saveBtn = createItem(Material.EMERALD_BLOCK, "§a§l✓ SAVE & CREATE", saveLore);
        inv.setItem(52, saveBtn);
    }
    
    private String formatMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        // Capitalize first letter of each word
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
        
        // Allow placing items in icon area (slot 33 or 34)
        if ((slot == 33 || slot == 34) && event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
            builder.iconMaterial = event.getCursor().getType();
            event.setCancelled(true);
            updateInventory(event.getInventory(), builder);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.3f, 1.5f);
            player.sendMessage("§aIcon set to: §f" + builder.iconMaterial.name());
            return;
        }

        event.setCancelled(true);

        switch (slot) {
            case 11 -> { // Name (moved from 13)
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§a§l✎ §eType the new strain name in chat:");
                player.sendMessage("§7(The name will be set and you'll return to the GUI)");
                player.sendMessage("");
                builder.awaitingName = true;
                
                // Register chat listener
                plugin.getServer().getPluginManager().registerEvents(new ChatListener(plugin, player, builder, this), plugin);
            }
            case 15 -> { // Rarity (moved from 20)
                builder.rarity = builder.rarity.next();
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 1.0f + (builder.rarity.ordinal() * 0.2f));
            }
            case 24 -> { // Seed star rating (same)
                int currentStars = builder.seedStarRating.getStars();
                builder.seedStarRating = StarRating.fromValue((currentStars % 5) + 1);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.4f, 0.8f + (builder.seedStarRating.getStars() * 0.15f));
            }
            case 19 -> { // Potency - (moved from 21)
                builder.potency = Math.max(1, builder.potency - amount);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.8f);
            }
            case 21 -> { // Potency + (moved from 23)
                builder.potency = Math.min(100, builder.potency + amount);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 1.2f);
            }
            case 28 -> { // Yield - (moved from 29)
                builder.yield = Math.max(1, builder.yield - (shift ? 2 : 1));
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.8f);
            }
            case 30 -> { // Yield + (moved from 31)
                builder.yield = Math.min(20, builder.yield + (shift ? 2 : 1));
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 1.2f);
            }
            case 37 -> { // Quality - (same)
                builder.packagingQuality = Math.max(1, builder.packagingQuality - amount);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.8f);
            }
            case 39 -> { // Quality + (same)
                builder.packagingQuality = Math.min(100, builder.packagingQuality + amount);
                updateInventory(event.getInventory(), builder);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 1.2f);
            }
            case 41 -> { // Effects selector
                player.closeInventory();
                effectSelectorGUI.open(player);
            }
            case 46 -> { // Cancel (moved from 45)
                player.closeInventory();
                player.sendMessage("§c✗ Strain creation cancelled.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
            case 52 -> { // Save (moved from 49)
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
        
        // Mark as admin-created to allow more effects
        strain.setAdminCreated(true);
        
        // Add effects from builder
        strain.setEffects(builder.effects);
        
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
        
        // Show effects if any
        if (!builder.effects.isEmpty()) {
            player.sendMessage("§7Effects: §d" + builder.effects.size());
            for (StrainEffect effect : builder.effects) {
                player.sendMessage("  " + effect.getCompactDisplay());
            }
        }
        
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
        // Only remove the builder if the player is NOT awaiting name input
        // and is NOT opening another GUI (like the effect selector)
        // We keep the builder active for a short time to allow returning to the GUI
        if (builder != null && !builder.awaitingName) {
            // Schedule removal with a delay to allow effect selector to access it
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Only remove if the player doesn't have the effect selector open
                Player p = plugin.getServer().getPlayer(player.getUniqueId());
                if (p != null && p.getOpenInventory() != null) {
                    String title = p.getOpenInventory().getTitle();
                    if (title != null && (title.contains("Effect Selector") || title.contains("Strain Creator"))) {
                        return; // Don't remove if in related GUI
                    }
                }
                // Check if not awaiting name (could have started name input)
                StrainBuilder currentBuilder = activeBuilders.get(player.getUniqueId());
                if (currentBuilder != null && !currentBuilder.awaitingName) {
                    activeBuilders.remove(player.getUniqueId());
                }
            }, 10L); // 0.5 second delay
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
        List<com.budlords.effects.StrainEffect> effects = new ArrayList<>();
        
        public StrainBuilder() {
            // Default constructor
        }
        
        public boolean addEffect(com.budlords.effects.StrainEffectType type, int intensity) {
            // Admin strains can have up to MAX_EFFECTS_ADMIN effects!
            if (effects.size() >= Strain.MAX_EFFECTS_ADMIN) {
                return false;
            }
            // Check for duplicates
            for (com.budlords.effects.StrainEffect existing : effects) {
                if (existing.getType() == type) {
                    return false;
                }
            }
            effects.add(new com.budlords.effects.StrainEffect(type, intensity));
            return true;
        }
        
        public boolean removeEffect(com.budlords.effects.StrainEffectType type) {
            return effects.removeIf(e -> e.getType() == type);
        }
        
        public boolean hasEffect(com.budlords.effects.StrainEffectType type) {
            return effects.stream().anyMatch(e -> e.getType() == type);
        }
    }

    public Map<UUID, StrainBuilder> getActiveBuilders() {
        return activeBuilders;
    }
}
