package gg.sunken.shop.entity.trades;

import org.bukkit.entity.Player;

public interface NpcCurrencyCost {

    double cost();

    boolean has(Player player);

    double withdraw(Player player);

    double deposit(Player player);

    String descriptor();
}
