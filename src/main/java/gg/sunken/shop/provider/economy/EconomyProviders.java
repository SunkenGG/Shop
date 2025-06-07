package gg.sunken.shop.provider.economy;

import gg.sunken.shop.provider.economy.impl.ExperienceEconomyProvider;
import gg.sunken.shop.provider.economy.impl.VaultEconomyProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyProviders {

    private static final Map<String, EconomyProvider> ECONOMY_PROVIDERS = new ConcurrentHashMap<>();

    static {
        ECONOMY_PROVIDERS.put("vault", new VaultEconomyProvider());
        ECONOMY_PROVIDERS.put("exp", new ExperienceEconomyProvider());
    }

    public static EconomyProvider provider(String name) {
        return ECONOMY_PROVIDERS.get(name);
    }

    public static List<String> providerNames() {
        return List.copyOf(ECONOMY_PROVIDERS.keySet());
    }
}
