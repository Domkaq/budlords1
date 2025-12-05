package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.joint.JointItems;
import com.budlords.joint.JointRollingManager;
import com.budlords.packaging.DroppedBudTracker;
import com.budlords.packaging.PackagedProduct;
import com.budlords.packaging.PackItems;
import com.budlords.packaging.PackagingManager;
import com.budlords.quality.StarRating;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.UUID;

/**
 * Listener for the drag-and-drop packaging system and joint rolling interactions.
 * 
 * Packaging flow:
 * 1. Player drops buds on ground (tracked)
 * 2. Player drops pack on the buds
 * 3. System combines them into packaged product
 * 
 * Joint rolling flow:
 * 1. Player right-clicks with grinded bud while holding paper and tobacco
 * 2. Opens the minigame GUI
 */
public class ItemDropListener implements Listener {

    private final BudLords plugin;
    private final StrainManager strainManager;
    private final PackagingManager packagingManager;
    private final DroppedBudTracker budTracker;
    private final JointRollingManager jointRollingManager;

    public ItemDropListener(BudLords plugin, StrainManager strainManager, 
                            PackagingManager packagingManager, 
                            DroppedBudTracker budTracker,
                            JointRollingManager jointRollingManager) {
        this.plugin = plugin;
        this.strainManager = strainManager;
        this.packagingManager = packagingManager;
        this.budTracker = budTracker;
        this.jointRollingManager = jointRollingManager;
    }

    /**
     * Track when players drop bud items.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item itemEntity = event.getItemDrop();
        ItemStack item = itemEntity.getItemStack();

        // Check if it's a bud item
        if (strainManager.isBudItem(item)) {
            String strainId = strainManager.getStrainIdFromItem(item);
            StarRating rating = strainManager.getBudStarRating(item);
            
            if (strainId != null) {
                budTracker.trackBud(itemEntity, strainId, item.getAmount(), 
                    rating != null ? rating : StarRating.ONE_STAR, player.getUniqueId());
                
                // Visual feedback
                player.sendMessage("§7Dropped §a" + item.getAmount() + "x §7buds. Drop a pack on them to package!");
                
                // Highlight effect
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (itemEntity.isValid()) {
                        itemEntity.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, 
                            itemEntity.getLocation().add(0, 0.5, 0), 
                            10, 0.3, 0.2, 0.3, 0);
                    }
                }, 5L);
            }
            return;
        }

        // Check if it's a pack item
        if (PackItems.isPack(item)) {
            int packGrams = PackItems.getPackGrams(item);
            
            // Schedule check for nearby buds (after item lands)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!itemEntity.isValid()) return;
                
                tryPackageNearbyBuds(player, itemEntity, packGrams);
            }, 10L); // Half second delay for item to land
        }
    }

    /**
     * Attempts to package nearby dropped buds with the pack.
     */
    private void tryPackageNearbyBuds(Player player, Item packEntity, int packGrams) {
        Location packLoc = packEntity.getLocation();
        
        // Find nearby bud items
        Collection<Entity> nearby = packLoc.getWorld().getNearbyEntities(packLoc, 1.5, 1.5, 1.5, 
            e -> e instanceof Item);
        
        for (Entity entity : nearby) {
            if (entity.equals(packEntity)) continue;
            if (!(entity instanceof Item budEntity)) continue;
            
            ItemStack budItem = budEntity.getItemStack();
            if (!strainManager.isBudItem(budItem)) continue;
            
            int budAmount = budItem.getAmount();
            String strainId = strainManager.getStrainIdFromItem(budItem);
            StarRating budRating = strainManager.getBudStarRating(budItem);
            
            if (strainId == null) continue;
            
            // Check if we have enough buds
            if (budAmount >= packGrams) {
                Strain strain = strainManager.getStrain(strainId);
                if (strain == null) continue;
                
                // Success! Package the buds
                PackagedProduct.WeightType weightType = PackagedProduct.WeightType.fromGrams(packGrams);
                if (weightType == null) continue;
                
                // Create the packaged item
                ItemStack packagedItem = packagingManager.createPackagedItem(strain, weightType);
                
                // Remove used items
                if (budAmount == packGrams) {
                    budEntity.remove();
                } else {
                    budItem.setAmount(budAmount - packGrams);
                }
                packEntity.remove();
                
                // Spawn the packaged item
                Item newItem = packLoc.getWorld().dropItemNaturally(packLoc, packagedItem);
                
                // Visual effects
                packLoc.getWorld().spawnParticle(Particle.TOTEM, 
                    packLoc.add(0, 0.5, 0), 
                    20, 0.3, 0.3, 0.3, 0.1);
                packLoc.getWorld().playSound(packLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                
                // Notify player
                player.sendMessage("");
                player.sendMessage("§a§l✦ PACKAGED! §7" + packGrams + "g of §f" + strain.getName());
                player.sendMessage("§7Pick up your packaged product!");
                player.sendMessage("");
                
                // Untrack the bud
                budTracker.untrackBud(budEntity.getUniqueId());
                
                return; // Only package one set at a time
            } else {
                // Not enough buds
                player.sendMessage("§cNot enough buds! Need §e" + packGrams + "§c, found §e" + budAmount);
            }
        }
        
        // No suitable buds found
        player.sendMessage("§7No buds nearby to package. Drop buds first, then drop the pack on them!");
    }

    /**
     * Prevent tracked buds from merging with other items.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemMerge(ItemMergeEvent event) {
        UUID entityId = event.getEntity().getUniqueId();
        UUID targetId = event.getTarget().getUniqueId();
        
        // Don't merge if either is a tracked bud
        if (budTracker.getTrackedBud(entityId) != null || 
            budTracker.getTrackedBud(targetId) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle right-click interactions for joint rolling and grinding.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Check for grinder + buds interaction
        if (JointItems.isGrinder(mainHand) && strainManager.isBudItem(offHand)) {
            event.setCancelled(true);
            handleGrinding(player, mainHand, offHand);
            return;
        }
        if (strainManager.isBudItem(mainHand) && JointItems.isGrinder(offHand)) {
            event.setCancelled(true);
            handleGrinding(player, offHand, mainHand);
            return;
        }

        // Check for joint rolling initiation (grinded bud + paper + tobacco in inventory)
        if (JointItems.isGrindedBud(mainHand)) {
            event.setCancelled(true);
            tryStartJointRolling(player, mainHand);
        }
    }
    
    /**
     * Prevent grinder from being placed as a block.
     * The BlockPlaceEvent is the most reliable way to prevent block placement.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (JointItems.isGrinder(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou can't place a grinder! Use it with buds to grind them.");
        }
    }

    /**
     * Handles grinding buds into grinded bud.
     */
    private void handleGrinding(Player player, ItemStack grinder, ItemStack buds) {
        StarRating grinderRating = JointItems.getGrinderRating(grinder);
        String strainId = strainManager.getStrainIdFromItem(buds);
        StarRating budRating = strainManager.getBudStarRating(buds);
        
        if (strainId == null) {
            player.sendMessage("§cInvalid bud item!");
            return;
        }
        
        Strain strain = strainManager.getStrain(strainId);
        if (strain == null) {
            player.sendMessage("§cStrain not found!");
            return;
        }
        
        if (grinderRating == null) grinderRating = StarRating.ONE_STAR;
        if (budRating == null) budRating = StarRating.ONE_STAR;
        
        // Calculate final rating (average of grinder and bud, with small boost)
        int combinedStars = (int) Math.round((grinderRating.getStars() + budRating.getStars() + 1) / 2.0);
        StarRating finalRating = StarRating.fromValue(combinedStars);
        
        // Create grinded bud
        ItemStack grindedBud = JointItems.createGrindedBud(strainId, strain.getName(), finalRating, 1);
        
        // Consume 1 bud
        buds.setAmount(buds.getAmount() - 1);
        
        // Give grinded bud
        player.getInventory().addItem(grindedBud);
        
        // Effects
        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.5f, 1.2f);
        player.spawnParticle(Particle.FALLING_DUST, player.getLocation().add(0, 1.5, 0), 
            10, 0.3, 0.2, 0.3, Material.SAND.createBlockData());
        
        player.sendMessage("§a✦ Ground 1 " + strain.getName() + " bud into " + finalRating.getDisplay() + " §agrinded bud!");
    }

    /**
     * Tries to start joint rolling if player has all required items.
     */
    private void tryStartJointRolling(Player player, ItemStack grindedBud) {
        // Check for rolling paper in inventory
        ItemStack rollingPaper = findItemInInventory(player, JointItems::isRollingPaper);
        if (rollingPaper == null) {
            player.sendMessage("§cYou need Rolling Paper to roll a joint!");
            player.sendMessage("§7Buy from Market Joe or craft it.");
            return;
        }
        
        // Check for tobacco in inventory
        ItemStack tobacco = findItemInInventory(player, JointItems::isTobacco);
        if (tobacco == null) {
            player.sendMessage("§cYou need Tobacco to roll a joint!");
            player.sendMessage("§7Buy from Market Joe or find it.");
            return;
        }
        
        // Start the rolling minigame
        jointRollingManager.startRolling(player, grindedBud, rollingPaper, tobacco);
    }

    /**
     * Finds an item in player's inventory matching the predicate.
     */
    private ItemStack findItemInInventory(Player player, java.util.function.Predicate<ItemStack> predicate) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Handle joint rolling minigame clicks.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check if it's the joint rolling GUI
        if (event.getInventory().getHolder() instanceof JointRollingManager) {
            event.setCancelled(true);
            
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < event.getInventory().getSize()) {
                jointRollingManager.handleMinigameClick(player, slot);
            }
        }
    }
    
    /**
     * Handle joint rolling session cleanup when inventory is closed.
     * This ensures players can roll again after closing the GUI.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // Check if it's the joint rolling GUI
        if (event.getInventory().getHolder() instanceof JointRollingManager) {
            jointRollingManager.handleInventoryClose(player);
        }
    }
    
    /**
     * Handle joint rolling session cleanup when player dies.
     * This ensures players can roll again after death.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (jointRollingManager.hasActiveSession(player)) {
            jointRollingManager.forceCleanup(player.getUniqueId());
        }
    }
    
    /**
     * Handle joint rolling session cleanup when player quits.
     * This ensures no lingering sessions in memory.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (jointRollingManager.hasActiveSession(player)) {
            jointRollingManager.forceCleanup(player.getUniqueId());
        }
    }
}
