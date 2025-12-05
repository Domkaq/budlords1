package com.budlords.gui;

import com.budlords.BudLords;
import com.budlords.effects.StrainEffect;
import com.budlords.effects.StrainEffectType;
import com.budlords.strain.Strain;
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

/**
 * GUI for selecting strain effects in the Strain Creator.
 */
public class EffectSelectorGUI implements InventoryHolder, Listener {
    
    private final BudLords plugin;
    private final StrainCreatorGUI strainCreatorGUI;
    private final Map<UUID, EffectSession> activeSessions;
    
    // Category pages
    private static final StrainEffectType.EffectCategory[] CATEGORIES = StrainEffectType.EffectCategory.values();
    
    public EffectSelectorGUI(BudLords plugin, StrainCreatorGUI strainCreatorGUI) {
        this.plugin = plugin;
        this.strainCreatorGUI = strainCreatorGUI;
        this.activeSessions = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @SuppressWarnings("deprecation")
    public void open(Player player) {
        // Check if there's already an active builder for this player
        StrainCreatorGUI.StrainBuilder builder = strainCreatorGUI.getActiveBuilders().get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage("§cNo active strain builder session! Please open the Strain Creator first with /straincreator");
            return;
        }
        
        // Ensure the builder is not awaiting name input (which would mess up the flow)
        if (builder.awaitingName) {
            player.sendMessage("§cPlease finish naming your strain first!");
            return;
        }
        
        EffectSession session = activeSessions.computeIfAbsent(player.getUniqueId(), 
            k -> new EffectSession(builder));
        
        // Update the builder reference in case it was recreated
        session.builder = builder;
        
        Inventory inv = Bukkit.createInventory(this, 54, "§5§l✦ Effect Selector ✦ §7Page " + (session.page + 1));
        updateInventory(inv, session);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
    }
    
    private void updateInventory(Inventory inv, EffectSession session) {
        inv.clear();
        
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
        inv.setItem(4, createItem(Material.BEACON, 
            "§d§l✦ Select Effects ✦",
            Arrays.asList(
                "",
                "§7Add special effects to your strain!",
                "",
                "§7Selected: §e" + session.builder.effects.size() + " effects",
                "§6Admin tool - §e§lUNLIMITED!",
                "",
                "§aLeft-click §7to add/remove effect",
                "§eRight-click §7to adjust intensity"
            )));
        
        // Category filter buttons
        int catSlot = 1;
        for (StrainEffectType.EffectCategory category : CATEGORIES) {
            if (catSlot > 7) break;
            boolean isSelected = session.selectedCategory == category;
            
            ItemStack catItem = createItem(
                isSelected ? Material.GLOWSTONE : Material.GRAY_CONCRETE,
                (isSelected ? "§e§l" : "§7") + category.getDisplayName(),
                Arrays.asList(
                    "",
                    isSelected ? "§aCurrently viewing" : "§7Click to view",
                    "§7effects in this category"
                ));
            inv.setItem(catSlot++, catItem);
        }
        
        // Get effects for current category
        List<StrainEffectType> effectsInCategory = getEffectsForCategory(session.selectedCategory);
        
        // Paginate effects
        int effectsPerPage = 28; // 4 rows of 7
        int startIndex = session.page * effectsPerPage;
        int endIndex = Math.min(startIndex + effectsPerPage, effectsInCategory.size());
        
        // Display effects
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            StrainEffectType effectType = effectsInCategory.get(i);
            
            // Check if already selected
            boolean isSelected = session.builder.hasEffect(effectType);
            StrainEffect existingEffect = null;
            if (isSelected) {
                for (StrainEffect eff : session.builder.effects) {
                    if (eff.getType() == effectType) {
                        existingEffect = eff;
                        break;
                    }
                }
            }
            
            // Create effect item
            Material icon = isSelected ? Material.ENCHANTED_BOOK : effectType.getIconMaterial();
            String name = (isSelected ? "§a✓ " : "") + effectType.getColoredName();
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(effectType.getCategoryColor() + effectType.getSymbol() + " " + effectType.getDescription());
            lore.add("");
            
            if (isSelected && existingEffect != null) {
                lore.add("§eIntensity: " + existingEffect.getIntensityDisplay());
                lore.add("");
                lore.add("§aLeft-click §7to remove");
                lore.add("§eRight-click §7to change intensity");
            } else {
                if (effectType.isLegendary()) {
                    lore.add("§6§lLEGENDARY EFFECT");
                    lore.add("");
                }
                lore.add("§aLeft-click §7to add");
            }
            
            inv.setItem(slot, createItem(icon, name, lore));
            
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2; // Skip borders
        }
        
        // Current selections display
        ItemStack selectionsItem = createItem(Material.WRITABLE_BOOK,
            "§e§lCurrent Effects §7(" + session.builder.effects.size() + " §6UNLIMITED§7)",
            getSelectedEffectsLore(session));
        inv.setItem(49, selectionsItem);
        
        // Navigation
        boolean hasNextPage = endIndex < effectsInCategory.size();
        boolean hasPrevPage = session.page > 0;
        
        if (hasPrevPage) {
            inv.setItem(45, createItem(Material.ARROW, "§a« Previous Page", 
                Arrays.asList("", "§7Go to page " + session.page)));
        }
        
        if (hasNextPage) {
            inv.setItem(53, createItem(Material.ARROW, "§aNext Page »", 
                Arrays.asList("", "§7Go to page " + (session.page + 2))));
        }
        
        // Back button
        inv.setItem(48, createItem(Material.BARRIER, "§c§lBack to Creator",
            Arrays.asList("", "§7Return to the Strain Creator")));
        
        // Clear all button
        if (!session.builder.effects.isEmpty()) {
            inv.setItem(50, createItem(Material.TNT, "§c§lClear All Effects",
                Arrays.asList("", "§7Remove all selected effects")));
        }
        
        // Random effect button (always show for admin)
        inv.setItem(51, createItem(Material.ENDER_EYE, "§d§lRandom Effect!",
            Arrays.asList("", "§7Add a random effect", "§7Rarer effects have lower chance!")));
    }
    
    private List<String> getSelectedEffectsLore(EffectSession session) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        if (session.builder.effects.isEmpty()) {
            lore.add("§7No effects selected");
        } else {
            for (StrainEffect effect : session.builder.effects) {
                lore.add(effect.getLoreDisplay());
            }
        }
        
        return lore;
    }
    
    private List<StrainEffectType> getEffectsForCategory(StrainEffectType.EffectCategory category) {
        List<StrainEffectType> effects = new ArrayList<>();
        for (StrainEffectType type : StrainEffectType.values()) {
            if (type.getCategory() == category) {
                effects.add(type);
            }
        }
        return effects;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EffectSelectorGUI)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        event.setCancelled(true);
        
        EffectSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;
        
        int slot = event.getRawSlot();
        boolean isRightClick = event.isRightClick();
        
        // Category selection (slots 1-7)
        if (slot >= 1 && slot <= 7) {
            int catIndex = slot - 1;
            if (catIndex < CATEGORIES.length) {
                session.selectedCategory = CATEGORIES[catIndex];
                session.page = 0;
                updateInventory(event.getInventory(), session);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
            return;
        }
        
        // Back button
        if (slot == 48) {
            player.closeInventory();
            strainCreatorGUI.reopenForPlayer(player);
            return;
        }
        
        // Clear all
        if (slot == 50 && !session.builder.effects.isEmpty()) {
            session.builder.effects.clear();
            updateInventory(event.getInventory(), session);
            player.sendMessage("§cCleared all effects!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
            return;
        }
        
        // Random effect
        if (slot == 51 && session.builder.effects.size() < Strain.MAX_EFFECTS) {
            StrainEffectType randomEffect = getRandomEffect(session);
            if (randomEffect != null) {
                int randomIntensity = 2 + (int) (Math.random() * 3); // 2-4
                session.builder.addEffect(randomEffect, randomIntensity);
                updateInventory(event.getInventory(), session);
                player.sendMessage("§d✦ Added random effect: " + randomEffect.getColoredName());
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            } else {
                player.sendMessage("§cNo more effects available!");
            }
            return;
        }
        
        // Pagination
        if (slot == 45 && session.page > 0) {
            session.page--;
            updateInventory(event.getInventory(), session);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        List<StrainEffectType> effectsInCategory = getEffectsForCategory(session.selectedCategory);
        int effectsPerPage = 28;
        if (slot == 53 && (session.page + 1) * effectsPerPage < effectsInCategory.size()) {
            session.page++;
            updateInventory(event.getInventory(), session);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        // Effect selection (main grid)
        if (isEffectSlot(slot)) {
            int effectIndex = getEffectIndexFromSlot(slot, session.page);
            
            if (effectIndex >= 0 && effectIndex < effectsInCategory.size()) {
                StrainEffectType effectType = effectsInCategory.get(effectIndex);
                
                if (session.builder.hasEffect(effectType)) {
                    if (isRightClick) {
                        // Cycle intensity
                        for (StrainEffect eff : session.builder.effects) {
                            if (eff.getType() == effectType) {
                                int newIntensity = (eff.getIntensity() % 5) + 1;
                                eff.setIntensity(newIntensity);
                                player.sendMessage("§eSet " + effectType.getDisplayName() + " intensity to " + newIntensity);
                                break;
                            }
                        }
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                    } else {
                        // Remove effect
                        session.builder.removeEffect(effectType);
                        player.sendMessage("§cRemoved: " + effectType.getDisplayName());
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    }
                } else {
                    // Add effect - NO LIMIT for admin strains!
                    session.builder.addEffect(effectType, 3);
                    player.sendMessage("§aAdded: " + effectType.getColoredName() + " §7(Total: " + session.builder.effects.size() + ")");
                    
                    if (effectType.isLegendary()) {
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.0f);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                    }
                }
                
                updateInventory(event.getInventory(), session);
            }
        }
    }
    
    private boolean isEffectSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row >= 1 && row <= 4 && col >= 1 && col <= 7;
    }
    
    private int getEffectIndexFromSlot(int slot, int page) {
        int row = slot / 9 - 1; // 0-3
        int col = slot % 9 - 1; // 0-6
        
        if (row < 0 || col < 0 || col > 6 || row > 3) return -1;
        
        int indexOnPage = row * 7 + col;
        return page * 28 + indexOnPage;
    }
    
    private StrainEffectType getRandomEffect(EffectSession session) {
        // Build weighted list
        List<StrainEffectType> available = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int totalWeight = 0;
        
        for (StrainEffectType type : StrainEffectType.values()) {
            if (!session.builder.hasEffect(type)) {
                available.add(type);
                weights.add(type.getRarityWeight());
                totalWeight += type.getRarityWeight();
            }
        }
        
        if (available.isEmpty()) return null;
        
        // Weighted random selection
        int roll = (int) (Math.random() * totalWeight);
        int cumulative = 0;
        for (int i = 0; i < available.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) {
                return available.get(i);
            }
        }
        
        return available.get(available.size() - 1);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof EffectSelectorGUI)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // Keep session for reopening
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
    
    /**
     * Session data for effect selection.
     */
    private static class EffectSession {
        StrainCreatorGUI.StrainBuilder builder;
        StrainEffectType.EffectCategory selectedCategory = StrainEffectType.EffectCategory.VISUAL;
        int page = 0;
        
        EffectSession(StrainCreatorGUI.StrainBuilder builder) {
            this.builder = builder;
        }
    }
}
