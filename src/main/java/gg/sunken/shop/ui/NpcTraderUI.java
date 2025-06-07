package gg.sunken.shop.ui;

import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.entity.trades.NpcOffer;
import gg.sunken.shop.entity.trades.NpcTrader;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(fluent = true)
public class NpcTraderUI {

    private final NpcTrader trader;
    private final Gui gui;

    public NpcTraderUI(NpcTrader trader) {
        this.trader = trader;

        gui = Gui.empty(9, 6);
        List<NpcOffer> trades = trader.trades();

        if (trades.isEmpty()) {
            gui.setItem(1, 1, new SimpleItem(new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§cNo trades available")
                    .addLoreLines("§7This trader has no trades available at the moment.")));
            return;
        }

        int row = 1;
        int column = 1;

        for (NpcOffer offer : trades) {
            ShopItemButton button = new ShopItemButton(offer);
            if (column > 8) {
                column = 1;
                row++;
            }
            gui.setItem(row, column, button);
            column++;
        }
    }

    public void open(Player player, String title) {
        title = title == null ? trader.name() : title;

        Window window = Window.single()
                .setViewer(player)
                .setTitle(title)
                .setGui(gui)
                .build();

        window.open();
    }

}
