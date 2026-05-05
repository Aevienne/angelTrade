package me.angelique.angelTrade.managers;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.models.ShopItem;
import me.angelique.angelTrade.models.TradeShop;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeShopManager {

    private final AngelTrade plugin;
    private final Map<String, TradeShop> shops = new ConcurrentHashMap<>();

    public TradeShopManager(AngelTrade plugin) { this.plugin = plugin; }

    public void loadAll() {
        shops.clear();
        plugin.getDataManager().loadAllShops().forEach(s -> shops.put(s.getId(), s));
        plugin.getLogger().info("Loaded " + shops.size() + " trade shops.");
    }

    public TradeShop createShop(Player player, String companyId, Location location) {
        String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        TradeShop shop = new TradeShop(id, player.getUniqueId(), companyId, location);
        shops.put(id, shop);
        plugin.getDataManager().saveShop(shop);
        return shop;
    }

    public boolean relocateShop(Player player, String shopId, Location newLocation) {
        TradeShop shop = shops.get(shopId);
        if (shop == null) { player.sendMessage(color("&cShop not found.")); return false; }
        if (!shop.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("angeltrade.admin")) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no-permission"))); return false;
        }
        long cooldown = plugin.getConfig().getLong("tradeshop-relocate-cooldown", 300);
        if (!shop.canRelocate(cooldown)) {
            player.sendMessage(color("&cYou must wait before relocating this shop."));
            return false;
        }
        shop.relocate(newLocation);
        plugin.getDataManager().saveShop(shop);
        player.sendMessage(color("&aShop relocated!"));
        return true;
    }

    public boolean addItem(Player player, String shopId, String itemKey, double price, int stock) {
        TradeShop shop = shops.get(shopId);
        if (shop == null) { player.sendMessage(color("&cShop not found.")); return false; }
        if (!shop.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("angeltrade.admin")) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no-permission"))); return false;
        }
        if (shop.getItem(itemKey) != null) {
            player.sendMessage(color("&eItem already listed. Use /tradeshop setprice to update."));
            return false;
        }
        shop.addItem(new ShopItem(itemKey, price, stock));
        plugin.getDataManager().saveShopItems(shop);
        player.sendMessage(color("&aItem &e" + itemKey + " &aadded at &e$" + String.format("%.2f", price)));
        return true;
    }

    public boolean removeItem(Player player, String shopId, String itemKey) {
        TradeShop shop = shops.get(shopId);
        if (shop == null) { player.sendMessage(color("&cShop not found.")); return false; }
        if (!shop.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("angeltrade.admin")) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no-permission"))); return false;
        }
        if (!shop.removeItem(itemKey)) {
            player.sendMessage(color("&cItem not found in shop."));
            return false;
        }
        plugin.getDataManager().saveShopItems(shop);
        player.sendMessage(color("&aItem removed."));
        return true;
    }

    public boolean setPrice(Player player, String shopId, String itemKey, double newPrice) {
        TradeShop shop = shops.get(shopId);
        if (shop == null) { player.sendMessage(color("&cShop not found.")); return false; }
        if (!shop.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("angeltrade.admin")) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no-permission"))); return false;
        }
        ShopItem item = shop.getItem(itemKey);
        if (item == null) { player.sendMessage(color("&cItem not found.")); return false; }
        item.setPrice(newPrice);
        plugin.getDataManager().saveShopItems(shop);
        player.sendMessage(color("&aPrice updated to &e$" + String.format("%.2f", newPrice)));
        return true;
    }

    public TradeShop getShopAtLocation(Location loc) {
        for (TradeShop s : shops.values()) {
            Location sl = s.getLocation();
            if (sl.getWorld().equals(loc.getWorld())
                    && sl.getBlockX() == loc.getBlockX()
                    && sl.getBlockY() == loc.getBlockY()
                    && sl.getBlockZ() == loc.getBlockZ()) return s;
        }
        return null;
    }

    public TradeShop getShop(String id) { return shops.get(id); }
    public Map<String, TradeShop> getShops() { return shops; }

    private String color(String s) { return s.replace('&', '\u00A7'); }
}
