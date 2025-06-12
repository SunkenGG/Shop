package gg.sunken.shop.entity.trades;

import org.bukkit.entity.Player;

public interface NpcCurrencyCost {

    default boolean canBuy() {
        return true;
    }

    default boolean canSell() {
        return true;
    }

    double buyCost();

    double sellCost();

    boolean has(Player player);

    double withdraw(Player player);

    double deposit(Player player);

    String buyDescriptor();

    String sellDescriptor();
}
