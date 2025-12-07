package com.budlords.listeners;

import com.budlords.BudLords;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener for entity-related events.
 * Handles dynamic buyer profile generation on entity spawn.
 */
public class EntityListener implements Listener {
    
    private final BudLords plugin;
    
    public EntityListener(BudLords plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Generates dynamic buyer profiles for newly spawned entities.
     * Uses a delay to ensure the entity is fully initialized.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        // Skip if event was cancelled or dynamic buyer manager is not available
        if (event.isCancelled() || plugin.getDynamicBuyerManager() == null) {
            return;
        }
        
        Entity entity = event.getEntity();
        
        // Check if this entity type can become a buyer
        if (!plugin.getDynamicBuyerManager().canEntityBeBuyer(entity)) {
            return;
        }
        
        // Delay buyer generation to next tick to ensure entity is fully spawned
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead()) {
                    return;
                }
                
                // Attempt to create a buyer profile for this entity
                // This has a chance to succeed based on config
                plugin.getDynamicBuyerManager().getOrCreateBuyer(entity);
            }
        }.runTaskLater(plugin, 1L);
    }
}
