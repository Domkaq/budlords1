package com.budlords.commands;

import com.budlords.npc.NPCManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnMarketCommand implements CommandExecutor, TabCompleter {

    private final NPCManager npcManager;

    public SpawnMarketCommand(NPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!player.hasPermission("budlords.admin")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        npcManager.spawnMarketJoe(player.getLocation());
        player.sendMessage("§aSpawned Market Joe at your location!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
