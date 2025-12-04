package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.skills.Skill;
import com.budlords.skills.SkillManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for viewing and managing skills in BudLords v2.0.0.
 */
public class SkillsCommand implements CommandExecutor, TabCompleter {

    private final BudLords plugin;

    public SkillsCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        SkillManager skillManager = plugin.getSkillManager();
        if (skillManager == null) {
            sender.sendMessage("§cSkill system is not enabled!");
            return true;
        }

        // Determine skill tree to view
        Skill.SkillTree tree = Skill.SkillTree.FARMING; // Default
        
        if (args.length > 0) {
            try {
                tree = Skill.SkillTree.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid skill tree! Use: farming, quality, trading, genetics, effects");
                return true;
            }
        }

        skillManager.openSkillTreeGUI(player, tree);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Skill.SkillTree.values())
                .map(t -> t.name().toLowerCase())
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
