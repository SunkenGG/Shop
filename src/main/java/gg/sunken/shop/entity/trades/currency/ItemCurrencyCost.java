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

    @Override
    public double cost() {
        return amount;
    }

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

        for (ItemStack item : inventory) {
            if (item != null && item.isSimilar(this.itemStack)) {
                int itemAmount = item.getAmount();
                if (itemAmount >= requiredAmount) {
                    item.setAmount(itemAmount - requiredAmount);
                    player.getInventory().setItem(player.getInventory().firstEmpty(), item);
                    return amount;
                } else {
                    requiredAmount -= itemAmount;
                    player.getInventory().removeItem(item);
                }
            }
        }
        return 0; // Not enough items to withdraw
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
    public String descriptor() {
        return "x" + (this.amount) + " " + StringUtils.formatEnum(itemId);
    }
}
