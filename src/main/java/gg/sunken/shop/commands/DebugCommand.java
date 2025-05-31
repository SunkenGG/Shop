package gg.sunken.shop.commands;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DebugCommand extends BukkitCommand {

    private final ShopPlugin plugin = ShopPlugin.instance();


    public DebugCommand() {
        super("ecodebug");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage("/ecodebug <sell/buy/check> <id> [amount]");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            DynamicPriceItem item = plugin.items().get(args[1]);
            if (item == null) {
                sender.sendMessage("Item does not exist.");
                return true;
            }
            sender.sendMessage("Item: " + item.id());
            sender.sendMessage("Stock: " + item.stock());
            sender.sendMessage("Price: " + item.price());
            return true;
        }

        String id;
        int amount = 1;

        if (args.length >= 2) {
            id = args[1];
            if (args.length >= 3) {
                amount = Integer.parseInt(args[2]);
            }
        } else {
            sender.sendMessage("/ecodebug <sell/buy/check> <id> [amount]");
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            DynamicPriceItem item = plugin.items().get(id);
            if (item == null) {
                sender.sendMessage("Item does not exist.");
                return true;
            }

            plugin.repository().addHistory(id, amount);
            double price = item.calculateTransactionPrice(amount);
            plugin.syncManager().updateStock(id, amount);
            plugin.repository().save(item);
            sender.sendMessage("Sold " + amount + " of " + item.id() + " for " + price + " coins.");
        }

        if (args[0].equalsIgnoreCase("buy")) {
            DynamicPriceItem item = plugin.items().get(id);
            if (item == null) {
                sender.sendMessage("Item does not exist.");
                return true;
            }


            plugin.repository().removeHistory(id, amount);
            double price = item.calculateTransactionPrice(-amount);
            plugin.syncManager().updateStock(id, -amount);
            plugin.repository().save(item);
            sender.sendMessage("Purchased " + amount + " of " + item.id() + " for " + price + " coins.");
        }

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            return List.of("sell", "buy", "check");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("check"))) {
            return plugin.items().keySet().stream().toList();
        }
        return List.of();
    }
}
