package gg.sunken.shop.entity.trades.currency;

import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.provider.economy.impl.ExperienceEconomyProvider;
import org.bukkit.entity.Player;

public class ExpCurrencyCost implements NpcCurrencyCost {

    private final ExperienceEconomyProvider economyProvider;
    private final int buyAmount;
    private final int sellAmount;

    public ExpCurrencyCost(ExperienceEconomyProvider economyProvider, int buyAmount, int sellAmount) {
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
        economyProvider.withdraw(player, (int) buyAmount);
        return buyAmount;
    }

    @Override
    public double deposit(Player player) {
        economyProvider.deposit(player, (int) sellAmount);
        return sellAmount;
    }

    @Override
    public String buyDescriptor() {
        return buyAmount + " XP";
    }

    @Override
    public String sellDescriptor() {
        return sellAmount + " XP";
    }
}
