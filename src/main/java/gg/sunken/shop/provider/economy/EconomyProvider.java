package gg.sunken.shop.provider.economy;

import org.bukkit.entity.Player;

public interface EconomyProvider {

    String symbol();

    void withdraw(Player player, double amount);

    void deposit(Player player, double amount);

    boolean has(Player player, double amount);

    double balance(Player player);

    void set(Player player, double amount);
}
