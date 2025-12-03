package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.farming.FarmingManager;
import com.budlords.farming.Plant;
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

import java.util.HashMap;

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

        // Check if planting a seed
        if (strainManager.isSeedItem(item)) {
            Block targetBlock = clickedBlock.getRelative(BlockFace.UP);
            
            // Check if target is air and clicked block is farmland
            if (clickedBlock.getType() == Material.FARMLAND && targetBlock.getType() == Material.AIR) {
                String strainId = strainManager.getStrainIdFromItem(item);
                if (strainId != null) {
                    event.setCancelled(true);
                    
                    if (farmingManager.plantSeed(player, targetBlock.getLocation(), strainId)) {
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
            }
            return;
        }

        // Check if harvesting a plant
        if (clickedBlock.getType() == Material.WHEAT) {
            Plant plant = farmingManager.getPlantAt(clickedBlock.getLocation());
            if (plant != null) {
                event.setCancelled(true);
                
                if (plant.isFullyGrown()) {
                    Plant harvested = farmingManager.harvestPlant(player, clickedBlock.getLocation());
                    if (harvested != null) {
                        giveHarvest(player, harvested);
                    }
                } else {
                    player.sendMessage("§eThis plant is " + plant.getGrowthStageName().toLowerCase() + 
                                      " (" + (plant.getGrowthStage() + 1) + "/4 stages)");
                }
            }
        }
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
                    Plant harvested = farmingManager.harvestPlant(player, block.getLocation());
                    if (harvested != null) {
                        giveHarvest(player, harvested);
                    }
                } else {
                    // Return seed if not fully grown
                    Strain strain = strainManager.getStrain(plant.getStrainId());
                    if (strain != null) {
                        ItemStack seed = strainManager.createSeedItem(strain, 1);
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(seed);
                        if (!leftover.isEmpty()) {
                            leftover.values().forEach(i -> 
                                player.getWorld().dropItemNaturally(player.getLocation(), i)
                            );
                        }
                    }
                    farmingManager.removePlant(block.getLocation());
                    block.setType(Material.AIR);
                    player.sendMessage("§eHarvested early - returned seed.");
                }
            }
        }
        
        // Check if breaking farmland with plant on top
        if (block.getType() == Material.FARMLAND) {
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
                        ItemStack seed = strainManager.createSeedItem(strain, 1);
                        event.getPlayer().getWorld().dropItemNaturally(above.getLocation(), seed);
                    }
                }
            }
        }
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

        ItemStack buds = strainManager.createBudItem(strain, actualYield);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(buds);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> 
                player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
        }

        player.sendMessage("§aHarvested §e" + actualYield + "x §a" + strain.getName() + " Buds!");
        player.sendMessage("§7Quality: " + getQualityDisplay(plant.getQuality()));
    }

    private String getQualityDisplay(int quality) {
        if (quality >= 90) return "§6★★★★★ Legendary";
        if (quality >= 75) return "§9★★★★☆ Excellent";
        if (quality >= 60) return "§a★★★☆☆ Good";
        if (quality >= 40) return "§e★★☆☆☆ Average";
        return "§c★☆☆☆☆ Poor";
    }
}
