package gg.sunken.shop.entity.trades.impl;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.trades.NpcTrade;
import gg.sunken.shop.provider.economy.EconomyProvider;
import gg.sunken.shop.provider.economy.impl.VaultEconomyProvider;
import gg.sunken.shop.provider.item.ItemProviders;
import lombok.Data;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

import java.util.List;

@Data
@Accessors(fluent = true)
public class EconomyTrade implements NpcTrade {

    private final ShopPlugin plugin;
    private final EconomyProvider economyProvider;
    private final String itemId;
    private final int amount;

    @Override
    public ItemStack givingIcon() {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Currency Trade"));
        meta.lore(List.of(Component.text("Trade for currency: " + economyProvider.symbol() + amount)));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public ItemStack receivingIcon() {
        ItemStack clone = ItemProviders.fromId(itemId).orElse(new ItemStack(Material.BARRIER));
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Trade Item"));
            meta.lore(List.of(Component.text("Receive: " + amount + "x " + clone.getType().name())));
            clone.setItemMeta(meta);
        }
        return clone;
    }

    @Override
    public double cost() {
        return plugin.shopService().price(itemId, amount);
    }

    @Override
    public int amount() {
        return amount;
    }

    @Override
    public void trade(Player player) {

        ItemStack item = ItemProviders.fromId(itemId).orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId)).clone();
        if (item.getType() == Material.AIR) throw new IllegalArgumentException("Cannot trade AIR item: " + itemId);

        economyProvider.withdraw(player, cost());
        item.setAmount(amount);

        player.getInventory().addItem(item).forEach((integer, itemStack) -> {
            if (!itemStack.isEmpty()) {
                // Should not happen, but just in case
                player.getWorld().dropItem(player.getLocation(), itemStack);
            }
        });
    }

    @Override
    public boolean canTrade(Player player) {
        return economyProvider.has(player, cost());
    }
}
