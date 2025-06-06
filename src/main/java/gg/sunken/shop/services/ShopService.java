package gg.sunken.shop.services;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.ItemTemplate;
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
     * Processes the purchase of a specified item for the given player.
     * Deducts stock for the item, calculates the transaction price, and adds the item
     * to the player's inventory if sufficient inventory space is available.
     * Throws an exception if the item does not exist, stock is insufficient, or the inventory has insufficient space.
     *
     * @param player The player performing the purchase.
     * @param id The unique identifier of the item to be purchased.
     * @param amount The quantity of the item to be purchased.
     * @return The total price for the purchased items.
     * @throws IllegalArgumentException If the item does not exist, there is insufficient stock, or the player's inventory is full.
     */
    public double buy(Player player, String id, int amount) throws IllegalArgumentException {
        DynamicPriceItem item = item(id);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        try {
            item.stock(item.stock() - amount);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not enough space in stock.");
        }

        double price = item.calculateTransactionPrice(-amount);

        Optional<ItemStack> stack = ItemProviders.fromId(id);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        ItemStack itemStack = stack.get();
        int neededSlots = roundUp(amount * itemStack.getAmount());
        if (player.getInventory().firstEmpty() == -1) {
            throw new IllegalArgumentException("Not enough space in inventory.");
        }

        int freeSlots = player.getInventory().getStorageContents().length - player.getInventory().getContents().length;
        if (neededSlots > freeSlots) {
            throw new IllegalArgumentException("Not enough space in inventory.");
        }

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

        repository.removeHistory(id, amount);
        priceSyncManager.updateStock(id, -amount);
        repository.save(item);

        return price;
    }

    public void sell(Player player, String id, int amount) {
        DynamicPriceItem item = item(id);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist.");
        }

        double price = item.calculateTransactionPrice(amount);

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null || !itemStack.getType().isItem()) {
            throw new IllegalArgumentException("You must hold the item in your hand to sell it.");
        }

        if (itemStack.getAmount() < amount) {
            throw new IllegalArgumentException("Not enough items in hand to sell.");
        }

        item.stock(item.stock() + amount);
        repository.addHistory(id, amount);
        priceSyncManager.updateStock(id, amount);
        repository.save(item);

        itemStack.setAmount(itemStack.getAmount() - amount);
        if (itemStack.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(itemStack);
        }
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
