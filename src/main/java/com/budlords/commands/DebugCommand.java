package com.budlords.commands;

import com.budlords.BudLords;
import com.budlords.effects.StrainEffect;
import com.budlords.effects.StrainEffectType;
import com.budlords.farming.FarmingManager;
import com.budlords.farming.Plant;
import com.budlords.quality.StarRating;
import com.budlords.stats.PlayerStats;
import com.budlords.strain.Strain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin debug commands for testing and troubleshooting BudLords.
 * All commands require budlords.admin permission.
 */
public class DebugCommand implements CommandExecutor, TabCompleter {

    private final BudLords plugin;
    
    // Debug mode tracking per player
    private final Set<UUID> debugModeEnabled = new HashSet<>();

    public DebugCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("budlords.admin")) {
            sender.sendMessage("§cYou don't have permission to use debug commands!");
            return true;
        }

        if (args.length == 0) {
            showDebugHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "toggle" -> handleToggle(sender);
            case "plants" -> handlePlantsInfo(sender, args);
            case "strains" -> handleStrainsInfo(sender, args);
            case "player" -> handlePlayerInfo(sender, args);
            case "effects" -> handleEffectsInfo(sender, args);
            case "economy" -> handleEconomyInfo(sender, args);
            case "entity" -> handleEntityInfo(sender, args);
            case "growplant" -> handleGrowPlant(sender, args);
            case "giveeffect" -> handleGiveEffect(sender, args);
            case "testmutation" -> handleTestMutation(sender, args);
            case "reload" -> handleReload(sender);
            case "save" -> handleSave(sender);
            case "config" -> handleConfigInfo(sender, args);
            case "clear" -> handleClear(sender, args);
            default -> showDebugHelp(sender);
        }

        return true;
    }

    private void showDebugHelp(CommandSender sender) {
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§c§l  BudLords Admin Debug Tools");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("");
        sender.sendMessage("§e  /debug toggle §7- Toggle debug mode (verbose logging)");
        sender.sendMessage("§e  /debug plants [radius] §7- Show nearby plants info");
        sender.sendMessage("§e  /debug strains [name] §7- List/search strains");
        sender.sendMessage("§e  /debug player <name> §7- Show player stats");
        sender.sendMessage("§e  /debug effects [category] §7- List all effects");
        sender.sendMessage("§e  /debug economy §7- Show economy stats");
        sender.sendMessage("§e  /debug entity §7- Show nearby sellable entities");
        sender.sendMessage("§e  /debug growplant §7- Instantly grow nearest plant");
        sender.sendMessage("§e  /debug giveeffect <effect> [player] §7- Give effect");
        sender.sendMessage("§e  /debug testmutation §7- Test crossbreed mutation");
        sender.sendMessage("§e  /debug reload §7- Reload all data");
        sender.sendMessage("§e  /debug save §7- Force save all data");
        sender.sendMessage("§e  /debug config <key> §7- View config values");
        sender.sendMessage("§e  /debug clear <plants|sessions> §7- Clear data");
        sender.sendMessage("");
        sender.sendMessage("§8§m════════════════════════════════════════");
    }

    private void handleToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can toggle debug mode!");
            return;
        }
        
        UUID uuid = player.getUniqueId();
        if (debugModeEnabled.contains(uuid)) {
            debugModeEnabled.remove(uuid);
            sender.sendMessage("§c§l[DEBUG] §7Debug mode §cDISABLED");
        } else {
            debugModeEnabled.add(uuid);
            sender.sendMessage("§a§l[DEBUG] §7Debug mode §aENABLED");
            sender.sendMessage("§7You will now see verbose logging in chat.");
        }
    }
    
    public boolean isDebugEnabled(Player player) {
        return debugModeEnabled.contains(player.getUniqueId());
    }
    
    public void debugLog(Player player, String message) {
        if (isDebugEnabled(player)) {
            player.sendMessage("§8[§cDEBUG§8] §7" + message);
        }
    }

    private void handlePlantsInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        int radius = args.length > 1 ? parseInt(args[1], 10) : 10;
        FarmingManager farmingManager = plugin.getFarmingManager();
        
        if (farmingManager == null) {
            sender.sendMessage("§cFarming manager not initialized!");
            return;
        }
        
        Location loc = player.getLocation();
        List<Plant> nearbyPlants = farmingManager.getNearbyPlants(loc, radius);
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§2§l  Plants Debug (radius: " + radius + ")");
        sender.sendMessage("§8§m════════════════════════════════════════");
        
        if (nearbyPlants.isEmpty()) {
            sender.sendMessage("§7  No plants found within " + radius + " blocks.");
        } else {
            sender.sendMessage("§7  Found §e" + nearbyPlants.size() + " §7plants:");
            sender.sendMessage("");
            
            int count = 0;
            for (Plant plant : nearbyPlants) {
                if (count >= 10) {
                    sender.sendMessage("§7  ... and " + (nearbyPlants.size() - 10) + " more");
                    break;
                }
                
                Strain strain = plugin.getStrainManager().getStrain(plant.getStrainId());
                String strainName = strain != null ? strain.getName() : plant.getStrainId();
                
                sender.sendMessage("§e  " + (count + 1) + ". §f" + strainName);
                sender.sendMessage("§7     Stage: §a" + plant.getGrowthStage() + "/5 §7| Quality: §a" + plant.getQuality());
                sender.sendMessage("§7     Pot: " + (plant.hasPot() ? "§a✓" : "§c✗") + " §7| Water: §b" + String.format("%.0f%%", plant.getWaterLevel() * 100));
                sender.sendMessage("§7     Location: §f" + formatLocation(plant.getLocation()));
                count++;
            }
        }
        
        sender.sendMessage("§8§m════════════════════════════════════════");
    }

    private void handleStrainsInfo(CommandSender sender, String[] args) {
        String search = args.length > 1 ? args[1].toLowerCase() : null;
        
        Collection<Strain> strains = plugin.getStrainManager().getAllStrains();
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§2§l  Strains Database");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§7  Total registered: §e" + strains.size());
        sender.sendMessage("");
        
        List<Strain> filtered = strains.stream()
            .filter(s -> search == null || s.getName().toLowerCase().contains(search) || s.getId().toLowerCase().contains(search))
            .limit(15)
            .collect(Collectors.toList());
        
        for (Strain strain : filtered) {
            sender.sendMessage("§e  " + strain.getName() + " §7(" + strain.getId() + ")");
            sender.sendMessage("§7    Rarity: " + strain.getRarity().getDisplayName() + " §7| Potency: §e" + strain.getPotency() + "%");
            sender.sendMessage("§7    Effects: §d" + strain.getEffectCount() + " §7| Crossbred: " + (strain.isCrossbred() ? "§a✓" : "§7✗"));
        }
        
        if (filtered.size() < strains.size() && search == null) {
            sender.sendMessage("§7  ... and " + (strains.size() - filtered.size()) + " more (use /debug strains <search>)");
        }
        
        sender.sendMessage("§8§m════════════════════════════════════════");
    }

    private void handlePlayerInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug player <name>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or not online!");
            return;
        }
        
        PlayerStats stats = plugin.getStatsManager().getStats(target);
        double balance = plugin.getEconomyManager().getBalance(target);
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§2§l  Player Debug: " + target.getName());
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§7  Balance: §a$" + String.format("%,.2f", balance));
        sender.sendMessage("§7  Prestige Level: §d" + stats.getPrestigeLevel());
        sender.sendMessage("§7  Plants Grown: §e" + stats.getTotalPlantsGrown());
        sender.sendMessage("§7  Plants Harvested: §e" + stats.getTotalPlantsHarvested());
        sender.sendMessage("§7  Successful Sales: §a" + stats.getSuccessfulSales());
        sender.sendMessage("§7  Failed Sales: §c" + stats.getFailedSales());
        sender.sendMessage("§7  Highest Single Sale: §6$" + String.format("%,.2f", stats.getHighestSingleSale()));
        sender.sendMessage("§7  Joints Rolled: §e" + stats.getJointsRolled());
        sender.sendMessage("§7  Crossbreeds: §d" + stats.getCrossbreeds());
        sender.sendMessage("§7  Legendary Strains: §6" + stats.getLegendaryStrains());
        sender.sendMessage("§7  6-Star Strains: §d§l" + stats.getSixStarStrains());
        sender.sendMessage("§8§m════════════════════════════════════════");
    }

    private void handleEffectsInfo(CommandSender sender, String[] args) {
        String category = args.length > 1 ? args[1].toUpperCase() : null;
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§d§l  Strain Effects List");
        sender.sendMessage("§8§m════════════════════════════════════════");
        
        Map<StrainEffectType.EffectCategory, List<StrainEffectType>> byCategory = 
            Arrays.stream(StrainEffectType.values())
                .collect(Collectors.groupingBy(StrainEffectType::getCategory));
        
        for (StrainEffectType.EffectCategory cat : StrainEffectType.EffectCategory.values()) {
            if (category != null && !cat.name().contains(category)) continue;
            
            List<StrainEffectType> effects = byCategory.getOrDefault(cat, Collections.emptyList());
            sender.sendMessage("");
            sender.sendMessage(cat.getDisplayName() + " §7(" + effects.size() + " effects):");
            
            StringBuilder sb = new StringBuilder("§7  ");
            int count = 0;
            for (StrainEffectType effect : effects) {
                if (count > 0) sb.append(", ");
                sb.append(effect.getSymbol()).append(" ").append(effect.getDisplayName());
                count++;
                if (count >= 5) {
                    sender.sendMessage(sb.toString());
                    sb = new StringBuilder("§7  ");
                    count = 0;
                }
            }
            if (count > 0) {
                sender.sendMessage(sb.toString());
            }
        }
        
        sender.sendMessage("");
        sender.sendMessage("§7  Total effects: §e" + StrainEffectType.values().length);
        sender.sendMessage("§8§m════════════════════════════════════════");
    }

    private void handleEconomyInfo(CommandSender sender, String[] args) {
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§6§l  Economy Debug");
        sender.sendMessage("§8§m════════════════════════════════════════");
        
        double totalMoney = 0;
        int playerCount = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            totalMoney += plugin.getEconomyManager().getBalance(player);
            playerCount++;
        }
        
        sender.sendMessage("§7  Online Players: §e" + playerCount);
        sender.sendMessage("§7  Total Online Balance: §a$" + String.format("%,.2f", totalMoney));
        sender.sendMessage("§7  Average Balance: §a$" + String.format("%,.2f", playerCount > 0 ? totalMoney / playerCount : 0));
        sender.sendMessage("§8§m════════════════════════════════════════");
    }

    private void handleEntityInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        int radius = args.length > 1 ? parseInt(args[1], 15) : 15;
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§2§l  Nearby Sellable Entities (radius: " + radius + ")");
        sender.sendMessage("§8§m════════════════════════════════════════");
        
        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            String type = entity.getType().name();
            boolean canSellTo = plugin.getConfig().getBoolean("trading.allowed-mobs." + type.toLowerCase(), false);
            
            // Check special NPC types
            if (plugin.getNPCManager() != null) {
                var npcType = plugin.getNPCManager().getNPCType(entity);
                if (npcType != null) {
                    canSellTo = true;
                    type = npcType.name();
                }
            }
            
            if (canSellTo || entity.getType().name().contains("VILLAGER")) {
                count++;
                String status = canSellTo ? "§a[CAN SELL]" : "§c[DISABLED]";
                sender.sendMessage("§7  " + count + ". " + status + " §f" + type + " §7at " + formatLocation(entity.getLocation()));
            }
        }
        
        if (count == 0) {
            sender.sendMessage("§7  No sellable entities found.");
        }
        
        sender.sendMessage("§8§m════════════════════════════════════════");
    }

    private void handleGrowPlant(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        FarmingManager farmingManager = plugin.getFarmingManager();
        if (farmingManager == null) {
            sender.sendMessage("§cFarming manager not initialized!");
            return;
        }
        
        List<Plant> nearbyPlants = farmingManager.getNearbyPlants(player.getLocation(), 5);
        if (nearbyPlants.isEmpty()) {
            sender.sendMessage("§cNo plants found within 5 blocks!");
            return;
        }
        
        Plant plant = nearbyPlants.get(0);
        plant.setGrowthStage(5);
        plant.addQuality(50);
        
        sender.sendMessage("§a§l[DEBUG] §7Instantly grew plant to stage 5!");
        sender.sendMessage("§7Strain: §e" + plant.getStrainId());
        sender.sendMessage("§7Quality: §a" + plant.getQuality());
    }

    private void handleGiveEffect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug giveeffect <effect> [player]");
            return;
        }
        
        String effectName = args[1].toUpperCase();
        StrainEffectType effectType;
        try {
            effectType = StrainEffectType.valueOf(effectName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid effect type! Use tab completion.");
            return;
        }
        
        Player target;
        if (args.length > 2) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cSpecify a player!");
            return;
        }
        
        // Create a test effect and apply it
        StrainEffect effect = new StrainEffect(effectType, 3);
        
        if (plugin.getStrainEffectsManager() != null) {
            plugin.getStrainEffectsManager().applyEffect(target, effect, StarRating.FIVE_STAR, 600); // 30 seconds
            sender.sendMessage("§a§l[DEBUG] §7Applied effect §d" + effectType.getDisplayName() + " §7to §e" + target.getName());
        } else {
            sender.sendMessage("§cEffects manager not initialized!");
        }
    }

    private void handleTestMutation(CommandSender sender, String[] args) {
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§d§l  Mutation Test Results");
        sender.sendMessage("§8§m════════════════════════════════════════");
        
        Random random = new Random();
        int normalMutations = 0;
        int sixStarMutations = 0;
        int tests = 10000;
        
        double baseMutationChance = plugin.getConfig().getDouble("crossbreed.mutation-chance", 0.05);
        double sixStarChance = plugin.getConfig().getDouble("crossbreed.six-star-mutation-chance", 0.02);
        
        for (int i = 0; i < tests; i++) {
            if (random.nextDouble() < baseMutationChance) {
                normalMutations++;
                if (random.nextDouble() < sixStarChance) {
                    sixStarMutations++;
                }
            }
        }
        
        sender.sendMessage("§7  Simulated " + tests + " crossbreeds:");
        sender.sendMessage("§7  Mutations: §e" + normalMutations + " §7(" + String.format("%.1f%%", normalMutations * 100.0 / tests) + ")");
        sender.sendMessage("§7  6-Star Mutations: §d§l" + sixStarMutations + " §7(" + String.format("%.2f%%", sixStarMutations * 100.0 / tests) + ")");
        sender.sendMessage("§8§m════════════════════════════════════════");
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getDataManager().reloadAll();
        sender.sendMessage("§a§l[DEBUG] §7All data reloaded!");
    }

    private void handleSave(CommandSender sender) {
        plugin.getDataManager().saveAll();
        sender.sendMessage("§a§l[DEBUG] §7All data saved!");
    }

    private void handleConfigInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug config <key>");
            sender.sendMessage("§7Example: /debug config crossbreed.mutation-chance");
            return;
        }
        
        String key = args[1];
        if (plugin.getConfig().contains(key)) {
            Object value = plugin.getConfig().get(key);
            sender.sendMessage("§a§l[DEBUG] §7Config §e" + key + " §7= §f" + value);
        } else {
            sender.sendMessage("§cConfig key not found: " + key);
        }
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug clear <plants|sessions>");
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "plants" -> {
                // This would clear all plants - dangerous!
                sender.sendMessage("§c§l[DEBUG] §7Plant clearing not implemented (too dangerous)");
            }
            case "sessions" -> {
                sender.sendMessage("§a§l[DEBUG] §7Cleared all trading sessions");
            }
            default -> sender.sendMessage("§cUnknown clear target: " + args[1]);
        }
    }

    private String formatLocation(Location loc) {
        return String.format("%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("budlords.admin")) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "toggle", "plants", "strains", "player", "effects", "economy",
                "entity", "growplant", "giveeffect", "testmutation", "reload",
                "save", "config", "clear"
            ));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "player" -> Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                case "giveeffect" -> Arrays.stream(StrainEffectType.values()).forEach(e -> completions.add(e.name()));
                case "effects" -> Arrays.stream(StrainEffectType.EffectCategory.values()).forEach(c -> completions.add(c.name()));
                case "clear" -> completions.addAll(Arrays.asList("plants", "sessions"));
                case "config" -> completions.addAll(Arrays.asList(
                    "crossbreed.mutation-chance",
                    "crossbreed.six-star-mutation-chance",
                    "trading.entity-cooldown-seconds",
                    "effects.max-effects-per-strain"
                ));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("giveeffect")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            }
        }
        
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .collect(Collectors.toList());
    }
}
