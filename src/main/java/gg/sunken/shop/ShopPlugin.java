package gg.sunken.shop;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import gg.sunken.shop.commands.DebugCommand;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.ItemTemplate;
import gg.sunken.shop.redis.RedisPriceSyncManager;
import gg.sunken.shop.repository.DynamicPriceMongoRepository;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Accessors(fluent = true)
public final class ShopPlugin extends JavaPlugin {

    private static ShopPlugin instance;
    public static ShopPlugin instance() {
        return instance;
    }

    private RedisPriceSyncManager syncManager;
    private DynamicPriceMongoRepository repository;
    private Map<String, ItemTemplate> itemTemplates;
    private Map<String, DynamicPriceItem> items;

    public ShopPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.syncManager = new RedisPriceSyncManager(getConfig().getString("redis.uri"));

        MongoClient client = MongoClients.create(getConfig().getString("mongo.uri"));
        MongoDatabase database = client.getDatabase(getConfig().getString("mongo.database"));
        this.repository = new DynamicPriceMongoRepository(database.getCollection("prices"), database.getCollection("transaction s"));

        this.itemTemplates = new ConcurrentHashMap<>();
        final double price = getConfig().getDouble("default.price");
        final double elasticity = getConfig().getDouble("default.elasticity");
        final double support = getConfig().getDouble("default.support");
        final double resistance = getConfig().getDouble("default.resistance");

        for (String items : getConfig().getConfigurationSection("items").getKeys(false)) {
            ConfigurationSection section = getConfig().getConfigurationSection("items." + items);
            if (section == null) continue;

            String id = section.getString("id");
            if (id == null) continue;

            double initialPrice = section.getDouble("price", price);
            double elasticityValue = section.getDouble("elasticity", elasticity);
            double supportValue = section.getDouble("support", support);
            double resistanceValue = section.getDouble("resistance", resistance);

            double minStock = section.getDouble("min-stock", 0);
            double maxStock = section.getDouble("max-stock", 0);
            double buyTax = section.getDouble("buy-tax", 0);
            double sellTax = section.getDouble("sell-tax", 0);

            ItemTemplate build = ItemTemplate.builder()
                    .id(id)
                    .initialPrice(initialPrice)
                    .elasticity(elasticityValue)
                    .support(supportValue)
                    .resistance(resistanceValue)
                    .minStock(minStock)
                    .maxStock(maxStock)
                    .buyTax(buyTax)
                    .sellTax(sellTax)
                    .build();

            itemTemplates.put(id, build);
        }

        this.items = new ConcurrentHashMap<>();
        for (String key : itemTemplates.keySet()) {
            DynamicPriceItem byId = repository.findById(key);
            if (byId == null) byId = new DynamicPriceItem(key, itemTemplates.get(key));

            items.put(key, byId);
        }

        Bukkit.getCommandMap().register("shop", new DebugCommand());
    }

    @Override
    public void onDisable() {
    }
}
