package gg.sunken.shop.entity.trades.currency;

import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.provider.economy.impl.VaultEconomyProvider;
import org.bukkit.entity.Player;

public class StaticVaultCurrencyCost implements NpcCurrencyCost {

    private final VaultEconomyProvider economyProvider;
    private final double amount;

    public StaticVaultCurrencyCost(VaultEconomyProvider economyProvider, double amount) {
        this.economyProvider = economyProvider;
        this.amount = amount;
    }

    @Override
    public double cost() {
        return amount;
    }

    @Override
    public boolean has(Player player) {
        return economyProvider.has(player, amount);
    }

    @Override
    public double withdraw(Player player) {
        economyProvider.withdraw(player, amount);
        return amount;
    }

    @Override
    public double deposit(Player player) {
        economyProvider.deposit(player, amount);
        return amount;
    }

    @Override
    public String descriptor() {
        return String.format("$%1$,.2f", cost());
    }
}
