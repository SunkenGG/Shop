package gg.sunken.shop.entity;

import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

@Data
@Getter
@Accessors(fluent = true)
public class TradingHour {

    private final long hourEnd;
    private final String itemId;
    private final long amount;

    public boolean isLoss() {
        return amount < 0;
    }

    public boolean isGain() {
        return amount >= 0;
    }
}
