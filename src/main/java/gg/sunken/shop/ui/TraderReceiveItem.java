package gg.sunken.shop.ui;

import gg.sunken.shop.entity.trades.NpcTrade;
import gg.sunken.shop.provider.item.ItemProviders;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

@Data
@Accessors(fluent = true)
public class TraderReceiveItem extends AbstractItem {

    private final NpcTrade trade;

    @Override
    public ItemProvider getItemProvider(Player viewer) {
        return s -> trade.receivingIcon();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if (trade == null) {
            return;
        }

        if (!trade.canTrade(player)) {
            inventoryClickEvent.setCancelled(true);
            return;
        }

        trade.trade(player);
    }
}
