package gg.sunken.shop.entity.trades.impl.offers;

import gg.sunken.shop.entity.trades.NpcOffer;
import gg.sunken.shop.entity.trades.NpcCurrency;
import gg.sunken.shop.provider.item.ItemProviders;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemStackOffer implements NpcOffer {

    private final ItemStack itemStack;
    private final int amount;
    private final List<NpcCurrency> buyCost;
    private final List<NpcCurrency> sellCost;

    public ItemStackOffer(String id, int amount, List<NpcCurrency> buyCost, List<NpcCurrency> sellCost) {
        this.amount = amount;
        this.buyCost = buyCost;
        this.sellCost = sellCost;
        this.itemStack = ItemProviders.fromId(id)
                .orElseThrow(() -> new IllegalArgumentException("Item with id '" + id + "' not found"));

        this.itemStack.setAmount(this.amount);
    }

    @Override
    public ItemStack receiveIcon() {
        return itemStack.clone();
    }

    @Override
    public List<NpcCurrency> buyCost() {
        return buyCost;
    }

    @Override
    public List<NpcCurrency> sellCost() {
        return sellCost;
    }

    @Override
    public void buy(Player player) {
        if (player.getInventory().firstEmpty() == -1) {
            throw new IllegalStateException("Player's inventory is full, cannot trade item");
        }

        // cost
        for (NpcCurrency trade : buyCost()) {
            trade.take(player);
        }

        // reward
        ItemStack clonedItem = itemStack.clone();
        clonedItem.setAmount(amount);

        player.getInventory().addItem(clonedItem);
    }

    @Override
    public void sell(Player player) {
        if (!player.getInventory().containsAtLeast(itemStack, amount)) {
            throw new IllegalStateException("Player does not have enough items to sell");
        }

        // cost
        ItemStack clone = itemStack.clone();
        clone.setAmount(amount);
        player.getInventory().removeItem(clone);

        // reward
        for (NpcCurrency trade : sellCost()) {
            trade.give(player);
        }
    }
}
