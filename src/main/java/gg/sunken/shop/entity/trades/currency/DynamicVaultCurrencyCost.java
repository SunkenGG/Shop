package gg.sunken.shop.entity.trades.currency;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.provider.economy.impl.VaultEconomyProvider;
import gg.sunken.shop.services.ShopService;
import org.bukkit.entity.Player;

public class DynamicVaultCurrencyCost implements NpcCurrencyCost {

    private final VaultEconomyProvider economyProvider;
    private final String itemId;
    private final double amount;
    private final ShopService shopService;

    public DynamicVaultCurrencyCost(VaultEconomyProvider economyProvider, String itemId, double amount) {
        this.economyProvider = economyProvider;
        this.itemId = itemId;
        this.amount = amount;
        this.shopService = ShopPlugin.instance().shopService();
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
        return economyProvider.has(player, amount);
    }

    @Override
    public double withdraw(Player player) {
        economyProvider.withdraw(player, amount);
        shopService.fakeBuy(itemId, (int) amount);
        return amount;
    }

    @Override
    public double deposit(Player player) {
        economyProvider.deposit(player, amount);
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
