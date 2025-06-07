package gg.sunken.shop.entity.trades;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface NpcOffer {

    ItemStack receiveIcon();

    List<NpcCurrency> buyCost();

    List<NpcCurrency> sellCost();

    void buy(Player player);

    default boolean canBuy(Player player) {
        boolean canTrade = true;
        for (NpcCurrency npcCurrency : this.buyCost()) {
            if (!npcCurrency.has(player)) {
                canTrade = false;
                break;
            }
        }

        return canTrade;
    }

    void sell(Player player);

    default boolean canSell(Player player) {
        boolean canTrade = true;
        for (NpcCurrency npcCurrency : this.sellCost()) {
            if (!npcCurrency.has(player)) {
                canTrade = false;
                break;
            }
        }

        return canTrade;
    }
}
