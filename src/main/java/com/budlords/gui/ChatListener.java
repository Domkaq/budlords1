package com.budlords.gui;

import com.budlords.BudLords;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    private final BudLords plugin;
    private final Player player;
    private final StrainCreatorGUI.StrainBuilder builder;
    private final StrainCreatorGUI gui;
    private boolean cancelled = false;

    public ChatListener(BudLords plugin, Player player, StrainCreatorGUI.StrainBuilder builder, StrainCreatorGUI gui) {
        this.plugin = plugin;
        this.player = player;
        this.builder = builder;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (!builder.awaitingName) return;
        if (cancelled) return;

        event.setCancelled(true);
        String newName = event.getMessage().trim();
        
        // Check for cancel command
        if (newName.equalsIgnoreCase("cancel")) {
            cancelled = true;
            builder.awaitingName = false;
            player.sendMessage("§c✗ Name change cancelled.");
            
            // Return to GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                gui.reopenForPlayer(player);
                HandlerList.unregisterAll(this);
            });
            return;
        }
        
        // Validate name length
        if (newName.length() < 2 || newName.length() > 32) {
            player.sendMessage("§c✗ Name must be between 2 and 32 characters!");
            player.sendMessage("§7Type a new name, or type §fcancel §7to go back.");
            return;
        }
        
        // Validate name characters (basic alphanumeric + spaces)
        if (!newName.matches("^[a-zA-Z0-9 ]+$")) {
            player.sendMessage("§c✗ Name can only contain letters, numbers, and spaces!");
            player.sendMessage("§7Type a new name, or type §fcancel §7to go back.");
            return;
        }

        builder.name = newName;
        builder.awaitingName = false;
        
        player.sendMessage("");
        player.sendMessage("§a✓ Strain name set to: §f" + newName);
        player.sendMessage("§7Returning to the Creator GUI...");
        player.sendMessage("");

        // Re-open GUI on main thread (this is the fix for the name entry flow)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            gui.reopenForPlayer(player);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.2f);
            HandlerList.unregisterAll(this);
        });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().equals(player)) {
            builder.awaitingName = false;
            gui.getActiveBuilders().remove(player.getUniqueId());
            HandlerList.unregisterAll(this);
        }
    }
}
