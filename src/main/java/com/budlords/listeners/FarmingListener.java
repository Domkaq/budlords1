package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.farming.FarmingManager;
import com.budlords.farming.Plant;
import com.budlords.quality.*;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FarmingListener implements Listener {

    private final BudLords plugin;
    private final FarmingManager farmingManager;
    private final StrainManager strainManager;

    public FarmingListener(BudLords plugin, FarmingManager farmingManager, StrainManager strainManager) {
        this.plugin = plugin;
        this.farmingManager = farmingManager;
        this.strainManager = strainManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) return;

        // Check if placing a pot
        if (GrowingPot.isPotItem(item)) {
            handlePotPlacement(event, player, item, clickedBlock);
            return;
        }
        
        // Check if using a lamp on a plant
        if (GrowLamp.isLampItem(item)) {
            handleLampUsage(event, player, item, clickedBlock);
            return;
        }

        // Check if planting a seed
        if (strainManager.isSeedItem(item)) {
            handleSeedPlanting(event, player, item, clickedBlock);
            return;
        }
        
        // Check if using fertilizer
        if (Fertilizer.isFertilizerItem(item)) {
            handleFertilizer(event, player, item, clickedBlock);
            return;
        }
        
        // Check if using watering can (BudLords custom item)
        if (plugin.getQualityItemManager().isWateringCanItem(item)) {
            handleWateringCan(event, player, item, clickedBlock);
            return;
        }
        
        // Check if using watering (water bucket on plant)
        if (item.getType() == Material.WATER_BUCKET) {
            handleWatering(event, player, clickedBlock);
            return;
        }
        
        // Check if harvesting with scissors
        if (HarvestScissors.isScissorsItem(item)) {
            handleScissorsHarvest(event, player, item, clickedBlock);
            return;
        }

        // Check if harvesting a plant (without scissors)
        if (clickedBlock.getType() == Material.WHEAT) {
            handlePlantInteraction(event, player, clickedBlock);
        }
    }
    
    private void handlePotPlacement(PlayerInteractEvent event, Player player, ItemStack item, Block clickedBlock) {
        Block targetBlock = clickedBlock.getRelative(BlockFace.UP);
        
        if (targetBlock.getType() != Material.AIR) {
            player.sendMessage("§cCannot place pot here - space is not empty!");
            return;
        }
        
        event.setCancelled(true);
        
        StarRating potRating = GrowingPot.getRatingFromItem(item);
        if (potRating == null) potRating = StarRating.ONE_STAR;
        
        // Place the pot block
        targetBlock.setType(Material.FLOWER_POT);
        
        player.sendMessage("§aPlaced " + potRating.getDisplay() + " §aGrowing Pot!");
        player.sendMessage("§7Right-click with seeds to plant.");
        
        // Consume pot item
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (item.getAmount() == 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
    
    private void handleLampUsage(PlayerInteractEvent event, Player player, ItemStack item, Block clickedBlock) {
        // Check if there's a plant at the clicked location or above a pot
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        if (plant == null) {
            // Check block above (for pot-based plants)
            plant = farmingManager.getPlantAt(clickedBlock.getRelative(BlockFace.UP).getLocation());
        }
        
        if (plant == null) {
            player.sendMessage("§cNo plant found! Use the lamp on a growing plant.");
            return;
        }
        
        event.setCancelled(true);
        
        StarRating lampRating = GrowLamp.getRatingFromItem(item);
        if (lampRating == null) lampRating = StarRating.ONE_STAR;
        
        if (farmingManager.addLamp(player, plant.getLocation(), lampRating)) {
            // Consume lamp item
            if (player.getGameMode() != GameMode.CREATIVE) {
                if (item.getAmount() == 1) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }

    private void handleSeedPlanting(PlayerInteractEvent event, Player player, ItemStack item, Block clickedBlock) {
        Block targetBlock;
        StarRating potRating = null;
        boolean isPotPlanting = false;
        
        // Check if clicking on a pot (new system) or farmland (legacy)
        if (clickedBlock.getType() == Material.FLOWER_POT) {
            // Plant on top of the pot (one block above)
            targetBlock = clickedBlock.getRelative(BlockFace.UP);
            isPotPlanting = true;
            
            // Check if space above pot is empty
            if (targetBlock.getType() != Material.AIR) {
                player.sendMessage("§cCannot plant here - space above pot is not empty!");
                return;
            }
            
            // For pot-based planting, try to get pot rating from stored data
            // Default to basic pot if not found
            potRating = StarRating.ONE_STAR; // Could be enhanced with pot NBT data
        } else if (clickedBlock.getType() == Material.FARMLAND) {
            targetBlock = clickedBlock.getRelative(BlockFace.UP);
            
            if (targetBlock.getType() != Material.AIR) {
                return;
            }
        } else {
            return;
        }
        
        String strainId = strainManager.getStrainIdFromItem(item);
        if (strainId == null) return;
        
        event.setCancelled(true);
        
        // Get seed star rating
        StarRating seedRating = strainManager.getSeedStarRating(item);
        if (seedRating == null) seedRating = StarRating.ONE_STAR;
        
        boolean success;
        if (isPotPlanting) {
            // Pot-based planting - plant is placed above the pot
            success = farmingManager.plantSeed(player, targetBlock.getLocation(), strainId, potRating, seedRating);
        } else {
            // Legacy farmland planting
            success = farmingManager.plantSeed(player, targetBlock.getLocation(), strainId);
        }
        
        if (success) {
            // Consume seed
            if (player.getGameMode() != GameMode.CREATIVE) {
                if (item.getAmount() == 1) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }
    
    private void handleFertilizer(PlayerInteractEvent event, Player player, ItemStack item, Block clickedBlock) {
        // Check if there's a plant at the clicked location
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        if (plant == null) {
            // Also check block above (for pot-based plants)
            plant = farmingManager.getPlantAt(clickedBlock.getRelative(BlockFace.UP).getLocation());
        }
        
        if (plant == null) {
            return;
        }
        
        event.setCancelled(true);
        
        StarRating fertilizerRating = Fertilizer.getRatingFromItem(item);
        if (fertilizerRating == null) fertilizerRating = StarRating.ONE_STAR;
        
        if (farmingManager.fertilizePlant(player, plant.getLocation(), fertilizerRating)) {
            // Consume fertilizer
            if (player.getGameMode() != GameMode.CREATIVE) {
                if (item.getAmount() == 1) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }
    
    private void handleWateringCan(PlayerInteractEvent event, Player player, ItemStack item, Block clickedBlock) {
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        if (plant == null) {
            plant = farmingManager.getPlantAt(clickedBlock.getRelative(BlockFace.UP).getLocation());
        }
        
        if (plant == null) {
            return;
        }
        
        event.setCancelled(true);
        
        // Get watering can rating for efficiency bonus
        StarRating canRating = plugin.getQualityItemManager().getWateringCanRating(item);
        if (canRating == null) canRating = StarRating.ONE_STAR;
        
        if (farmingManager.waterPlant(player, plant.getLocation())) {
            // Watering can has durability - track uses via lore
            if (player.getGameMode() != GameMode.CREATIVE) {
                int maxUses = canRating.getStars() * 5;
                int currentUses = getWateringCanUses(item);
                currentUses++;
                
                if (currentUses >= maxUses) {
                    // Watering can is depleted - remove it
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage("§cYour watering can has run out of water!");
                } else {
                    // Update uses in lore
                    setWateringCanUses(item, currentUses, maxUses);
                    player.sendMessage("§aWatered plant! §7(" + (maxUses - currentUses) + " uses remaining)");
                }
            }
        }
    }
    
    private int getWateringCanUses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return 0;
        
        List<String> lore = meta.getLore();
        if (lore == null) return 0;
        
        for (String line : lore) {
            if (line.startsWith("§8Uses: ")) {
                try {
                    String usesStr = line.substring(8).split("/")[0];
                    return Integer.parseInt(usesStr);
                } catch (Exception e) {
                    return 0;
                }
            }
        }
        return 0;
    }
    
    private void setWateringCanUses(ItemStack item, int uses, int maxUses) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        List<String> lore = meta.getLore();
        if (lore == null) lore = new java.util.ArrayList<>();
        
        // Find and update or add the uses line
        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).startsWith("§8Uses: ")) {
                lore.set(i, "§8Uses: " + uses + "/" + maxUses);
                found = true;
                break;
            }
        }
        
        if (!found) {
            // Add uses before the Rating line
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).startsWith("§8Rating: ")) {
                    lore.add(i, "§8Uses: " + uses + "/" + maxUses);
                    break;
                }
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
    
    private void handleWatering(PlayerInteractEvent event, Player player, Block clickedBlock) {
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        if (plant == null) {
            plant = farmingManager.getPlantAt(clickedBlock.getRelative(BlockFace.UP).getLocation());
        }
        
        if (plant == null) {
            return;
        }
        
        event.setCancelled(true);
        
        if (farmingManager.waterPlant(player, plant.getLocation())) {
            // Replace water bucket with empty bucket
            if (player.getGameMode() != GameMode.CREATIVE) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
            }
        }
    }
    
    private void handleScissorsHarvest(PlayerInteractEvent event, Player player, ItemStack item, Block clickedBlock) {
        if (clickedBlock.getType() != Material.WHEAT) {
            return;
        }
        
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        if (plant == null) return;
        
        event.setCancelled(true);
        
        if (!plant.isFullyGrown()) {
            player.sendMessage("§eThis plant is " + plant.getGrowthStageName().toLowerCase() + 
                              " (" + (plant.getGrowthStage() + 1) + "/4 stages)");
            return;
        }
        
        StarRating scissorsRating = HarvestScissors.getRatingFromItem(item);
        if (scissorsRating == null) scissorsRating = StarRating.ONE_STAR;
        
        Plant harvested = farmingManager.harvestPlant(player, clickedBlock.getLocation(), scissorsRating);
        if (harvested != null) {
            giveHarvestWithScissors(player, harvested, scissorsRating);
        }
    }

    private void handlePlantInteraction(PlayerInteractEvent event, Player player, Block clickedBlock) {
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        if (plant != null) {
            event.setCancelled(true);
            
            if (plant.isFullyGrown()) {
                Plant harvested = farmingManager.harvestPlant(player, clickedBlock.getLocation());
                if (harvested != null) {
                    giveHarvest(player, harvested);
                }
            } else {
                // Show plant status with star info
                showPlantStatus(player, plant);
            }
        }
    }
    
    private void showPlantStatus(Player player, Plant plant) {
        Strain strain = strainManager.getStrain(plant.getStrainId());
        String strainName = strain != null ? strain.getName() : "Unknown";
        
        player.sendMessage("");
        player.sendMessage("§2§l" + strainName + " §7- " + plant.getGrowthStageName());
        player.sendMessage("§7Growth: §e" + (plant.getGrowthStage() + 1) + "/4 stages");
        
        if (plant.hasPot()) {
            if (plant.getPotRating() != null) {
                player.sendMessage("§7Pot: " + plant.getPotRating().getDisplay());
            }
            if (plant.getSeedRating() != null) {
                player.sendMessage("§7Seed: " + plant.getSeedRating().getDisplay());
            }
            player.sendMessage("§7Water: §b" + String.format("%.0f%%", plant.getWaterLevel() * 100));
            player.sendMessage("§7Nutrients: §e" + String.format("%.0f%%", plant.getNutrientLevel() * 100));
            if (plant.getLampRating() != null) {
                player.sendMessage("§7Lamp: " + plant.getLampRating().getDisplay());
            }
        }
        
        player.sendMessage("§7Quality: " + getQualityDisplay(plant.getQuality()));
        player.sendMessage("");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Check if breaking a plant
        if (block.getType() == Material.WHEAT) {
            Plant plant = farmingManager.getPlantAt(block.getLocation());
            if (plant != null) {
                event.setCancelled(true);
                event.setDropItems(false);
                
                Player player = event.getPlayer();
                
                if (plant.isFullyGrown()) {
                    // Check for scissors in hand for bonus
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    StarRating scissorsRating = null;
                    if (HarvestScissors.isScissorsItem(handItem)) {
                        scissorsRating = HarvestScissors.getRatingFromItem(handItem);
                    }
                    
                    Plant harvested = farmingManager.harvestPlant(player, block.getLocation(), scissorsRating);
                    if (harvested != null) {
                        if (scissorsRating != null) {
                            giveHarvestWithScissors(player, harvested, scissorsRating);
                        } else {
                            giveHarvest(player, harvested);
                        }
                        
                        // Return pot if plant was pot-based
                        if (harvested.hasPot() && harvested.getPotRating() != null) {
                            ItemStack pot = GrowingPot.createPotItem(harvested.getPotRating(), 1);
                            HashMap<Integer, ItemStack> potLeftover = player.getInventory().addItem(pot);
                            if (!potLeftover.isEmpty()) {
                                potLeftover.values().forEach(i -> 
                                    player.getWorld().dropItemNaturally(player.getLocation(), i)
                                );
                            }
                            player.sendMessage("§7Returned pot: " + harvested.getPotRating().getDisplay());
                            
                            // Return lamp if present
                            if (harvested.getLampRating() != null) {
                                ItemStack lamp = plugin.getQualityItemManager().createLamp(harvested.getLampRating(), 1);
                                HashMap<Integer, ItemStack> lampLeftover = player.getInventory().addItem(lamp);
                                if (!lampLeftover.isEmpty()) {
                                    lampLeftover.values().forEach(i -> 
                                        player.getWorld().dropItemNaturally(player.getLocation(), i)
                                    );
                                }
                                player.sendMessage("§7Returned lamp: " + harvested.getLampRating().getDisplay());
                            }
                        }
                    }
                } else {
                    // Return seed if not fully grown
                    Strain strain = strainManager.getStrain(plant.getStrainId());
                    if (strain != null) {
                        StarRating seedRating = plant.getSeedRating() != null ? 
                            plant.getSeedRating() : StarRating.ONE_STAR;
                        ItemStack seed = strainManager.createSeedItem(strain, 1, seedRating);
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(seed);
                        if (!leftover.isEmpty()) {
                            leftover.values().forEach(i -> 
                                player.getWorld().dropItemNaturally(player.getLocation(), i)
                            );
                        }
                    }
                    
                    // Return pot if plant was pot-based
                    if (plant.hasPot() && plant.getPotRating() != null) {
                        ItemStack pot = GrowingPot.createPotItem(plant.getPotRating(), 1);
                        HashMap<Integer, ItemStack> potLeftover = player.getInventory().addItem(pot);
                        if (!potLeftover.isEmpty()) {
                            potLeftover.values().forEach(i -> 
                                player.getWorld().dropItemNaturally(player.getLocation(), i)
                            );
                        }
                        player.sendMessage("§7Returned pot: " + plant.getPotRating().getDisplay());
                        
                        // Return lamp if present
                        if (plant.getLampRating() != null) {
                            ItemStack lamp = plugin.getQualityItemManager().createLamp(plant.getLampRating(), 1);
                            HashMap<Integer, ItemStack> lampLeftover = player.getInventory().addItem(lamp);
                            if (!lampLeftover.isEmpty()) {
                                lampLeftover.values().forEach(i -> 
                                    player.getWorld().dropItemNaturally(player.getLocation(), i)
                                );
                            }
                            player.sendMessage("§7Returned lamp: " + plant.getLampRating().getDisplay());
                        }
                    }
                    
                    farmingManager.removePlant(block.getLocation());
                    block.setType(Material.AIR);
                    player.sendMessage("§eHarvested early - returned seed.");
                }
            }
        }
        
        // Check if breaking farmland with plant on top
        if (block.getType() == Material.FARMLAND || block.getType() == Material.FLOWER_POT) {
            Block above = block.getRelative(BlockFace.UP);
            if (above.getType() == Material.WHEAT) {
                Plant plant = farmingManager.getPlantAt(above.getLocation());
                if (plant != null) {
                    // Remove the plant
                    farmingManager.removePlant(above.getLocation());
                    above.setType(Material.AIR);
                    
                    // Return seed
                    Strain strain = strainManager.getStrain(plant.getStrainId());
                    if (strain != null) {
                        StarRating seedRating = plant.getSeedRating() != null ? 
                            plant.getSeedRating() : StarRating.ONE_STAR;
                        ItemStack seed = strainManager.createSeedItem(strain, 1, seedRating);
                        event.getPlayer().getWorld().dropItemNaturally(above.getLocation(), seed);
                    }
                    
                    // Return pot if it's pot-based planting and breaking the pot
                    if (block.getType() == Material.FLOWER_POT && plant.hasPot() && plant.getPotRating() != null) {
                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        
                        ItemStack pot = GrowingPot.createPotItem(plant.getPotRating(), 1);
                        event.getPlayer().getWorld().dropItemNaturally(block.getLocation(), pot);
                        
                        // Return lamp if present
                        if (plant.getLampRating() != null) {
                            ItemStack lamp = plugin.getQualityItemManager().createLamp(plant.getLampRating(), 1);
                            event.getPlayer().getWorld().dropItemNaturally(block.getLocation(), lamp);
                        }
                    }
                }
            }
        }
    }
    
    private void giveHarvestWithScissors(Player player, Plant plant, StarRating scissorsRating) {
        Strain strain = strainManager.getStrain(plant.getStrainId());
        if (strain == null) {
            player.sendMessage("§cError: Strain not found!");
            return;
        }
        
        HarvestScissors scissors = new HarvestScissors(scissorsRating);

        // Calculate actual yield based on quality and scissors bonus
        int baseYield = strain.getYield();
        double qualityMultiplier = 0.5 + (plant.getQuality() / 100.0);
        int yieldWithQuality = Math.max(1, (int) Math.round(baseYield * qualityMultiplier));
        int actualYield = scissors.calculateFinalYield(yieldWithQuality);

        // Calculate final bud star rating
        StarRating finalRating = plant.calculateFinalBudRating(scissorsRating);
        
        // Check for quality upgrade from scissors
        if (scissors.triggersQualityUpgrade() && finalRating.getStars() < 5) {
            finalRating = StarRating.fromValue(finalRating.getStars() + 1);
            player.sendMessage("§6✦ Quality Upgrade! §7Scissors improved the bud quality!");
        }
        
        // Check for rare drop
        if (scissors.triggersRareDrop()) {
            // Give bonus seed with higher rating
            StarRating bonusSeedRating = StarRating.fromValue(
                Math.min(5, (plant.getSeedRating() != null ? plant.getSeedRating().getStars() : 1) + 1)
            );
            ItemStack bonusSeed = strainManager.createSeedItem(strain, 1, bonusSeedRating);
            player.getInventory().addItem(bonusSeed);
            player.sendMessage("§d✦ Rare Drop! §7Found a bonus " + bonusSeedRating.getDisplay() + " §7seed!");
        }

        ItemStack buds = strainManager.createBudItem(strain, actualYield, finalRating);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(buds);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> 
                player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
        }
        
        // Update stats and challenges
        updateHarvestStatsAndChallenges(player, plant, strain, finalRating);

        player.sendMessage("§aHarvested §e" + actualYield + "x §a" + strain.getName() + " Buds " + finalRating.getDisplay() + "!");
        player.sendMessage("§7Quality: " + getQualityDisplay(plant.getQuality()));
    }

    private void giveHarvest(Player player, Plant plant) {
        Strain strain = strainManager.getStrain(plant.getStrainId());
        if (strain == null) {
            player.sendMessage("§cError: Strain not found!");
            return;
        }

        // Calculate actual yield based on quality
        int baseYield = strain.getYield();
        double qualityMultiplier = 0.5 + (plant.getQuality() / 100.0);
        int actualYield = Math.max(1, (int) Math.round(baseYield * qualityMultiplier));

        // Calculate final bud star rating (without scissors bonus)
        StarRating finalRating = plant.calculateFinalBudRating(null);

        ItemStack buds = strainManager.createBudItem(strain, actualYield, finalRating);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(buds);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> 
                player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
        }
        
        // Update stats and challenges
        updateHarvestStatsAndChallenges(player, plant, strain, finalRating);

        player.sendMessage("§aHarvested §e" + actualYield + "x §a" + strain.getName() + " Buds " + finalRating.getDisplay() + "!");
        player.sendMessage("§7Quality: " + getQualityDisplay(plant.getQuality()));
        player.sendMessage("§7§oTip: Use Harvest Scissors for better yields!");
    }
    
    /**
     * Updates player stats and challenge progress after a harvest.
     */
    private void updateHarvestStatsAndChallenges(Player player, Plant plant, Strain strain, StarRating finalRating) {
        // Update stats
        if (plugin.getStatsManager() != null) {
            com.budlords.stats.PlayerStats stats = plugin.getStatsManager().getStats(player);
            stats.incrementPlantsHarvested();
            
            if (finalRating == StarRating.FIVE_STAR) {
                stats.incrementFiveStarBuds();
            }
            if (strain.getRarity() == Strain.Rarity.LEGENDARY) {
                stats.incrementLegendaryBuds();
            }
            if (plant.getQuality() >= 90) {
                stats.incrementPerfectHarvests();
            }
        }
        
        // Update challenge progress
        if (plugin.getChallengeManager() != null) {
            plugin.getChallengeManager().updateProgress(player, 
                com.budlords.challenges.Challenge.ChallengeType.HARVEST_PLANTS, 1);
            
            if (finalRating == StarRating.FIVE_STAR) {
                plugin.getChallengeManager().updateProgress(player, 
                    com.budlords.challenges.Challenge.ChallengeType.FIVE_STAR_BUDS, 1);
            }
            if (strain.getRarity() == Strain.Rarity.LEGENDARY) {
                plugin.getChallengeManager().updateProgress(player, 
                    com.budlords.challenges.Challenge.ChallengeType.LEGENDARY_HARVESTS, 1);
            }
            if (plant.getQuality() >= 90) {
                plugin.getChallengeManager().updateProgress(player, 
                    com.budlords.challenges.Challenge.ChallengeType.PERFECT_HARVESTS, 1);
            }
        }
    }

    private String getQualityDisplay(int quality) {
        if (quality >= 90) return "§6★★★★★ Legendary";
        if (quality >= 75) return "§9★★★★☆ Excellent";
        if (quality >= 60) return "§a★★★☆☆ Good";
        if (quality >= 40) return "§e★★☆☆☆ Average";
        return "§c★☆☆☆☆ Poor";
    }
}
