package me.angelique.angelTrade.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RouteGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(RouteGui.TITLE)) return;
        event.setCancelled(true);
        if (event.getSlot() == 40) {
            event.getWhoClicked().closeInventory();
        }
    }
}
