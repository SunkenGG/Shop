package gg.sunken.shop.commands;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.trades.NpcOffer;
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

import java.util.ArrayList;
import java.util.List;

@Log
public class DebugCommand extends BukkitCommand {

    private final ShopPlugin plugin = ShopPlugin.instance();
    private final DynamicPriceRepository repository;
    private final PriceSyncController syncManager;
    private final NpcTrader trader;

    public DebugCommand(DynamicPriceRepository repository, PriceSyncController syncManager) {
        super("ecodebug");
        this.repository = repository;
        this.syncManager = syncManager;

        List<NpcOffer> offers = new ArrayList<>();
        String[] materials = {
                // Ores and minerals
                "DIAMOND", "GOLD_INGOT", "IRON_INGOT", "EMERALD", "COAL", "NETHERITE_INGOT", "REDSTONE",

                // Wood types
                "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG", "MANGROVE_LOG",
                "CHERRY_LOG", "PALE_OAK_LOG", "BAMBOO_BLOCK", "CRIMSON_STEM", "WARPED_STEM",

                // Stone and building materials
                "STONE", "TUFF", "DEEPSLATE", "DIORITE", "ANDESITE", "GRANITE", "SAND", "GRAVEL", "CLAY_BALL",

                // Miscellaneous
                "OBSIDIAN", "NETHER_BRICK", "QUARTZ", "PRISMARINE", "GLASS", "WHITE_WOOL", "TERRACOTTA"
        };

        for (String material : materials) {
            offers.add(createOffer(material));
        }

        trader = new NpcTrader("test_trader", "Test Trader", offers);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage("/ecodebug <sell/buy/check/testui> <id> [amount]");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("testui")) {
            if (sender instanceof Player player) {

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
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid amount. Please enter a valid number.");
                   return true;
               }
           }
        } else {
            sender.sendMessage("/ecodebug <sell/buy/check> <id> [amount]");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("Amount must be greater than 0.");
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
            return plugin.shopService().items().stream()
                    .map(DynamicPriceItem::id)
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("buy"))) {
            return List.of("1", "5", "10", "64");
        }
        return List.of();
    }

    private ItemStackOffer createOffer(String id) {
        // I know this is ugly A) test command & the line is long without it
        return new ItemStackOffer(
                id,
                1,
                List.of(
                        new DynamicVaultCurrencyCost(
                                (VaultEconomyProvider) EconomyProviders.VAULT,
                                id,
                                1
                        )
                ),
                List.of(
                        new DynamicVaultCurrencyCost(
                                (VaultEconomyProvider) EconomyProviders.VAULT,
                                id,
                                1
                        )
                )

        );
    }
}
