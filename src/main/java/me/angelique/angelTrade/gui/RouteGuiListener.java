package me.angelique.angelTrade.gui;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.models.TradeRoute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RouteGuiListener implements Listener {

    private final AngelTrade plugin;

    public RouteGuiListener(AngelTrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(RouteGui.TITLE)) return;
        event.setCancelled(true);

        int slot = event.getSlot();
        if (slot == 44) { player.performCommand("menu"); return; }

        // Route click (slots 19-25) -> show info and options
        java.util.List<TradeRoute> routes = RouteGui.routeCache.get(player.getUniqueId());
        if (routes != null) {
            int idx = slot - 19;
            if (idx >= 0 && idx < Math.min(routes.size(), 7)) {
                TradeRoute route = routes.get(idx);
                player.closeInventory();
                player.sendMessage(RouteGui.color("&6Route #" + route.getId().substring(0, 6)));
                player.sendMessage(RouteGui.color("&7Tier: &f" + route.getTier() + "  Status: &f" + route.getStatus() + "  Risk: &f" + route.getRiskLabel()));
                player.sendMessage(RouteGui.color("&7Uses: &f" + route.getUses() + "  Insured: " + (route.isInsured() ? "&aYes" : "&cNo")));
                player.sendMessage(RouteGui.color("&e/route info " + route.getId().substring(0, 6) + "  /route insure " + route.getId().substring(0, 6) + "  /route remove " + route.getId().substring(0, 6)));
            }
        }
    }
}
