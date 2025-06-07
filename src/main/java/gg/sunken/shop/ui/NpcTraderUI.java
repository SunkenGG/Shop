package gg.sunken.shop.ui;

import gg.sunken.shop.entity.trades.NpcOffer;
import gg.sunken.shop.entity.trades.NpcCurrency;
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

        List<SimpleItem> items = new ArrayList<>();
        for (NpcOffer offer : trades) {
            ItemBuilder itemBuilder = new ItemBuilder(offer.receiveIcon())
                    .addLoreLines(
                            ""
                    );
            if (!offer.buyCost().isEmpty()) {
                itemBuilder.addLoreLines("§7Buy Cost:");
                for (NpcCurrency trade : offer.buyCost()) {
                    itemBuilder.addLoreLines("§7- " + trade.description());
                }

                if (!offer.sellCost().isEmpty()) {
                    itemBuilder.addLoreLines("");
                }
            }

            if (!offer.sellCost().isEmpty()) {
                itemBuilder.addLoreLines("§7Sell Cost:");
                for (NpcCurrency trade : offer.sellCost()) {
                    itemBuilder.addLoreLines("§7- " + trade.description());
                }
            }

            SimpleItem item = new SimpleItem(itemBuilder);

            items.add(item);
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
