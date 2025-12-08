package com.budlords.commands;

import com.budlords.BudLords;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BudLordsCommand implements CommandExecutor, TabCompleter {

    private final BudLords plugin;

    public BudLordsCommand(BudLords plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    if (!sender.hasPermission("budlords.admin")) {
                        sender.sendMessage("§cYou don't have permission to use this command!");
                        return true;
                    }
                    plugin.reloadConfig();
                    plugin.getDataManager().reloadAll();
                    sender.sendMessage("§aBudLords configuration reloaded!");
                    return true;
                }
                case "guide" -> {
                    if (args.length > 1) {
                        showGuide(sender, args[1].toLowerCase());
                    } else {
                        showGuideMenu(sender);
                    }
                    return true;
                }
                case "joint", "joints", "rolling" -> {
                    showJointGuide(sender);
                    return true;
                }
                case "growing", "farm", "farming" -> {
                    showGrowingGuide(sender);
                    return true;
                }
                case "selling", "sell", "trade" -> {
                    showSellingGuide(sender);
                    return true;
                }
            }
        }

        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§2§l  BudLords §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("");
        sender.sendMessage("§7  A weed farming economy plugin");
        sender.sendMessage("");
        sender.sendMessage("§e  Commands:");
        sender.sendMessage("§7  /bal §8- §fCheck your balance");
        sender.sendMessage("§7  /pay <player> <amount> §8- §fPay someone");
        sender.sendMessage("§7  /package <amount> §8- §fPackage buds for sale");
        sender.sendMessage("§7  /stats §8- §fView your statistics");
        sender.sendMessage("§7  /daily §8- §fClaim daily reward & streak bonus");
        sender.sendMessage("§7  /market §8- §fView market conditions & prices");
        sender.sendMessage("§7  /challenges §8- §fView daily/weekly challenges");
        sender.sendMessage("§7  /crossbreed §8- §fOpen crossbreeding lab");
        sender.sendMessage("§7  /leaderboard §8- §fView leaderboards");
        sender.sendMessage("§7  /prestige §8- §fPrestige system");
        sender.sendMessage("");
        sender.sendMessage("§a  Guides:");
        sender.sendMessage("§7  /budlords guide §8- §fView all guides");
        sender.sendMessage("§7  /budlords joint §8- §fHow to roll joints");
        sender.sendMessage("§7  /budlords growing §8- §fHow to grow plants");
        sender.sendMessage("§7  /budlords selling §8- §fHow to sell products");
        sender.sendMessage("");
        if (sender.hasPermission("budlords.admin")) {
            sender.sendMessage("§c  Admin Commands:");
            sender.sendMessage("§7  /addmoney <player> <amount> §8- §fAdd money");
            sender.sendMessage("§7  /straincreator §8- §fCreate new strains");
            sender.sendMessage("§7  /spawnmarket §8- §fSpawn Market Joe");
            sender.sendMessage("§7  /spawnblackmarket §8- §fSpawn BlackMarket Joe");
            sender.sendMessage("§7  /budlords reload §8- §fReload config");
            sender.sendMessage("");
        }
        sender.sendMessage("§8§m                                          ");

        return true;
    }
    
    private void showGuideMenu(CommandSender sender) {
        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§2§l  BudLords Guides");
        sender.sendMessage("");
        sender.sendMessage("§e  Available Guides:");
        sender.sendMessage("§7  /budlords guide growing §8- §fHow to grow plants");
        sender.sendMessage("§7  /budlords guide joint §8- §fHow to roll joints");
        sender.sendMessage("§7  /budlords guide selling §8- §fHow to sell products");
        sender.sendMessage("§7  /budlords guide quality §8- §fStar quality system");
        sender.sendMessage("§7  /budlords guide crossbreed §8- §fHow to crossbreed");
        sender.sendMessage("§7  /budlords guide formations §8- §fPlant formations");
        sender.sendMessage("§8§m                                          ");
    }
    
    private void showGuide(CommandSender sender, String topic) {
        switch (topic) {
            case "growing", "farm", "farming" -> showGrowingGuide(sender);
            case "joint", "joints", "rolling" -> showJointGuide(sender);
            case "selling", "sell", "trade" -> showSellingGuide(sender);
            case "quality", "star", "stars" -> showQualityGuide(sender);
            case "crossbreed", "breed" -> showCrossbreedGuide(sender);
            case "formations", "formation", "666" -> showFormationsGuide(sender);
            default -> showGuideMenu(sender);
        }
    }
    
    private void showJointGuide(CommandSender sender) {
        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§6§l  How to Roll Joints");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 1: §7Get the required items");
        sender.sendMessage("§7  - §fGrinder §7(buy from Market Joe)");
        sender.sendMessage("§7  - §fRolling Paper §7(buy from Market Joe)");
        sender.sendMessage("§7  - §fTobacco §7(buy from Market Joe)");
        sender.sendMessage("§7  - §fCannabis Buds §7(harvest from plants)");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 2: §7Grind your buds");
        sender.sendMessage("§7  Hold §fGrinder §7in main hand and");
        sender.sendMessage("§7  §fBuds §7in off-hand, then right-click!");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 3: §7Roll the joint");
        sender.sendMessage("§7  Hold §fGrinded Bud §7and right-click!");
        sender.sendMessage("§7  (Make sure you have Paper and Tobacco)");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 4: §7Complete the minigame!");
        sender.sendMessage("§7  Follow the on-screen instructions");
        sender.sendMessage("§7  for each rolling stage.");
        sender.sendMessage("§8§m                                          ");
    }
    
    private void showGrowingGuide(CommandSender sender) {
        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§2§l  How to Grow Plants");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 1: §7Get a Growing Pot");
        sender.sendMessage("§7  Buy from Market Joe (better stars = faster growth)");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 2: §7Place the pot");
        sender.sendMessage("§7  Right-click on any block to place it");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 3: §7Plant seeds");
        sender.sendMessage("§7  Right-click the pot with seeds in hand");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 4: §7Care for your plant");
        sender.sendMessage("§7  - Use §fWater Bucket §7or §fWatering Can §7to water");
        sender.sendMessage("§7  - Use §fFertilizer §7to boost nutrients");
        sender.sendMessage("§7  - Use §fGrow Lamp §7for faster growth");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 5: §7Harvest");
        sender.sendMessage("§7  Right-click mature plant to harvest!");
        sender.sendMessage("§7  Use §fHarvest Scissors §7for bonus yield!");
        sender.sendMessage("§8§m                                          ");
    }
    
    private void showSellingGuide(CommandSender sender) {
        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§a§l  How to Sell Products");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 1: §7Package your buds");
        sender.sendMessage("§7  Drop buds on ground, then drop");
        sender.sendMessage("§7  a packaging item (bags/packs) on them!");
        sender.sendMessage("§7  Or use: §f/package <amount>");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 2: §7Find a buyer");
        sender.sendMessage("§7  - §aMarket Joe §7- Standard prices");
        sender.sendMessage("§7  - §5BlackMarket Joe §7- Premium prices (+50%)");
        sender.sendMessage("§7  - §eVillage Vendors §7- Lower prices (-20%)");
        sender.sendMessage("");
        sender.sendMessage("§e  Step 3: §7Sell");
        sender.sendMessage("§7  Hold packaged product and right-click");
        sender.sendMessage("§7  the NPC to open the selling interface!");
        sender.sendMessage("");
        sender.sendMessage("§e  Tips:");
        sender.sendMessage("§7  - Higher quality = higher prices");
        sender.sendMessage("§7  - Rare strains sell for more");
        sender.sendMessage("§8§m                                          ");
    }
    
    private void showQualityGuide(CommandSender sender) {
        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§e§l  Star Quality System");
        sender.sendMessage("");
        sender.sendMessage("§7  Items in BudLords have §e1-5 stars§7:");
        sender.sendMessage("§7  §7★☆☆☆☆ §8- Basic");
        sender.sendMessage("§e  ★★☆☆☆ §8- Improved");
        sender.sendMessage("§a  ★★★☆☆ §8- Good");
        sender.sendMessage("§9  ★★★★☆ §8- Excellent");
        sender.sendMessage("§6  ★★★★★ §8- Legendary");
        sender.sendMessage("");
        sender.sendMessage("§e  Better stars provide:");
        sender.sendMessage("§7  - Faster plant growth");
        sender.sendMessage("§7  - Higher quality harvests");
        sender.sendMessage("§7  - Better yields");
        sender.sendMessage("§7  - Higher selling prices");
        sender.sendMessage("");
        sender.sendMessage("§e  Star-rated items:");
        sender.sendMessage("§7  Pots, Lamps, Fertilizer, Seeds,");
        sender.sendMessage("§7  Buds, Scissors, Watering Cans");
        sender.sendMessage("§8§m                                          ");
    }
    
    private void showCrossbreedGuide(CommandSender sender) {
        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§d§l  Crossbreeding Guide");
        sender.sendMessage("");
        sender.sendMessage("§7  Create new strains by combining");
        sender.sendMessage("§7  two parent strains!");
        sender.sendMessage("");
        sender.sendMessage("§e  How to crossbreed:");
        sender.sendMessage("§7  1. Open the lab: §f/crossbreed");
        sender.sendMessage("§7  2. Drop seeds into Parent 1 slot");
        sender.sendMessage("§7  3. Drop seeds into Parent 2 slot");
        sender.sendMessage("§7  4. Click Crossbreed button");
        sender.sendMessage("§7  5. Pay the fee to create new strain!");
        sender.sendMessage("");
        sender.sendMessage("§e  Tips:");
        sender.sendMessage("§7  - Higher rarity parents = better results");
        sender.sendMessage("§7  - There's a chance for mutations!");
        sender.sendMessage("§7  - Legendary parents have bonus mutation chance");
        sender.sendMessage("§8§m                                          ");
    }
    
    private void showFormationsGuide(CommandSender sender) {
        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§5§l  Plant Formation Bonuses");
        sender.sendMessage("");
        sender.sendMessage("§7  When you plant §asame-strain plants §7in");
        sender.sendMessage("§7  specific patterns, they gain §eformation bonuses§7!");
        sender.sendMessage("");
        sender.sendMessage("§e  Formation Tiers:");
        sender.sendMessage("§7  §7Basic (0 XP) §8- Line, Corner, L-Shape");
        sender.sendMessage("§e  §eMedium (500 XP) §8- T-Shape, Cross, Square");
        sender.sendMessage("§b  §bAdvanced (1500 XP) §8- Diamond, Star, Spiral");
        sender.sendMessage("§d  §dMaster (3000 XP) §8- Pentagon, Hexagon");
        sender.sendMessage("§6  §6Legendary (5000 XP) §8- Yin-Yang, Infinity");
        sender.sendMessage("§c  §cMythic (10000 XP) §8- Dragon, Phoenix");
        sender.sendMessage("");
        sender.sendMessage("§4§l  SECRET: 666 Demon Formation");
        sender.sendMessage("§7  §7Plant §c6 pots §7in the §4666 pattern§7:");
        sender.sendMessage("§7    §c    P");
        sender.sendMessage("§7    §c  P");
        sender.sendMessage("§7    §cP");
        sender.sendMessage("§7  §f[C] §8← §7Center pot");
        sender.sendMessage("§7    §cP");
        sender.sendMessage("§7    §c  P");
        sender.sendMessage("§7    §c    P");
        sender.sendMessage("");
        sender.sendMessage("§4  Effect: §7Center pot upgraded §a+1 star§7!");
        sender.sendMessage("§7  §7Pattern must be exact same strain");
        sender.sendMessage("§7  §7No farming XP required - always available!");
        sender.sendMessage("");
        sender.sendMessage("§e  Tips:");
        sender.sendMessage("§7  - Formations grant §a+star bonuses §7on harvest");
        sender.sendMessage("§7  - Higher XP = more formations unlocked");
        sender.sendMessage("§7  - Special effects trigger randomly!");
        sender.sendMessage("§8§m                                          ");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> options = new ArrayList<>();
            
            // Guide options
            if ("guide".startsWith(input)) options.add("guide");
            if ("joint".startsWith(input)) options.add("joint");
            if ("growing".startsWith(input)) options.add("growing");
            if ("selling".startsWith(input)) options.add("selling");
            
            // Admin options
            if (sender.hasPermission("budlords.admin")) {
                if ("reload".startsWith(input)) options.add("reload");
            }
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("guide")) {
            String input = args[1].toLowerCase();
            List<String> guides = Arrays.asList("growing", "joint", "selling", "quality", "crossbreed", "formations");
            List<String> filtered = new ArrayList<>();
            for (String guide : guides) {
                if (guide.startsWith(input)) filtered.add(guide);
            }
            return filtered;
        }
        return new ArrayList<>();
    }
}
