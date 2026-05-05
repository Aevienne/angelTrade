package me.angelique.angelTrade.managers;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.listeners.TradeShopListener;
import me.angelique.angelTrade.listeners.WaystoneListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public class RecipeManager {

    public static void register(AngelTrade plugin) {
        registerWaystone(plugin);
        registerRouteDeed(plugin);
        registerTradeShop(plugin);
    }

    private static void registerWaystone(AngelTrade plugin) {
        // Waystone: Lodestone surrounded by gold ingots
        //  G G G
        //  G L G
        //  G G G
        NamespacedKey key = new NamespacedKey(plugin, "waystone_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, WaystoneListener.createWaystone(plugin));
        recipe.shape("GGG", "GLG", "GGG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('L', Material.LODESTONE);
        plugin.getServer().addRecipe(recipe);
    }

    private static void registerRouteDeed(AngelTrade plugin) {
        // Route Deed: paper + feather + ink sac
        //  _ F _
        //  _ P _
        //  _ I _
        NamespacedKey key = new NamespacedKey(plugin, "route_deed_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, WaystoneListener.createRouteDeed(plugin));
        recipe.shape(" F ", " P ", " I ");
        recipe.setIngredient('F', Material.FEATHER);
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('I', Material.INK_SAC);
        plugin.getServer().addRecipe(recipe);
    }

    private static void registerTradeShop(AngelTrade plugin) {
        // Trade Shop block: Chest flanked by emeralds, gold block on top
        //  _ G _
        //  E C E
        //  _ E _
        NamespacedKey key = new NamespacedKey(plugin, "trade_shop_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, TradeShopListener.createTradeShopBlock(plugin));
        recipe.shape(" G ", "ECE", " E ");
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('C', Material.CHEST);
        plugin.getServer().addRecipe(recipe);
    }
}
