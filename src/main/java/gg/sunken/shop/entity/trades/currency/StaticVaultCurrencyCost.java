package gg.sunken.shop.entity.trades.currency;

import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.provider.economy.impl.VaultEconomyProvider;
import org.bukkit.entity.Player;

public class StaticVaultCurrencyCost implements NpcCurrencyCost {

    private final VaultEconomyProvider economyProvider;
    private final double buyAmount;
    private final double sellAmount;

    public StaticVaultCurrencyCost(VaultEconomyProvider economyProvider, double buyAmount, double sellAmount) {
        this.economyProvider = economyProvider;
        this.buyAmount = buyAmount;
        this.sellAmount = sellAmount;
    }

    @Override
    public double buyCost() {
        return buyAmount;
    }

    @Override
    public double sellCost() {
        return sellAmount;
    }

    @Override
    public boolean has(Player player) {
        return economyProvider.has(player, buyAmount);
    }

    @Override
    public double withdraw(Player player) {
        economyProvider.withdraw(player, buyAmount);
        return buyAmount;
    }

    @Override
    public double deposit(Player player) {
        economyProvider.deposit(player, sellAmount);
        return sellAmount;
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
