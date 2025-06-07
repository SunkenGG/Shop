package gg.sunken.shop.commands;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.trades.NpcTrader;
import gg.sunken.shop.entity.trades.currency.DynamicVaultCurrencyCost;
import gg.sunken.shop.entity.trades.offers.ItemStackOffer;
import gg.sunken.shop.provider.economy.EconomyProviders;
import gg.sunken.shop.provider.economy.impl.VaultEconomyProvider;
import gg.sunken.shop.controller.PriceSyncController;
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
    private final PriceSyncController syncManager;

    public DebugCommand(DynamicPriceRepository repository, PriceSyncController syncManager) {
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

                NpcTrader trader = new NpcTrader("test_trader", "Test Trader", List.of(
                ));
                NpcTraderUI ui = new NpcTraderUI(trader);

                ui.open(player, null);

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
                plugin.shopService().sell((Player) sender, id, amount, EconomyProviders.VAULT);
                sender.sendMessage("Sold " + amount + " of " + item.id() + " for " + item.price() + " coins.");
            } catch (Exception e) {
                sender.sendMessage("Transaction failed: " + e.getMessage());
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("buy")) {
            DynamicPriceItem item = plugin.shopService().item(id);
            if (item == null) {
                sender.sendMessage("Item does not exist.");
                return true;
            }

            try {
                plugin.shopService().buy((Player) sender, id, amount, EconomyProviders.VAULT);
                sender.sendMessage("Bought " + amount + " of " + item.id() + " for " + item.price() + " coins.");
            } catch (Exception e) {
                sender.sendMessage("Transaction failed: " + e.getMessage());
            }
        }

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            return List.of("sell", "buy", "check", "testui");
        } else if (args.length == 2) {
            return plugin.shopService().items();
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("buy"))) {
            return List.of("1", "5", "10", "64");
        }
        return List.of();
    }

    private ItemStackOffer createOffer(String id, int amount) {
        // I know this is ugly A) test command & the line is long without it
        return new ItemStackOffer(
                id,
                amount,
                List.of(
                        new DynamicVaultCurrencyCost(
                                (VaultEconomyProvider) EconomyProviders.VAULT,
                                id,
                                amount
                        )
                ),
                List.of(
                        new DynamicVaultCurrencyCost(
                                (VaultEconomyProvider) EconomyProviders.VAULT,
                                id,
                                amount
                        )
                )

        );
    }
}
