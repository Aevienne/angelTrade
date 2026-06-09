package me.angelique.angelTrade.gui;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.models.TradeRoute;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class RouteGui {

    public static final String TITLE = color("&8Trade Routes &7\u2014 &dCaravan Network");
    static final int SIZE = 45;

    private RouteGui() {}

    public static void open(Player player, AngelTrade plugin) {
        List<TradeRoute> routes = plugin.getRouteManager().getRoutesForPlayer(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        fillBorder(inv);

        inv.setItem(4, item(Material.COMPASS, "&dYour Trade Routes &7(" + routes.size() + ")",
                "&7Connect waystones to create trade paths",
                "&7Routes grant shop bonuses & travel speed"));

        int[] slots = {19,20,21,22,23,24,25};
        int slotIdx = 0;
        for (TradeRoute route : routes) {
            if (slotIdx >= slots.length) break;
            Material icon = switch (route.getTier()) {
                case DIRT_ROAD -> Material.DIRT_PATH;
                case STONE_ROAD -> Material.STONE_BRICKS;
                case GOLD_ROAD -> Material.GOLD_BLOCK;
                case ROYAL_ROAD -> Material.DIAMOND_BLOCK;
            };
            String riskColor = switch (route.getRiskLabel()) {
                case "SAFE" -> "&a";
                case "LOW" -> "&e";
                case "MODERATE" -> "&6";
                case "DANGEROUS" -> "&c";
                default -> "&7";
            };
            inv.setItem(slots[slotIdx++], item(icon, "&eRoute #" + route.getId().substring(0, 6),
                    "&7Tier: &f" + route.getTier().name(),
                    "&7Status: &f" + route.getStatus().name(),
                    "&7Risk: " + riskColor + route.getRiskLabel(),
                    "&7Uses: &f" + route.getUses(),
                    "&7Insured: " + (route.isInsured() ? "&aYes" : "&cNo"),
                    "&7A: " + route.getLocationA().getBlockX() + "," + route.getLocationA().getBlockZ(),
                    "&7B: " + route.getLocationB().getBlockX() + "," + route.getLocationB().getBlockZ()));
        }

        if (routes.isEmpty()) {
            inv.setItem(22, item(Material.BARRIER, "&cNo routes yet",
                    "&7Use &e/route create &7to start",
                    "&7Right-click two waystones with a Route Deed"));
        }

        inv.setItem(40, item(Material.EMERALD, "&aCreate Route",
                "&7Hold Route Deed, right-click 2 waystones",
                "&7Cost: $5-40 upkeep per tier",
                "",
                "&eClick for help"));
        inv.setItem(44, item(Material.OAK_DOOR, "&cBack to Hub"));

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
            meta.setLore(Arrays.stream(lore).map(RouteGui::color).toList());
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
