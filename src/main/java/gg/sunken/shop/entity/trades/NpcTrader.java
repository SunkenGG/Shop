package gg.sunken.shop.entity.trades;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(fluent = true)
public class NpcTrader {
    private final String id;
    private final String name;
    private final List<NpcTrade> trades;
}
