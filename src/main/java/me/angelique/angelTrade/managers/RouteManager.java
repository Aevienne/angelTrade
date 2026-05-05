package me.angelique.angelTrade.managers;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.models.TradeRoute;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class RouteManager {

    private final AngelTrade plugin;
    private final Map<String, TradeRoute> routes = new ConcurrentHashMap<>();
    // pending: player UUID -> waystone Location A, waiting for second waystone
    private final Map<UUID, Location> pendingCreation = new ConcurrentHashMap<>();

    public RouteManager(AngelTrade plugin) { this.plugin = plugin; }

    public void loadAll() {
        routes.clear();
        plugin.getDataManager().loadAllRoutes().forEach(r -> routes.put(r.getId(), r));
        plugin.getLogger().info("Loaded " + routes.size() + " trade routes.");
    }

    public void saveAll() {
        routes.values().forEach(r -> plugin.getDataManager().saveRoute(r));
    }

    public boolean createRoute(Player player, Location locA, Location locB) {
        double maxDist = plugin.getConfig().getDouble("max-route-distance", 1000);
        if (!locA.getWorld().equals(locB.getWorld())) {
            player.sendMessage(color("&cBoth waystones must be in the same world."));
            return false;
        }
        if (locA.distance(locB) > maxDist) {
            player.sendMessage(color("&cWaystones are too far apart. Max: &e" + (int) maxDist + " blocks."));
            return false;
        }
        // Check for duplicate
        for (TradeRoute r : routes.values()) {
            if (locsMatch(r.getLocationA(), locA) && locsMatch(r.getLocationB(), locB)) {
                player.sendMessage(color("&cA route between these waystones already exists."));
                return false;
            }
        }
        String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        TradeRoute route = new TradeRoute(id, player.getUniqueId(), locA, locB);
        routes.put(id, route);
        plugin.getDataManager().saveRoute(route);
        player.sendMessage(color(plugin.getConfig().getString("messages.route-created", "&aRoute created!") + " &7ID: &e" + id));
        return true;
    }

    public boolean removeRoute(Player player, String id) {
        TradeRoute r = routes.get(id);
        if (r == null) {
            player.sendMessage(color("&cRoute not found."));
            return false;
        }
        if (!r.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("angeltrade.admin")) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no-permission")));
            return false;
        }
        routes.remove(id);
        plugin.getDataManager().deleteRoute(id);
        player.sendMessage(color(plugin.getConfig().getString("messages.route-removed", "&cRoute removed.")));
        return true;
    }

    public boolean insureRoute(Player player, String id) {
        TradeRoute r = routes.get(id);
        if (r == null) { player.sendMessage(color("&cRoute not found.")); return false; }
        if (!r.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("angeltrade.admin")) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no-permission"))); return false;
        }
        if (r.isInsured()) { player.sendMessage(color("&eThis route is already insured.")); return false; }
        double cost = getUpkeepCost(r.getTier()) * 7;
        if (!plugin.getEconomy().has(player, cost)) {
            player.sendMessage(color("&cInsufficient funds. Insurance costs &e$" + String.format("%.2f", cost)));
            return false;
        }
        plugin.getEconomy().withdrawPlayer(player, cost);
        r.setInsured(true);
        r.setInsurancePool(cost * plugin.getConfig().getDouble("insurance-pool-fraction", 0.25));
        plugin.getDataManager().saveRoute(r);
        player.sendMessage(color(plugin.getConfig().getString("messages.route-insured")));
        return true;
    }

    public void handleWaystoneDestroyed(Location loc, UUID destroyerUUID) {
        for (TradeRoute r : new ArrayList<>(routes.values())) {
            if (!locsMatch(r.getLocationA(), loc) && !locsMatch(r.getLocationB(), loc)) continue;
            boolean sabotage = destroyerUUID != null && !r.getOwnerUUID().equals(destroyerUUID);
            if (sabotage) {
                plugin.getDataManager().recordSaboteur(destroyerUUID);
                applySaboteurTag(destroyerUUID);
                Player owner = Bukkit.getPlayer(r.getOwnerUUID());
                if (owner != null) {
                    String msg = plugin.getConfig().getString("messages.waystone-broken-owner", "&cYour waystone was destroyed!")
                            .replace("%location%", formatLoc(loc))
                            .replace("%player%", Bukkit.getOfflinePlayer(destroyerUUID).getName());
                    owner.sendMessage(color(msg));
                }
                if (r.isInsured() && r.getInsurancePool() > 0) {
                    plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(r.getOwnerUUID()), r.getInsurancePool());
                    if (owner != null) owner.sendMessage(color("&aInsurance payout: &e$" + String.format("%.2f", r.getInsurancePool())));
                }
            }
            r.setStatus(TradeRoute.Status.BROKEN);
            r.setBrokenAt(Instant.now());
            routes.remove(r.getId());
            plugin.getDataManager().saveRoute(r);
        }
    }

    public boolean tryReviveRoute(String routeId, Location newLoc) {
        // Grace period — route still in DB with BROKEN status
        TradeRoute r = plugin.getDataManager().loadAllRoutes().stream()
                .filter(x -> x.getId().equals(routeId) && x.getStatus() == TradeRoute.Status.BROKEN)
                .findFirst().orElse(null);
        if (r == null) return false;
        long graceDays = plugin.getConfig().getLong("revival-grace-days", 3);
        if (r.getBrokenAt() != null && r.daysSinceLastUse() > graceDays) return false;
        // Replace broken location
        if (locsMatch(r.getLocationA(), newLoc) || locsMatch(r.getLocationB(), newLoc)) {
            r.setStatus(TradeRoute.Status.HEALTHY);
            r.setLastUsed(Instant.now());
            r.setBrokenAt(null);
            routes.put(r.getId(), r);
            plugin.getDataManager().saveRoute(r);
            return true;
        }
        return false;
    }

    public void runDecayCycle() {
        int warnDays = plugin.getConfig().getInt("decay.warn-days", 7);
        int inactiveDays = plugin.getConfig().getInt("decay.inactive-days", 14);
        int brokenDays = plugin.getConfig().getInt("decay.broken-days", 21);
        int graceDays = plugin.getConfig().getInt("revival-grace-days", 3);

        for (TradeRoute r : new ArrayList<>(routes.values())) {
            long days = r.daysSinceLastUse();
            Player owner = Bukkit.getPlayer(r.getOwnerUUID());

            if (days >= brokenDays) {
                routes.remove(r.getId());
                plugin.getDataManager().deleteRoute(r.getId());
                if (owner != null) owner.sendMessage(color("&c[Route " + r.getId() + "] has BROKEN due to inactivity and has been deleted."));
            } else if (days >= inactiveDays) {
                if (r.getStatus() != TradeRoute.Status.INACTIVE) {
                    if (r.isInsured() && r.getInsurancePool() > 0) {
                        plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(r.getOwnerUUID()), r.getInsurancePool());
                        r.setInsurancePool(0);
                    }
                    r.setStatus(TradeRoute.Status.INACTIVE);
                    plugin.getDataManager().saveRoute(r);
                    if (owner != null) owner.sendMessage(color("&e[Route " + r.getId() + "] is now INACTIVE (" + days + " days unused). " + (brokenDays - days) + " days until deletion."));
                }
            } else if (days >= warnDays) {
                if (owner != null) owner.sendMessage(color("&e[Route " + r.getId() + "] has not been used in " + days + " days. Maintain it or it will decay!"));
            }
        }

        // Purge truly expired broken routes past grace period from DB
        plugin.getDataManager().loadAllRoutes().stream()
                .filter(r -> r.getStatus() == TradeRoute.Status.BROKEN && r.getBrokenAt() != null
                        && (Instant.now().getEpochSecond() - r.getBrokenAt().getEpochSecond()) / 86400 > graceDays)
                .forEach(r -> plugin.getDataManager().deleteRoute(r.getId()));
    }

    private void applySaboteurTag(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) p.sendMessage(color(plugin.getConfig().getString("messages.saboteur-tag", "&cSaboteur") + " &7tag applied to your profile."));
        // Stub: hook into external reputation system here
    }

    public TradeRoute getRoute(String id) { return routes.get(id); }

    public List<TradeRoute> getRoutesForPlayer(UUID uuid) {
        List<TradeRoute> list = new ArrayList<>();
        for (TradeRoute r : routes.values()) if (r.getOwnerUUID().equals(uuid)) list.add(r);
        return list;
    }

    public Map<UUID, Location> getPendingCreation() { return pendingCreation; }

    public Map<String, TradeRoute> getRoutes() { return routes; }

    public double getUpkeepCost(TradeRoute.Tier tier) {
        return switch (tier) {
            case DIRT_ROAD -> plugin.getConfig().getDouble("upkeep.dirt-road", 5.0);
            case STONE_ROAD -> plugin.getConfig().getDouble("upkeep.stone-road", 10.0);
            case GOLD_ROAD -> plugin.getConfig().getDouble("upkeep.gold-road", 20.0);
            case ROYAL_ROAD -> plugin.getConfig().getDouble("upkeep.royal-road", 40.0);
        };
    }

    private boolean locsMatch(Location a, Location b) {
        if (a == null || b == null) return false;
        if (!a.getWorld().equals(b.getWorld())) return false;
        return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    private String formatLoc(Location l) {
        return l.getWorld().getName() + " (" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + ")";
    }

    private String color(String s) { return s.replace('&', '\u00A7'); }
}
