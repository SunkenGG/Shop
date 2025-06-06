package gg.sunken.shop.commands;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.trades.NpcTrader;
import gg.sunken.shop.entity.trades.impl.EconomyTrade;
import gg.sunken.shop.provider.economy.EconomyProviders;
import gg.sunken.shop.redis.PriceSyncManager;
import gg.sunken.shop.redis.RedisPriceSyncManager;
import gg.sunken.shop.repository.DynamicPriceRepository;
import gg.sunken.shop.ui.NpcTraderUI;
import lombok.extern.java.Log;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Log
public class DebugCommand extends BukkitCommand {

    private final ShopPlugin plugin = ShopPlugin.instance();
    private final DynamicPriceRepository repository;
    private final PriceSyncManager syncManager;

    public DebugCommand(DynamicPriceRepository repository, PriceSyncManager syncManager) {
        super("ecodebug");
        this.repository = repository;
        this.syncManager = syncManager;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage("/ecodebug <sell/buy/check/testui> <id> [amount]");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("testui")) {
            if (sender instanceof Player player) {
                NpcTrader trader = new NpcTrader("paul", "Paul", List.of(new EconomyTrade(plugin, EconomyProviders.provider("vault"), "DIAMOND", 1)));
                new NpcTraderUI(trader).open(player);
                return true;
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            DynamicPriceItem item = plugin.shopService().item(args[1]);
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
            DynamicPriceItem item = plugin.shopService().item(id);
            if (item == null) {
                sender.sendMessage("Item does not exist.");
                return true;
            }

            try {
                item.stock(item.stock() - amount);
            } catch (Exception e) {
                sender.sendMessage("Not enough stock.");
                return true;
            }

            repository.addHistory(id, amount);
            double price = item.calculateTransactionPrice(amount);
            syncManager.updateStock(id, amount);
            repository.save(item);
            log.info("Selling " + amount + " of " + item.id() + " for " + price + " coins.");
            sender.sendMessage("Sold " + amount + " of " + item.id() + " for " + price + " coins.");
        }

        if (args[0].equalsIgnoreCase("buy")) {
            DynamicPriceItem item = plugin.shopService().item(id);
            if (item == null) {
                sender.sendMessage("Item does not exist.");
                return true;
            }

            try {
                item.stock(item.stock() + amount);
            } catch (Exception e) {
                sender.sendMessage("Not enough space in stock.");
                return true;
            }

            repository.removeHistory(id, amount);
            double price = item.calculateTransactionPrice(-amount);
            syncManager.updateStock(id, -amount);
            repository.save(item);
            log.info("Buying " + amount + " of " + item.id() + " for " + price + " coins.");
            sender.sendMessage("Purchased " + amount + " of " + item.id() + " for " + price + " coins.");
        }

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            return List.of("sell", "buy", "check");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("check"))) {
            return plugin.shopService().repository().allPrices().stream().map(DynamicPriceItem::id).toList();
        }
        return List.of();
    }
}
