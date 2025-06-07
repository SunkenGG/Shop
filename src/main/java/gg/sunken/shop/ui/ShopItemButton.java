package gg.sunken.shop.ui;

import gg.sunken.shop.entity.trades.NpcOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class ShopItemButton extends AbstractItem {

    private final NpcOffer npcOffer;

    public ShopItemButton(NpcOffer npcOffer) {
        this.npcOffer = npcOffer;
    }

    @Override
    public ItemProvider getItemProvider(Player viewer) {
        return s -> npcOffer.receiveIcon();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        switch (clickType) {
            case LEFT -> {
                if (npcOffer.buyCost().isEmpty()) {
                    return;
                }

                if (!npcOffer.canBuy(player)) {
                    player.playSound(player.getLocation(), "block.note_block.bit", 1, 1);
                    return;
                }

                npcOffer.buy(player);
                player.playSound(player.getLocation(), "block.note_block.pling", 1, 1);
            }
            case RIGHT -> {
                if (npcOffer.sellCost().isEmpty()) {
                    return;
                }

                if (!npcOffer.canSell(player)) {
                    player.playSound(player.getLocation(), "block.note_block.bit", 1, 1);
                    return;
                }

                npcOffer.sell(player);
                player.playSound(player.getLocation(), "block.note_block.pling", 1, 1);
            }
            case SHIFT_LEFT -> {
                if (npcOffer.buyCost().isEmpty()) {
                    return;
                }

                if (!npcOffer.canBuy(player)) {
                    player.playSound(player.getLocation(), "block.note_block.bit", 1, 1);
                    return;
                }

                for (int i = 0; i < 16; i++) {
                    if (!npcOffer.canBuy(player)) {
                        break;
                    }

                    npcOffer.buy(player);
                }

                player.playSound(player.getLocation(), "block.note_block.pling", 1, 1);
            }
            case SHIFT_RIGHT -> {
                if (npcOffer.sellCost().isEmpty()) {
                    return;
                }

                if (!npcOffer.canSell(player)) {
                    player.playSound(player.getLocation(), "block.note_block.bit", 1, 1);
                    return;
                }

                for (int i = 0; i < 16; i++) {
                    if (!npcOffer.canSell(player)) {
                        break;
                    }

                    npcOffer.sell(player);
                }

                player.playSound(player.getLocation(), "block.note_block.pling", 1, 1);
            }
        }
    }
}
