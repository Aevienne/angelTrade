package me.angelique.angelTrade.gui;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.managers.TradeShopManager;
import me.angelique.angelTrade.models.TradeShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TradeShopGuiListener implements Listener {

    private final AngelTrade plugin;
    private final Map<UUID, Integer> pages = new HashMap<>();

    public TradeShopGuiListener(AngelTrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(TradeShopBrowserGui.TITLE)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        UUID id = player.getUniqueId();
        int page = pages.getOrDefault(id, 0);
        TradeShopManager tm = plugin.getTradeShopManager();

        if (event.getSlot() == 45 && clicked.getType() == Material.ARROW) {
            pages.put(id, page - 1);
            TradeShopBrowserGui.open(player, plugin, page - 1);
            return;
        }
        if (event.getSlot() == 53 && clicked.getType() == Material.ARROW) {
            pages.put(id, page + 1);
            TradeShopBrowserGui.open(player, plugin, page + 1);
            return;
        }

        // Find the clicked shop
        List<TradeShop> all = new ArrayList<>(tm.getShops().values());
        all.sort(Comparator.comparing(TradeShop::getId));
        int perPage = 21;
        int idx = page * perPage + getShopIndex(event.getSlot());
        if (idx >= 0 && idx < all.size()) {
            TradeShop shop = all.get(idx);
            player.closeInventory();
            player.chat("/tradeshop info " + shop.getId());
        }
    }

    private int getShopIndex(int slot) {
        for (int i = 0; i < TradeShopBrowserGui.SHOP_SLOTS.length; i++) {
            if (TradeShopBrowserGui.SHOP_SLOTS[i] == slot) return i;
        }
        return -1;
    }
}
