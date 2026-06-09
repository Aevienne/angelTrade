package me.angelique.angelTrade.gui;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.managers.TradeShopManager;
import me.angelique.angelTrade.models.ShopItem;
import me.angelique.angelTrade.models.TradeShop;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class TradeShopBrowserGui {

    static final String TITLE = "\u00A78Trade Shops \u00A77\u2014 \u00A7dBrowse";
    static final int SIZE = 54;
    static final int[] SHOP_SLOTS = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34};

    private TradeShopBrowserGui() {}

    public static void open(Player player, AngelTrade plugin, int page) {
        TradeShopManager tm = plugin.getTradeShopManager();
        List<TradeShop> all = new ArrayList<>(tm.getShops().values());
        all.sort(Comparator.comparing(TradeShop::getId));

        Inventory inv = Bukkit.createInventory(null, SIZE, Component.text(TITLE));
        fillBorder(inv);

        int perPage = SHOP_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / perPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        int start = page * perPage;
        int end = Math.min(start + perPage, all.size());

        for (int i = start; i < end; i++) {
            TradeShop shop = all.get(i);
            int totalItems = shop.getItems().size();
            OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.getOwnerUUID());
            ItemStack icon = item(Material.CHEST, "&e" + (owner.getName() != null ? owner.getName() + "'s Shop" : "Shop #" + shop.getId().substring(0,6)),
                    "&7Items listed: &f" + totalItems,
                    "&7Location: &f" + shop.getLocation().getBlockX() + "," + shop.getLocation().getBlockY() + "," + shop.getLocation().getBlockZ(),
                    totalItems > 0 ? "&aOpen for business" : "&cNo items listed",
                    "",
                    "&eClick to open");
            inv.setItem(SHOP_SLOTS[i - start], icon);
        }

        if (page > 0) inv.setItem(45, item(Material.ARROW, "&e\u2190 Previous", "&7Page " + (page) + " of " + totalPages));
        if (page < totalPages - 1) inv.setItem(53, item(Material.ARROW, "&eNext \u2192", "&7Page " + (page+2) + " of " + totalPages));
        inv.setItem(49, item(Material.KNOWLEDGE_BOOK, "&7Page " + (page+1) + " of " + totalPages, "&7Click a shop to view items"));

        player.openInventory(inv);
    }

    static void fillBorder(Inventory inv) {
        ItemStack glass = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, glass);
    }

    static ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(Arrays.stream(lore).map(TradeShopBrowserGui::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    static String color(String s) { return s.replace('&', '\u00A7'); }
}
