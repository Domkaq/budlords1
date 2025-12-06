package com.budlords.listeners;

import com.budlords.economy.EconomyManager;
import com.budlords.npc.NPCManager;
import com.budlords.packaging.PackagingManager;
import com.budlords.progression.RankManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class NPCListener implements Listener {

    private final NPCManager npcManager;
    private final EconomyManager economyManager;
    private final RankManager rankManager;
    private final PackagingManager packagingManager;

    public NPCListener(NPCManager npcManager, EconomyManager economyManager, 
                       RankManager rankManager, PackagingManager packagingManager) {
        this.npcManager = npcManager;
        this.economyManager = economyManager;
        this.rankManager = rankManager;
        this.packagingManager = packagingManager;
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

        // Check if holding a packaged product
        if (packagingManager.isPackagedProduct(item)) {
            NPCManager.TradeResult result = npcManager.attemptTrade(player, entity, item);
            player.sendMessage(result.message());

            if (result.success()) {
                // Show action bar with rank info
                String rankDisplay = rankManager.getRankDisplayName(player);
                player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        "§a+" + economyManager.formatMoney(result.amount()) + " §7| Rank: " + rankDisplay
                    )[0]
                );
            }
        } else {
            // Show trader info
            String traderName = switch (npcType) {
                case MARKET_JOE -> "§a§lMarket Joe";
                case BLACKMARKET_JOE -> "§5§lBlackMarket Joe";
                case VILLAGE_VENDOR -> "§e§lVillage Vendor";
                default -> "§7Trader";
            };

            player.sendMessage("");
            player.sendMessage(traderName);
            player.sendMessage("§7Hold a packaged product to sell!");
            player.sendMessage("§7Use §f/package <amount>§7 to package buds.");
            player.sendMessage("");

            if (npcType == NPCManager.NPCType.BLACKMARKET_JOE) {
                player.sendMessage("§5§oBetter prices for rare strains...");
            } else if (npcType == NPCManager.NPCType.VILLAGE_VENDOR) {
                player.sendMessage("§e§oLower prices, but always willing to buy.");
            }
        }
    }
}
