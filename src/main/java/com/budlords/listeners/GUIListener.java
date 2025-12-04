package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.achievements.AchievementManager;
import com.budlords.challenges.ChallengeManager;
import com.budlords.collections.CollectionManager;
import com.budlords.crossbreed.CrossbreedManager;
import com.budlords.prestige.PrestigeManager;
import com.budlords.skills.SkillManager;
import com.budlords.skills.Skill;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles click events for the new GUI systems:
 * - Prestige Menu
 * - Challenge Menu
 * - Crossbreeding Lab
 * - Achievements
 * - Skill Tree
 * - Collection Book
 */
public class GUIListener implements Listener {

    private final BudLords plugin;

    public GUIListener(BudLords plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() == null) return;
        
        // Handle Prestige GUI
        if (event.getInventory().getHolder() instanceof PrestigeManager) {
            event.setCancelled(true);
            plugin.getPrestigeManager().handlePrestigeClick(player, event.getRawSlot());
            return;
        }
        
        // Handle Challenge GUI
        if (event.getInventory().getHolder() instanceof ChallengeManager) {
            event.setCancelled(true);
            plugin.getChallengeManager().handleChallengeClick(player, event.getRawSlot());
            return;
        }
        
        // Handle Achievement GUI - prevent item extraction
        if (event.getInventory().getHolder() instanceof AchievementManager) {
            event.setCancelled(true);
            // Handle category switching (slots 46-52)
            int slot = event.getRawSlot();
            if (slot >= 46 && slot <= 52) {
                int categoryIndex = slot - 46;
                com.budlords.achievements.Achievement.AchievementCategory[] categories = 
                    com.budlords.achievements.Achievement.AchievementCategory.values();
                if (categoryIndex < categories.length) {
                    plugin.getAchievementManager().openAchievementsGUI(player, categories[categoryIndex]);
                }
            }
            return;
        }
        
        // Handle Skill Tree GUI - prevent item extraction and handle skill unlocking
        if (event.getInventory().getHolder() instanceof SkillManager) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            // Handle tree selection at bottom (slots 46-50)
            if (slot >= 46 && slot <= 50) {
                int treeIndex = slot - 46;
                Skill.SkillTree[] trees = Skill.SkillTree.values();
                if (treeIndex < trees.length) {
                    plugin.getSkillManager().openSkillTreeGUI(player, trees[treeIndex]);
                }
                return;
            }
            
            // Handle skill unlocking at tier slots
            // Tier 1: slots 19, 22, 25
            // Tier 2: slots 28, 31, 34
            // Tier 3: slots 37, 40
            int[][] tierSlots = {
                {19, 22, 25},  // Tier 1
                {28, 31, 34},  // Tier 2
                {37, 40, -1}   // Tier 3
            };
            
            // Determine which tree is currently viewed from title
            String title = event.getView().getTitle();
            Skill.SkillTree currentTree = null;
            for (Skill.SkillTree tree : Skill.SkillTree.values()) {
                if (title.contains(tree.getDisplayName())) {
                    currentTree = tree;
                    break;
                }
            }
            
            if (currentTree != null) {
                // Find which skill was clicked
                for (int tier = 1; tier <= 3; tier++) {
                    int[] slots = tierSlots[tier - 1];
                    for (int i = 0; i < slots.length && slots[i] >= 0; i++) {
                        if (slot == slots[i]) {
                            // Find the skill at this position
                            final Skill.SkillTree finalTree = currentTree;
                            final int finalTier = tier;
                            java.util.List<Skill> tierSkills = java.util.Arrays.stream(Skill.values())
                                .filter(s -> s.getTree() == finalTree && s.getTier() == finalTier)
                                .toList();
                            if (i < tierSkills.size()) {
                                Skill skill = tierSkills.get(i);
                                if (plugin.getSkillManager().unlockSkill(player, skill)) {
                                    // Refresh the GUI
                                    plugin.getSkillManager().openSkillTreeGUI(player, currentTree);
                                }
                            }
                            return;
                        }
                    }
                }
            }
            return;
        }
        
        // Handle Collection Book GUI - prevent item extraction and handle pagination
        if (event.getInventory().getHolder() instanceof CollectionManager) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            // Handle navigation (Previous: 47, Next: 51)
            String title = event.getView().getTitle();
            int currentPage = 0;
            if (title.contains("Page ")) {
                try {
                    int pageStart = title.indexOf("Page ") + 5;
                    currentPage = Integer.parseInt(title.substring(pageStart)) - 1;
                } catch (NumberFormatException ignored) {}
            }
            
            if (slot == 47 && currentPage > 0) {
                // Previous page
                plugin.getCollectionManager().openCollectionGUI(player, currentPage - 1);
            } else if (slot == 51) {
                // Next page
                plugin.getCollectionManager().openCollectionGUI(player, currentPage + 1);
            }
            return;
        }
        
        // Handle Crossbreed GUI
        if (event.getInventory().getHolder() instanceof CrossbreedManager) {
            int slot = event.getRawSlot();
            
            // Allow clicking in player inventory to pick up seeds
            if (slot >= 45) {
                // Player clicked in their own inventory
                // Check if they're clicking a seed to then drop on parent slots
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && plugin.getStrainManager().isSeedItem(clicked)) {
                    // Allow picking up seeds from player inventory
                    return; // Don't cancel this - let them pick up the seed
                }
            }
            
            // Check if it's a seed on cursor being placed on parent slots
            if ((slot == 20 || slot == 22) && event.getCursor() != null && !event.getCursor().getType().isAir()) {
                ItemStack cursor = event.getCursor();
                if (plugin.getStrainManager().isSeedItem(cursor)) {
                    event.setCancelled(true);
                    plugin.getCrossbreedManager().handleSeedDrop(player, cursor, slot);
                    return;
                }
            }
            
            // Check if clicking parent slot with a seed in hand (current item)
            if ((slot == 20 || slot == 22) && event.getClick() == ClickType.LEFT) {
                ItemStack currentItem = event.getCurrentItem();
                // Check if there's a seed in the player's cursor or if they clicked holding shift with a seed
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (currentItem == null || currentItem.getType().isAir()) {
                    // Empty slot - check if player has seed in cursor
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && plugin.getStrainManager().isSeedItem(cursor)) {
                        event.setCancelled(true);
                        plugin.getCrossbreedManager().handleSeedDrop(player, cursor, slot);
                        return;
                    }
                }
            }
            
            event.setCancelled(true);
            plugin.getCrossbreedManager().handleCrossbreedClick(player, slot);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Any cleanup needed when closing GUIs can go here
    }
}
