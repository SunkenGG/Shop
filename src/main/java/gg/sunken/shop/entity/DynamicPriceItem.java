package gg.sunken.shop.entity;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.bson.Document;

@Getter
@Accessors(fluent = true)
@Log
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
        this.minStock = getStockFromValue(template.maxPrice());
        this.maxStock = getStockFromValue(template.minPrice());
        this.price = computePrice(stock);
    }

    /**
     * Updates the stock quantity of the item and recalculates its price.
     * The provided stock value must lie within the defined minimum and maximum stock range.
     * Logs the updated stock value and recalculates the price based on the provided stock.
     *
     * @param stock the new stock quantity to set, must be between {@code minStock} and {@code maxStock}.
     * @throws IllegalArgumentException if the provided stock is outside the valid range.
     */
    public void stock(int stock) throws IllegalArgumentException {
        if (stock < minStock || stock > maxStock) {
            throw new IllegalArgumentException("Stock must be between " + minStock + " and " + maxStock);
        }
        if (stock == this.stock) return;

        this.stock = stock;
        this.price = computePrice(stock);
    }

    /**
     * Calculates the total transaction price for a given stock change (delta).
     * The method computes the price difference using the current stock and the new stock
     * (after applying the delta), accounting for adjustments such as rounding
     * and a minimum enforced transaction price.
     * @param delta Positive or negative number for the change made to stock
     * @return Buy or sell amount for a given delta (always positive)
     **/
    public double calculateTransactionPrice(int delta) {
        if (delta == 0) {
            return 0.0;
        }

        int newStock = stock + delta;

        double totalPrice = integratePrice(stock, newStock);
        return Math.max(Math.abs(round(totalPrice, 2)), 0.01);
    }

    private double integratePrice(int currentStock, int newStock) {
        currentStock = (int) Math.max(minStock, Math.min(currentStock, maxStock));
        newStock = (int) Math.max(minStock, Math.min(newStock, maxStock));

        double totalBaseIntegral = computeBaseIntegral(currentStock, newStock);
        double totalAdjustedIntegral = computeAdjustedIntegral(currentStock, newStock);

        return totalBaseIntegral + totalAdjustedIntegral;
    }

    private double computeBaseIntegral(double lowerBound, double upperBound) {
        double P0 = template.initialPrice();
        double elasticity = template.elasticity();
        double k = elasticity * 0.0005;

        return -(P0 / k) * Math.exp(-k * upperBound) + (P0 / k) * Math.exp(-k * lowerBound);
    }

    private double computeAdjustedIntegral(double lowerBound, double upperBound) {
        double support = template.support();
        double resistance = template.resistance();
        double P0 = template.initialPrice();
        double elasticity = template.elasticity();
        double k = elasticity * 0.0005;

        double adjustedIntegral = 0.0;

        if (computePrice(lowerBound) < support || computePrice(upperBound) < support) {
            adjustedIntegral += 0.1 * supportIntegral(lowerBound, upperBound, support, k, P0);
        }

        if (computePrice(lowerBound) > resistance || computePrice(upperBound) > resistance) {
            adjustedIntegral -= 0.1 * resistanceIntegral(lowerBound, upperBound, resistance, k, P0);
        }

        return adjustedIntegral;
    }

    private double computePrice(double stock) {
        if (stock < minStock || stock > maxStock) {
            throw new IllegalArgumentException("Stock must be between " + minStock + " and " + maxStock);
        }

        double price = template.initialPrice() * Math.exp(-stock * template.elasticity() * 0.0005);

        if (price < template.support()) price += (template.support() - price) * 0.1;
        if (price > template.resistance()) price -= (price - template.resistance()) * 0.1;

        return Math.max(round(price, 2), 0.01);
    }

    private double supportIntegral(double lowerBound, double upperBound, double support, double k, double P0) {
        double expLower = Math.exp(-k * lowerBound);
        double expUpper = Math.exp(-k * upperBound);
        return -(P0 / k) * (expUpper - expLower) + support * (upperBound - lowerBound);
    }

    private double resistanceIntegral(double lowerBound, double upperBound, double resistance, double k, double P0) {
        double expLower = Math.exp(-k * lowerBound);
        double expUpper = Math.exp(-k * upperBound);
        return -(P0 / k) * (expUpper - expLower) - resistance * (upperBound - lowerBound);
    }

    private double getStockFromValue(double price) {
        double initialPrice = template.initialPrice();
        double elasticity = template.elasticity();
        double k = elasticity * 0.0005;

        return -Math.log(price / initialPrice) / k;
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    /**
     * Serializes the current state of the DynamicPriceItem instance into a MongoDB Document.
     * The serialized document contains the unique identifier of the item and its current stock value.
     *
     * @return a Document representing the serialized state of the item, including its "_id" and "stock".
     */
    public Document serialize() {
        return new Document()
                .append("_id", id)
                .append("stock", stock);
    }
}