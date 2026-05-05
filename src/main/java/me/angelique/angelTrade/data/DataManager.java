package me.angelique.angelTrade.data;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.models.ShopItem;
import me.angelique.angelTrade.models.TradeRoute;
import me.angelique.angelTrade.models.TradeShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DataManager {

    private final AngelTrade plugin;
    private Connection connection;
    private final String dbPath;

    public DataManager(AngelTrade plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder().getAbsolutePath() + File.separator + "angeltrade.db";
    }

    public boolean init() {
        try {
            plugin.getDataFolder().mkdirs();
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.createStatement().execute("PRAGMA journal_mode=WAL;");
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "DB init error", e);
            return false;
        }
    }

    private Connection getConn() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.createStatement().execute("PRAGMA journal_mode=WAL;");
        }
        return connection;
    }

    private void createTables() throws SQLException {
        Connection c = getConn();
        c.createStatement().execute(
            "CREATE TABLE IF NOT EXISTS trade_routes (" +
            "id TEXT PRIMARY KEY," +
            "owner_uuid TEXT," +
            "company_id TEXT," +
            "world_a TEXT, x_a REAL, y_a REAL, z_a REAL," +
            "world_b TEXT, x_b REAL, y_b REAL, z_b REAL," +
            "uses INTEGER DEFAULT 0," +
            "tier TEXT DEFAULT 'DIRT_ROAD'," +
            "status TEXT DEFAULT 'HEALTHY'," +
            "last_used INTEGER," +
            "insured INTEGER DEFAULT 0," +
            "insurance_pool REAL DEFAULT 0.0," +
            "broken_at INTEGER" +
            ")"
        );
        c.createStatement().execute(
            "CREATE TABLE IF NOT EXISTS trade_shops (" +
            "id TEXT PRIMARY KEY," +
            "owner_uuid TEXT," +
            "company_id TEXT," +
            "world TEXT, x REAL, y REAL, z REAL," +
            "last_relocated INTEGER," +
            "linked_route_id TEXT" +
            ")"
        );
        c.createStatement().execute(
            "CREATE TABLE IF NOT EXISTS shop_items (" +
            "shop_id TEXT," +
            "item_key TEXT," +
            "price REAL," +
            "stock INTEGER," +
            "discount REAL DEFAULT 0.0," +
            "PRIMARY KEY (shop_id, item_key)" +
            ")"
        );
        c.createStatement().execute(
            "CREATE TABLE IF NOT EXISTS saboteurs (" +
            "uuid TEXT PRIMARY KEY," +
            "count INTEGER DEFAULT 0," +
            "last_offense INTEGER" +
            ")"
        );
    }

    // ── Trade Routes ─────────────────────────────────────────────────────────

    public void saveRoute(TradeRoute r) {
        String sql = "INSERT OR REPLACE INTO trade_routes VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getOwnerUUID().toString());
            ps.setString(3, r.getOwnerCompanyId());
            Location a = r.getLocationA(), b = r.getLocationB();
            ps.setString(4, a.getWorld().getName()); ps.setDouble(5, a.getX()); ps.setDouble(6, a.getY()); ps.setDouble(7, a.getZ());
            ps.setString(8, b.getWorld().getName()); ps.setDouble(9, b.getX()); ps.setDouble(10, b.getY()); ps.setDouble(11, b.getZ());
            ps.setInt(12, r.getUses());
            ps.setString(13, r.getTier().name());
            ps.setString(14, r.getStatus().name());
            ps.setLong(15, r.getLastUsed().getEpochSecond());
            ps.setInt(16, r.isInsured() ? 1 : 0);
            ps.setDouble(17, r.getInsurancePool());
            // broken_at handled separately via update if needed
            ps.executeUpdate();
            if (r.getBrokenAt() != null) {
                try (PreparedStatement p2 = getConn().prepareStatement(
                        "UPDATE trade_routes SET broken_at=? WHERE id=?")) {
                    p2.setLong(1, r.getBrokenAt().getEpochSecond());
                    p2.setString(2, r.getId());
                    p2.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "saveRoute error", e);
        }
    }

    public void deleteRoute(String id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM trade_routes WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "deleteRoute error", e);
        }
    }

    public List<TradeRoute> loadAllRoutes() {
        List<TradeRoute> list = new ArrayList<>();
        try (ResultSet rs = getConn().createStatement().executeQuery("SELECT * FROM trade_routes")) {
            while (rs.next()) {
                World wa = Bukkit.getWorld(rs.getString("world_a"));
                World wb = Bukkit.getWorld(rs.getString("world_b"));
                if (wa == null || wb == null) continue;
                Location la = new Location(wa, rs.getDouble("x_a"), rs.getDouble("y_a"), rs.getDouble("z_a"));
                Location lb = new Location(wb, rs.getDouble("x_b"), rs.getDouble("y_b"), rs.getDouble("z_b"));
                TradeRoute r = new TradeRoute(rs.getString("id"), UUID.fromString(rs.getString("owner_uuid")), la, lb);
                r.setOwnerCompanyId(rs.getString("company_id"));
                r.setUses(rs.getInt("uses"));
                r.setTier(TradeRoute.Tier.valueOf(rs.getString("tier")));
                r.setStatus(TradeRoute.Status.valueOf(rs.getString("status")));
                r.setLastUsed(Instant.ofEpochSecond(rs.getLong("last_used")));
                r.setInsured(rs.getInt("insured") == 1);
                r.setInsurancePool(rs.getDouble("insurance_pool"));
                long brokenAt = rs.getLong("broken_at");
                if (!rs.wasNull()) r.setBrokenAt(Instant.ofEpochSecond(brokenAt));
                list.add(r);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "loadAllRoutes error", e);
        }
        return list;
    }

    // ── Trade Shops ──────────────────────────────────────────────────────────

    public void saveShop(TradeShop s) {
        String sql = "INSERT OR REPLACE INTO trade_shops VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getOwnerUUID().toString());
            ps.setString(3, s.getCompanyId());
            Location l = s.getLocation();
            ps.setString(4, l.getWorld().getName()); ps.setDouble(5, l.getX()); ps.setDouble(6, l.getY()); ps.setDouble(7, l.getZ());
            ps.setLong(8, s.getLastRelocated().getEpochSecond());
            ps.setString(9, s.getLinkedRouteId());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "saveShop error", e);
        }
        saveShopItems(s);
    }

    public void saveShopItems(TradeShop s) {
        try {
            getConn().createStatement().execute("DELETE FROM shop_items WHERE shop_id='" + s.getId() + "'");
            for (ShopItem item : s.getItems()) {
                try (PreparedStatement ps = getConn().prepareStatement(
                        "INSERT INTO shop_items VALUES (?,?,?,?,?)")) {
                    ps.setString(1, s.getId());
                    ps.setString(2, item.getItemKey());
                    ps.setDouble(3, item.getPrice());
                    ps.setInt(4, item.getStock());
                    ps.setDouble(5, item.getDiscount());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "saveShopItems error", e);
        }
    }

    public void deleteShop(String id) {
        try {
            getConn().createStatement().execute("DELETE FROM trade_shops WHERE id='" + id + "'");
            getConn().createStatement().execute("DELETE FROM shop_items WHERE shop_id='" + id + "'");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "deleteShop error", e);
        }
    }

    public List<TradeShop> loadAllShops() {
        List<TradeShop> list = new ArrayList<>();
        try (ResultSet rs = getConn().createStatement().executeQuery("SELECT * FROM trade_shops")) {
            while (rs.next()) {
                World w = Bukkit.getWorld(rs.getString("world"));
                if (w == null) continue;
                Location l = new Location(w, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                TradeShop s = new TradeShop(rs.getString("id"), UUID.fromString(rs.getString("owner_uuid")),
                        rs.getString("company_id"), l);
                s.setLastRelocated(Instant.ofEpochSecond(rs.getLong("last_relocated")));
                s.setLinkedRouteId(rs.getString("linked_route_id"));
                loadShopItems(s);
                list.add(s);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "loadAllShops error", e);
        }
        return list;
    }

    private void loadShopItems(TradeShop s) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM shop_items WHERE shop_id=?")) {
            ps.setString(1, s.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ShopItem item = new ShopItem(rs.getString("item_key"), rs.getDouble("price"), rs.getInt("stock"));
                item.setDiscount(rs.getDouble("discount"));
                s.addItem(item);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "loadShopItems error", e);
        }
    }

    // ── Saboteur ─────────────────────────────────────────────────────────────

    public void recordSaboteur(UUID uuid) {
        try {
            getConn().createStatement().execute(
                "INSERT INTO saboteurs (uuid, count, last_offense) VALUES ('" + uuid + "', 1, " + Instant.now().getEpochSecond() + ")" +
                " ON CONFLICT(uuid) DO UPDATE SET count=count+1, last_offense=" + Instant.now().getEpochSecond()
            );
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "recordSaboteur error", e);
        }
    }

    public int getSaboteurCount(UUID uuid) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT count FROM saboteurs WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("count");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "getSaboteurCount error", e);
        }
        return 0;
    }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException ignored) {}
    }
}
