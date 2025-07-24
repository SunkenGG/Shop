package gg.sunken.shop.entity.trades.currency;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.provider.economy.impl.VaultEconomyProvider;
import gg.sunken.shop.service.ShopService;
import org.bukkit.entity.Player;

public class DynamicVaultCurrencyCost implements NpcCurrencyCost {

    private final VaultEconomyProvider economyProvider;
    private final String itemId;
    private final double amount;
    private final ShopService shopService;
    private final DynamicPriceItem item;

    public DynamicVaultCurrencyCost(VaultEconomyProvider economyProvider, String itemId, double amount) {
        this.economyProvider = economyProvider;
        this.itemId = itemId;
        this.amount = amount;
        this.shopService = ShopPlugin.instance().shopService();
        this.item = shopService.item(itemId);
    }

    @Override
    public boolean canBuy() {
        return item.hasEnoughStockToBuy();
    }

    @Override
    public boolean canSell() {
        return item.hasEnoughStockToSell();
    }

    @Override
    public double buyCost() {
        return shopService.buyPrice(itemId, (int) amount);
    }

    @Override
    public double sellCost() {
        return shopService.sellPrice(itemId, (int) amount);
    }

    @Override
    public boolean has(Player player) {
        return economyProvider.has(player, buyCost());
    }

    @Override
    public double withdraw(Player player) {
        economyProvider.withdraw(player, buyCost());
        shopService.fakeBuy(itemId, (int) amount);
        return buyCost();
    }

    @Override
    public double deposit(Player player) {
        economyProvider.deposit(player, sellCost());
        shopService.fakeSell(itemId, (int) amount);
        return amount;
    }

    @Override
    public String buyDescriptor() {
        return String.format("$%1$,.2f", buyCost());
    }

    @Override
    public String sellDescriptor() {
        return String.format("$%1$,.2f", sellCost());
    }
}
