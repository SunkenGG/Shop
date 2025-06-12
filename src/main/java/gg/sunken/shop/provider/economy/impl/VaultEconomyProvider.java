package gg.sunken.shop.provider.economy.impl;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.provider.economy.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class VaultEconomyProvider implements EconomyProvider {

    private final Economy economy;

    public VaultEconomyProvider() {
        economy = ShopPlugin.instance().economy();
    }

    @Override
    public String symbol() {
        return "$";
    }

    @Override
    public void withdraw(Player player, double amount) {
        if (economy.has(player, amount)) {
            economy.withdrawPlayer(player, amount);
        } else {
            throw new IllegalArgumentException("Player does not have enough balance to withdraw " + amount);
        }
    }

    @Override
    public void deposit(Player player, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to deposit cannot be negative");
        }
        economy.depositPlayer(player, amount);
    }

    @Override
    public boolean has(Player player, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to check cannot be negative");
        }
        return economy.has(player, amount);
    }

    @Override
    public double balance(Player player) {
        double balance = economy.getBalance(player);
        if (balance < 0) {
            throw new IllegalStateException("Player's balance cannot be negative");
        }
        return balance;
    }

    @Override
    public void set(Player player, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to set cannot be negative");
        }

        double balance = balance(player);
        if (balance > amount) {
            withdraw(player, balance - amount);
        } else if (balance < amount) {
            deposit(player, amount - balance);
        }
    }
}
