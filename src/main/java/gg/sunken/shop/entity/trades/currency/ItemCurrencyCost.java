package gg.sunken.shop.entity.trades.currency;

import gg.sunken.shop.entity.trades.NpcCurrencyCost;
import gg.sunken.shop.provider.item.ItemProviders;
import gg.sunken.shop.utils.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemCurrencyCost implements NpcCurrencyCost {

    private final String itemId;
    private final int amount;
    private final ItemStack itemStack;

    public ItemCurrencyCost(String itemId, int amount) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        this.itemId = itemId;
        this.amount = amount;

        this.itemStack = ItemProviders.fromId(itemId).orElseThrow(
                () -> new IllegalArgumentException("Item ID " + itemId + " does not exist."));

        this.itemStack.setAmount(amount);
    }

    @Override
    public double buyCost() {
        return amount;
    }

    @Override
    public double sellCost() {
        return amount;
    }

    @Override
    public boolean has(Player player) {
        if (player == null || amount <= 0) {
            return false;
        }

        ItemStack[] inventory = player.getInventory().getContents();
        int requiredAmount = this.amount;

        for (ItemStack item : inventory) {
            if (item != null && item.isSimilar(this.itemStack)) {
                requiredAmount -= item.getAmount();
                if (requiredAmount <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public double withdraw(Player player) {
        if (player == null || amount <= 0) {
            return 0;
        }

        int requiredAmount = this.amount;
        ItemStack[] inventory = player.getInventory().getContents();

        for (ItemStack stack : inventory) {
            if (stack != null && stack.isSimilar(this.itemStack)) {
                int amountToRemove = Math.min(stack.getAmount(), requiredAmount);
                stack.setAmount(stack.getAmount() - amountToRemove);
                requiredAmount -= amountToRemove;

                if (requiredAmount <= 0) {
                    return this.amount;
                }
            }
        }

        return 0; // This line should never be reached
    }

    @Override
    public double deposit(Player player) {
        if (player == null || amount <= 0) {
            return 0;
        }

        int requiredAmount = this.amount;
        while (requiredAmount > 0) {
            ItemStack clone = itemStack.clone();
            clone.setAmount(Math.min(clone.getMaxStackSize(), requiredAmount));
            player.getInventory().addItem(clone).forEach((integer, itemStack1) -> {
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack1);
            });
            requiredAmount -= clone.getAmount();
        }

        return amount;
    }

    @Override
    public String buyDescriptor() {
        return "x" + (this.amount) + " " + StringUtils.formatEnum(itemId);
    }

    @Override
    public String sellDescriptor() {
        return "x" + (this.amount) + " " + StringUtils.formatEnum(itemId);
    }
}
