package gg.sunken.shop.entity.trades;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface NpcTrade {

    ItemStack givingIcon();

    ItemStack receivingIcon();

    double cost();

    int amount();

    void trade(Player player);

    boolean canTrade(Player player);

}
