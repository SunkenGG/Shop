package gg.sunken.shop.entity.trades.impl.trades;

import gg.sunken.shop.entity.trades.NpcCurrency;
import gg.sunken.shop.provider.item.ItemProviders;
import gg.sunken.shop.utils.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemStackCurrency implements NpcCurrency {

    private final String id;
    private final ItemStack itemStack;
    private final double amount;

    public ItemStackCurrency(String id, double amount) {
        this.id = id;
        this.amount = amount;
        this.itemStack = ItemProviders.fromId(id)
                .orElseThrow(() -> new IllegalArgumentException("Item with id '" + id + "' not found"));
    }


    @Override
    public double amount() {
        return amount;
    }

    @Override
    public boolean has(Player player) {
        return player.getInventory().contains(itemStack, (int) amount);
    }

    @Override
    public void take(Player player) {
        int taken = 0;
        do {
            int slot = player.getInventory().first(itemStack);
            if (slot == -1) {
                break; // No more items found
            }

            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || !stack.isSimilar(itemStack)) {
                continue;
            }

            int toRemove = Math.min(stack.getAmount(), (int) amount - taken);
            stack.setAmount(stack.getAmount() - toRemove);
            taken += toRemove;
        } while (taken < amount);

        if (taken < amount) {
            throw new IllegalArgumentException("Not enough items to trade. Required: " + amount + ", but only " + taken + " were found.");
        }
    }

    @Override
    public void give(Player player) {
        ItemStack stackToGive = itemStack.clone();
        stackToGive.setAmount((int) amount);

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), stackToGive);
        } else {
            player.getInventory().addItem(stackToGive).forEach((integer, itemStack1) -> {
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack1);
            });
        }
    }

    @Override
    public String description() {
        return "x" + (int) amount + " " + StringUtils.formatEnum(id);
    }
}
