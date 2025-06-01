package gg.sunken.shop.provider.economy;

import org.bukkit.entity.Player;

public interface EconomyProvider {

    void withdraw(Player player, double amount);

    void deposit(Player player, double amount);

    void has(Player player, double amount);

    double balance(Player player);

    void set(Player player, double amount);
}
