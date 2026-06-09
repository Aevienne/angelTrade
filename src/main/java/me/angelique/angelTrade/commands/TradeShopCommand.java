package me.angelique.angelTrade.commands;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.gui.TradeShopBrowserGui;
import me.angelique.angelTrade.listeners.TradeShopListener;
import me.angelique.angelTrade.models.ShopItem;
import me.angelique.angelTrade.models.TradeShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TradeShopCommand implements CommandExecutor {

    private final AngelTrade plugin;

    public TradeShopCommand(AngelTrade plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("angeltrade.tradeshop.use")) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        if (args.length == 0) { TradeShopBrowserGui.open(player, plugin, 0); return true; }

        switch (args[0].toLowerCase()) {
            case "place" -> {
                player.getInventory().addItem(TradeShopListener.createTradeShopBlock(plugin));
                player.sendMessage(color("&aTrade Shop block added to your inventory. Place it in the world."));
            }
            case "give_waystone" -> {
                // Admin helper
                if (!player.hasPermission("angeltrade.admin")) {
                    player.sendMessage(color(plugin.getConfig().getString("messages.no-permission"))); return true;
                }
                player.getInventory().addItem(me.angelique.angelTrade.listeners.WaystoneListener.createWaystone(plugin));
                player.sendMessage(color("&aWaystone added to inventory."));
            }
            case "give_deed" -> {
                if (!player.hasPermission("angeltrade.admin")) {
                    player.sendMessage(color(plugin.getConfig().getString("messages.no-permission"))); return true;
                }
                player.getInventory().addItem(me.angelique.angelTrade.listeners.WaystoneListener.createRouteDeed(plugin));
                player.sendMessage(color("&aRoute Deed added to inventory."));
            }
            case "relocate" -> {
                if (args.length < 2) { player.sendMessage(color("&cUsage: /tradeshop relocate <shopId>")); return true; }
                TradeShop shop = plugin.getTradeShopManager().getShop(args[1].toUpperCase());
                if (shop == null) { player.sendMessage(color("&cShop not found.")); return true; }
                player.sendMessage(color("&eBreak and replace the shop chest to relocate. (Stub — full relocation via right-click planned)"));
                // Full drag-and-drop relocation is a UX improvement; stub for now
            }
            case "additem" -> {
                if (args.length < 4) {
                    player.sendMessage(color("&cUsage: /tradeshop additem <shopId> <MATERIAL> <price>")); return true;
                }
                try {
                    double price = Double.parseDouble(args[3]);
                    plugin.getTradeShopManager().addItem(player, args[1].toUpperCase(), args[2].toUpperCase(), price, 64);
                } catch (NumberFormatException ex) {
                    player.sendMessage(color("&cInvalid price."));
                }
            }
            case "removeitem" -> {
                if (args.length < 3) {
                    player.sendMessage(color("&cUsage: /tradeshop removeitem <shopId> <MATERIAL>")); return true;
                }
                plugin.getTradeShopManager().removeItem(player, args[1].toUpperCase(), args[2].toUpperCase());
            }
            case "setprice" -> {
                if (args.length < 4) {
                    player.sendMessage(color("&cUsage: /tradeshop setprice <shopId> <MATERIAL> <price>")); return true;
                }
                try {
                    double price = Double.parseDouble(args[3]);
                    plugin.getTradeShopManager().setPrice(player, args[1].toUpperCase(), args[2].toUpperCase(), price);
                } catch (NumberFormatException ex) {
                    player.sendMessage(color("&cInvalid price."));
                }
            }
            case "info" -> {
                if (args.length < 2) { player.sendMessage(color("&cUsage: /tradeshop info <shopId>")); return true; }
                TradeShop shop = plugin.getTradeShopManager().getShop(args[1].toUpperCase());
                if (shop == null) { player.sendMessage(color("&cShop not found.")); return true; }
                player.sendMessage(color("&6--- Trade Shop " + shop.getId() + " ---"));
                player.sendMessage(color("&7Company: &e" + shop.getCompanyId()));
                player.sendMessage(color("&7Items: &e" + shop.getItems().size()));
                player.sendMessage(color("&7Location: &e" + shop.getLocation().getWorld().getName()
                    + " " + shop.getLocation().getBlockX() + "," + shop.getLocation().getBlockY()
                    + "," + shop.getLocation().getBlockZ()));
                player.sendMessage(color("&7Linked Route: &e" + (shop.getLinkedRouteId() != null ? shop.getLinkedRouteId() : "None")));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&6--- angelTrade Shop Help ---"));
        player.sendMessage(color("&e/tradeshop place &7- Get a Trade Shop block"));
        player.sendMessage(color("&e/tradeshop relocate <id> &7- Relocate your shop"));
        player.sendMessage(color("&e/tradeshop additem <id> <material> <price> &7- Add item"));
        player.sendMessage(color("&e/tradeshop removeitem <id> <material> &7- Remove item"));
        player.sendMessage(color("&e/tradeshop setprice <id> <material> <price> &7- Update price"));
        player.sendMessage(color("&e/tradeshop info <id> &7- Shop info"));
    }

    private String color(String s) { return s.replace('&', '\u00A7'); }
}
