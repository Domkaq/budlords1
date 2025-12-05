package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.diseases.PlantDisease;
import com.budlords.farming.FarmingManager;
import com.budlords.farming.Plant;
import com.budlords.quality.*;
import com.budlords.strain.Strain;
import com.budlords.strain.StrainManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
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
    
    /**
     * Prevents watering can items from being converted to plain water buckets
     * when the player picks up water. This event fires BEFORE the bucket becomes
     * a water bucket.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemStack();
        
        // Check if the item being used is a watering can
        if (plugin.getQualityItemManager().isWateringCanItem(itemInHand)) {
            // Cancel the default bucket fill behavior
            event.setCancelled(true);
            
            // The custom filling is handled by handleWateringCan in onPlayerInteract
            // But sometimes the bucket fill event fires first, so we handle it here too
            QualityItemManager qim = plugin.getQualityItemManager();
            
            int currentWater = qim.getWateringCanWater(itemInHand);
            int maxCapacity = qim.getWateringCanMaxCapacity(itemInHand);
            
            if (currentWater >= maxCapacity) {
                player.sendMessage("§7Your watering can is already full!");
                return;
            }
            
            // Fill the can (fills to full)
            ItemStack filledCan = qim.fillWateringCan(itemInHand);
            player.getInventory().setItemInMainHand(filledCan);
            // After filling, water level is maxCapacity
            player.sendMessage("§bWatering can filled! §7Water: " + maxCapacity + "/" + maxCapacity);
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 0.5f, 1.2f);
        }
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
        
        // Check if using a cure item on a plant
        if (isCureItem(item)) {
            handleCureUsage(event, player, item, clickedBlock);
            return;
        }

        // Check if harvesting a plant (without scissors)
        if (clickedBlock.getType() == Material.WHEAT) {
            handlePlantInteraction(event, player, clickedBlock);
        }
    }
    
    /**
     * Checks if an item is a cure item.
     */
    private boolean isCureItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line.equals("§8Type: cure")) return true;
        }
        return false;
    }
    
    /**
     * Gets the cure type from a cure item.
     */
    private PlantDisease.Cure getCureFromItem(ItemStack item) {
        if (!isCureItem(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8Cure: ")) {
                String cureName = line.substring(8);
                try {
                    return PlantDisease.Cure.valueOf(cureName);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    /**
     * Handles using a cure item on a plant.
     */
    private void handleCureUsage(PlayerInteractEvent event, Player player, ItemStack item, Block clickedBlock) {
        // Find plant at or near clicked location
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        if (plant == null) {
            plant = farmingManager.getPlantAt(clickedBlock.getRelative(BlockFace.UP).getLocation());
        }
        if (plant == null) {
            plant = farmingManager.getPlantAt(clickedBlock.getRelative(BlockFace.DOWN).getLocation());
        }
        
        if (plant == null) {
            player.sendMessage("§cNo plant found nearby! Click on or near an infected plant.");
            return;
        }
        
        PlantDisease.Cure cure = getCureFromItem(item);
        if (cure == null) {
            player.sendMessage("§cInvalid cure item!");
            return;
        }
        
        event.setCancelled(true);
        
        // Try to cure the plant using the disease manager
        if (plugin.getDiseaseManager() != null) {
            boolean success = plugin.getDiseaseManager().curePlant(player, plant.getLocation(), cure);
            if (success) {
                // Consume the cure item
                if (player.getGameMode() != GameMode.CREATIVE) {
                    if (item.getAmount() == 1) {
                        player.getInventory().setItemInMainHand(null);
                    } else {
                        item.setAmount(item.getAmount() - 1);
                    }
                }
            }
        } else {
            player.sendMessage("§cDisease system is not available!");
        }
    }
    
    private void handlePotPlacement(PlayerInteractEvent event, Player player, ItemStack item, Block clickedBlock) {
        Block targetBlock = clickedBlock.getRelative(BlockFace.UP);
        
        if (targetBlock.getType() != Material.AIR) {
            player.sendMessage("§cCannot place pot here - space is not empty!");
            return;
        }
        
        // Check if there's already a pot or plant at the adjacent locations
        // This prevents issues with pots being placed next to each other
        Block below = targetBlock.getRelative(BlockFace.DOWN);
        if (below.getType() == Material.FLOWER_POT && farmingManager.hasPotAt(below.getLocation())) {
            // Allow placing next to existing pot
        }
        
        event.setCancelled(true);
        
        StarRating potRating = GrowingPot.getRatingFromItem(item);
        if (potRating == null) potRating = StarRating.ONE_STAR;
        
        // Place the pot block
        targetBlock.setType(Material.FLOWER_POT);
        
        // Register the pot in the tracking system with its star rating
        farmingManager.placePot(targetBlock.getLocation(), potRating, player.getUniqueId());
        
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
        event.setCancelled(true);
        
        StarRating lampRating = GrowLamp.getRatingFromItem(item);
        if (lampRating == null) lampRating = StarRating.ONE_STAR;
        
        // Calculate lamp effect radius based on star rating (1-5 blocks)
        int lampRadius = lampRating.getStars();
        
        // Find all plants within the lamp's effect radius
        Location centerLocation = clickedBlock.getLocation().add(0.5, 0.5, 0.5);
        List<Plant> nearbyPlants = farmingManager.getNearbyPlants(centerLocation, lampRadius);
        
        if (nearbyPlants.isEmpty()) {
            // Also try to search around the clicked location in case it's far from plants
            nearbyPlants = farmingManager.getNearbyPlants(clickedBlock.getRelative(BlockFace.UP).getLocation(), lampRadius);
        }
        
        if (nearbyPlants.isEmpty()) {
            nearbyPlants = farmingManager.getNearbyPlants(clickedBlock.getRelative(BlockFace.DOWN).getLocation(), lampRadius);
        }
        
        if (nearbyPlants.isEmpty()) {
            player.sendMessage("§cNo plants found nearby! Place the lamp closer to growing plants.");
            player.sendMessage("§7Lamp range: §e" + lampRadius + " blocks §7based on star rating.");
            return;
        }
        
        // Apply lamp buff to ALL nearby plants
        int affectedCount = 0;
        for (Plant plant : nearbyPlants) {
            // Apply the lamp rating to each plant
            plant.setLampRating(lampRating);
            affectedCount++;
        }
        
        // Consume lamp item (only consume 1 lamp even if multiple plants affected)
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (item.getAmount() == 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                item.setAmount(item.getAmount() - 1);
            }
        }
        
        // Show success message with affected plant count
        player.sendMessage("§a✦ Installed " + lampRating.getDisplay() + " §aGrow Lamp!");
        player.sendMessage("§7Affected plants: §e" + affectedCount);
        player.sendMessage("§7Light bonus radius: §e" + lampRadius + " blocks");
        
        // Lamp glow particles for all affected area
        Location particleLoc = centerLocation.clone().add(0, 1.0, 0);
        centerLocation.getWorld().spawnParticle(Particle.GLOW, particleLoc, 20 + (lampRating.getStars() * 5), 
            lampRadius * 0.5, 0.5, lampRadius * 0.5, 0.02);
        centerLocation.getWorld().playSound(centerLocation, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.2f);
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
            
            // Get pot rating from the tracking system
            potRating = farmingManager.getPotRatingAt(clickedBlock.getLocation());
            if (potRating == null) {
                // If pot is not tracked (legacy pot), try to detect from world
                // or default to one star
                potRating = StarRating.ONE_STAR;
                // Register this pot as 1-star since it wasn't tracked
                farmingManager.placePot(clickedBlock.getLocation(), potRating, player.getUniqueId());
            }
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
        QualityItemManager qim = plugin.getQualityItemManager();
        
        // Check if right-clicking on water to fill the can
        if (clickedBlock.getType() == Material.WATER) {
            event.setCancelled(true);
            
            int currentWater = qim.getWateringCanWater(item);
            int maxCapacity = qim.getWateringCanMaxCapacity(item);
            
            if (currentWater >= maxCapacity) {
                player.sendMessage("§7Your watering can is already full!");
                return;
            }
            
            // Fill the can
            ItemStack filledCan = qim.fillWateringCan(item);
            player.getInventory().setItemInMainHand(filledCan);
            player.sendMessage("§bFilled watering can! §7(" + maxCapacity + "/" + maxCapacity + " water)");
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 0.5f, 1.2f);
            return;
        }
        
        // Check if watering a plant
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        if (plant == null) {
            plant = farmingManager.getPlantAt(clickedBlock.getRelative(BlockFace.UP).getLocation());
        }
        
        if (plant == null) {
            // Check if clicking on a pot - maybe want to fill from cauldron or water nearby
            return;
        }
        
        event.setCancelled(true);
        
        // Check if can has water
        int currentWater = qim.getWateringCanWater(item);
        if (currentWater <= 0) {
            player.sendMessage("§cYour watering can is empty! §7Right-click water to fill it.");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.5f, 1.0f);
            return;
        }
        
        // Get watering can quality for bonus
        StarRating wateringCanRating = qim.getWateringCanRating(item);
        
        // Water the plant with quality bonus
        if (farmingManager.waterPlant(player, plant.getLocation(), wateringCanRating)) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                // Decrease water level
                int newWater = currentWater - 1;
                int maxCapacity = qim.getWateringCanMaxCapacity(item);
                ItemStack updatedCan = qim.setWateringCanWater(item, newWater);
                player.getInventory().setItemInMainHand(updatedCan);
                
                if (newWater > 0) {
                    player.sendMessage("§7Watering can: " + newWater + "/" + maxCapacity + " water remaining");
                } else {
                    player.sendMessage("§cWatering can is now empty.");
                }
            }
        }
    }
    
    private void handleWatering(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // Try to find plant at clicked location
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        
        // Check block above (for pot-based plants where player clicks on pot)
        if (plant == null) {
            plant = farmingManager.getPlantAt(clickedBlock.getRelative(BlockFace.UP).getLocation());
        }
        
        // Check block below (player may have clicked above the plant)
        if (plant == null) {
            plant = farmingManager.getPlantAt(clickedBlock.getRelative(BlockFace.DOWN).getLocation());
        }
        
        // Also check adjacent blocks horizontally
        if (plant == null) {
            BlockFace[] horizontalFaces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            for (BlockFace face : horizontalFaces) {
                plant = farmingManager.getPlantAt(clickedBlock.getRelative(face).getLocation());
                if (plant != null) break;
            }
        }
        
        if (plant == null) {
            // Provide feedback when no plant is found
            player.sendMessage("§7No plant found nearby. Click directly on a plant or pot to water it.");
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
                        
                        // Remove pot from tracking
                        farmingManager.removePot(block.getLocation());
                        
                        // Return lamp if present
                        if (plant.getLampRating() != null) {
                            ItemStack lamp = plugin.getQualityItemManager().createLamp(plant.getLampRating(), 1);
                            event.getPlayer().getWorld().dropItemNaturally(block.getLocation(), lamp);
                        }
                    }
                }
            } else if (block.getType() == Material.FLOWER_POT) {
                // Breaking an empty pot - return the pot item with its star rating
                GrowingPot removedPot = farmingManager.removePot(block.getLocation());
                if (removedPot != null) {
                    event.setCancelled(true);
                    block.setType(Material.AIR);
                    
                    ItemStack potItem = GrowingPot.createPotItem(removedPot.getStarRating(), 1);
                    event.getPlayer().getWorld().dropItemNaturally(block.getLocation(), potItem);
                    event.getPlayer().sendMessage("§7Returned pot: " + removedPot.getStarRating().getDisplay());
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
        
        // Update collection book
        if (plugin.getCollectionManager() != null) {
            plugin.getCollectionManager().addToCollection(player, strain.getId());
            plugin.getCollectionManager().saveCollections();
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
        
        // Award skill XP for harvesting (Farming tree)
        if (plugin.getSkillManager() != null) {
            com.budlords.skills.SkillManager skillManager = plugin.getSkillManager();
            java.util.UUID uuid = player.getUniqueId();
            
            // Award Farming XP - base 5 XP per harvest, bonus for quality
            int farmingXP = 5;
            if (finalRating == StarRating.FIVE_STAR) {
                farmingXP += 10; // Bonus for 5-star harvest
            }
            if (strain.getRarity() == Strain.Rarity.LEGENDARY) {
                farmingXP += 15; // Bonus for legendary strain
            }
            if (plant.getQuality() >= 90) {
                farmingXP += 5; // Bonus for perfect harvest
            }
            skillManager.addTreeXP(uuid, com.budlords.skills.Skill.SkillTree.FARMING, farmingXP);
            
            // Award Quality XP based on bud quality
            int qualityXP = (int) (plant.getQuality() / 20); // 0-5 XP based on quality
            if (qualityXP > 0) {
                skillManager.addTreeXP(uuid, com.budlords.skills.Skill.SkillTree.QUALITY, qualityXP);
            }
        }
        
        // Sync achievements with stats
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().syncWithStats(player);
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
