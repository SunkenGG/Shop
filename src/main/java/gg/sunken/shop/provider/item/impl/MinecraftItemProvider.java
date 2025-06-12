package gg.sunken.shop.provider.item.impl;

import gg.sunken.shop.provider.item.ItemProvider;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MinecraftItemProvider implements ItemProvider {


    @Override
    public @Nullable ItemStack fromId(String id) {
        try {
            Material material = Material.valueOf(id);
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
