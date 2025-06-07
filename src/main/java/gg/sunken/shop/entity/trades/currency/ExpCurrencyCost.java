package gg.sunken.shop.entity.trades.currency;

import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.provider.economy.impl.ExperienceEconomyProvider;
import org.bukkit.entity.Player;

public class ExpCurrencyCost implements NpcCurrencyCost {

    private final ExperienceEconomyProvider economyProvider;
    private final int amount;

    public ExpCurrencyCost(ExperienceEconomyProvider economyProvider, int amount) {
        this.economyProvider = economyProvider;
        this.amount = amount;
    }

    @Override
    public double cost() {
        return amount;
    }

    @Override
    public boolean has(Player player) {
        return economyProvider.has(player, (int) amount);
    }

    @Override
    public double withdraw(Player player) {
        economyProvider.withdraw(player, (int) amount);
        return amount;
    }

    @Override
    public double deposit(Player player) {
        economyProvider.deposit(player, (int) amount);
        return amount;
    }

    @Override
    public String descriptor() {
        return amount + " XP";
    }
}
