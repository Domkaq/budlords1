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
import org.bukkit.inventory.ItemStack;

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
            case "setplantstage" -> handleSetPlantStage(sender, args);
            case "refreshplant" -> handleRefreshPlant(sender, args);
            case "waterplant" -> handleWaterPlant(sender, args);
            case "givemoney" -> handleGiveMoney(sender, args);
            case "giveeffect" -> handleGiveEffect(sender, args);
            case "testmutation" -> handleTestMutation(sender, args);
            case "reload" -> handleReload(sender);
            case "save" -> handleSave(sender);
            case "config" -> handleConfigInfo(sender, args);
            case "clear" -> handleClear(sender, args);
            // New comprehensive debug commands
            case "weather" -> handleWeather(sender, args);
            case "infect" -> handleInfect(sender, args);
            case "cure" -> handleCure(sender, args);
            case "giveseed" -> handleGiveSeed(sender, args);
            case "givebud" -> handleGiveBud(sender, args);
            case "giveitem" -> handleGiveItem(sender, args);
            case "reputation" -> handleReputation(sender, args);
            case "challenge" -> handleChallenge(sender, args);
            case "bulkorder" -> handleBulkOrder(sender, args);
            case "crossbreed" -> handleCrossbreed(sender, args);
            case "setquality" -> handleSetQuality(sender, args);
            case "removeplant" -> handleRemovePlant(sender, args);
            case "spawnnpc" -> handleSpawnNpc(sender, args);
            case "skills" -> handleSkills(sender, args);
            case "addskillxp" -> handleAddSkillXp(sender, args);
            case "prestige" -> handlePrestige(sender, args);
            case "market" -> handleMarket(sender, args);
            case "joint" -> handleJoint(sender, args);
            // New comprehensive admin commands
            case "vampire" -> handleVampire(sender, args);
            case "666" -> handleVampire(sender, args);
            case "buyers" -> handleBuyers(sender, args);
            case "formations" -> handleFormations(sender, args);
            case "teleport" -> handleTeleport(sender, args);
            case "speed" -> handleSpeed(sender, args);
            case "heal" -> handleHeal(sender, args);
            case "clearinv" -> handleClearInventory(sender, args);
            case "time" -> handleTime(sender, args);
            case "god" -> handleGodMode(sender, args);
            case "fly" -> handleFly(sender, args);
            case "unlock" -> handleUnlockAll(sender, args);
            case "maxout" -> handleMaxOut(sender, args);
            case "analytics" -> handleAnalytics(sender);
            case "dump" -> handleDump(sender, args);
            default -> showDebugHelp(sender);
        }

        return true;
    }

    private void showDebugHelp(CommandSender sender) {
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§c§l  BudLords Admin Debug Tools");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("");
        sender.sendMessage("§6§lPlant Commands:");
        sender.sendMessage("§e  /debug plants [radius] §7- Show nearby plants info");
        sender.sendMessage("§e  /debug growplant §7- Instantly grow nearest plant");
        sender.sendMessage("§e  /debug setplantstage <0-3> §7- Set plant stage");
        sender.sendMessage("§e  /debug refreshplant §7- Refresh plant visual");
        sender.sendMessage("§e  /debug waterplant §7- Max water/nutrients");
        sender.sendMessage("§e  /debug setquality <0-100> §7- Set plant quality");
        sender.sendMessage("§e  /debug removeplant §7- Remove nearest plant");
        sender.sendMessage("§e  /debug infect §7- Infect nearest plant");
        sender.sendMessage("§e  /debug cure §7- Cure nearest plant");
        sender.sendMessage("");
        sender.sendMessage("§6§lItem Commands:");
        sender.sendMessage("§e  /debug giveseed <strain> [stars] §7- Give seed item");
        sender.sendMessage("§e  /debug givebud <strain> [stars] [amount] §7- Give bud item");
        sender.sendMessage("§e  /debug giveitem <type> [stars] §7- Give quality item");
        sender.sendMessage("§e  /debug joint <give|info> [strain] §7- Joint commands");
        sender.sendMessage("");
        sender.sendMessage("§6§lEconomy Commands:");
        sender.sendMessage("§e  /debug economy §7- Show economy stats");
        sender.sendMessage("§e  /debug givemoney <amount> §7- Give money");
        sender.sendMessage("§e  /debug market [set <multiplier>] §7- Market info/set");
        sender.sendMessage("§e  /debug reputation [add <buyer> <amount>] §7- Rep info/add");
        sender.sendMessage("§e  /debug bulkorder [generate] §7- Bulk order info");
        sender.sendMessage("");
        sender.sendMessage("§6§lPlayer Commands:");
        sender.sendMessage("§e  /debug player <name> §7- Show player stats");
        sender.sendMessage("§e  /debug prestige [set <level>] §7- Prestige info/set");
        sender.sendMessage("§e  /debug skills [add <skill> <xp>] §7- Skills info");
        sender.sendMessage("§e  /debug challenge [complete] §7- Challenge info");
        sender.sendMessage("§e  /debug giveeffect <effect> [player] §7- Apply effect");
        sender.sendMessage("");
        sender.sendMessage("§6§lWorld Commands:");
        sender.sendMessage("§e  /debug weather [set <type>] §7- Weather info/set");
        sender.sendMessage("§e  /debug entity [radius] §7- Show sellable entities");
        sender.sendMessage("§e  /debug spawnnpc <type> §7- Spawn NPC buyer");
        sender.sendMessage("");
        sender.sendMessage("§6§lStrain Commands:");
        sender.sendMessage("§e  /debug strains [search] §7- List/search strains");
        sender.sendMessage("§e  /debug effects [category] §7- List all effects");
        sender.sendMessage("§e  /debug crossbreed <strain1> <strain2> §7- Test crossbreed");
        sender.sendMessage("§e  /debug testmutation §7- Test mutation chances");
        sender.sendMessage("");
        sender.sendMessage("§6§lSystem Commands:");
        sender.sendMessage("§e  /debug toggle §7- Toggle debug mode");
        sender.sendMessage("§e  /debug reload §7- Reload all data");
        sender.sendMessage("§e  /debug save §7- Force save all data");
        sender.sendMessage("§e  /debug config <key> §7- View config values");
        sender.sendMessage("§e  /debug clear <plants|sessions> §7- Clear data");
        sender.sendMessage("");
        sender.sendMessage("§4§l🔥 Secret/Admin Commands:");
        sender.sendMessage("§c  /debug vampire [give|info|spawn] §7- 666 Vampire Seed");
        sender.sendMessage("§c  /debug 666 [give|info|spawn] §7- Same as vampire");
        sender.sendMessage("§e  /debug buyers [list|stats] §7- Buyer registry info");
        sender.sendMessage("§e  /debug formations §7- Formation detection info");
        sender.sendMessage("§e  /debug analytics §7- Full system analytics");
        sender.sendMessage("§e  /debug dump <type> §7- Dump data (json)");
        sender.sendMessage("");
        sender.sendMessage("§d§lUtility Commands:");
        sender.sendMessage("§e  /debug teleport <x> <y> <z> §7- Teleport");
        sender.sendMessage("§e  /debug speed <0-10> §7- Set speed");
        sender.sendMessage("§e  /debug heal §7- Full heal");
        sender.sendMessage("§e  /debug clearinv §7- Clear inventory");
        sender.sendMessage("§e  /debug time <set|add> <value> §7- Time control");
        sender.sendMessage("§e  /debug god §7- Toggle god mode");
        sender.sendMessage("§e  /debug fly §7- Toggle fly");
        sender.sendMessage("§e  /debug unlock §7- Unlock all skills");
        sender.sendMessage("§e  /debug maxout §7- Max everything");
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
                sender.sendMessage("§7     Stage: §a" + plant.getGrowthStage() + "/3 §7(" + plant.getGrowthStageName() + ") §7| Quality: §a" + plant.getQuality());
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
        sender.sendMessage("§7  Successful Sales: §a" + stats.getTotalSalesSuccess());
        sender.sendMessage("§7  Failed Sales: §c" + stats.getTotalSalesFailed());
        sender.sendMessage("§7  Highest Single Sale: §6$" + String.format("%,.2f", stats.getHighestSingleSale()));
        sender.sendMessage("§7  Joints Rolled: §e" + stats.getJointsRolled());
        sender.sendMessage("§7  Crossbreeds: §d" + stats.getCrossbreeds());
        sender.sendMessage("§7  Legendary Strains: §6" + stats.getLegendaryStrainsDiscovered());
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
            if (plugin.getNpcManager() != null) {
                var npcType = plugin.getNpcManager().getNPCType(entity);
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
        int previousStage = plant.getGrowthStage();
        
        // Set to max growth stage (3 = Mature/Flowering - fully grown)
        plant.setGrowthStage(3);
        plant.addQuality(50);
        
        // Update the visual appearance to match the new growth stage
        if (plugin.getPlantVisualizationManager() != null) {
            plugin.getPlantVisualizationManager().updatePlantVisual(plant);
        }
        
        sender.sendMessage("§a§l[DEBUG] §7Instantly grew plant to stage 3 (Mature)!");
        sender.sendMessage("§7Strain: §e" + plant.getStrainId());
        sender.sendMessage("§7Previous Stage: §e" + previousStage + " §7→ §aNow: 3 (Mature)");
        sender.sendMessage("§7Quality: §a" + plant.getQuality());
        sender.sendMessage("§7The plant is now ready to harvest!");
    }
    
    private void handleSetPlantStage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug setplantstage <0-3>");
            sender.sendMessage("§7  0 = Seed, 1 = Sprout, 2 = Growing, 3 = Mature");
            return;
        }
        
        int stage = parseInt(args[1], -1);
        if (stage < 0 || stage > 3) {
            sender.sendMessage("§cStage must be between 0 and 3!");
            sender.sendMessage("§7  0 = Seed, 1 = Sprout, 2 = Growing, 3 = Mature");
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
        int previousStage = plant.getGrowthStage();
        plant.setGrowthStage(stage);
        
        // Update the visual appearance
        if (plugin.getPlantVisualizationManager() != null) {
            plugin.getPlantVisualizationManager().updatePlantVisual(plant);
        }
        
        // Use plant's built-in method which handles all edge cases
        String previousStageName = getPreviousStageName(previousStage);
        String newStageName = plant.getGrowthStageName();
        
        sender.sendMessage("§a§l[DEBUG] §7Set plant stage!");
        sender.sendMessage("§7Strain: §e" + plant.getStrainId());
        sender.sendMessage("§7Stage: §e" + previousStage + " (" + previousStageName + ") §7→ §a" + stage + " (" + newStageName + ")");
    }
    
    /**
     * Gets a stage name for display, handling edge cases.
     */
    private String getPreviousStageName(int stage) {
        return switch (stage) {
            case 0 -> "Seed";
            case 1 -> "Sprout";
            case 2 -> "Growing";
            case 3 -> "Mature";
            default -> "Unknown";
        };
    }
    
    private void handleRefreshPlant(CommandSender sender, String[] args) {
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
        
        // Force refresh the visual
        if (plugin.getPlantVisualizationManager() != null) {
            plugin.getPlantVisualizationManager().updatePlantVisual(plant);
            sender.sendMessage("§a§l[DEBUG] §7Refreshed plant visualization!");
            sender.sendMessage("§7Strain: §e" + plant.getStrainId());
            sender.sendMessage("§7Stage: §e" + plant.getGrowthStage() + " (" + plant.getGrowthStageName() + ")");
        } else {
            sender.sendMessage("§cPlant visualization manager not initialized!");
        }
    }
    
    private void handleWaterPlant(CommandSender sender, String[] args) {
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
        double previousWater = plant.getWaterLevel();
        double previousNutrients = plant.getNutrientLevel();
        
        plant.setWaterLevel(1.0);
        plant.setNutrientLevel(1.0);
        
        sender.sendMessage("§a§l[DEBUG] §7Maxed plant water and nutrients!");
        sender.sendMessage("§7Strain: §e" + plant.getStrainId());
        sender.sendMessage("§7Water: §b" + String.format("%.0f%%", previousWater * 100) + " §7→ §a100%");
        sender.sendMessage("§7Nutrients: §e" + String.format("%.0f%%", previousNutrients * 100) + " §7→ §a100%");
    }
    
    private void handleGiveMoney(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug givemoney <amount>");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount!");
            return;
        }
        
        if (amount <= 0) {
            sender.sendMessage("§cAmount must be positive!");
            return;
        }
        
        plugin.getEconomyManager().addBalance(player, amount);
        double newBalance = plugin.getEconomyManager().getBalance(player);
        
        sender.sendMessage("§a§l[DEBUG] §7Added money to your account!");
        sender.sendMessage("§7Amount: §a+" + plugin.getEconomyManager().formatMoney(amount));
        sender.sendMessage("§7New Balance: §a" + plugin.getEconomyManager().formatMoney(newBalance));
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
            // Duration: 30 seconds = 30 * 20 ticks = 600 ticks
            final int EFFECT_DURATION_TICKS = 30 * 20;
            plugin.getStrainEffectsManager().applyEffect(target, effect, StarRating.FIVE_STAR, EFFECT_DURATION_TICKS);
            sender.sendMessage("§a§l[DEBUG] §7Applied effect §d" + effectType.getDisplayName() + " §7to §e" + target.getName() + " §7(30 seconds)");
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
            sender.sendMessage("§cUsage: /debug clear <sessions>");
            sender.sendMessage("§7  sessions - Clear all trading sessions");
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "sessions" -> {
                sender.sendMessage("§a§l[DEBUG] §7Cleared all trading sessions");
            }
            default -> sender.sendMessage("§cUnknown clear target: " + args[1]);
        }
    }
    
    // ═══════════════════════════════════════
    // NEW COMPREHENSIVE DEBUG COMMANDS
    // ═══════════════════════════════════════
    
    private void handleWeather(CommandSender sender, String[] args) {
        var weatherManager = plugin.getWeatherManager();
        if (weatherManager == null) {
            sender.sendMessage("§cWeather manager not initialized!");
            return;
        }
        
        if (args.length > 2 && args[1].equalsIgnoreCase("set")) {
            sender.sendMessage("§cWeather is automatically updated based on world conditions.");
            sender.sendMessage("§7Use Minecraft weather commands to affect in-game weather.");
            return;
        }
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§b§l  Weather Debug");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§7  Current: " + weatherManager.getCurrentWeather().getColoredDisplay());
        sender.sendMessage("§7  Growth Multiplier: §a" + String.format("%.2fx", weatherManager.getGrowthMultiplier()));
        sender.sendMessage("§7  Quality Multiplier: §e" + String.format("%.2fx", weatherManager.getQualityMultiplier()));
        sender.sendMessage("");
        sender.sendMessage("§7  Use §e/debug weather set <type> §7to change");
        sender.sendMessage("§8§m════════════════════════════════════════");
    }
    
    private void handleInfect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        var diseaseManager = plugin.getDiseaseManager();
        if (diseaseManager == null) {
            sender.sendMessage("§cDisease manager not initialized!");
            return;
        }
        
        FarmingManager farmingManager = plugin.getFarmingManager();
        List<Plant> nearbyPlants = farmingManager.getNearbyPlants(player.getLocation(), 5);
        if (nearbyPlants.isEmpty()) {
            sender.sendMessage("§cNo plants found within 5 blocks!");
            return;
        }
        
        Plant plant = nearbyPlants.get(0);
        // Infect with a random common disease for testing
        diseaseManager.infectPlant(plant, com.budlords.diseases.PlantDisease.ROOT_ROT);
        sender.sendMessage("§a§l[DEBUG] §7Infected plant at " + formatLocation(plant.getLocation()));
        sender.sendMessage("§7Strain: §e" + plant.getStrainId());
    }
    
    private void handleCure(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        var diseaseManager = plugin.getDiseaseManager();
        if (diseaseManager == null) {
            sender.sendMessage("§cDisease manager not initialized!");
            return;
        }
        
        FarmingManager farmingManager = plugin.getFarmingManager();
        List<Plant> nearbyPlants = farmingManager.getNearbyPlants(player.getLocation(), 5);
        if (nearbyPlants.isEmpty()) {
            sender.sendMessage("§cNo plants found within 5 blocks!");
            return;
        }
        
        Plant plant = nearbyPlants.get(0);
        // Use the recommended cure for whatever disease the plant has
        boolean cured = diseaseManager.curePlant(player, plant.getLocation(), 
            com.budlords.diseases.PlantDisease.Cure.UNIVERSAL_CURE);
        if (cured) {
            sender.sendMessage("§a§l[DEBUG] §7Cured plant at " + formatLocation(plant.getLocation()));
        } else {
            sender.sendMessage("§c§l[DEBUG] §7Plant was not infected or cure failed");
        }
    }
    
    private void handleGiveSeed(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug giveseed <strain> [stars]");
            return;
        }
        
        String strainId = args[1].toLowerCase();
        Strain strain = plugin.getStrainManager().getStrain(strainId);
        if (strain == null) {
            sender.sendMessage("§cStrain not found: " + strainId);
            return;
        }
        
        int stars = args.length > 2 ? parseInt(args[2], 3) : 3;
        stars = Math.max(1, Math.min(6, stars));
        StarRating rating = StarRating.fromValue(stars);
        
        org.bukkit.inventory.ItemStack seed = plugin.getStrainManager().createSeedItem(strain, 1, rating);
        player.getInventory().addItem(seed);
        sender.sendMessage("§a§l[DEBUG] §7Gave §e" + strain.getName() + " §7seed (" + rating.getDisplay() + "§7)");
    }
    
    private void handleGiveBud(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug givebud <strain> [stars] [amount]");
            return;
        }
        
        String strainId = args[1].toLowerCase();
        Strain strain = plugin.getStrainManager().getStrain(strainId);
        if (strain == null) {
            sender.sendMessage("§cStrain not found: " + strainId);
            return;
        }
        
        int stars = args.length > 2 ? parseInt(args[2], 3) : 3;
        stars = Math.max(1, Math.min(6, stars));
        int amount = args.length > 3 ? parseInt(args[3], 10) : 10;
        StarRating rating = StarRating.fromValue(stars);
        
        org.bukkit.inventory.ItemStack bud = plugin.getStrainManager().createBudItem(strain, amount, rating);
        player.getInventory().addItem(bud);
        sender.sendMessage("§a§l[DEBUG] §7Gave §e" + amount + "x " + strain.getName() + " §7bud (" + rating.getDisplay() + "§7)");
    }
    
    private void handleGiveItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug giveitem <pot|lamp|scissors|fertilizer|grinder|papers|phone> [stars]");
            return;
        }
        
        String itemType = args[1].toLowerCase();
        int stars = args.length > 2 ? parseInt(args[2], 3) : 3;
        stars = Math.max(1, Math.min(5, stars));
        StarRating rating = StarRating.fromValue(stars);
        
        var qim = plugin.getQualityItemManager();
        org.bukkit.inventory.ItemStack item = null;
        
        switch (itemType) {
            case "pot" -> item = com.budlords.quality.GrowingPot.createPotItem(rating, 1);
            case "lamp" -> item = qim.createLamp(rating, 1);
            case "scissors" -> item = qim.createScissors(rating, 1);
            case "fertilizer" -> item = qim.createFertilizer(rating, 1);
            case "grinder" -> item = com.budlords.joint.JointItems.createGrinder(rating, 1);
            case "papers" -> item = com.budlords.joint.JointItems.createRollingPaper(1);
            case "tobacco" -> item = com.budlords.joint.JointItems.createTobacco(1);
            default -> {
                sender.sendMessage("§cUnknown item type: " + itemType);
                sender.sendMessage("§7Options: pot, lamp, scissors, fertilizer, grinder, papers, tobacco");
                return;
            }
        }
        
        if (item != null) {
            player.getInventory().addItem(item);
            sender.sendMessage("§a§l[DEBUG] §7Gave §e" + itemType + " §7(" + rating.getDisplay() + "§7)");
        }
    }
    
    private void handleReputation(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        var repManager = plugin.getReputationManager();
        if (repManager == null) {
            sender.sendMessage("§cReputation manager not initialized!");
            return;
        }
        
        if (args.length > 3 && args[1].equalsIgnoreCase("add")) {
            String buyerType = args[2].toUpperCase();
            int amount = parseInt(args[3], 50);
            repManager.addReputation(player.getUniqueId(), buyerType, amount);
            sender.sendMessage("§a§l[DEBUG] §7Added §e" + amount + " §7reputation with §f" + buyerType);
            return;
        }
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§d§l  Reputation Debug");
        sender.sendMessage("§8§m════════════════════════════════════════");
        
        for (com.budlords.npc.NPCManager.NPCType type : com.budlords.npc.NPCManager.NPCType.values()) {
            if (type == com.budlords.npc.NPCManager.NPCType.NONE) continue;
            int rep = repManager.getReputation(player.getUniqueId(), type.name());
            String level = repManager.getReputationLevel(rep);
            sender.sendMessage("§7  " + type.name() + ": §e" + rep + " §7(" + level + ")");
        }
        
        sender.sendMessage("");
        sender.sendMessage("§7  Use §e/debug reputation add <buyer> <amount>");
        sender.sendMessage("§8§m════════════════════════════════════════");
    }
    
    private void handleChallenge(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        var challengeManager = plugin.getChallengeManager();
        if (challengeManager == null) {
            sender.sendMessage("§cChallenge manager not initialized!");
            return;
        }
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§6§l  Challenge Debug");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§7  Use §e/challenges §7to view and complete challenges.");
        sender.sendMessage("§7  Challenge system is managed through GUI.");
        sender.sendMessage("");
        sender.sendMessage("§7  Use §e/debug challenge gui §7to open challenges menu");
        sender.sendMessage("§8§m════════════════════════════════════════");
        
        if (args.length > 1 && args[1].equalsIgnoreCase("gui")) {
            challengeManager.openChallengesGUI(player);
        }
    }
    
    private void handleBulkOrder(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        var orderManager = plugin.getBulkOrderManager();
        if (orderManager == null) {
            sender.sendMessage("§cBulk order manager not initialized!");
            return;
        }
        
        if (args.length > 1 && args[1].equalsIgnoreCase("generate")) {
            orderManager.generateOrder(player.getUniqueId());
            sender.sendMessage("§a§l[DEBUG] §7Generated new bulk order!");
            return;
        }
        
        var order = orderManager.getActiveOrder(player.getUniqueId());
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§6§l  Bulk Order Debug");
        sender.sendMessage("§8§m════════════════════════════════════════");
        
        if (order == null) {
            sender.sendMessage("§7  No active bulk order.");
        } else {
            sender.sendMessage("§7  Buyer: §f" + order.buyerName);
            sender.sendMessage("§7  Strain: §e" + order.strainName);
            sender.sendMessage("§7  Quantity: §e" + order.quantity + "g");
            sender.sendMessage("§7  Bonus: §a+" + String.format("%.0f%%", (order.priceMultiplier - 1) * 100));
            sender.sendMessage("§7  Time Left: §e" + order.getTimeRemainingText());
        }
        
        sender.sendMessage("");
        sender.sendMessage("§7  Use §e/debug bulkorder generate §7for new order");
        sender.sendMessage("§8§m════════════════════════════════════════");
    }
    
    private void handleCrossbreed(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        var crossbreedManager = plugin.getCrossbreedManager();
        if (crossbreedManager == null) {
            sender.sendMessage("§cCrossbreed manager not initialized!");
            return;
        }
        
        // Open the crossbreed GUI for interactive crossbreeding
        sender.sendMessage("§a§l[DEBUG] §7Opening crossbreed GUI...");
        sender.sendMessage("§7Use the GUI to select strains and crossbreed.");
        crossbreedManager.openCrossbreedGUI(player);
    }
    
    private void handleSetQuality(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug setquality <0-100>");
            return;
        }
        
        int quality = parseInt(args[1], -1);
        if (quality < 0 || quality > 100) {
            sender.sendMessage("§cQuality must be between 0 and 100!");
            return;
        }
        
        FarmingManager farmingManager = plugin.getFarmingManager();
        List<Plant> nearbyPlants = farmingManager.getNearbyPlants(player.getLocation(), 5);
        if (nearbyPlants.isEmpty()) {
            sender.sendMessage("§cNo plants found within 5 blocks!");
            return;
        }
        
        Plant plant = nearbyPlants.get(0);
        int prevQuality = plant.getQuality();
        plant.setQuality(quality);
        
        sender.sendMessage("§a§l[DEBUG] §7Set plant quality!");
        sender.sendMessage("§7Quality: §e" + prevQuality + " §7→ §a" + quality);
    }
    
    private void handleRemovePlant(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        FarmingManager farmingManager = plugin.getFarmingManager();
        List<Plant> nearbyPlants = farmingManager.getNearbyPlants(player.getLocation(), 5);
        if (nearbyPlants.isEmpty()) {
            sender.sendMessage("§cNo plants found within 5 blocks!");
            return;
        }
        
        Plant plant = nearbyPlants.get(0);
        Location loc = plant.getLocation();
        
        // Remove visualization
        if (plugin.getPlantVisualizationManager() != null) {
            plugin.getPlantVisualizationManager().removeVisualization(loc);
        }
        
        // Remove from manager
        farmingManager.removePlant(loc);
        
        sender.sendMessage("§a§l[DEBUG] §7Removed plant at " + formatLocation(loc));
    }
    
    private void handleSpawnNpc(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug spawnnpc <type>");
            sender.sendMessage("§7Types: MARKET_JOE, BLACKMARKET_JOE");
            return;
        }
        
        var npcManager = plugin.getNpcManager();
        if (npcManager == null) {
            sender.sendMessage("§cNPC manager not initialized!");
            return;
        }
        
        String typeName = args[1].toUpperCase();
        switch (typeName) {
            case "MARKET_JOE" -> {
                npcManager.spawnMarketJoe(player.getLocation());
                sender.sendMessage("§a§l[DEBUG] §7Spawned Market Joe");
            }
            case "BLACKMARKET_JOE" -> {
                npcManager.spawnBlackMarketJoe(player.getLocation());
                sender.sendMessage("§a§l[DEBUG] §7Spawned BlackMarket Joe");
            }
            default -> sender.sendMessage("§cInvalid NPC type: " + typeName + ". Options: MARKET_JOE, BLACKMARKET_JOE");
        }
    }
    
    private void handleSkills(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        var skillManager = plugin.getSkillManager();
        if (skillManager == null) {
            sender.sendMessage("§cSkill manager not initialized!");
            return;
        }
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§a§l  Skills Debug");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§7  Skill Points: §e" + skillManager.getSkillPoints(player.getUniqueId()));
        sender.sendMessage("§7  Unlocked Skills: §e" + skillManager.getUnlockedCount(player.getUniqueId()));
        sender.sendMessage("");
        sender.sendMessage("§7  Tree XP:");
        for (com.budlords.skills.Skill.SkillTree tree : com.budlords.skills.Skill.SkillTree.values()) {
            int xp = skillManager.getTreeXP(player.getUniqueId(), tree);
            sender.sendMessage("§7    " + tree.getDisplayName() + ": §e" + xp + " XP");
        }
        sender.sendMessage("");
        sender.sendMessage("§7  Use §e/debug addskillxp <tree> <amount>");
        sender.sendMessage("§7  Trees: FARMING, QUALITY, TRADING, PROCESSING");
        sender.sendMessage("§8§m════════════════════════════════════════");
    }
    
    private void handleAddSkillXp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /debug addskillxp <tree> <amount>");
            sender.sendMessage("§7Trees: FARMING, QUALITY, TRADING, PROCESSING");
            return;
        }
        
        var skillManager = plugin.getSkillManager();
        if (skillManager == null) {
            sender.sendMessage("§cSkill manager not initialized!");
            return;
        }
        
        String treeName = args[1].toUpperCase();
        int amount = parseInt(args[2], 100);
        
        try {
            var tree = com.budlords.skills.Skill.SkillTree.valueOf(treeName);
            skillManager.addTreeXP(player.getUniqueId(), tree, amount);
            sender.sendMessage("§a§l[DEBUG] §7Added §e" + amount + " §7XP to " + tree.getDisplayName());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid skill tree: " + treeName);
            sender.sendMessage("§7Trees: FARMING, QUALITY, TRADING, PROCESSING");
        }
    }
    
    private void handlePrestige(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        var prestigeManager = plugin.getPrestigeManager();
        if (prestigeManager == null) {
            sender.sendMessage("§cPrestige manager not initialized!");
            return;
        }
        
        if (args.length > 2 && args[1].equalsIgnoreCase("set")) {
            int level = parseInt(args[2], 1);
            plugin.getStatsManager().getStats(player).setPrestigeLevel(level);
            sender.sendMessage("§a§l[DEBUG] §7Set prestige level to §d" + level);
            return;
        }
        
        if (args.length > 1 && args[1].equalsIgnoreCase("gui")) {
            prestigeManager.openPrestigeGUI(player);
            return;
        }
        
        var stats = plugin.getStatsManager().getStats(player);
        int currentPrestige = stats.getPrestigeLevel();
        double balance = plugin.getEconomyManager().getBalance(player);
        double cost = prestigeManager.getPrestigeCost(currentPrestige);
        boolean canAfford = balance >= cost;
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§d§l  Prestige Debug");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§7  Current Level: §d" + currentPrestige);
        sender.sendMessage("§7  Next Cost: §e$" + String.format("%,.0f", cost));
        sender.sendMessage("§7  Balance: §e$" + String.format("%,.0f", balance));
        sender.sendMessage("§7  Can Afford: " + (canAfford ? "§aYes" : "§cNo"));
        sender.sendMessage("");
        sender.sendMessage("§7  Use §e/debug prestige set <level>");
        sender.sendMessage("§7  Use §e/debug prestige gui §7to open menu");
        sender.sendMessage("§8§m════════════════════════════════════════");
    }
    
    private void handleMarket(CommandSender sender, String[] args) {
        var marketManager = plugin.getMarketDemandManager();
        if (marketManager == null) {
            sender.sendMessage("§cMarket demand manager not initialized!");
            return;
        }
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§6§l  Market Debug");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§7  Current Event: §e" + marketManager.getCurrentMarketEvent());
        sender.sendMessage("§7  Event Multiplier: §e" + String.format("%.2fx", marketManager.getEventMultiplier()));
        sender.sendMessage("§7  Time Remaining: §e" + marketManager.getEventTimeRemainingMinutes() + " minutes");
        sender.sendMessage("");
        sender.sendMessage(marketManager.getMarketStatusDisplay());
        sender.sendMessage("§8§m════════════════════════════════════════");
    }
    
    private void handleJoint(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug joint <give|info> [strain]");
            return;
        }
        
        if (args[1].equalsIgnoreCase("give")) {
            String strainId = args.length > 2 ? args[2].toLowerCase() : "og_kush";
            Strain strain = plugin.getStrainManager().getStrain(strainId);
            if (strain == null) {
                sender.sendMessage("§cStrain not found: " + strainId);
                return;
            }
            
            org.bukkit.inventory.ItemStack joint = com.budlords.joint.JointItems.createJoint(
                strain.getId(), strain.getName(), StarRating.FIVE_STAR, strain.getPotency(), 1);
            player.getInventory().addItem(joint);
            sender.sendMessage("§a§l[DEBUG] §7Gave §e" + strain.getName() + " §7joint (5★, " + strain.getPotency() + "% potency)");
            return;
        }
        
        if (args[1].equalsIgnoreCase("info")) {
            var rollingManager = plugin.getJointRollingManager();
            sender.sendMessage("§8§m════════════════════════════════════════");
            sender.sendMessage("§e§l  Joint Rolling Debug");
            sender.sendMessage("§8§m════════════════════════════════════════");
            sender.sendMessage("§7  Rolling Manager: §a" + (rollingManager != null ? "Active" : "Not loaded"));
            sender.sendMessage("§7  Has Active Session: " + (rollingManager != null && rollingManager.hasActiveSession(player) ? "§aYes" : "§cNo"));
            sender.sendMessage("§8§m════════════════════════════════════════");
            return;
        }
        
        sender.sendMessage("§cUsage: /debug joint <give|info> [strain]");
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

    // ═══════════════════════════════════════════════════════════════
    // NEW COMPREHENSIVE ADMIN COMMANDS
    // ═══════════════════════════════════════════════════════════════
    
    private void handleVampire(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        String action = args.length > 1 ? args[1].toLowerCase() : "info";
        
        switch (action) {
            case "give" -> {
                Strain vampireSeed = plugin.getStrainManager().getStrain("vampire_seed");
                if (vampireSeed == null) {
                    sender.sendMessage("§c§l[666] Vampire Seed not found!");
                    // Note: Default strains are already ensured in StrainManager constructor
                    vampireSeed = plugin.getStrainManager().getStrain("vampire_seed");
                }
                
                if (vampireSeed != null) {
                    ItemStack seed = plugin.getStrainManager().createSeedItem(vampireSeed, 1, StarRating.SIX_STAR);
                    player.getInventory().addItem(seed);
                    sender.sendMessage("§4§l[666] §cYou have received the §4§lVAMPIRE SEED§c!");
                    sender.sendMessage("§7This is a §6LEGENDARY §76-star guaranteed strain!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
                } else {
                    sender.sendMessage("§cFailed to create Vampire Seed!");
                }
            }
            case "info" -> {
                sender.sendMessage("§8§m════════════════════════════════════════");
                sender.sendMessage("§4§l  666 - VAMPIRE SEED");
                sender.sendMessage("§8§m════════════════════════════════════════");
                sender.sendMessage("");
                sender.sendMessage("§7The ultimate reward from §c666 Formation§7!");
                sender.sendMessage("§7Rarity: §6§lLEGENDARY");
                sender.sendMessage("§7Stars: §d§l★★★★★★ §7(6-Star Guaranteed!)");
                sender.sendMessage("§7Potency: §c100");
                sender.sendMessage("");
                sender.sendMessage("§4§lEffects:");
                sender.sendMessage("§c  • VAMPIRE §7- Life steal on hit");
                sender.sendMessage("§c  • SPEED DEMON §7- Maximum speed");
                sender.sendMessage("§c  • BLOOD TRAIL §7- Bloody particle trail");
                sender.sendMessage("§c  • TANK MODE §7- Damage reduction");
                sender.sendMessage("§c  • NIGHT VISION §7- See in darkness");
                sender.sendMessage("");
                sender.sendMessage("§7Visual: §4Deep blood red with soul flames");
                sender.sendMessage("§7Admin: §aYes §7(Unlimited effect slots)");
                sender.sendMessage("");
                sender.sendMessage("§e/debug vampire give §7- Get Vampire Seed");
                sender.sendMessage("§e/debug vampire spawn §7- Spawn Vampire plant");
                sender.sendMessage("§8§m════════════════════════════════════════");
            }
            case "spawn" -> {
                Strain vampireSeed = plugin.getStrainManager().getStrain("vampire_seed");
                if (vampireSeed == null) {
                    sender.sendMessage("§cVampire Seed not found!");
                    return;
                }
                
                Location loc = player.getLocation().add(0, 0, 2);
                plugin.getFarmingManager().plantSeed(player, loc, vampireSeed.getId(), StarRating.SIX_STAR, StarRating.SIX_STAR);
                sender.sendMessage("§4§l[666] §cVampire plant spawned!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            }
            default -> sender.sendMessage("§cUsage: /debug vampire [give|info|spawn]");
        }
    }
    
    private void handleBuyers(CommandSender sender, String[] args) {
        String action = args.length > 1 ? args[1].toLowerCase() : "list";
        
        if (plugin.getBuyerRegistry() == null) {
            sender.sendMessage("§cBuyer registry not initialized!");
            return;
        }
        
        switch (action) {
            case "list" -> {
                sender.sendMessage("§8§m════════════════════════════════════════");
                sender.sendMessage("§6§l  Buyer Registry");
                sender.sendMessage("§8§m════════════════════════════════════════");
                
                var buyers = plugin.getBuyerRegistry().getAllBuyers();
                sender.sendMessage("§7Total buyers: §e" + buyers.size());
                sender.sendMessage("");
                
                buyers.stream().limit(10).forEach(buyer -> {
                    sender.sendMessage("§e" + buyer.getName() + 
                        " §7(" + buyer.getPersonality().getDisplayName() + "§7)");
                    sender.sendMessage("  §7Purchases: §e" + buyer.getTotalPurchases() + 
                        " §7| Spent: §a$" + String.format("%.2f", buyer.getTotalMoneySpent()));
                });
                
                if (buyers.size() > 10) {
                    sender.sendMessage("§7... and §e" + (buyers.size() - 10) + " §7more");
                }
            }
            case "stats" -> {
                sender.sendMessage("§8§m════════════════════════════════════════");
                sender.sendMessage("§6§l  Buyer Statistics");
                sender.sendMessage("§8§m════════════════════════════════════════");
                
                var stats = plugin.getBuyerRegistry().getStatistics();
                sender.sendMessage("§7Total Buyers: §e" + stats.get("total_buyers"));
                sender.sendMessage("§7Total Purchases: §e" + stats.get("total_purchases"));
                sender.sendMessage("§7Total Money: §a$" + String.format("%.2f", (Double)stats.get("total_money")));
                sender.sendMessage("§7Top Customer: §e" + stats.get("most_purchases"));
                sender.sendMessage("§7Highest Value: §e" + stats.get("highest_value"));
            }
            default -> sender.sendMessage("§cUsage: /debug buyers [list|stats]");
        }
    }
    
    private void handleFormations(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (plugin.getFormationManager() == null) {
            sender.sendMessage("§cFormation manager not initialized!");
            return;
        }
        
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§5§l  Formation Detection");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("");
        sender.sendMessage("§7Formation system enabled!");
        sender.sendMessage("§7Create patterns with same-strain plants:");
        sender.sendMessage("");
        sender.sendMessage("§a• 3 plants §7= +1 star bonus");
        sender.sendMessage("§e• 5 plants §7= +2 star bonus");
        sender.sendMessage("§6• 7 plants §7= +3 star bonus");
        sender.sendMessage("§4• 6-6-6 formation §7= §4§lVAMPIRE SEED");
        sender.sendMessage("");
        sender.sendMessage("§7To trigger §c666§7: Create triangle of 6 plants");
        sender.sendMessage("§7positioned in a specific demonic pattern.");
    }
    
    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /debug teleport <x> <y> <z>");
            return;
        }
        
        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            
            Location loc = new Location(player.getWorld(), x, y, z);
            player.teleport(loc);
            sender.sendMessage("§a§l[TP] §7Teleported to §e" + x + ", " + y + ", " + z);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid coordinates!");
        }
    }
    
    private void handleSpeed(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug speed <0-10>");
            return;
        }
        
        try {
            float speed = Float.parseFloat(args[1]) / 10f;
            if (speed < 0) speed = 0;
            if (speed > 1) speed = 1;
            
            player.setWalkSpeed(speed);
            player.setFlySpeed(speed);
            sender.sendMessage("§a§l[SPEED] §7Set to §e" + (speed * 10));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid speed value!");
        }
    }
    
    private void handleHeal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        
        sender.sendMessage("§a§l[HEAL] §7Fully healed!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    }
    
    private void handleClearInventory(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        player.getInventory().clear();
        sender.sendMessage("§c§l[CLEAR] §7Inventory cleared!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
    }
    
    private void handleTime(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /debug time <set|add> <value>");
            sender.sendMessage("§7Examples: day=1000, noon=6000, night=13000, midnight=18000");
            return;
        }
        
        String action = args[1].toLowerCase();
        long value;
        
        try {
            value = switch (args[2].toLowerCase()) {
                case "day" -> 1000L;
                case "noon" -> 6000L;
                case "night" -> 13000L;
                case "midnight" -> 18000L;
                default -> Long.parseLong(args[2]);
            };
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid time value!");
            return;
        }
        
        if (action.equals("set")) {
            player.getWorld().setTime(value);
            sender.sendMessage("§a§l[TIME] §7Set to §e" + value);
        } else if (action.equals("add")) {
            player.getWorld().setTime(player.getWorld().getTime() + value);
            sender.sendMessage("§a§l[TIME] §7Added §e" + value);
        }
    }
    
    private void handleGodMode(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (player.isInvulnerable()) {
            player.setInvulnerable(false);
            sender.sendMessage("§c§l[GOD] §7God mode §cDISABLED");
        } else {
            player.setInvulnerable(true);
            sender.sendMessage("§a§l[GOD] §7God mode §aENABLED");
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
    }
    
    private void handleFly(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
            sender.sendMessage("§c§l[FLY] §7Flight §cDISABLED");
        } else {
            player.setAllowFlight(true);
            player.setFlying(true);
            sender.sendMessage("§a§l[FLY] §7Flight §aENABLED");
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.5f);
    }
    
    private void handleUnlockAll(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        if (plugin.getSkillManager() == null) {
            sender.sendMessage("§cSkill manager not initialized!");
            return;
        }
        
        UUID uuid = player.getUniqueId();
        int unlocked = 0;
        
        for (com.budlords.skills.Skill skill : com.budlords.skills.Skill.values()) {
            if (!plugin.getSkillManager().hasSkill(uuid, skill)) {
                plugin.getSkillManager().unlockSkill(player, skill);
                unlocked++;
            }
        }
        
        sender.sendMessage("§a§l[UNLOCK] §7Unlocked §e" + unlocked + " §7skills!");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
    
    private void handleMaxOut(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        
        UUID uuid = player.getUniqueId();
        
        sender.sendMessage("§6§l[MAXOUT] §7Maxing out everything...");
        
        // Max money
        if (plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().setBalance(player, 999999999.0);
            sender.sendMessage("§a  ✓ Money: $999,999,999");
        }
        
        // Max skills
        if (plugin.getSkillManager() != null) {
            for (com.budlords.skills.Skill skill : com.budlords.skills.Skill.values()) {
                plugin.getSkillManager().unlockSkill(player, skill);
            }
            for (com.budlords.skills.Skill.SkillTree tree : com.budlords.skills.Skill.SkillTree.values()) {
                plugin.getSkillManager().addTreeXP(uuid, tree, 10000);
            }
            sender.sendMessage("§a  ✓ All skills unlocked + 10k XP per tree");
        }
        
        // Max prestige
        if (plugin.getPrestigeManager() != null && plugin.getStatsManager() != null) {
            PlayerStats stats = plugin.getStatsManager().getStats(player);
            if (stats != null) {
                stats.setPrestigeLevel(10);
                sender.sendMessage("§a  ✓ Prestige level 10");
            }
        }
        
        // Max stats
        if (plugin.getStatsManager() != null) {
            PlayerStats stats = plugin.getStatsManager().getStats(player);
            if (stats != null) {
                stats.recordSale(500);
                // Increment plants harvested multiple times for boosting
                for (int i = 0; i < 500; i++) {
                    stats.incrementPlantsHarvested();
                }
                sender.sendMessage("§a  ✓ Stats boosted");
            }
        }
        
        sender.sendMessage("§6§l[MAXOUT] §a§lCOMPLETE!");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.5f);
    }
    
    private void handleAnalytics(CommandSender sender) {
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("§6§l  System Analytics");
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("");
        
        // Players
        sender.sendMessage("§e§lPlayers:");
        sender.sendMessage("§7  Online: §a" + Bukkit.getOnlinePlayers().size());
        sender.sendMessage("§7  Total: §a" + Bukkit.getOfflinePlayers().length);
        
        // Strains
        if (plugin.getStrainManager() != null) {
            sender.sendMessage("");
            sender.sendMessage("§e§lStrains:");
            sender.sendMessage("§7  Total: §a" + plugin.getStrainManager().getAllStrains().size());
        }
        
        // Plants
        if (plugin.getFarmingManager() != null) {
            sender.sendMessage("");
            sender.sendMessage("§e§lPlants:");
            sender.sendMessage("§7  Active: §a" + plugin.getFarmingManager().getPlantCount());
        }
        
        // Buyers
        if (plugin.getBuyerRegistry() != null) {
            var stats = plugin.getBuyerRegistry().getStatistics();
            sender.sendMessage("");
            sender.sendMessage("§e§lBuyers:");
            sender.sendMessage("§7  Total: §a" + stats.get("total_buyers"));
            sender.sendMessage("§7  Purchases: §a" + stats.get("total_purchases"));
        }
        
        // Economy
        if (plugin.getEconomyManager() != null) {
            sender.sendMessage("");
            sender.sendMessage("§e§lEconomy:");
            double totalMoney = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                totalMoney += plugin.getEconomyManager().getBalance(p);
            }
            sender.sendMessage("§7  Online player money: §a$" + String.format("%.2f", totalMoney));
        }
        
        sender.sendMessage("");
        sender.sendMessage("§8§m════════════════════════════════════════");
    }
    
    private void handleDump(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /debug dump <strains|buyers|plants|skills>");
            return;
        }
        
        String type = args[1].toLowerCase();
        
        sender.sendMessage("§7Dumping §e" + type + "§7 data...");
        
        switch (type) {
            case "strains" -> {
                if (plugin.getStrainManager() != null) {
                    sender.sendMessage("§8[JSON] Strains:");
                    plugin.getStrainManager().getAllStrains().stream().limit(5).forEach(s -> {
                        sender.sendMessage("§7  {\"id\": \"" + s.getId() + "\", \"name\": \"" + 
                            s.getName() + "\", \"rarity\": \"" + s.getRarity() + "\"}");
                    });
                    sender.sendMessage("§7... (showing 5/" + plugin.getStrainManager().getAllStrains().size() + ")");
                }
            }
            case "buyers" -> {
                if (plugin.getBuyerRegistry() != null) {
                    sender.sendMessage("§8[JSON] Buyers:");
                    plugin.getBuyerRegistry().getAllBuyers().stream().limit(5).forEach(b -> {
                        sender.sendMessage("§7  {\"name\": \"" + b.getName() + "\", \"purchases\": " + 
                            b.getTotalPurchases() + ", \"spent\": " + b.getTotalMoneySpent() + "}");
                    });
                }
            }
            default -> sender.sendMessage("§cInvalid dump type!");
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
                // Plant commands
                "plants", "growplant", "setplantstage", "refreshplant", "waterplant", 
                "setquality", "removeplant", "infect", "cure",
                // Item commands
                "giveseed", "givebud", "giveitem", "joint",
                // Economy commands
                "economy", "givemoney", "market", "reputation", "bulkorder",
                // Player commands
                "player", "prestige", "skills", "addskillxp", "challenge", "giveeffect",
                // World commands
                "weather", "entity", "spawnnpc",
                // Strain commands
                "strains", "effects", "crossbreed", "testmutation",
                // System commands
                "toggle", "reload", "save", "config", "clear"
            ));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "player" -> Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                case "giveeffect" -> Arrays.stream(StrainEffectType.values()).forEach(e -> completions.add(e.name()));
                case "effects" -> Arrays.stream(StrainEffectType.EffectCategory.values()).forEach(c -> completions.add(c.name()));
                case "clear" -> completions.add("sessions");
                case "setplantstage" -> completions.addAll(Arrays.asList("0", "1", "2", "3"));
                case "setquality" -> completions.addAll(Arrays.asList("0", "25", "50", "75", "100"));
                case "givemoney" -> completions.addAll(Arrays.asList("1000", "10000", "100000", "1000000"));
                case "giveseed", "givebud", "crossbreed" -> {
                    plugin.getStrainManager().getAllStrains().forEach(s -> completions.add(s.getId()));
                }
                case "giveitem" -> completions.addAll(Arrays.asList("pot", "lamp", "scissors", "fertilizer", "grinder", "papers", "phone"));
                case "weather" -> completions.addAll(Arrays.asList("set", "SUNNY", "RAINY", "STORMY", "DROUGHT", "FOGGY", "WINDY", "PERFECT"));
                case "reputation" -> completions.add("add");
                case "bulkorder" -> completions.add("generate");
                case "challenge" -> completions.add("complete");
                case "prestige" -> completions.add("set");
                case "market" -> completions.add("set");
                case "joint" -> completions.addAll(Arrays.asList("give", "info"));
                case "addskillxp" -> completions.addAll(Arrays.asList("GROWING", "HARVESTING", "BREEDING", "PROCESSING", "SELLING"));
                case "spawnnpc" -> {
                    for (com.budlords.npc.NPCManager.NPCType type : com.budlords.npc.NPCManager.NPCType.values()) {
                        if (type != com.budlords.npc.NPCManager.NPCType.NONE) {
                            completions.add(type.name());
                        }
                    }
                }
                case "config" -> completions.addAll(Arrays.asList(
                    "crossbreed.mutation-chance",
                    "crossbreed.six-star-mutation-chance",
                    "crossbreed.effect-mutation-chance",
                    "trading.entity-cooldown-seconds",
                    "effects.max-effects-per-strain"
                ));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "giveeffect" -> Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                case "giveseed", "givebud", "giveitem" -> completions.addAll(Arrays.asList("1", "2", "3", "4", "5", "6"));
                case "crossbreed" -> plugin.getStrainManager().getAllStrains().forEach(s -> completions.add(s.getId()));
                case "weather" -> {
                    if (args[1].equalsIgnoreCase("set")) {
                        completions.addAll(Arrays.asList("SUNNY", "RAINY", "STORMY", "DROUGHT", "FOGGY", "WINDY", "PERFECT"));
                    }
                }
                case "reputation" -> {
                    if (args[1].equalsIgnoreCase("add")) {
                        for (com.budlords.npc.NPCManager.NPCType type : com.budlords.npc.NPCManager.NPCType.values()) {
                            if (type != com.budlords.npc.NPCManager.NPCType.NONE) {
                                completions.add(type.name());
                            }
                        }
                    }
                }
                case "prestige", "market" -> {
                    if (args[1].equalsIgnoreCase("set")) {
                        completions.addAll(Arrays.asList("1", "2", "3", "5", "10"));
                    }
                }
                case "addskillxp" -> completions.addAll(Arrays.asList("100", "500", "1000", "5000"));
                case "joint" -> {
                    if (args[1].equalsIgnoreCase("give")) {
                        plugin.getStrainManager().getAllStrains().forEach(s -> completions.add(s.getId()));
                    }
                }
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "givebud" -> completions.addAll(Arrays.asList("1", "10", "32", "64"));
                case "reputation" -> {
                    if (args[1].equalsIgnoreCase("add")) {
                        completions.addAll(Arrays.asList("50", "100", "200", "500"));
                    }
                }
            }
        }
        
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .collect(Collectors.toList());
    }
}
