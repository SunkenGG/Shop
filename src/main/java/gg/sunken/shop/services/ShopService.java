package gg.sunken.shop.services;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.provider.item.ItemProvider;
import gg.sunken.shop.provider.item.ItemProviders;
import lombok.extern.java.Log;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log
public class ShopService {

    private final static ShopPlugin PLUGIN = ShopPlugin.instance();

    public void buy(Player player, String id, int amount) throws IllegalArgumentException {
        DynamicPriceItem item = PLUGIN.items().get(id);
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



        PLUGIN.repository().removeHistory(id, amount);
        PLUGIN.syncManager().updateStock(id, -amount);
        PLUGIN.repository().save(item);

    }

    public void sell(Player player, String id, int amount) {}

    private int roundUp(double number) {
        return (int) Math.ceil(number);
    }

}
