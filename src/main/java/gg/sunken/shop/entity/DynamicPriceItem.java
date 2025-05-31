package gg.sunken.shop.entity;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bson.Document;
import org.bukkit.Bukkit;

@Getter
@Accessors(fluent = true)
public class DynamicPriceItem {

    private final String id;
    private int stock; // can be negative
    private double price; // last computed price
    private final transient double minStock;
    private final transient double maxStock;
    private final transient ItemTemplate template;

    public DynamicPriceItem(String id, ItemTemplate template) {
        this.id = id;
        this.template = template;
        this.stock = 0;
        this.minStock = getStockFromValue(template.minPrice());
        this.maxStock = getStockFromValue(template.maxPrice());
        this.price = computePrice(stock);
    }

    /**
     * Safely sets the current stock of an item while maintaining that you aren't going negative
     * Side effect of also recalculating the price based off the current stock.
     * @param stock New total stock
     */
    public void stock(int stock) {
        this.stock = stock;
        this.price = computePrice(stock);
    }

    /**
     * Compute the transaction price based on the current stock and the delta.
     * @param delta The change in stock, can be positive or negative.
     * @return The computed transaction price based on the new stock level.
     */
    public double calculateTransactionPrice(int delta) {
        int newStock = stock + delta;

        newStock = (int) Math.max(minStock, Math.min(newStock, maxStock));

        double totalPrice = integratePrice(stock, newStock);

        return Math.max(totalPrice, 0.01);
    }

    private double integratePrice(int currentStock, int newStock) {
        currentStock = (int) Math.max(minStock, Math.min(currentStock, maxStock));
        newStock = (int) Math.max(minStock, Math.min(newStock, maxStock));

        int lowerBound = Math.min(currentStock, newStock);
        int upperBound = Math.max(currentStock, newStock);

        double P0 = template.initialPrice();
        double elasticity = template.elasticity();
        double k = elasticity * 0.0005;

        double baseIntegral = -(P0 / k) * Math.exp(-k * upperBound) + (P0 / k) * Math.exp(-k * lowerBound);

        double support = template.support();
        double resistance = template.resistance();
        double adjustedIntegral = 0.0;

        if (computePrice(lowerBound) < support || computePrice(upperBound) < support) {
            adjustedIntegral += 0.1 * (supportIntegral(lowerBound, upperBound, support, k, P0));
        }

        if (computePrice(lowerBound) > resistance || computePrice(upperBound) > resistance) {
            adjustedIntegral -= 0.1 * (resistanceIntegral(lowerBound, upperBound, resistance, k, P0));
        }

        return baseIntegral + adjustedIntegral;
    }

    private double computePrice(int stock) {
        stock = (int) Math.max(minStock, Math.min(stock, maxStock));

        double price = template.initialPrice() * Math.exp(-stock * template.elasticity() * 0.0005);

        if (price < template.support()) price += (template.support() - price) * 0.1;
        if (price > template.resistance()) price -= (price - template.resistance()) * 0.1;

        return Math.max(price, 0.01);
    }

    private double supportIntegral(int lowerBound, int upperBound, double support, double k, double P0) {
        double expLower = Math.exp(-k * lowerBound);
        double expUpper = Math.exp(-k * upperBound);
        return -(P0 / k) * (expUpper - expLower) + support * (upperBound - lowerBound);
    }

    private double resistanceIntegral(int lowerBound, int upperBound, double resistance, double k, double P0) {
        double expLower = Math.exp(-k * lowerBound);
        double expUpper = Math.exp(-k * upperBound);
        return -(P0 / k) * (expUpper - expLower) - resistance * (upperBound - lowerBound);
    }

    private double getStockFromValue(double value) {
        return (Math.log(value / template.initialPrice()) / (-0.0005 * template.elasticity()));
    }

    public Document serialize() {
        return new Document()
                .append("_id", id)
                .append("stock", stock)
                .append("price", price);
    }
}
