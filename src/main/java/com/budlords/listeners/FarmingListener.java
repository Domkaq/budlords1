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
     * Helper method to update player's main hand item on next tick.
     * This prevents Minecraft from overwriting our changes.
     */
    private void updateMainHandNextTick(Player player, ItemStack item) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();
        });
    }
    
    /**
     * Prevents watering can items from being converted to plain water buckets
     * when the player picks up water. This event fires BEFORE the bucket becomes
     * a water bucket.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        // Get the actual item in the player's hand, not the event's item
        // because event.getItemStack() might return the result (water bucket)
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        
        // Check if the item being used is a watering can
        if (plugin.getQualityItemManager().isWateringCanItem(itemInHand)) {
            // Cancel the default bucket fill behavior - MUST cancel to prevent water bucket
            event.setCancelled(true);
            
            // Set the result to be the original watering can to maintain consistency
            event.setItemStack(itemInHand);
            
            QualityItemManager qim = plugin.getQualityItemManager();
            
            int currentWater = qim.getWateringCanWater(itemInHand);
            int maxCapacity = qim.getWateringCanMaxCapacity(itemInHand);
            
            if (currentWater >= maxCapacity) {
                player.sendMessage("§7Your watering can is already full!");
                return;
            }
            
            // Fill the can (fills to full)
            ItemStack filledCan = qim.fillWateringCan(itemInHand);
            
            // Use helper method to update inventory on next tick
            updateMainHandNextTick(player, filledCan);
            
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
        
        // Check if clicking on a placed lamp to show range (with empty hand or any item)
        if (farmingManager.hasPlacedLampAt(clickedBlock.getLocation())) {
            com.budlords.quality.PlacedLamp placedLamp = farmingManager.getPlacedLampAt(clickedBlock.getLocation());
            if (placedLamp != null) {
                event.setCancelled(true);
                farmingManager.showLampRange(placedLamp);
                player.sendMessage("§e✦ " + placedLamp.getStarRating().getDisplay() + " §eGrow Lamp");
                player.sendMessage("§7Range: §e" + placedLamp.getRange() + " blocks §7(below lamp)");
                player.sendMessage("§7Quality Bonus: §a+" + String.format("%.0f%%", placedLamp.getQualityBonus() * 100));
                return;
            }
        }

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

        // Check if clicking on a pot to show plant info or harvest
        if (clickedBlock.getType() == Material.FLOWER_POT) {
            // Check if there's a plant above the pot
            Location plantLocation = clickedBlock.getRelative(BlockFace.UP).getLocation();
            Plant plant = farmingManager.getPlantAt(plantLocation);
            if (plant != null) {
                handlePlantInteraction(event, player, clickedBlock.getRelative(BlockFace.UP));
                return;
            }
        }

        // Check if harvesting a plant (without scissors) - check for WHEAT (legacy) or AIR with plant tracking (3D)
        if (clickedBlock.getType() == Material.WHEAT || farmingManager.getPlantAt(clickedBlock.getLocation()) != null) {
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
        
        // Check if clicking on an existing placed lamp (show range)
        if (farmingManager.hasPlacedLampAt(clickedBlock.getLocation())) {
            com.budlords.quality.PlacedLamp existingLamp = farmingManager.getPlacedLampAt(clickedBlock.getLocation());
            if (existingLamp != null) {
                farmingManager.showLampRange(existingLamp);
                player.sendMessage("§e✦ Lamp Range: §7" + existingLamp.getRange() + " blocks");
                player.sendMessage("§7Quality: " + existingLamp.getStarRating().getDisplay());
                return;
            }
        }
        
        // Place lamp above the clicked block
        Block targetBlock = clickedBlock.getRelative(BlockFace.UP);
        
        // Check if space is available
        if (targetBlock.getType() != Material.AIR) {
            // Try to place on top of clicked block if it's solid
            if (clickedBlock.getType().isSolid()) {
                targetBlock = clickedBlock.getRelative(BlockFace.UP);
            } else {
                player.sendMessage("§cCannot place lamp here - space is not empty!");
                return;
            }
        }
        
        // Check if there's already a lamp at target
        if (farmingManager.hasPlacedLampAt(targetBlock.getLocation())) {
            player.sendMessage("§cThere is already a lamp here!");
            return;
        }
        
        // Place the lamp block (using the lamp's material based on rating)
        Material lampMaterial = getLampMaterial(lampRating);
        targetBlock.setType(lampMaterial);
        
        // Register the lamp in the tracking system
        if (farmingManager.placeLamp(targetBlock.getLocation(), lampRating, player.getUniqueId())) {
            // Consume lamp item
            if (player.getGameMode() != GameMode.CREATIVE) {
                if (item.getAmount() == 1) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
            }
            
            int lampRange = lampRating.getStars() + 1;
            
            // Show success message
            player.sendMessage("§a✦ Placed " + lampRating.getDisplay() + " §aGrow Lamp!");
            player.sendMessage("§7Effect range: §e" + lampRange + " blocks §7(below lamp)");
            player.sendMessage("§7Right-click the lamp to see its range.");
            
            // Show range visualization
            com.budlords.quality.PlacedLamp placedLamp = farmingManager.getPlacedLampAt(targetBlock.getLocation());
            if (placedLamp != null) {
                farmingManager.showLampRange(placedLamp);
            }
            
            // Lamp glow particles
            Location particleLoc = targetBlock.getLocation().add(0.5, 0.5, 0.5);
            targetBlock.getWorld().spawnParticle(Particle.GLOW, particleLoc, 20 + (lampRating.getStars() * 5), 
                0.3, 0.3, 0.3, 0.02);
            targetBlock.getWorld().playSound(particleLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.2f);
        } else {
            // Placement failed - revert the block
            targetBlock.setType(Material.AIR);
            player.sendMessage("§cFailed to place lamp!");
        }
    }
    
    /**
     * Gets the block material to use for a lamp based on its star rating.
     */
    private Material getLampMaterial(StarRating rating) {
        return switch (rating) {
            case ONE_STAR -> Material.LANTERN;
            case TWO_STAR -> Material.SEA_LANTERN;
            case THREE_STAR -> Material.GLOWSTONE;
            case FOUR_STAR -> Material.SHROOMLIGHT;
            case FIVE_STAR -> Material.END_ROD;
            case SIX_STAR -> Material.BEACON;
            default -> Material.LANTERN;
        };
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
            
            // Use helper method to update inventory on next tick
            updateMainHandNextTick(player, filledCan);
            
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
                
                // Use helper method to update inventory on next tick
                updateMainHandNextTick(player, updatedCan);
                
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
        // Check for WHEAT (legacy) or any block with plant tracking (3D visualization)
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        
        // If clicking on a pot, check the block above for the plant
        if (plant == null && clickedBlock.getType() == Material.FLOWER_POT) {
            Location plantLocation = clickedBlock.getRelative(BlockFace.UP).getLocation();
            plant = farmingManager.getPlantAt(plantLocation);
            if (plant != null) {
                // Use the plant location for harvesting
                handleScissorsHarvestPlant(event, player, item, plant, plantLocation);
                return;
            }
        }
        
        if (plant == null && clickedBlock.getType() != Material.WHEAT) {
            return;
        }
        
        // plant may still be null if WHEAT block but no tracking (shouldn't happen)
        if (plant == null) return;
        
        handleScissorsHarvestPlant(event, player, item, plant, clickedBlock.getLocation());
    }
    
    private void handleScissorsHarvestPlant(PlayerInteractEvent event, Player player, ItemStack item, Plant plant, Location plantLocation) {
        event.setCancelled(true);
        
        if (!plant.isFullyGrown()) {
            player.sendMessage("§eThis plant is " + plant.getGrowthStageName().toLowerCase() + 
                              " (" + (plant.getGrowthStage() + 1) + "/4 stages)");
            return;
        }
        
        // Check if player has an active mini-game
        if (plugin.getHarvestMinigame().hasActiveGame(player)) {
            // Register click for mini-game
            plugin.getHarvestMinigame().handleClick(player);
            return;
        }
        
        // Check if mini-game is completed
        com.budlords.minigames.HarvestMinigame.MinigameResult result = 
            plugin.getHarvestMinigame().getResult(player);
        
        StarRating scissorsRating = HarvestScissors.getRatingFromItem(item);
        if (scissorsRating == null) scissorsRating = StarRating.ONE_STAR;
        
        if (result != null) {
            // Mini-game completed - harvest with scissors AND minigame bonuses
            Plant harvested = farmingManager.harvestPlant(player, plantLocation, scissorsRating);
            if (harvested != null) {
                giveHarvestWithScissorsAndMinigame(player, harvested, scissorsRating, result);
            }
            plugin.getHarvestMinigame().cleanupSession(player);
        } else {
            // Start harvest mini-game
            plugin.getHarvestMinigame().startMinigame(player, plant, plantLocation);
        }
    }

    private void handlePlantInteraction(PlayerInteractEvent event, Player player, Block clickedBlock) {
        Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
        if (plant != null) {
            event.setCancelled(true);
            
            if (plant.isFullyGrown()) {
                // Check if player has an active mini-game
                if (plugin.getHarvestMinigame().hasActiveGame(player)) {
                    // Register click for mini-game
                    plugin.getHarvestMinigame().handleClick(player);
                    return;
                }
                
                // Check if mini-game is completed
                com.budlords.minigames.HarvestMinigame.MinigameResult result = 
                    plugin.getHarvestMinigame().getResult(player);
                
                if (result != null) {
                    // Mini-game completed - harvest with bonuses
                    Plant harvested = farmingManager.harvestPlant(player, clickedBlock.getLocation());
                    if (harvested != null) {
                        giveHarvestWithMinigameBonus(player, harvested, result);
                    }
                    plugin.getHarvestMinigame().cleanupSession(player);
                } else {
                    // Start harvest mini-game
                    plugin.getHarvestMinigame().startMinigame(player, plant, clickedBlock.getLocation());
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
        Player player = event.getPlayer();
        
        // Check if breaking a placed lamp
        if (farmingManager.hasPlacedLampAt(block.getLocation())) {
            com.budlords.quality.PlacedLamp placedLamp = farmingManager.removePlacedLamp(block.getLocation());
            if (placedLamp != null) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                
                // Return the lamp item to the player
                ItemStack lampItem = plugin.getQualityItemManager().createLamp(placedLamp.getStarRating(), 1);
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(lampItem);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(i -> 
                        player.getWorld().dropItemNaturally(player.getLocation(), i)
                    );
                }
                player.sendMessage("§7Returned lamp: " + placedLamp.getStarRating().getDisplay());
                player.playSound(player.getLocation(), Sound.BLOCK_LANTERN_BREAK, 0.5f, 1.0f);
                return;
            }
        }
        
        // Check if breaking a plant (WHEAT for legacy, or any block with plant tracking for 3D)
        Plant plant = farmingManager.getPlantAt(block.getLocation());
        if (block.getType() == Material.WHEAT || plant != null) {
            // plant may still be null if WHEAT block but no tracking (shouldn't happen)
            if (plant != null) {
                event.setCancelled(true);
                event.setDropItems(false);
                
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
                            // Note: Lamps are NOT returned when removing a pot - lamps are placed 
                            // separately as blocks and can affect multiple pots. The lamp rating
                            // on a plant is just a buff indicator, not ownership.
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
                        // Note: Lamps are NOT returned when removing a pot - lamps are placed 
                        // separately as blocks and can affect multiple pots. The lamp rating
                        // on a plant is just a buff indicator, not ownership.
                    }
                    
                    farmingManager.removePlant(block.getLocation());
                    block.setType(Material.AIR);
                    player.sendMessage("§eHarvested early - returned seed.");
                }
                return; // Return after handling plant break to prevent duplicate handling
            }
        }
        
        // Check if breaking farmland with plant on top
        if (block.getType() == Material.FARMLAND || block.getType() == Material.FLOWER_POT) {
            Block above = block.getRelative(BlockFace.UP);
            // Check for both WHEAT (legacy) and tracked plant (3D visualization)
            Plant plantAbove = farmingManager.getPlantAt(above.getLocation());
            if (above.getType() == Material.WHEAT || plantAbove != null) {
                // plantAbove may still be null if WHEAT block but no tracking
                if (plantAbove != null) {
                    // Remove the plant
                    farmingManager.removePlant(above.getLocation());
                    above.setType(Material.AIR);
                    
                    // Return seed
                    Strain strain = strainManager.getStrain(plantAbove.getStrainId());
                    if (strain != null) {
                        StarRating seedRating = plantAbove.getSeedRating() != null ? 
                            plantAbove.getSeedRating() : StarRating.ONE_STAR;
                        ItemStack seed = strainManager.createSeedItem(strain, 1, seedRating);
                        event.getPlayer().getWorld().dropItemNaturally(above.getLocation(), seed);
                    }
                    
                    // Return pot if it's pot-based planting and breaking the pot
                    if (block.getType() == Material.FLOWER_POT && plantAbove.hasPot() && plantAbove.getPotRating() != null) {
                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        
                        ItemStack pot = GrowingPot.createPotItem(plantAbove.getPotRating(), 1);
                        event.getPlayer().getWorld().dropItemNaturally(block.getLocation(), pot);
                        
                        // Remove pot from tracking
                        farmingManager.removePot(block.getLocation());
                        
                        // Note: Lamps are NOT returned when removing a pot - lamps are placed 
                        // separately as blocks and can affect multiple pots. The lamp rating
                        // on a plant is just a buff indicator, not ownership.
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
        
        // Check for formation bonus (same-strain plants in patterns)
        if (plugin.getFormationManager() != null) {
            int farmingXP = getPlayerFarmingXP(player);
            
            int formationBonus = plugin.getFormationManager().calculateFormationBonus(plant, player.getUniqueId());
            if (formationBonus > 0 && finalRating.getStars() + formationBonus <= 6) {
                com.budlords.farming.FormationManager.FormationType formation = 
                    plugin.getFormationManager().detectFormation(plant.getLocation(), plant.getStrainId(), farmingXP);
                finalRating = StarRating.fromValue(finalRating.getStars() + formationBonus);
                player.sendMessage("§a✦ " + com.budlords.farming.FormationManager.getFormationDisplay(formation) + 
                    " §7Bonus! §a+" + formationBonus + " star(s)!");
                
                // Apply formation special effects!
                com.budlords.farming.FormationManager.FormationEffect effect = 
                    plugin.getFormationManager().getFormationEffect(plant, player.getUniqueId());
                if (effect != null) {
                    plugin.getFormationManager().applyFormationEffects(player, effect);
                }
            }
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
        
        // Check for formation bonus (same-strain plants in patterns)
        if (plugin.getFormationManager() != null) {
            int farmingXP = getPlayerFarmingXP(player);
            
            int formationBonus = plugin.getFormationManager().calculateFormationBonus(plant, player.getUniqueId());
            if (formationBonus > 0 && finalRating.getStars() + formationBonus <= 6) {
                com.budlords.farming.FormationManager.FormationType formation = 
                    plugin.getFormationManager().detectFormation(plant.getLocation(), plant.getStrainId(), farmingXP);
                finalRating = StarRating.fromValue(finalRating.getStars() + formationBonus);
                player.sendMessage("§a✦ " + com.budlords.farming.FormationManager.getFormationDisplay(formation) + 
                    " §7Bonus! §a+" + formationBonus + " star(s)!");
                
                // Apply formation special effects!
                com.budlords.farming.FormationManager.FormationEffect effect = 
                    plugin.getFormationManager().getFormationEffect(plant, player.getUniqueId());
                if (effect != null) {
                    plugin.getFormationManager().applyFormationEffects(player, effect);
                }
            }
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
    
    /**
     * Gets the player's farming XP for formation detection.
     * @param player The player
     * @return The farming XP, or 0 if skill manager is unavailable
     */
    private int getPlayerFarmingXP(Player player) {
        if (plugin.getSkillManager() != null) {
            return plugin.getSkillManager().getTreeXP(player.getUniqueId(), 
                com.budlords.skills.Skill.SkillTree.FARMING);
        }
        return 0;
    }
    
    /**
     * Gives harvest with minigame bonuses only (no scissors).
     */
    private void giveHarvestWithMinigameBonus(Player player, Plant plant, 
                                              com.budlords.minigames.HarvestMinigame.MinigameResult result) {
        Strain strain = strainManager.getStrain(plant.getStrainId());
        if (strain == null) {
            player.sendMessage("§cError: Strain not found!");
            return;
        }

        // Calculate actual yield with minigame multiplier
        int baseYield = strain.getYield();
        double qualityMultiplier = 0.5 + (plant.getQuality() / 100.0);
        int yieldWithQuality = Math.max(1, (int) Math.round(baseYield * qualityMultiplier));
        int actualYield = (int) Math.round(yieldWithQuality * result.getYieldMultiplier());

        // Apply quality bonus from minigame
        int enhancedQuality = Math.min(100, plant.getQuality() + result.getQualityBonus());
        
        // Calculate final bud star rating with enhanced quality
        StarRating finalRating = plant.calculateFinalBudRating(null);
        
        // Check for rare drop from minigame
        if (result.hasRareDropChance()) {
            StarRating bonusSeedRating = StarRating.fromValue(
                Math.min(5, (plant.getSeedRating() != null ? plant.getSeedRating().getStars() : 1) + 1)
            );
            ItemStack bonusSeed = strainManager.createSeedItem(strain, 1, bonusSeedRating);
            player.getInventory().addItem(bonusSeed);
            player.sendMessage("§d✦ Mini-game Rare Drop! §7Found a bonus " + bonusSeedRating.getDisplay() + " §7seed!");
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
        player.sendMessage("§7Quality: " + getQualityDisplay(enhancedQuality));
    }
    
    /**
     * Gives harvest with both scissors AND minigame bonuses.
     */
    private void giveHarvestWithScissorsAndMinigame(Player player, Plant plant, StarRating scissorsRating,
                                                    com.budlords.minigames.HarvestMinigame.MinigameResult result) {
        Strain strain = strainManager.getStrain(plant.getStrainId());
        if (strain == null) {
            player.sendMessage("§cError: Strain not found!");
            return;
        }
        
        HarvestScissors scissors = new HarvestScissors(scissorsRating);

        // Calculate yield with both scissors and minigame multipliers
        int baseYield = strain.getYield();
        double qualityMultiplier = 0.5 + (plant.getQuality() / 100.0);
        int yieldWithQuality = Math.max(1, (int) Math.round(baseYield * qualityMultiplier));
        int yieldWithScissors = scissors.calculateFinalYield(yieldWithQuality);
        int actualYield = (int) Math.round(yieldWithScissors * result.getYieldMultiplier());

        // Apply quality bonus from minigame
        int enhancedQuality = Math.min(100, plant.getQuality() + result.getQualityBonus());

        // Calculate final bud star rating
        StarRating finalRating = plant.calculateFinalBudRating(scissorsRating);
        
        // Check for quality upgrade from scissors
        if (scissors.triggersQualityUpgrade() && finalRating.getStars() < 5) {
            finalRating = StarRating.fromValue(finalRating.getStars() + 1);
            player.sendMessage("§6✦ Quality Upgrade! §7Scissors improved the bud quality!");
        }
        
        // Check for rare drops (both scissors and minigame)
        boolean hadRareDrop = false;
        if (scissors.triggersRareDrop()) {
            StarRating bonusSeedRating = StarRating.fromValue(
                Math.min(5, (plant.getSeedRating() != null ? plant.getSeedRating().getStars() : 1) + 1)
            );
            ItemStack bonusSeed = strainManager.createSeedItem(strain, 1, bonusSeedRating);
            player.getInventory().addItem(bonusSeed);
            player.sendMessage("§d✦ Scissors Rare Drop! §7Found a bonus " + bonusSeedRating.getDisplay() + " §7seed!");
            hadRareDrop = true;
        }
        
        if (result.hasRareDropChance()) {
            StarRating bonusSeedRating = StarRating.fromValue(
                Math.min(5, (plant.getSeedRating() != null ? plant.getSeedRating().getStars() : 1) + (hadRareDrop ? 2 : 1))
            );
            ItemStack bonusSeed = strainManager.createSeedItem(strain, 1, bonusSeedRating);
            player.getInventory().addItem(bonusSeed);
            player.sendMessage("§d✦ Mini-game Rare Drop! §7Found a bonus " + bonusSeedRating.getDisplay() + " §7seed!");
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
        player.sendMessage("§7Quality: " + getQualityDisplay(enhancedQuality));
        player.sendMessage("§a§lCOMBO BONUS! §7Scissors + Perfect Mini-game!");
    }
}
