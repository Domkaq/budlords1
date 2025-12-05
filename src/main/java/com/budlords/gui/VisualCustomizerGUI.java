package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.strain.StrainVisualConfig;
import com.budlords.strain.StrainVisualConfig.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for customizing strain visual appearance.
 * Allows admins to set:
 * - Visual themes (werewolf, crystal, fire, etc.)
 * - Bud types (skulls, heads, blocks, etc.)
 * - Animation styles
 * - Particle effects
 * - Scale/size adjustments
 * - Glow effects
 */
public class VisualCustomizerGUI implements InventoryHolder, Listener {

    private final BudLords plugin;
    private final StrainCreatorGUI strainCreatorGUI;
    
    // Current page tracking
    private final Map<UUID, Integer> themePage;
    private final Map<UUID, Integer> budTypePage;
    
    private static final int ITEMS_PER_PAGE = 14;

    public VisualCustomizerGUI(BudLords plugin, StrainCreatorGUI strainCreatorGUI) {
        this.plugin = plugin;
        this.strainCreatorGUI = strainCreatorGUI;
        this.themePage = new HashMap<>();
        this.budTypePage = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @SuppressWarnings("deprecation")
    public void open(Player player) {
        themePage.put(player.getUniqueId(), 0);
        budTypePage.put(player.getUniqueId(), 0);
        
        Inventory inv = Bukkit.createInventory(this, 54, "§d§l✦ Visual Customizer ✦");
        updateInventory(inv, player);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.5f);
    }

    private void updateInventory(Inventory inv, Player player) {
        inv.clear();
        
        StrainCreatorGUI.StrainBuilder builder = strainCreatorGUI.getActiveBuilders().get(player.getUniqueId());
        if (builder == null) return;
        
        StrainVisualConfig config = builder.visualConfig;
        if (config == null) {
            config = new StrainVisualConfig();
            builder.visualConfig = config;
        }

        // Border
        ItemStack borderPurple = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null);
        ItemStack borderMagenta = createItem(Material.MAGENTA_STAINED_GLASS_PANE, " ", null);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i % 2 == 0 ? borderPurple : borderMagenta);
            inv.setItem(45 + i, i % 2 == 0 ? borderPurple : borderMagenta);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, borderPurple);
            inv.setItem(i + 8, borderPurple);
        }

        // Header
        ItemStack header = createItem(Material.NETHER_STAR, 
            "§d§l✦ Plant Visual Customizer ✦",
            Arrays.asList(
                "§7Customize how your strain looks",
                "§7when growing in the world!",
                "",
                "§e§lCurrent Theme: §f" + config.getVisualTheme().getDisplayName(),
                "§e§lBud Type: §f" + config.getBudType().getDisplayName(),
                "",
                "§7Click items to customize"
            ));
        inv.setItem(4, header);

        // ═══════════════════════════════════════
        // ROW 1: VISUAL THEMES
        // ═══════════════════════════════════════
        
        int page = themePage.getOrDefault(player.getUniqueId(), 0);
        VisualTheme[] themes = VisualTheme.values();
        int startIdx = page * ITEMS_PER_PAGE;
        
        // Theme section label
        inv.setItem(10, createItem(Material.PAINTING, "§6§l🎨 Visual Themes",
            Arrays.asList(
                "",
                "§7Pre-built visual styles",
                "§7for your plant!",
                "",
                "§7Page: §e" + (page + 1) + "§7/" + ((themes.length / ITEMS_PER_PAGE) + 1)
            )));
        
        // Show themes (slots 11-16, 19-25)
        int[] themeSlots = {11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 26};
        for (int i = 0; i < themeSlots.length && (startIdx + i) < themes.length; i++) {
            VisualTheme theme = themes[startIdx + i];
            boolean selected = config.getVisualTheme() == theme;
            
            ItemStack themeItem = createItem(
                theme.getLeafMaterial() != null ? theme.getLeafMaterial() : Material.GRASS_BLOCK,
                (selected ? "§a§l✓ " : "§e") + theme.getDisplayName(),
                Arrays.asList(
                    "",
                    "§7" + theme.getDescription(),
                    "",
                    "§7Bud: §f" + theme.getBudType().getDisplayName(),
                    "§7Particles: §f" + formatParticleName(theme.getAmbientParticle()),
                    "",
                    selected ? "§a✓ Currently selected" : "§e▶ Click to select"
                ));
            inv.setItem(themeSlots[i], themeItem);
        }
        
        // Theme navigation
        if (page > 0) {
            inv.setItem(18, createItem(Material.ARROW, "§e← Previous Themes", 
                Arrays.asList("", "§7Click to go back")));
        }
        if ((page + 1) * ITEMS_PER_PAGE < themes.length) {
            inv.setItem(17, createItem(Material.ARROW, "§eNext Themes →", 
                Arrays.asList("", "§7Click for more themes")));
        }

        // ═══════════════════════════════════════
        // ROW 2: BUD TYPES (Special ones!)
        // ═══════════════════════════════════════
        
        inv.setItem(28, createItem(Material.LIME_WOOL, "§a§l🌿 Bud Type",
            Arrays.asList(
                "",
                "§7Choose what your buds look like!",
                "§7Special strains can have skulls,",
                "§7heads, and unique materials!",
                "",
                "§eCurrent: §f" + config.getBudType().getDisplayName()
            )));
        
        // Show interesting bud types
        BudType[] specialBuds = {BudType.SKULL, BudType.ZOMBIE, BudType.CREEPER, BudType.WITHER, 
            BudType.DRAGON, BudType.PIGLIN, BudType.CRYSTAL, BudType.FIRE, BudType.ICE, 
            BudType.NETHER, BudType.END, BudType.AMETHYST};
        
        int[] budSlots = {29, 30, 31, 32, 33, 34};
        for (int i = 0; i < budSlots.length && i < specialBuds.length; i++) {
            BudType bud = specialBuds[i];
            boolean selected = config.getBudType() == bud;
            
            inv.setItem(budSlots[i], createItem(
                bud.getDefaultMaterial(),
                (selected ? "§a§l✓ " : "§f") + bud.getDisplayName(),
                Arrays.asList(
                    "",
                    "§7" + bud.getDescription(),
                    "",
                    selected ? "§a✓ Selected" : "§e▶ Click to select"
                )));
        }

        // ═══════════════════════════════════════
        // ROW 3: SETTINGS
        // ═══════════════════════════════════════
        
        // Animation Style
        inv.setItem(37, createItem(Material.FEATHER, 
            "§b§l💨 Animation: §f" + config.getAnimationStyle().getDisplayName(),
            Arrays.asList(
                "",
                "§7" + config.getAnimationStyle().getDescription(),
                "",
                "§7Speed: §e" + String.format("%.1fx", config.getAnimationSpeed()),
                "",
                "§e▶ Click to cycle animation",
                "§e▶ Shift-click to adjust speed"
            )));
        
        // Glow Effect
        inv.setItem(38, createItem(
            config.isGlowing() ? Material.GLOWSTONE : Material.GLASS,
            "§e§l✨ Glow: " + (config.isGlowing() ? "§aON" : "§cOFF"),
            Arrays.asList(
                "",
                "§7Makes your plant glow at night!",
                "§7Perfect for special strains.",
                "",
                "§e▶ Click to toggle"
            )));
        
        // Particle Intensity
        inv.setItem(39, createItem(Material.FIREWORK_ROCKET,
            "§d§l💫 Particles: §f" + config.getParticleIntensity() + "/10",
            Arrays.asList(
                "",
                "§7How many particles spawn",
                "§7around your plant.",
                "",
                createParticleBar(config.getParticleIntensity()),
                "",
                "§e▶ Click to increase",
                "§e▶ Shift-click to decrease"
            )));
        
        // Scale Settings
        inv.setItem(40, createItem(Material.PISTON,
            "§6§l📏 Size Settings",
            Arrays.asList(
                "",
                "§7Leaf Scale: §e" + String.format("%.1fx", config.getLeafScale()),
                "§7Bud Scale: §e" + String.format("%.1fx", config.getBudScale()),
                "§7Height Scale: §e" + String.format("%.1fx", config.getHeightScale()),
                "",
                "§e▶ Click to cycle size mode",
                "§e▶ Shift-click to reset"
            )));
        
        // More Bud Types Button
        inv.setItem(41, createItem(Material.CHEST, "§6§l📦 More Bud Types",
            Arrays.asList(
                "",
                "§7View all " + BudType.values().length + " bud types",
                "§7including normal and special",
                "",
                "§e▶ Click to open"
            )));
        
        // Preview
        ItemStack preview = createItem(
            config.getBudType().getDefaultMaterial(),
            "§a§l✓ Current Preview",
            Arrays.asList(
                "",
                "§7Theme: §f" + config.getVisualTheme().getDisplayName(),
                "§7Buds: §f" + config.getBudType().getDisplayName(),
                "§7Animation: §f" + config.getAnimationStyle().getDisplayName(),
                "§7Glow: " + (config.isGlowing() ? "§aYes" : "§cNo"),
                "§7Particles: §f" + config.getParticleIntensity(),
                "",
                "§7This is how your plant will look!"
            ));
        inv.setItem(43, preview);

        // ═══════════════════════════════════════
        // BOTTOM ROW
        // ═══════════════════════════════════════
        
        // Back button
        inv.setItem(47, createItem(Material.ARROW, "§e§l← Back to Strain Creator",
            Arrays.asList("", "§7Return without losing changes")));
        
        // Save & Back
        inv.setItem(51, createItem(Material.EMERALD, "§a§l✓ Save & Continue",
            Arrays.asList("", "§7Save visual settings", "§7and return to creator")));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof VisualCustomizerGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        StrainCreatorGUI.StrainBuilder builder = strainCreatorGUI.getActiveBuilders().get(player.getUniqueId());
        if (builder == null) {
            player.closeInventory();
            return;
        }

        StrainVisualConfig config = builder.visualConfig;
        if (config == null) {
            config = new StrainVisualConfig();
            builder.visualConfig = config;
        }

        int slot = event.getRawSlot();
        boolean shift = event.isShiftClick();
        
        // Check which GUI we're in by looking at the title
        String title = event.getView().getTitle();
        
        // Handle "All Bud Types" sub-GUI
        if (title.contains("All Bud Types")) {
            handleAllBudTypesClick(event, player, config, slot);
            return;
        }
        
        // Handle main Visual Customizer GUI
        handleMainCustomizerClick(event, player, config, slot, shift);
    }
    
    /**
     * Handles clicks in the "All Bud Types" sub-GUI.
     */
    private void handleAllBudTypesClick(InventoryClickEvent event, Player player, StrainVisualConfig config, int slot) {
        // Back button
        if (slot == 49) {
            open(player); // Return to main customizer
            return;
        }
        
        // Bud type selection slots
        BudType[] allBuds = BudType.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 
                       28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        
        for (int i = 0; i < slots.length && i < allBuds.length; i++) {
            if (slot == slots[i]) {
                BudType selectedBud = allBuds[i];
                config.setBudType(selectedBud);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.3f);
                player.sendMessage("§aBud type set to: §f" + selectedBud.getDisplayName());
                
                // Return to main customizer
                open(player);
                return;
            }
        }
    }
    
    /**
     * Handles clicks in the main Visual Customizer GUI.
     */
    private void handleMainCustomizerClick(InventoryClickEvent event, Player player, StrainVisualConfig config, int slot, boolean shift) {
        // Theme selection (slots 11-16, 19-26)
        int[] themeSlots = {11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 26};
        int page = themePage.getOrDefault(player.getUniqueId(), 0);
        VisualTheme[] themes = VisualTheme.values();
        int startIdx = page * ITEMS_PER_PAGE;
        
        for (int i = 0; i < themeSlots.length; i++) {
            if (slot == themeSlots[i]) {
                int themeIdx = startIdx + i;
                if (themeIdx < themes.length) {
                    config.applyTheme(themes[themeIdx]);
                    updateInventory(event.getInventory(), player);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                    player.sendMessage("§aTheme set to: §f" + themes[themeIdx].getDisplayName());
                }
                return;
            }
        }
        
        // Theme navigation
        if (slot == 18 && page > 0) {
            themePage.put(player.getUniqueId(), page - 1);
            updateInventory(event.getInventory(), player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
            return;
        }
        if (slot == 17 && (page + 1) * ITEMS_PER_PAGE < themes.length) {
            themePage.put(player.getUniqueId(), page + 1);
            updateInventory(event.getInventory(), player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
            return;
        }

        // Bud type selection (slots 29-34)
        BudType[] specialBuds = {BudType.SKULL, BudType.ZOMBIE, BudType.CREEPER, BudType.WITHER, 
            BudType.DRAGON, BudType.PIGLIN, BudType.CRYSTAL, BudType.FIRE, BudType.ICE, 
            BudType.NETHER, BudType.END, BudType.AMETHYST};
        int[] budSlots = {29, 30, 31, 32, 33, 34};
        
        for (int i = 0; i < budSlots.length && i < specialBuds.length; i++) {
            if (slot == budSlots[i]) {
                config.setBudType(specialBuds[i]);
                updateInventory(event.getInventory(), player);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.3f);
                player.sendMessage("§aBud type set to: §f" + specialBuds[i].getDisplayName());
                return;
            }
        }

        switch (slot) {
            case 37 -> { // Animation
                if (shift) {
                    // Adjust speed
                    double speed = config.getAnimationSpeed();
                    speed = (speed >= 2.5) ? 0.5 : speed + 0.5;
                    config.setAnimationSpeed(speed);
                } else {
                    // Cycle animation style
                    AnimationStyle[] styles = AnimationStyle.values();
                    int current = config.getAnimationStyle().ordinal();
                    config.setAnimationStyle(styles[(current + 1) % styles.length]);
                }
                updateInventory(event.getInventory(), player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
            }
            case 38 -> { // Glow toggle
                config.setGlowing(!config.isGlowing());
                updateInventory(event.getInventory(), player);
                player.playSound(player.getLocation(), config.isGlowing() ? 
                    Sound.BLOCK_BEACON_ACTIVATE : Sound.BLOCK_BEACON_DEACTIVATE, 0.3f, 1.0f);
            }
            case 39 -> { // Particle intensity
                int intensity = config.getParticleIntensity();
                if (shift) {
                    intensity = Math.max(1, intensity - 1);
                } else {
                    intensity = Math.min(10, intensity + 1);
                }
                config.setParticleIntensity(intensity);
                updateInventory(event.getInventory(), player);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.3f, 1.0f + intensity * 0.1f);
            }
            case 40 -> { // Scale settings
                if (shift) {
                    // Reset all scales
                    config.setLeafScale(1.0);
                    config.setBudScale(1.0);
                    config.setHeightScale(1.0);
                } else {
                    // Cycle through sizes
                    double scale = config.getLeafScale();
                    scale = (scale >= 1.8) ? 0.6 : scale + 0.2;
                    config.setLeafScale(scale);
                    config.setBudScale(scale);
                    config.setHeightScale(scale);
                }
                updateInventory(event.getInventory(), player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
            }
            case 41 -> { // More bud types
                openAllBudTypesGUI(player);
            }
            case 47 -> { // Back
                player.closeInventory();
                strainCreatorGUI.reopenForPlayer(player);
            }
            case 51 -> { // Save & Back
                player.closeInventory();
                strainCreatorGUI.reopenForPlayer(player);
                player.sendMessage("§aVisual settings saved!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 1.5f);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void openAllBudTypesGUI(Player player) {
        Inventory inv = Bukkit.createInventory(this, 54, "§d§l✦ All Bud Types ✦");
        
        StrainCreatorGUI.StrainBuilder builder = strainCreatorGUI.getActiveBuilders().get(player.getUniqueId());
        StrainVisualConfig config = builder != null ? builder.visualConfig : null;
        BudType currentBud = config != null ? config.getBudType() : BudType.NORMAL;
        
        // Border
        ItemStack border = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
        
        // Header
        inv.setItem(4, createItem(Material.LIME_WOOL, "§a§l🌿 All Bud Types",
            Arrays.asList("", "§7Click any bud type to select it", "", "§7Current: §f" + currentBud.getDisplayName())));
        
        // All bud types
        BudType[] allBuds = BudType.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 
                       28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        
        for (int i = 0; i < slots.length && i < allBuds.length; i++) {
            BudType bud = allBuds[i];
            boolean selected = bud == currentBud;
            
            inv.setItem(slots[i], createItem(
                bud.getDefaultMaterial(),
                (selected ? "§a§l✓ " : "§f") + bud.getDisplayName(),
                Arrays.asList(
                    "",
                    "§7" + bud.getDescription(),
                    "",
                    selected ? "§a✓ Currently selected" : "§e▶ Click to select"
                )));
        }
        
        // Back button
        inv.setItem(49, createItem(Material.ARROW, "§e§l← Back",
            Arrays.asList("", "§7Return to visual customizer")));
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
    }

    private String createParticleBar(int intensity) {
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 1; i <= 10; i++) {
            if (i <= intensity) {
                bar.append("§d✦");
            } else {
                bar.append("§7○");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }
    
    private String formatParticleName(Particle particle) {
        if (particle == null) return "None";
        String name = particle.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
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

    @Override
    public Inventory getInventory() {
        return null;
    }
}
