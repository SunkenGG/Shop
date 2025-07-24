package gg.sunken.shop.entity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@Builder
public class ItemTemplate {
    private final String id;
    private final double initialPrice;
    private final double elasticity;
    private final double support;
    private final double resistance;
    private final double minPrice;
    private final double maxPrice;
    private final double buyTax;
    private final double sellTax;
}