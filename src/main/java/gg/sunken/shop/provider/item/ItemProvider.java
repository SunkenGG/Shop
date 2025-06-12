package gg.sunken.shop.provider.item;

import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public interface ItemProvider {

    @Nullable
    ItemStack fromId(String id);
}
