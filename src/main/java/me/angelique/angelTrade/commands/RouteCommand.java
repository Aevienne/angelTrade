package me.angelique.angelTrade.commands;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.models.TradeRoute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RouteCommand implements CommandExecutor {

    private final AngelTrade plugin;

    public RouteCommand(AngelTrade plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                player.sendMessage(color("&eHold a Route Deed and right-click two Waystones to create a route."));
            }
            case "list" -> {
                List<TradeRoute> routes = plugin.getRouteManager().getRoutesForPlayer(player.getUniqueId());
                if (routes.isEmpty()) { player.sendMessage(color("&7You have no trade routes.")); return true; }
                player.sendMessage(color("&6--- Your Trade Routes ---"));
                for (TradeRoute r : routes) {
                    player.sendMessage(color("&e" + r.getId() + " &7| " + r.getTier().name().replace("_"," ")
                        + " | " + r.getStatus().name() + " | Risk: &" + riskColor(r) + r.getRiskLabel()
                        + " &7| Uses: " + r.getUses()));
                }
            }
            case "info" -> {
                if (args.length < 2) { player.sendMessage(color("&cUsage: /route info <id>")); return true; }
                TradeRoute r = plugin.getRouteManager().getRoute(args[1].toUpperCase());
                if (r == null) { player.sendMessage(color("&cRoute not found.")); return true; }
                player.sendMessage(color("&6--- Route " + r.getId() + " ---"));
                player.sendMessage(color("&7Tier: &e" + r.getTier().name().replace("_"," ")));
                player.sendMessage(color("&7Status: &e" + r.getStatus().name()));
                player.sendMessage(color("&7Uses: &e" + r.getUses()));
                player.sendMessage(color("&7Last Used: &e" + r.getLastUsed()));
                player.sendMessage(color("&7Insured: &e" + r.isInsured()));
                player.sendMessage(color("&7Insurance Pool: &e$" + String.format("%.2f", r.getInsurancePool())));
                player.sendMessage(color("&7Risk: &" + riskColor(r) + r.getRiskLabel() + " &7(" + String.format("%.0f", r.getRiskRating()*100) + "%)"));
                double bonus = plugin.getBonusManager().getRouteValueBonus(r.getTier()) * 100;
                player.sendMessage(color("&7Value Bonus: &a+" + (int)bonus + "%"));
            }
            case "insure" -> {
                if (args.length < 2) { player.sendMessage(color("&cUsage: /route insure <id>")); return true; }
                plugin.getRouteManager().insureRoute(player, args[1].toUpperCase());
            }
            case "remove" -> {
                if (args.length < 2) { player.sendMessage(color("&cUsage: /route remove <id>")); return true; }
                plugin.getRouteManager().removeRoute(player, args[1].toUpperCase());
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&6--- angelTrade Route Help ---"));
        player.sendMessage(color("&e/route create &7- Begin route creation (use Route Deed)"));
        player.sendMessage(color("&e/route list &7- List your routes"));
        player.sendMessage(color("&e/route info <id> &7- Route details"));
        player.sendMessage(color("&e/route insure <id> &7- Insure a route"));
        player.sendMessage(color("&e/route remove <id> &7- Remove a route"));
    }

    private String color(String s) { return s.replace('&', '\u00A7'); }

    private String riskColor(TradeRoute r) {
        double v = r.getRiskRating();
        if (v < 0.25) return "a";
        if (v < 0.50) return "e";
        if (v < 0.75) return "6";
        return "c";
    }
}
