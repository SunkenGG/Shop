package gg.sunken.shop.services;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.ItemTemplate;
import gg.sunken.shop.provider.economy.EconomyProvider;
import gg.sunken.shop.provider.item.ItemProviders;
import gg.sunken.shop.redis.PriceSyncManager;
import gg.sunken.shop.repository.DynamicPriceRepository;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log
@Getter
@Accessors(fluent = true)
public class ShopService {

    private final DynamicPriceRepository repository;
    private final PriceSyncManager priceSyncManager;

    public ShopService(DynamicPriceRepository repository, PriceSyncManager priceSyncManager) {
        this.repository = repository;
        this.priceSyncManager = priceSyncManager;
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
        priceSyncManager.updateStock(id, -amount);
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
        priceSyncManager.updateStock(id, amount);
        repository.save(item);

        // give money to player
        economyProvider.deposit(player, price);
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
    public double price(String id, int amount) {
        DynamicPriceItem item = item(id);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist.");
        }
        return item.calculateTransactionPrice(amount);
    }

    /**
     * Calculates the price of a product based on its identifier.
     *
     * @param id the unique identifier of the product
     * @return the calculated price of the product
     */
    public double price(String id) {
        return price(id, 1);
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

    private int roundUp(double number) {
        return (int) Math.ceil(number);
    }

}
