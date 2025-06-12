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
    private final double minStock;
    private final double maxStock;
    private final ItemTemplate template;
    private double price; // last computed price
    private int stock; // can be negative

    /**
     * Constructs a new DynamicPriceItem instance with the specified unique identifier
     * and the associated item template. The constructor initializes the item's stock,
     * minimum stock, maximum stock, and price based on the given template.
     *
     * @param id the unique identifier for the dynamic price item.
     * @param template the template defining the item's pricing rules, including initial
     *                 price, elasticity, support, resistance, and price range.
     * @throws IllegalArgumentException if the computed stock is outside the valid range
     *                                  defined by the template.
     */
    public DynamicPriceItem(String id, ItemTemplate template) {
        this.id = id;
        this.template = template;
        this.stock = 0;
        this.minStock = getStockFromValue(template.maxPrice());
        log.info("Min stock: " + minStock + " Max price: " + template.maxPrice());
        this.maxStock = getStockFromValue(template.minPrice());
        log.info("Max stock: " + maxStock + " Min price: " + template.minPrice());
        this.price = currentPrice();
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
        if (stock == this.stock) return;

        stock = (int) Math.clamp(stock, minStock, maxStock);

        if (stock < minStock || stock > maxStock) {
            throw new IllegalArgumentException("Stock adjustment is out of valid range.");
        }

        this.stock = stock;

        this.price = currentPrice();
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
            return 0.0; // No transaction
        }

        int newStock = stock + delta;

        newStock = (int) Math.clamp(newStock, minStock, maxStock);

        double totalPrice = integratePrice(stock, newStock);

        double minTransactionPrice = 0.01; // Prevent free transactions
        return Math.max(round(totalPrice), minTransactionPrice);
    }
    /**
     * Determines whether the current stock is sufficient to meet the minimum stock required for a purchase.
     *
     * @return true if the current stock is greater than the minimum stock; false otherwise.
     */
    public boolean hasEnoughStockToBuy() {
        return stock > minStock;
    }

    /**
     * Determines whether the current stock is below the maximum stock limit,
     * indicating that there are enough stocks available to sell.
     *
     * @return true if the current stock is less than the allowed maximum stock;
     *         false otherwise.
     */
    public boolean hasEnoughStockToSell() {
        return stock < maxStock;
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

    private double integratePrice(int currentStock, int newStock) {
        currentStock = (int) Math.clamp(currentStock, minStock, maxStock);
        newStock = (int) Math.clamp(newStock, minStock, maxStock);

        double totalBaseIntegral = computeBaseIntegral(currentStock, newStock);
        double totalAdjustedIntegral = computeAdjustedIntegral(currentStock, newStock);

        return totalBaseIntegral + totalAdjustedIntegral;
    }

    private double computeBaseIntegral(double lowerBound, double upperBound) {
        double pZero = template.initialPrice();
        double elasticity = template.elasticity();
        double k = elasticity * 0.0005;

        return -(pZero / k) * Math.exp(-k * upperBound) + (pZero / k) * Math.exp(-k * lowerBound);
    }

    private double computeAdjustedIntegral(double lowerBound, double upperBound) {
        double support = template.support();
        double resistance = template.resistance();
        double pZero = template.initialPrice();
        double elasticity = template.elasticity();
        double k = elasticity * 0.0005;

        double adjustedIntegral = 0.0;

        if (computePrice(lowerBound) < support || computePrice(upperBound) < support) {
            adjustedIntegral += 0.1 * supportIntegral(lowerBound, upperBound, support, k, pZero);
        } else if (computePrice(lowerBound) > resistance || computePrice(upperBound) > resistance) {
            adjustedIntegral -= 0.1 * resistanceIntegral(lowerBound, upperBound, resistance, k, pZero);
        }

        return adjustedIntegral;
    }

    private double computePrice(double stock) {
        double initialPrice = template.initialPrice();
        double elasticity = template.elasticity();
        double k = elasticity * 0.0005;

        double basePrice = initialPrice * Math.exp(-k * stock);

        return round(basePrice);
    }

    private double currentPrice() {
        double initialPrice = template.initialPrice();
        double elasticity = template.elasticity();
        double k = elasticity * 0.0005;
        double support = template.support();
        double resistance = template.resistance();

        double adjustedPrice = initialPrice * Math.exp(-k * stock);

        if (adjustedPrice < support) {
            double diff = support - adjustedPrice;
            adjustedPrice = support + diff * 0.1;
        } else if (adjustedPrice > resistance) {
            double diff = adjustedPrice - resistance;
            adjustedPrice = resistance - diff * 0.1;
        }

        double minPrice = Math.max(template.minPrice(), 0.01);
        adjustedPrice = Math.max(adjustedPrice, minPrice);

        return round(adjustedPrice);
    }

    private double supportIntegral(double lowerBound, double upperBound, double support, double k, double pZero) {
        double expLower = Math.exp(-k * lowerBound);
        double expUpper = Math.exp(-k * upperBound);
        return -(pZero / k) * (expUpper - expLower) + support * (upperBound - lowerBound);
    }

    private double resistanceIntegral(double lowerBound, double upperBound, double resistance, double k, double pZero) {
        double expLower = Math.exp(-k * lowerBound);
        double expUpper = Math.exp(-k * upperBound);
        return -(pZero / k) * (expUpper - expLower) - resistance * (upperBound - lowerBound);
    }

    private double getStockFromValue(double price) {
        double initialPrice = template.initialPrice();
        double elasticity = template.elasticity();
        double k = elasticity * 0.0005;

        return -Math.log(price / initialPrice) / k;
    }

    private double round(double value) {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}