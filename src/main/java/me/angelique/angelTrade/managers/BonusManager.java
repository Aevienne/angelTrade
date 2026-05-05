package me.angelique.angelTrade.managers;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.models.TradeRoute;
import me.angelique.angelTrade.models.TradeShop;

public class BonusManager {

    private final AngelTrade plugin;

    public BonusManager(AngelTrade plugin) { this.plugin = plugin; }

    public double getRouteValueBonus(TradeRoute.Tier tier) {
        return switch (tier) {
            case DIRT_ROAD -> plugin.getConfig().getDouble("route-bonus.dirt-road", 0.05);
            case STONE_ROAD -> plugin.getConfig().getDouble("route-bonus.stone-road", 0.12);
            case GOLD_ROAD -> plugin.getConfig().getDouble("route-bonus.gold-road", 0.20);
            case ROYAL_ROAD -> plugin.getConfig().getDouble("route-bonus.royal-road", 0.30);
        };
    }

    public double getShopPassiveIncome(TradeRoute.Tier tier) {
        return switch (tier) {
            case DIRT_ROAD -> plugin.getConfig().getDouble("route-passive-income.dirt-road", 0.02);
            case STONE_ROAD -> plugin.getConfig().getDouble("route-passive-income.stone-road", 0.05);
            case GOLD_ROAD -> plugin.getConfig().getDouble("route-passive-income.gold-road", 0.10);
            case ROYAL_ROAD -> plugin.getConfig().getDouble("route-passive-income.royal-road", 0.15);
        };
    }

    /**
     * Called when a sale occurs at a TradeShop.
     * Deposits passive income bonus to shop owner if shop is on an active route.
     */
    public void processSaleBonus(TradeShop shop, double saleAmount) {
        if (shop.getLinkedRouteId() == null) return;
        TradeRoute route = plugin.getRouteManager().getRoute(shop.getLinkedRouteId());
        if (route == null || route.getStatus() != TradeRoute.Status.HEALTHY) return;
        double bonus = saleAmount * getShopPassiveIncome(route.getTier());
        if (bonus <= 0) return;
        plugin.getEconomy().depositPlayer(
            plugin.getServer().getOfflinePlayer(shop.getOwnerUUID()), bonus
        );
        var p = plugin.getServer().getPlayer(shop.getOwnerUUID());
        if (p != null) p.sendMessage(color("&a[Route Bonus] +$" + String.format("%.2f", bonus) + " from sale at your shop."));
    }

    private String color(String s) { return s.replace('&', '\u00A7'); }
}
