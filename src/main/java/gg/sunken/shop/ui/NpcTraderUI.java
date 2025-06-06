package gg.sunken.shop.ui;

import gg.sunken.shop.entity.trades.NpcTrade;
import gg.sunken.shop.entity.trades.NpcTrader;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(fluent = true)
public class NpcTraderUI {

    private final NpcTrader trader;
    private final List<Gui> pages = new ArrayList<>();
    private int page = 0;

    public NpcTraderUI(NpcTrader trader) {
        this.trader = trader;

        final int itemsPerPage = 6;
        final int rowSize = 9;
        final int tradesColumn = 0;

        final List<Gui> pages = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < trader.trades().size(); pageIndex += itemsPerPage) {
            List<NpcTrade> trades = trader.trades()
                    .subList(pageIndex, Math.min(pageIndex + itemsPerPage, trader.trades().size()));

            Gui page = Gui.empty(rowSize, 6);

            for (int tradeIndex = 0; tradeIndex < trades.size(); tradeIndex++) {
                int tradeSlot = tradeIndex * rowSize + tradesColumn;

                NpcTrade npcTrade = trades.get(tradeIndex);

                page.setItem(tradeSlot, new TraderGivingItem(npcTrade));
                page.setItem(tradeSlot + 1, new SimpleItem(new ItemBuilder(Material.SPECTRAL_ARROW)));
                page.setItem(tradeSlot + 2, new TraderReceiveItem(npcTrade));
            }

            pages.add(page);
        }

        this.pages.addAll(pages);
    }

    public void open(Player player) {
        if (page < 0 || page >= pages.size()) {
            throw new IndexOutOfBoundsException("Page index out of bounds: " + page);
        }

        Window window = Window.single()
                .setViewer(player)
                .setTitle("Traders - " + trader.name())
                .setGui(pages.getFirst())
                .build();

        window.open();
    }

}
