package gg.sunken.shop.ui;

import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.entity.trades.NpcOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class ShopItemButton extends AbstractItem {

    private final NpcOffer offer;

    public ShopItemButton(NpcOffer offer) {
        this.offer = offer;
    }

    @Override
    public ItemProvider getItemProvider(Player viewer) {
        ItemBuilder itemBuilder = new ItemBuilder(offer.icon())
                .addLoreLines(
                        ""
                );
        if (!offer.buyCost().isEmpty()) {
            itemBuilder.addLoreLines("§7Buy Cost:");
            for (NpcCurrencyCost currency : offer.buyCost()) {
                itemBuilder.addLoreLines(
                        currency.descriptor()
                );
            }

            if (!offer.sellCost().isEmpty()) {
                itemBuilder.addLoreLines("");
            }
        }

        if (!offer.sellCost().isEmpty()) {
            itemBuilder.addLoreLines("§7Sell Cost:");
            for (NpcCurrencyCost currency : offer.sellCost()) {
                itemBuilder.addLoreLines(
                        currency.descriptor()
                );
            }
        }

        return itemBuilder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        switch (clickType) {
            case LEFT -> {
                if (offer.buyCost().isEmpty()) {
                    return;
                }

                if (!offer.canBuy(player)) {
                    return;
                }

                offer.buy(player);
                notifyWindows();
            }
            case RIGHT -> {
                if (offer.sellCost().isEmpty()) {
                    return;
                }

                if (!offer.canSell(player)) {
                    return;
                }

                offer.sell(player);
                notifyWindows();
            }
            case SHIFT_LEFT -> {
                if (offer.buyCost().isEmpty()) {
                    return;
                }

                if (!offer.canBuy(player)) {
                    return;
                }

                for (int i = 0; i < 16; i++) {
                    if (!offer.canBuy(player)) {
                        break;
                    }

                    offer.buy(player);
                }

                notifyWindows();
            }
            case SHIFT_RIGHT -> {
                if (offer.sellCost().isEmpty()) {
                    return;
                }

                if (!offer.canSell(player)) {
                    return;
                }

                for (int i = 0; i < 16; i++) {
                    if (!offer.canSell(player)) {
                        break;
                    }

                    offer.sell(player);
                }

                notifyWindows();
            }
        }
    }
}
