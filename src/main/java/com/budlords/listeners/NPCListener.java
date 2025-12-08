package com.budlords.listeners;

import com.budlords.BudLords;
import com.budlords.economy.EconomyManager;
import com.budlords.gui.BlackMarketShopGUI;
import com.budlords.gui.BuyerProfileGUI;
import com.budlords.gui.MarketShopGUI;
import com.budlords.gui.MobSaleGUI;
import com.budlords.items.PhoneItems;
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

    // Helpful hint message shown to players about phone usage
    private static final String PHONE_HINT = "§b§l📱 §7Use your phone to view reputation!";

    private final BudLords plugin;
    private final NPCManager npcManager;
    private final EconomyManager economyManager;
    private final RankManager rankManager;
    private final PackagingManager packagingManager;
    private final MarketShopGUI marketShopGUI;
    private final BlackMarketShopGUI blackMarketShopGUI;
    private final MobSaleGUI mobSaleGUI;
    private final StrainManager strainManager;
    private final BuyerProfileGUI buyerProfileGUI;

    public NPCListener(BudLords plugin, NPCManager npcManager, EconomyManager economyManager, 
                       RankManager rankManager, PackagingManager packagingManager,
                       MarketShopGUI marketShopGUI, BlackMarketShopGUI blackMarketShopGUI,
                       MobSaleGUI mobSaleGUI, StrainManager strainManager,
                       BuyerProfileGUI buyerProfileGUI) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.economyManager = economyManager;
        this.rankManager = rankManager;
        this.packagingManager = packagingManager;
        this.marketShopGUI = marketShopGUI;
        this.blackMarketShopGUI = blackMarketShopGUI;
        this.mobSaleGUI = mobSaleGUI;
        this.strainManager = strainManager;
        this.buyerProfileGUI = buyerProfileGUI;
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
        
        // Try to generate/get dynamic buyer profile for configurable mobs
        if (npcType == NPCManager.NPCType.CONFIGURABLE_MOB || npcType == NPCManager.NPCType.NONE) {
            if (plugin.getDynamicBuyerManager() != null) {
                com.budlords.npc.IndividualBuyer buyer = plugin.getDynamicBuyerManager().getOrCreateBuyer(entity);
                if (buyer != null) {
                    // Successfully created/retrieved buyer - treat as configurable mob
                    npcType = NPCManager.NPCType.CONFIGURABLE_MOB;
                }
            }
        }
        
        if (npcType == NPCManager.NPCType.NONE) return;

        event.setCancelled(true);

        ItemStack item = player.getInventory().getItemInMainHand();

        // PRIORITY 1: Check if holding a packaged product or joint - ALWAYS open sale GUI first
        // This ensures selling takes priority over all other interactions
        if (packagingManager.isPackagedProduct(item) || JointItems.isJoint(item)) {
            // Open the sale GUI to negotiate and complete the transaction
            mobSaleGUI.open(player, entity, npcType);
            return;
        }

        // PRIORITY 2: Check if holding phone - open buyer profile GUI to view reputation
        // Phone shows detailed buyer information and reputation levels
        if (PhoneItems.isPhone(item)) {
            if (buyerProfileGUI != null) {
                buyerProfileGUI.openBuyerProfile(player, npcType, entity);
            } else {
                player.sendMessage("§cPhone system is not available!");
            }
            return;
        }

        // PRIORITY 3: Check if holding seeds - BlackMarket Joe doesn't buy seeds
        if (strainManager.isSeedItem(item)) {
            if (npcType == NPCManager.NPCType.BLACKMARKET_JOE) {
                player.sendMessage("§5BlackMarket Joe doesn't buy seeds!");
                player.sendMessage("§7Sell packaged buds instead.");
                return;
            }
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
        
        // Configurable mobs (enabled in config) - show message about what they buy
        if (npcType == NPCManager.NPCType.CONFIGURABLE_MOB) {
            // Check if this mob has a dynamic buyer profile
            if (plugin.getDynamicBuyerManager() != null) {
                com.budlords.npc.IndividualBuyer buyer = plugin.getDynamicBuyerManager().getBuyer(entity);
                if (buyer != null) {
                    // Show buyer greeting
                    player.sendMessage("");
                    player.sendMessage(buyer.getGreeting());
                    player.sendMessage("§7Personality: §e" + buyer.getPersonality().name().replace("_", " "));
                    
                    // Show demand indicator
                    if (plugin.getDynamicBuyerManager().hasDemand(entity)) {
                        player.sendMessage("§a✦ §7Looking to buy right now!");
                    } else {
                        player.sendMessage("§7Not interested in buying at the moment.");
                    }
                    
                    player.sendMessage("§7Hold a packaged product or joint to sell!");
                    player.sendMessage(PHONE_HINT);
                    player.sendMessage("");
                    return;
                }
            }
            
            // Default message for non-dynamic buyers
            String entityName = entity.getCustomName() != null ? entity.getCustomName() : entity.getType().name().replace("_", " ");
            player.sendMessage("");
            player.sendMessage("§e§l" + entityName);
            player.sendMessage("§7This buyer is interested in your products!");
            player.sendMessage("§7Hold a packaged product or joint to sell!");
            player.sendMessage(PHONE_HINT);
            player.sendMessage("");
            return;
        }

        // PRIORITY 4: Default NPC interaction - show trader info for Village Vendors and others
        String traderName = switch (npcType) {
            case MARKET_JOE -> "§a§lMarket Joe";
            case BLACKMARKET_JOE -> "§5§lBlackMarket Joe";
            case VILLAGE_VENDOR -> "§e§lVillage Vendor";
            case CONFIGURABLE_MOB -> "§e§l" + entity.getType().name().replace("_", " ");
            default -> "§7Trader";
        };

        player.sendMessage("");
        player.sendMessage(traderName);
        player.sendMessage("§7Hold a packaged product or joint to sell!");
        player.sendMessage("§7Use §f/package <amount>§7 to package buds.");
        player.sendMessage("");
        player.sendMessage(PHONE_HINT);
        player.sendMessage("");

        if (npcType == NPCManager.NPCType.VILLAGE_VENDOR) {
            player.sendMessage("§e§oLower prices, but always willing to buy.");
        }
    }
}
