package gg.sunken.shop.provider.item;

import gg.sunken.shop.provider.item.impl.MinecraftItemProvider;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemProviders {

    private final static List<ItemProvider> PROVIDERS = new ArrayList<>();

    static {
        PROVIDERS.add(new MinecraftItemProvider());
    }

    /**
     * Retrieves an {@link ItemStack} from the provided identifier by iterating through all registered {@link ItemProvider}s.
     *
     * @param id The unique identifier of the item to be retrieved. Must not be null.
     * @return An {@link Optional} containing the {@link ItemStack} if a matching item is found, or an empty {@link Optional} if no match is found.
     */
    public static Optional<ItemStack> fromId(String id) {
        for (ItemProvider provider : PROVIDERS) {
            ItemStack itemStack = provider.fromId(id);
            if (itemStack != null) {
                return Optional.of(itemStack);
            }
        }

        return Optional.empty();
    }
}
