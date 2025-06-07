package gg.sunken.shop.entity.trades.impl.trades;

import gg.sunken.shop.entity.trades.NpcCurrency;
import gg.sunken.shop.provider.economy.impl.ExperienceEconomyProvider;
import org.bukkit.entity.Player;

public class ExpEconomyCurrency implements NpcCurrency {

    private final ExperienceEconomyProvider provider;
    private final double amount;

    public ExpEconomyCurrency(ExperienceEconomyProvider provider, double amount) {
        this.provider = provider;
        this.amount = amount;
    }

    @Override
    public double amount() {
        return amount;
    }

    @Override
    public boolean has(Player player) {
        return provider.has(player, amount);
    }

    @Override
    public void take(Player player) {
        provider.withdraw(player, amount);
    }

    @Override
    public void give(Player player) {
        provider.deposit(player, amount);
    }

    @Override
    public String description() {
        return String.format("%s %s", amount, provider.symbol());
    }
}
