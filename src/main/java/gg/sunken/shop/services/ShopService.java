package gg.sunken.shop.services;

import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.ItemTemplate;
import gg.sunken.shop.provider.economy.EconomyProvider;
import gg.sunken.shop.provider.item.ItemProviders;
import gg.sunken.shop.controller.PriceSyncController;
import gg.sunken.shop.repository.DynamicPriceRepository;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log
@Getter
@Accessors(fluent = true)
public class ShopService {

    private final DynamicPriceRepository repository;
    private final PriceSyncController priceSyncController;

    public ShopService(DynamicPriceRepository repository, PriceSyncController priceSyncController) {
        this.repository = repository;
        this.priceSyncController = priceSyncController;
    }

    /**
     * Facilitates the purchase of an item by a player, deducts the appropriate amount of money
     * from the player's account, adds the item to the player's inventory, and updates item stock.
     *
     * @param player The player who is buying the item.
     * @param id The unique identifier of the item being purchased.
     * @param amount The quantity of the item the player wants to purchase.
     * @param economyProvider The economy provider responsible for handling player transactions.
     * @return The total price of the purchased items.
     * @throws IllegalArgumentException if the item does not exist, the price is zero,
     *                                  the player does not have sufficient funds, the player does not
     *                                  have enough inventory space, or there is insufficient stock.
     */
    public double buy(Player player, String id, int amount, EconomyProvider economyProvider) throws IllegalArgumentException {
        DynamicPriceItem item = item(id);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        // calculate price as if transaction is made
        double price = item.calculateTransactionPrice(-amount);

        if (price == 0) {
            throw new IllegalArgumentException("Price cannot be zero.");
        }

        price *= (1 + item.template().buyTax());

        if (!economyProvider.has(player, price)) {
            throw new IllegalArgumentException("Not enough money to buy this item.");
        }

        Optional<ItemStack> stack = ItemProviders.fromId(id);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        // check if player has enough space
        ItemStack itemStack = stack.get();
        int neededSlots = roundUp(amount * itemStack.getAmount());
        if (player.getInventory().firstEmpty() == -1) {
            throw new IllegalArgumentException("Not enough space in inventory.");
        }

        int freeSlots = player.getInventory().getStorageContents().length - player.getInventory().getContents().length;
        if (neededSlots > freeSlots) {
            throw new IllegalArgumentException("Not enough space in inventory.");
        }

        // check stock
        try {
            item.stock(item.stock() - amount);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not enough space in stock.");
        }

        // give items to player
        int amountToGive = amount;
        List<ItemStack> toGive = new ArrayList<>();
        while (amountToGive > 0) {
            ItemStack clone = itemStack.clone();
            clone.setAmount(Math.min(amountToGive, clone.getMaxStackSize()));
            amountToGive -= clone.getAmount();
            toGive.add(clone);
        }

        for (ItemStack stackToGive : toGive) {
            player.getInventory().addItem(stackToGive).forEach((integer, itemStack1) -> {
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack1);
            });
        }

        // charge player
        economyProvider.withdraw(player, price);

        // update stock and history + publish to redis
        repository.removeHistory(id, amount);
        priceSyncController.updateStock(id, -amount);
        repository.save(item);

        return price;
    }

    /**
     * Simulates a purchase of a specified quantity of an item without actually processing the transaction.
     * This method calculates the price as if the transaction is completed, performs stock validation,
     * updates internal records, and publishes the changes.
     *
     * @param id The unique identifier of the item to simulate buying.
     * @param amount The quantity of the item to simulate purchasing. Must be positive and valid within available stock.
     * @return The calculated price as if the transaction was made.
     * @throws IllegalArgumentException If the item does not exist, the calculated price is zero, or there is insufficient stock.
     */
    public double fakeBuy(String id, int amount) throws IllegalArgumentException {
        DynamicPriceItem item = item(id);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        // calculate price as if transaction is made
        double price = item.calculateTransactionPrice(-amount);

        if (price == 0) {
            throw new IllegalArgumentException("Price cannot be zero.");
        }

        price *= (1 + item.template().buyTax());

        Optional<ItemStack> stack = ItemProviders.fromId(id);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        // check stock
        try {
            item.stock(item.stock() - amount);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not enough space in stock.");
        }

        // update stock and history + publish to redis
        repository.removeHistory(id, amount);
        priceSyncController.updateStock(id, -amount);
        repository.save(item);

        return price;
    }

    /**
     * Sells a specified amount of an item from a player's inventory, adjusts the item's stock,
     * generates a transaction price, and deposits the price into the player's account using the provided economy provider.
     *
     * @param player The player performing the sell action.
     *               This is the source of the item and recipient of the transaction payment.
     * @param id The unique identifier of the item to be sold.
     *           This is used to locate the item and validate its presence in both inventory and repository.
     * @param amount The quantity of the item to be sold.
     *               This determines the stock adjustment and the transaction's total price.
     * @param economyProvider The economy provider responsible for depositing the calculated price into the player's account.
     *                        This ensures the player is paid appropriately after the transaction.
     * @return The total price of the transaction based on the item's dynamic pricing and the specified amount.
     * @throws IllegalArgumentException If the item does not exist, the price is zero,
     *                                  the player does not have enough inventory space or items,
     *                                  or the item's stock cannot accommodate the transaction.
     */
    public double sell(Player player, String id, int amount, EconomyProvider economyProvider) {
        DynamicPriceItem item = item(id);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        // calculate price as if transaction is made
        double price = item.calculateTransactionPrice(amount);

        if (price == 0) {
            throw new IllegalArgumentException("Price cannot be zero.");
        }

        price *= (1 - item.template().sellTax());

        Optional<ItemStack> stack = ItemProviders.fromId(id);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        ItemStack itemStack = stack.get();
        int neededSlots = roundUp(amount * itemStack.getAmount());
        if (neededSlots > player.getInventory().getStorageContents().length) {
            throw new IllegalArgumentException("Not enough space in inventory.");
        }

        // check if player has enough items
        int totalAmount = 0;
        for (ItemStack content : player.getInventory().getContents()) {
            if (content != null && content.isSimilar(itemStack)) {
                totalAmount += content.getAmount();
            }
        }

        if (totalAmount < amount) {
            throw new IllegalArgumentException("Not enough items to sell.");
        }

        // update stock and history + publish to redis
        try {
            item.stock(item.stock() + amount);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not enough space in stock.");
        }

        // remove items from player
        player.getInventory().removeItem(itemStack);

        repository.addHistory(id, amount);
        priceSyncController.updateStock(id, amount);
        repository.save(item);

        // give money to player
        economyProvider.deposit(player, price);
        return price;
    }

    /**
     * Simulates the sale of an item by calculating the transaction price
     * for the given quantity, updating the stock, and maintaining the transaction history.
     * Does not actually perform the sale, but adjusts internal records accordingly.
     *
     * @param id The unique identifier of the item to be "sold".
     * @param amount The quantity of the item to simulate selling. Must be a positive integer.
     * @return The transaction price calculated as if the sale were made.
     * @throws IllegalArgumentException If the item does not exist, the calculated price is zero,
     *                                   or there is insufficient space in stock to adjust.
     */
    public double fakeSell(String id, int amount) {
        DynamicPriceItem item = item(id);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        // calculate price as if transaction is made
        double price = item.calculateTransactionPrice(amount);

        if (price == 0) {
            throw new IllegalArgumentException("Price cannot be zero.");
        }

        price *= (1 - item.template().sellTax());

        // check stock
        try {
            item.stock(item.stock() + amount);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not enough space in stock.");
        }

        // update stock and history + publish to redis
        repository.addHistory(id, amount);
        priceSyncController.updateStock(id, amount);
        repository.save(item);
        return price;
    }

    /**
     * Calculates the transaction price for a given item based on its ID and the specified amount.
     *
     * @param id the unique identifier of the item
     * @param amount the quantity of the item to calculate the price for
     * @return the calculated price for the specified item and quantity
     * @throws IllegalArgumentException if the item does not exist
     */
    public double buyPrice(String id, int amount) {
        DynamicPriceItem item = item(id);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist.");
        }
        return item.calculateTransactionPrice(amount) * (1 + item.template().buyTax());
    }

    /**
     * Calculates the price of a product based on its identifier.
     *
     * @param id the unique identifier of the product
     * @return the calculated price of the product
     */
    public double buyPrice(String id) {
        return buyPrice(id, 1);
    }

    /**
     * Calculates the selling price of an item based on the provided item ID and quantity.
     *
     * @param id the unique identifier of the item to be sold
     * @param amount the quantity of the item to be sold
     * @return the total selling price after applying the item's transaction price and sell tax
     * @throws IllegalArgumentException if the item with the specified ID does not exist
     */
    public double sellPrice(String id, int amount) {
        DynamicPriceItem item = item(id);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist.");
        }
        return item.calculateTransactionPrice(amount) * (1 - item.template().sellTax());
    }

    /**
     * Calculates the selling price for an item based on its ID and a default quantity of 1.
     *
     * @param id the unique identifier of the item
     * @return the calculated selling price of the item
     */
    public double sellPrice(String id) {
        return sellPrice(id, 1);
    }

    /**
     * Retrieves a DynamicPriceItem by its identifier.
     * @param id the unique identifier of the item
     * @return the DynamicPriceItem associated with the given identifier, or null if it does not exist
     */
    @Nullable
    public DynamicPriceItem item(String id) {
        return repository.priceById(id);
    }

    /**
     * Retrieves an ItemTemplate based on its unique identifier.
     *
     * @param id the unique identifier of the item template to retrieve
     * @return the ItemTemplate associated with the provided identifier, or null if not found
     */
    public ItemTemplate template(String id) {
        return repository.templateById(id);
    }

    /**
     * Retrieves a list of all DynamicPriceItem objects from the repository.
     * If the repository returns null, an empty list is returned.
     *
     * @return a list of DynamicPriceItem objects; never null, an empty list if no items are available
     */
    public @NotNull List<DynamicPriceItem> items() {
        List<DynamicPriceItem> items = repository.allPrices();
        if (items == null) {
            return new ArrayList<>();
        }
        return items;
    }

    /**
     * Retrieves a list of all item templates from the repository.
     * If no templates are found, returns an empty list.
     *
     * @return a list of item templates, or an empty list if none are available
     */
    public @NotNull List<ItemTemplate> templates() {
        List<ItemTemplate> templates = repository.allTemplates();
        if (templates == null) {
            return new ArrayList<>();
        }
        return templates;
    }

    /**
     * Adds an item template to the repository. This method ensures that the provided
     * template is not null before adding it to the repository.
     *
     * @param template the item template to be added
     * @throws IllegalArgumentException if the provided template is null
     */
    public void template(ItemTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null.");
        }
        repository.addTemplate(template);
    }

    /**
     * Adds a dynamic price item to the repository.
     *
     * @param item the dynamic price item to be added. Must not be null.
     * @throws IllegalArgumentException if the item is null.
     */
    public void item(DynamicPriceItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null.");
        }
        repository.addDynamicPriceItem(item);
    }

    /**
     * Removes a template with the specified ID from the repository.
     *
     * @param id the unique identifier of the template to be removed; must not be null or empty
     * @throws IllegalArgumentException if the provided ID is null or empty
     */
    public void removeTemplate(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Template ID cannot be null or empty.");
        }
        repository.removeTemplate(id);
    }

    /**
     * Removes an item identified by the given ID from the repository.
     *
     * @param id the unique identifier of the item to be removed. Must not be null or empty.
     * @throws IllegalArgumentException if the provided ID is null or empty.
     */
    public void removeItem(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty.");
        }
        repository.removeDynamicPriceItem(id);
    }

    private int roundUp(double number) {
        return (int) Math.ceil(number);
    }
}
