package me.angelique.angelTrade.commands;

import me.angelique.angelTrade.AngelTrade;
import me.angelique.angelTrade.models.EscrowTrade;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EscrowCommand implements CommandExecutor, TabCompleter {

    private final AngelTrade plugin;
    private final Map<String, EscrowTrade> trades = new ConcurrentHashMap<>();

    public EscrowCommand(AngelTrade plugin) { this.plugin = plugin; }

    private double getBalance(Player p) {
        return plugin.getEconomy().getBalance(p);
    }
    private boolean takeMoney(Player p, double amt) {
        return plugin.getEconomy().withdrawPlayer(p, amt).transactionSuccess();
    }
    private void giveMoney(Player p, double amt) {
        plugin.getEconomy().depositPlayer(p, amt);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e/escrow offer <player> <item> <qty> <price> §7| §e/escrow accept <id> §7| §e/escrow confirm <id> §7| §e/escrow cancel <id> §7| §e/escrow list");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "offer" -> {
                if (args.length < 5) { player.sendMessage("§c/escrow offer <player> <item> <qty> <price>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { player.sendMessage("§cPlayer not found."); return true; }
                if (target.equals(player)) { player.sendMessage("§cCan't trade with yourself."); return true; }
                Material mat = Material.matchMaterial(args[2].toUpperCase());
                if (mat == null) { player.sendMessage("§cInvalid item."); return true; }
                int qty = Integer.parseInt(args[3]);
                double price = Double.parseDouble(args[4]);
                if (!player.getInventory().contains(mat, qty)) { player.sendMessage("§cYou don't have enough."); return true; }

                String id = UUID.randomUUID().toString().substring(0, 8);
                EscrowTrade t = new EscrowTrade(id, player.getUniqueId(), target.getUniqueId(), mat.name(), qty, price);
                trades.put(id, t);

                player.sendMessage("§aEscrow offer §e" + id + " §asent to §e" + target.getName() +
                    " §7(" + qty + "x " + mat.name() + " @ $" + String.format("%.2f", price) + ")");
                target.sendMessage("§6[Escrow] §e" + player.getName() + " wants to trade " + qty + "x " + mat.name() +
                    " for $" + String.format("%.2f", price) + " §7(§e/escrow accept " + id + "§7)");
            }
            case "accept" -> {
                if (args.length < 2) { player.sendMessage("§c/escrow accept <id>"); return true; }
                EscrowTrade t = trades.get(args[1]);
                if (t == null || t.getStatus() != EscrowTrade.Status.PENDING || t.isExpired()) {
                    player.sendMessage("§cOffer not found or expired."); return true;
                }
                if (!t.getReceiverUUID().equals(player.getUniqueId())) { player.sendMessage("§cNot for you."); return true; }
                if (getBalance(player) < t.getPrice()) { player.sendMessage("§cInsufficient funds."); return true; }

                takeMoney(player, t.getPrice());
                Player offerSender = Bukkit.getPlayer(t.getSenderUUID());
                ItemStack stack = new ItemStack(Material.valueOf(t.getItemType()), t.getQuantity());
                offerSender.getInventory().removeItem(stack);
                t.setStatus(EscrowTrade.Status.DEPOSITED);
                player.sendMessage("§aEscrow deposit complete. §e/escrow confirm " + t.getId() + " §awhen delivered.");
                if (offerSender != null) offerSender.sendMessage("§a" + player.getName() + " deposited $" + String.format("%.2f", t.getPrice()) + " in escrow.");
            }
            case "confirm" -> {
                if (args.length < 2) { player.sendMessage("§c/escrow confirm <id>"); return true; }
                EscrowTrade t = trades.get(args[1]);
                if (t == null || t.getStatus() != EscrowTrade.Status.DEPOSITED) {
                    player.sendMessage("§cNot in deposited state."); return true;
                }
                if (!t.getReceiverUUID().equals(player.getUniqueId())) { player.sendMessage("§cNot your trade."); return true; }

                Player offerSender2 = Bukkit.getPlayer(t.getSenderUUID());
                giveMoney(offerSender2 != null ? offerSender2 : player, t.getPrice()); // pay sender
                // give item to receiver
                ItemStack reward = new ItemStack(Material.valueOf(t.getItemType()), t.getQuantity());
                player.getInventory().addItem(reward);
                t.setStatus(EscrowTrade.Status.COMPLETED);
                player.sendMessage("§aTrade complete!");
                if (offerSender2 != null) offerSender2.sendMessage("§a" + player.getName() + " confirmed delivery. You received $" + String.format("%.2f", t.getPrice()) + ".");
            }
            case "cancel" -> {
                if (args.length < 2) { player.sendMessage("§c/escrow cancel <id>"); return true; }
                EscrowTrade t = trades.get(args[1]);
                if (t == null) { player.sendMessage("§cNot found."); return true; }
                if (!t.getSenderUUID().equals(player.getUniqueId()) && !t.getReceiverUUID().equals(player.getUniqueId())) {
                    player.sendMessage("§cNot your trade."); return true;
                }
                if (t.getStatus() == EscrowTrade.Status.DEPOSITED) {
                    // refund buyer
                    Player buyer = Bukkit.getPlayer(t.getReceiverUUID());
                    if (buyer != null) giveMoney(buyer, t.getPrice());
                    // return items to seller
                    Player s = Bukkit.getPlayer(t.getSenderUUID());
                    if (s != null) s.getInventory().addItem(new ItemStack(Material.valueOf(t.getItemType()), t.getQuantity()));
                }
                t.setStatus(EscrowTrade.Status.CANCELLED);
                player.sendMessage("§cEscrow cancelled.");
            }
            case "list" -> {
                player.sendMessage("§6=== Your Escrow Trades ===");
                for (EscrowTrade t : trades.values()) {
                    if (t.getSenderUUID().equals(player.getUniqueId()) || t.getReceiverUUID().equals(player.getUniqueId())) {
                        player.sendMessage("§e" + t.getId() + " §7| " + t.getStatus() + " | " +
                            t.getQuantity() + "x " + t.getItemType() + " @ $" + String.format("%.2f", t.getPrice()));
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("offer", "accept", "confirm", "cancel", "list");
        return List.of();
    }
}
