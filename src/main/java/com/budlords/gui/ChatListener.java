package com.budlords.gui;

import com.budlords.BudLords;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final BudLords plugin;
    private final Player player;
    private final StrainCreatorGUI.StrainBuilder builder;
    private final StrainCreatorGUI gui;

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

        event.setCancelled(true);
        String newName = event.getMessage().trim();
        
        if (newName.length() < 2 || newName.length() > 32) {
            player.sendMessage("§cName must be between 2 and 32 characters!");
            return;
        }

        builder.name = newName;
        builder.awaitingName = false;
        player.sendMessage("§aStrain name set to: §f" + newName);

        // Re-open GUI on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            gui.reopenForPlayer(player);
            HandlerList.unregisterAll(this);
        });
    }
}
