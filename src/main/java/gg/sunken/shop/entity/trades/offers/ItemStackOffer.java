package gg.sunken.shop.entity.trades.offers;

import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.entity.trades.NpcOffer;
import gg.sunken.shop.entity.trades.currency.ItemCurrencyCost;
import gg.sunken.shop.provider.item.ItemProviders;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemStackOffer implements NpcOffer {

    private final String itemId;
    private final List<NpcCurrencyCost> buyPrice;
    private final List<NpcCurrencyCost> buyReward;
    private final List<NpcCurrencyCost> sellPrice;
    private final List<NpcCurrencyCost> sellReward;

    public ItemStackOffer(String itemId, int amount, List<NpcCurrencyCost> buyPrice, List<NpcCurrencyCost> sellReward) {
        this.itemId = itemId;
        this.buyPrice = buyPrice;
        this.sellReward = sellReward;

        if (buyPrice.isEmpty()) {
            this.buyReward = List.of();
        } else {
            this.buyReward = List.of(
                    new ItemCurrencyCost(itemId, amount)
            );
        }

        if (sellReward.isEmpty()) {
            this.sellPrice = List.of();
        } else {
            this.sellPrice = List.of(
                    new ItemCurrencyCost(itemId, amount)
            );
        }
    }

    @Override
    public List<NpcCurrencyCost> buyCost() {
        return buyPrice;
    }

    @Override
    public List<NpcCurrencyCost> buyReward() {
        return buyReward;
    }

    @Override
    public List<NpcCurrencyCost> sellCost() {
        return sellPrice;
    }

    @Override
    public List<NpcCurrencyCost> sellReward() {
        return sellReward;
    }

    @Override
    public ItemStack icon() {
        return ItemProviders.fromId(itemId).orElseThrow(() -> new IllegalArgumentException("Item ID " + itemId + " does not exist."));
    }
}
