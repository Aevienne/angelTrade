package me.angelique.angelTrade.gui;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.models.ShopItem;
import me.angelique.angelTrade.models.TradeRoute;
import me.angelique.angelTrade.models.TradeShop;
import me.angelique.angelNCore.events.ContractBreachedEvent;
import me.angelique.angelNCore.events.EventBus;
import me.angelique.angelNCore.events.TradeCompletedEvent;
import me.angelique.angelNCore.services.MarketService;
import me.angelique.angelNCore.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TradeShopGUI {

    private final AngelTrade plugin;
    private final TradeShop shop;
    private static final int SIZE = 54;

    public TradeShopGUI(AngelTrade plugin, TradeShop shop) {
        this.plugin = plugin;
        this.shop = shop;
    }

    public void open(Player player) {
        String sym = plugin.getConfig().getString("currency-symbol", "$");
        Inventory inv = Bukkit.createInventory(null, SIZE,
            Component.text("Trade Shop — " + (shop.getCompanyId() != null ? shop.getCompanyId() : "Unknown Company")));

        List<ShopItem> items = shop.getItems();
        for (int i = 0; i < Math.min(items.size(), 45); i++) {
            ShopItem si = items.get(i);
            Material mat;
            try { mat = Material.valueOf(si.getItemKey().toUpperCase()); }
            catch (Exception ex) { mat = Material.PAPER; }

            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName("\u00A7e" + formatName(si.getItemKey()));

            List<String> lore = new ArrayList<>();
            lore.add("\u00A77Company Price: \u00A7a" + sym + String.format("%.2f", si.getEffectivePrice()));
            MarketService mkt = ServiceRegistry.getMarketService();
            String serverPrice = mkt != null
                    ? sym + String.format("%.2f", mkt.getPrice(si.getItemKey().toUpperCase()))
                    : "§8N/A";
            lore.add("§7Market Price:  §f" + serverPrice);
            lore.add("\u00A77Stock: \u00A7f" + si.getStock());
            if (si.getDiscount() > 0) lore.add("\u00A7cDiscount: \u00A7e" + (int)(si.getDiscount()*100) + "%");

            // Route bonus indicator
            if (shop.getLinkedRouteId() != null) {
                TradeRoute route = plugin.getRouteManager().getRoute(shop.getLinkedRouteId());
                if (route != null && route.getStatus() == TradeRoute.Status.HEALTHY) {
                    double bonus = plugin.getBonusManager().getShopPassiveIncome(route.getTier()) * 100;
                    lore.add("\u00A76\u2605 Route Bonus: +" + (int)bonus + "% passive income");
                }
            }

            lore.add("");
            lore.add("\u00A7eClick to purchase");
            meta.setLore(lore);
            stack.setItemMeta(meta);
            inv.setItem(i, stack);
        }

        // Info panel at bottom row
        ItemStack info = new ItemStack(Material.OAK_SIGN);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("\u00A76Shop Info");
        List<String> il = new ArrayList<>();
        il.add("\u00A77Company: \u00A7f" + (shop.getCompanyId() != null ? shop.getCompanyId() : "None"));
        il.add("\u00A77Items Listed: \u00A7f" + shop.getItems().size());
        if (shop.getLinkedRouteId() != null) {
            TradeRoute r = plugin.getRouteManager().getRoute(shop.getLinkedRouteId());
            il.add("\u00A77Route: \u00A7a" + (r != null ? r.getId() + " [" + r.getTier().name().replace("_", " ") + "]" : "Inactive"));
        }
        im.setLore(il);
        info.setItemMeta(im);
        inv.setItem(49, info);

        player.openInventory(inv);
    }

    public static void handleClick(AngelTrade plugin, InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
        if (e.getSlot() == 49) return; // info panel

        // Find shop by inventory title
        String title = e.getView().title().toString();
        String companyId = title.replace("Trade Shop — ", "").trim();
        TradeShop shop = plugin.getTradeShopManager().getShops().values().stream()
            .filter(s -> companyId.equals(s.getCompanyId() != null ? s.getCompanyId() : "Unknown Company"))
            .findFirst().orElse(null);
        if (shop == null) return;

        List<ShopItem> items = shop.getItems();
        if (e.getSlot() >= items.size()) return;
        ShopItem si = items.get(e.getSlot());

        if (si.getStock() <= 0) { player.sendMessage(color("&cThis item is out of stock.")); return; }

        double price = si.getEffectivePrice();
        if (!plugin.getEconomy().has(player, price)) {
            player.sendMessage(color("&cInsufficient funds."));
            EventBus.publish(new ContractBreachedEvent(shop.getId(), player.getName(), shop.getCompanyId()));
            return;
        }

        plugin.getEconomy().withdrawPlayer(player, price);
        si.setStock(si.getStock() - 1);
        plugin.getDataManager().saveShopItems(shop);

        Material mat;
        try { mat = Material.valueOf(si.getItemKey().toUpperCase()); }
        catch (Exception ex) { mat = Material.PAPER; }
        player.getInventory().addItem(new ItemStack(mat));

        plugin.getBonusManager().processSaleBonus(shop, price);

        String itemType = si.getItemKey().toUpperCase();
        EventBus.publish(new TradeCompletedEvent(
                shop.getId(),
                shop.getOwnerUUID().toString(),
                player.getName(),
                itemType,
                1,
                price
        ));
        MarketService market = ServiceRegistry.getMarketService();
        if (market != null) {
            market.recordTransaction(itemType, 1, price);
        }

        String sym = plugin.getConfig().getString("currency-symbol", "$");
        player.sendMessage(color("&aPurchased &e" + formatName(si.getItemKey()) + " &afor &e" + sym + String.format("%.2f", price)));
        player.closeInventory();
    }

    private static String formatName(String key) {
        String[] words = key.toLowerCase().replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private static String color(String s) { return s.replace('&', '\u00A7'); }
}
