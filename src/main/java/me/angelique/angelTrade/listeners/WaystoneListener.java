package me.angelique.angelTrade.listeners;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.managers.RouteManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class WaystoneListener implements Listener {

    private final AngelTrade plugin;
    private final NamespacedKey waystoneKey;
    private final NamespacedKey routeDeedKey;

    public WaystoneListener(AngelTrade plugin) {
        this.plugin = plugin;
        this.waystoneKey = new NamespacedKey(plugin, "waystone");
        this.routeDeedKey = new NamespacedKey(plugin, "route_deed");
    }

    public static ItemStack createWaystone(AngelTrade plugin) {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00A76Waystone");
        meta.setLore(List.of("\u00A77Place this to establish a trade route node."));
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "waystone"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createRouteDeed(AngelTrade plugin) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00A76Route Deed");
        meta.setLore(List.of("\u00A77Right-click a Waystone to link a route."));
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "route_deed"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (!isWaystone(item)) return;
        e.getBlock().getLocation().getBlock().setType(Material.LODESTONE);
        e.getPlayer().sendMessage(color(plugin.getConfig().getString("messages.waystone-placed")));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.LODESTONE) return;
        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!isRouteDeed(hand)) return;
        e.setCancelled(true);

        Location clickedLoc = e.getClickedBlock().getLocation();
        RouteManager rm = plugin.getRouteManager();

        if (!rm.getPendingCreation().containsKey(player.getUniqueId())) {
            // First waystone selected
            rm.getPendingCreation().put(player.getUniqueId(), clickedLoc);
            player.sendMessage(color("&aWaystone A selected. Right-click another Waystone to complete the route."));
        } else {
            // Second waystone — create route
            Location locA = rm.getPendingCreation().remove(player.getUniqueId());
            if (locA.equals(clickedLoc)) {
                player.sendMessage(color("&cYou must select a different Waystone."));
                return;
            }
            boolean created = rm.createRoute(player, locA, clickedLoc);
            if (created) {
                if (hand.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
                else hand.setAmount(hand.getAmount() - 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.LODESTONE) return;
        plugin.getRouteManager().handleWaystoneDestroyed(b.getLocation(), e.getPlayer().getUniqueId());
    }

    private boolean isWaystone(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(waystoneKey, PersistentDataType.BYTE);
    }

    private boolean isRouteDeed(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(routeDeedKey, PersistentDataType.BYTE);
    }

    private String color(String s) { return s.replace('&', '\u00A7'); }
}
