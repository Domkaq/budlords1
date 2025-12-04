package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
import com.budlords.gui.BlackMarketShopGUI;
import com.budlords.gui.MarketShopGUI;
import com.budlords.gui.MobSaleGUI;
import com.budlords.joint.JointItems;
import com.budlords.npc.NPCManager;
import com.budlords.packaging.PackagingManager;
import com.budlords.progression.RankManager;
import com.budlords.strain.StrainManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class NPCListener implements Listener {

    private final BudLords plugin;
    private final NPCManager npcManager;
    private final EconomyManager economyManager;
    private final RankManager rankManager;
    private final PackagingManager packagingManager;
    private final MarketShopGUI marketShopGUI;
    private final BlackMarketShopGUI blackMarketShopGUI;
    private final MobSaleGUI mobSaleGUI;
    private final StrainManager strainManager;

    public NPCListener(BudLords plugin, NPCManager npcManager, EconomyManager economyManager, 
                       RankManager rankManager, PackagingManager packagingManager,
                       MarketShopGUI marketShopGUI, BlackMarketShopGUI blackMarketShopGUI,
                       MobSaleGUI mobSaleGUI, StrainManager strainManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.economyManager = economyManager;
        this.rankManager = rankManager;
        this.packagingManager = packagingManager;
        this.marketShopGUI = marketShopGUI;
        this.blackMarketShopGUI = blackMarketShopGUI;
        this.mobSaleGUI = mobSaleGUI;
        this.strainManager = strainManager;
    }

    // Using deprecated sendMessage and BungeeCord Chat API for Bukkit/Spigot compatibility
    // Paper servers can replace with Adventure API's sendActionBar(Component) method
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        NPCManager.NPCType npcType = npcManager.getNPCType(entity);
        if (npcType == NPCManager.NPCType.NONE) return;

        event.setCancelled(true);

        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if holding seeds - BlackMarket Joe doesn't buy seeds
        if (strainManager.isSeedItem(item)) {
            if (npcType == NPCManager.NPCType.BLACKMARKET_JOE) {
                player.sendMessage("§5BlackMarket Joe doesn't buy seeds!");
                player.sendMessage("§7Sell packaged buds instead.");
                return;
            }
        }

        // Check if holding a packaged product or joint - open the sale GUI
        if (packagingManager.isPackagedProduct(item) || JointItems.isJoint(item)) {
            // Open the Schedule 1 style sale GUI
            mobSaleGUI.open(player, entity, npcType);
            return;
        }
        
        // Market Joe opens shop GUI when not holding packaged product
        if (npcType == NPCManager.NPCType.MARKET_JOE) {
            marketShopGUI.open(player);
            return;
        }
        
        // BlackMarket Joe opens his shop GUI when not holding packaged product
        if (npcType == NPCManager.NPCType.BLACKMARKET_JOE) {
            blackMarketShopGUI.open(player);
            return;
        }

        // Show trader info for Village Vendors
        String traderName = switch (npcType) {
            case MARKET_JOE -> "§a§lMarket Joe";
            case BLACKMARKET_JOE -> "§5§lBlackMarket Joe";
            case VILLAGE_VENDOR -> "§e§lVillage Vendor";
            default -> "§7Trader";
        };

        player.sendMessage("");
        player.sendMessage(traderName);
        player.sendMessage("§7Hold a packaged product or joint to sell!");
        player.sendMessage("§7Use §f/package <amount>§7 to package buds.");
        player.sendMessage("");

        if (npcType == NPCManager.NPCType.VILLAGE_VENDOR) {
            player.sendMessage("§e§oLower prices, but always willing to buy.");
        }
    }
}
