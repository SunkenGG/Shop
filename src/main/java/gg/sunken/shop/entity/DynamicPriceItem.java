package gg.sunken.shop.entity;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bson.Document;
import org.bukkit.Bukkit;

@Getter
@Accessors(fluent = true)
public class DynamicPriceItem {

    private final String id;
    private int stock;
    private double price; // last computed price
    private final transient ItemTemplate template;

    public DynamicPriceItem(String id, ItemTemplate template) {
        this.id = id;
        this.template = template;
        this.stock = 0;
        this.price = computePrice(stock);
    }

    /**
     * Safely sets the current stock of an item while maintaining that you aren't going negative
     * Side effect of also recalculating the price based off the current stock.
     * @param stock New total stock
     * @throws IllegalArgumentException Stock cannot be negative
     */
    public void stock(int stock) throws IllegalArgumentException {
        this.stock = stock;
        this.price = computePrice(stock);
    }

    public double calculateTransactionPrice(int delta) throws IllegalArgumentException {
        int newStock = stock + delta;

        double totalPrice = integratePiecewise(stock, newStock);
        Bukkit.broadcastMessage(String.valueOf(totalPrice));
        return Math.max(totalPrice, 0.01);
    }

    private double computePrice(int stock) {
        double price = template.initialPrice() * Math.exp(-stock * template.elasticity() * 0.0005);
        if (price < template.support()) price += (template.support() - price) * 0.1;
        if (price > template.resistance()) price -= (price - template.resistance()) * 0.1;
        return Math.max(price, 0.01);
    }

    private double integratePiecewise(double x1, double x2) {
        double initialStock = Math.min(x1, x2);
        double finalStock = Math.max(x1, x2);
        double totalIntegral = 0.0;

        double y1 = template.initialPrice() * Math.exp(-0.0005 * template.elasticity() * template.maxStock());
        double y2 = template.initialPrice() * Math.exp(-0.0005 * template.elasticity() * template.minStock());

        if (finalStock > initialStock) {
            totalIntegral += Math.min(finalStock, template.maxStock()) - initialStock * y1;
            totalIntegral += integrateAnalytically(
                    Math.max(initialStock, template.maxStock()),
                    Math.min(finalStock, template.minStock())
            );
            totalIntegral += Math.max(finalStock - Math.max(initialStock, template.minStock()), 0) * y2;
        }

        return totalIntegral;
    }

    private double integrateAnalytically(double x1, double x2) {
        final double k = 0.0005 * template().elasticity();

        double factor = template.initialPrice() / k;
        double expTerm1 = Math.exp(-k * x1);
        double expTerm2 = Math.exp(-k * x2);
        return factor * (expTerm1 - expTerm2);
    }

    public void applyStockDelta(int delta) {
        stock(stock + delta);
    }

    public Document serialize() {
        return new Document()
                .append("_id", id)
                .append("stock", stock)
                .append("price", price);
    }
}
