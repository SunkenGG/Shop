package gg.sunken.shop.entity.trades.impl.trades;

import gg.sunken.shop.entity.trades.NpcCurrency;
import gg.sunken.shop.provider.economy.impl.VaultEconomyProvider;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public class VaultEconomyCurrency implements NpcCurrency {

    private final VaultEconomyProvider provider;
    private final Supplier<Double> amount;

    public VaultEconomyCurrency(VaultEconomyProvider provider, Supplier<Double> amount) {
        this.provider = provider;
        this.amount = amount;
    }

    @Override
    public double amount() {
        return amount.get();
    }

    @Override
    public boolean has(Player player) {
        return provider.has(player, amount());
    }

    @Override
    public void take(Player player) {
        provider.withdraw(player, amount());
    }

    @Override
    public void give(Player player) {
        provider.deposit(player, amount());
    }

    @Override
    public String description() {
        return String.format("%s%s", provider.symbol(), amount);
    }
}
