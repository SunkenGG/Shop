package gg.sunken.shop.entity.trades;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface NpcOffer {

    List<NpcCurrencyCost> buyCost();

    List<NpcCurrencyCost> buyReward();

    List<NpcCurrencyCost> sellCost();

    List<NpcCurrencyCost> sellReward();

    ItemStack icon();

    default boolean canBuy() {
        return !buyCost().isEmpty() || !buyReward().isEmpty();
    }

    default boolean canBuy(Player player) {
        if (!canBuy()) return false;

        for (NpcCurrencyCost cost : buyCost()) {
            if (!cost.canBuy()) {
                return false;
            }

            if (!cost.has(player)) {
                return false;
            }
        }

        return true;
    }

    default void buy(Player player) {
        buyCost().forEach(cost -> cost.withdraw(player));
        buyReward().forEach(reward -> reward.deposit(player));
    }

    default boolean canSell() {
        return !sellCost().isEmpty() || !sellReward().isEmpty();
    }

    default boolean canSell(Player player) {
        if (!canSell()) return false;

        for (NpcCurrencyCost cost : sellCost()) {
            if (!cost.canSell()) {
                return false;
            }

            if (!cost.has(player)) {
                return false;
            }
        }

        return true;
    }

    default void sell(Player player) {
        sellCost().forEach(cost -> cost.withdraw(player));
        sellReward().forEach(reward -> reward.deposit(player));
    }
}
