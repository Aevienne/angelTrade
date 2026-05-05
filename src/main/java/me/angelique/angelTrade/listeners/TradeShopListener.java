package me.angelique.angelTrade.listeners;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.gui.TradeShopGUI;
import me.angelique.angelTrade.models.TradeShop;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class TradeShopListener implements Listener {

    private final AngelTrade plugin;
    private final NamespacedKey shopBlockKey;

    public TradeShopListener(AngelTrade plugin) {
        this.plugin = plugin;
        this.shopBlockKey = new NamespacedKey(plugin, "trade_shop_block");
    }

    public static ItemStack createTradeShopBlock(AngelTrade plugin) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00A76Trade Shop");
        meta.setLore(List.of(
            "\u00A77Place this in the marketplace",
            "\u00A77to open a company trade stall."
        ));
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "trade_shop_block"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.CHEST) return;
        Player player = e.getPlayer();
        TradeShop shop = plugin.getTradeShopManager().getShopAtLocation(e.getClickedBlock().getLocation());
        if (shop == null) return;
        e.setCancelled(true);
        new TradeShopGUI(plugin, shop).open(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView().title().toString().contains("Trade Shop")) {
            TradeShopGUI.handleClick(plugin, e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.CHEST) return;
        TradeShop shop = plugin.getTradeShopManager().getShopAtLocation(e.getBlock().getLocation());
        if (shop == null) return;
        // Prevent breaking — owner must use /tradeshop relocate to move
        if (!e.getPlayer().getUniqueId().equals(shop.getOwnerUUID())
                && !e.getPlayer().hasPermission("angeltrade.admin")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(color("&cYou cannot break another company's Trade Shop."));
            return;
        }
        e.setCancelled(true);
        e.getPlayer().sendMessage(color("&eUse &6/tradeshop relocate &eto move your shop."));
    }

    private String color(String s) { return s.replace('&', '\u00A7'); }
}
