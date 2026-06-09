package me.angelique.angelTrade;

import me.angelique.angelTrade.commands.EscrowCommand;
import me.angelique.angelTrade.commands.RouteCommand;
import me.angelique.angelTrade.commands.TradeShopCommand;
import me.angelique.angelTrade.data.DataManager;
import me.angelique.angelTrade.gui.RouteGui;
import me.angelique.angelTrade.gui.RouteGuiListener;
import me.angelique.angelTrade.gui.TradeShopBrowserGui;
import me.angelique.angelTrade.gui.TradeShopGuiListener;
import me.angelique.angelTrade.listeners.TradeShopListener;
import me.angelique.angelTrade.listeners.WaystoneListener;
import me.angelique.angelTrade.managers.BonusManager;
import me.angelique.angelTrade.managers.RouteManager;
import me.angelique.angelTrade.managers.TradeShopManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class AngelTrade extends JavaPlugin {

    private static AngelTrade instance;
    private Economy economy;
    private DataManager dataManager;
    private RouteManager routeManager;
    private TradeShopManager tradeShopManager;
    private BonusManager bonusManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found — disabling angelTrade.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        dataManager = new DataManager(this);
        if (!dataManager.init()) {
            getLogger().severe("Database init failed — disabling angelTrade.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        routeManager = new RouteManager(this);
        tradeShopManager = new TradeShopManager(this);
        bonusManager = new BonusManager(this);

        routeManager.loadAll();
        tradeShopManager.loadAll();

        getServer().getPluginManager().registerEvents(new WaystoneListener(this), this);
        getServer().getPluginManager().registerEvents(new TradeShopListener(this), this);
        getServer().getPluginManager().registerEvents(new TradeShopGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new RouteGuiListener(), this);

        getCommand("route").setExecutor(new RouteCommand(this));
        getCommand("tradeshop").setExecutor(new TradeShopCommand(this));
        getCommand("escrow").setExecutor(new EscrowCommand(this));
        getCommand("escrow").setTabCompleter(new EscrowCommand(this));

        me.angelique.angelTrade.managers.RecipeManager.register(this);
        startDecayTask();
        getLogger().info("angelTrade enabled.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) dataManager.close();
        getLogger().info("angelTrade disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    private void startDecayTask() {
        long ticksPerDay = 20L * 60 * 60 * 24;
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    routeManager.runDecayCycle();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Decay cycle error", e);
                }
            }
        }.runTaskTimerAsynchronously(this, ticksPerDay, ticksPerDay);
    }

    public static AngelTrade getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public DataManager getDataManager() { return dataManager; }
    public RouteManager getRouteManager() { return routeManager; }
    public TradeShopManager getTradeShopManager() { return tradeShopManager; }
    public BonusManager getBonusManager() { return bonusManager; }
}
