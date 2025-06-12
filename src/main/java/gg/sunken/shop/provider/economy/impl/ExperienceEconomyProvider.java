package gg.sunken.shop.provider.economy.impl;

import gg.sunken.shop.provider.economy.EconomyProvider;
import gg.sunken.shop.utils.ExperienceUtils;
import org.bukkit.entity.Player;

public class ExperienceEconomyProvider implements EconomyProvider {

    @Override
    public String symbol() {
        return "XP";
    }

    @Override
    public void withdraw(Player player, double amount) {
        ExperienceUtils.changeExp(player, -amount);
    }

    @Override
    public void deposit(Player player, double amount) {
        ExperienceUtils.changeExp(player, amount);
    }

    @Override
    public boolean has(Player player, double amount) {
        return ExperienceUtils.getExp(player) >= amount;
    }

    @Override
    public double balance(Player player) {
        return ExperienceUtils.getExp(player);
    }

    @Override
    public void set(Player player, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Experience amount cannot be negative");
        }

        double currentExp = ExperienceUtils.getExp(player);
        currentExp = Math.min(currentExp, amount);

        ExperienceUtils.changeExp(player, currentExp - amount);
    }
}
