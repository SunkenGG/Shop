package gg.sunken.shop;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import gg.sunken.shop.commands.DebugCommand;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.ItemTemplate;
import gg.sunken.shop.redis.PriceSyncManager;
import gg.sunken.shop.redis.RedisPriceSyncManager;
import gg.sunken.shop.repository.DynamicPriceMongoRepository;
import gg.sunken.shop.repository.DynamicPriceRepository;
import gg.sunken.shop.services.ShopService;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.InvUI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Accessors(fluent = true)
public final class ShopPlugin extends JavaPlugin {

    private static ShopPlugin instance;
    public static ShopPlugin instance() {
        return instance;
    }

    private ShopService shopService;
    private Economy economy;

    public ShopPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerEconomy();

        InvUI.getInstance().setPlugin(this);

        PriceSyncManager syncManager = new RedisPriceSyncManager(getConfig().getString("redis.uri"));

        MongoClient client = MongoClients.create(getConfig().getString("mongo.uri"));
        MongoDatabase database = client.getDatabase(getConfig().getString("mongo.database"));
        DynamicPriceRepository repository = new DynamicPriceMongoRepository(database.getCollection("prices"), database.getCollection("transactions"));

        this.shopService = new ShopService(repository, syncManager);

        Bukkit.getCommandMap().register("shop", new DebugCommand(repository, syncManager));
    }

    @Override
    public void onDisable() {
    }

    private void registerEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found, economy features will not be available.");
            return;
        }

        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (economyProvider == null) {
            getLogger().warning("No economy provider found, economy features will not be available.");
            return;
        }

        this.economy = economyProvider.getProvider();
    }
}
