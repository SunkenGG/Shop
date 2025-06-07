package gg.sunken.shop.entity.trades;

import org.bukkit.entity.Player;

public interface NpcCurrency {

    double amount();

    boolean has(Player player);

    void take(Player player);

    void give(Player player);

    String description();
}
