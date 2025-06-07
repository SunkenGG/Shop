package gg.sunken.shop.repository;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.ItemTemplate;
import org.bson.Document;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DynamicPriceMongoRepository implements DynamicPriceRepository {

    private final static ShopPlugin PLUGIN = ShopPlugin.instance();
    private final static ReplaceOptions UPSERT = new ReplaceOptions().upsert(true);
    private final static Executor DATABASE_THREAD_POOL = Executors.newWorkStealingPool();
    private final MongoCollection<Document> itemCollection;
    private final MongoCollection<Document> dayCollection;
    private final Map<String, ItemTemplate> itemTemplates;
    private final Map<String, DynamicPriceItem> items;

    public DynamicPriceMongoRepository(MongoCollection<Document> itemCollection, MongoCollection<Document> dayCollection) {
        this.itemCollection = itemCollection;
        this.dayCollection = dayCollection;

        dayCollection.createIndex(new Document("itemId", 1).append("hourEnd", 1));

        this.itemTemplates = new ConcurrentHashMap<>();
        final double price = ShopPlugin.instance().getConfig().getDouble("default.price");
        final double elasticity = ShopPlugin.instance().getConfig().getDouble("default.elasticity");
        final double support = ShopPlugin.instance().getConfig().getDouble("default.support");
        final double resistance = ShopPlugin.instance().getConfig().getDouble("default.resistance");

        final double minPrice = ShopPlugin.instance().getConfig().getDouble("default.min-price", 0);
        final double maxPrice = ShopPlugin.instance().getConfig().getDouble("default.max-price", 0);
        final double defaultBuyTax = ShopPlugin.instance().getConfig().getDouble("default.buy-tax", 0);
        final double defaultSellTax = ShopPlugin.instance().getConfig().getDouble("default.sell-tax", 0);

        for (String items : ShopPlugin.instance().getConfig().getConfigurationSection("items").getKeys(false)) {
            ConfigurationSection section = ShopPlugin.instance().getConfig().getConfigurationSection("items." + items);
            if (section == null) continue;

            String id = section.getString("id");
            if (id == null) continue;

            double initialPrice = section.getDouble("price", price);
            double elasticityValue = section.getDouble("elasticity", elasticity);
            double supportValue = section.getDouble("support", support);
            double resistanceValue = section.getDouble("resistance", resistance);

            double minimumPrice = section.getDouble("min-price",  minPrice);
            double maximumPrice = section.getDouble("max-price", maxPrice);
            double buyTax = section.getDouble("buy-tax", defaultBuyTax);
            double sellTax = section.getDouble("sell-tax", defaultSellTax);

            ItemTemplate build = ItemTemplate.builder()
                    .id(id)
                    .initialPrice(initialPrice)
                    .elasticity(elasticityValue)
                    .support(supportValue)
                    .resistance(resistanceValue)
                    .minPrice(minimumPrice)
                    .maxPrice(maximumPrice)
                    .buyTax(buyTax)
                    .sellTax(sellTax)
                    .build();

            itemTemplates.put(id, build);
        }

        this.items = new ConcurrentHashMap<>();
        for (String key : itemTemplates.keySet()) {
            findById(key).thenAccept(item -> {
                if (item != null) {
                    items.put(key, item);
                } else {
                    DynamicPriceItem newItem = new DynamicPriceItem(key, itemTemplates.get(key));
                    items.put(key, newItem);
                    save(newItem);
                }
            }).exceptionally(ex -> {
                PLUGIN.getLogger().severe("Failed to load item " + key + ": " + ex.getMessage());
                return null;
            });
        }
    }

    @Override
    public CompletableFuture<Void> save(DynamicPriceItem item) {
        return CompletableFuture.runAsync(() -> {
            Document doc = item.serialize();
            itemCollection.replaceOne(new Document("_id", item.id()), doc, UPSERT);
        }, DATABASE_THREAD_POOL);
    }

    @Override
    public CompletableFuture<DynamicPriceItem> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = itemCollection.find(new Document("_id", id)).first();
            if (doc == null) return null;
            DynamicPriceItem item = new DynamicPriceItem(doc.getString("_id"), itemTemplates.get(id));
            item.stock(doc.getInteger("stock", 0));
            return item;
        }, DATABASE_THREAD_POOL);
    }

    @Override
    public CompletableFuture<Void> deletePriceData(String id) {
        return CompletableFuture.runAsync(() -> {
            itemCollection.deleteOne(new Document("_id", id));
        }, DATABASE_THREAD_POOL);
    }

    @Override
    public CompletableFuture<Void> addHistory(String id, int amount) {
        return CompletableFuture.runAsync(() -> {
            long hour = Instant.now().truncatedTo(ChronoUnit.HOURS).getEpochSecond();

            Document existingDoc = dayCollection.find(
                    Filters.and(
                            Filters.eq("itemId", id),
                            Filters.eq("hourEnd", hour)
                    )
            ).first();

            if (existingDoc == null) {
                dayCollection.insertOne(
                        new Document("itemId", id)
                                .append("hourEnd", hour)
                                .append("amount", amount)
                );
            } else {
                dayCollection.updateOne(
                        Filters.and(
                                Filters.eq("itemId", id),
                                Filters.eq("hourEnd", hour)
                        ),
                        new Document("$inc", new Document("amount", amount))
                );
            }
        }, DATABASE_THREAD_POOL);
    }

    @Override
    public CompletableFuture<Void> removeHistory(String id, int amount) {
        return CompletableFuture.runAsync(() -> {
            long hour = Instant.now().truncatedTo(ChronoUnit.HOURS).getEpochSecond();

            Document existingDoc = dayCollection.find(
                    Filters.and(
                            Filters.eq("itemId", id),
                            Filters.eq("hourEnd", hour)
                    )
            ).first();

            if (existingDoc == null) {
                int currentStock = getCurrentStock(id) - amount;
                dayCollection.insertOne(
                        new Document("itemId", id)
                                .append("hourEnd", hour)
                                .append("amount", currentStock)
                );
            } else {
                dayCollection.updateOne(
                        Filters.and(
                                Filters.eq("itemId", id),
                                Filters.eq("hourEnd", hour)
                        ),
                        new Document("$inc", new Document("amount", -amount))
                );
            }
        }, DATABASE_THREAD_POOL);
    }

    @Override
    public DynamicPriceItem priceFromCache(String id) {
        return items.get(id);
    }

    @Override
    public ItemTemplate templateFromCache(String id) {
        return itemTemplates.get(id);
    }

    @Override
    public List<DynamicPriceItem> allPrices() {
        return List.copyOf(items.values());
    }

    @Override
    public List<ItemTemplate> allTemplates() {
        return List.copyOf(itemTemplates.values());
    }

    @Override
    public void addTemplate(ItemTemplate template) {
        if (itemTemplates.containsKey(template.id())) {
            throw new IllegalArgumentException("Template with id " + template.id() + " already exists.");
        }
        itemTemplates.put(template.id(), template);
    }

    @Override
    public void addDynamicPriceItem(DynamicPriceItem item) {
        if (items.containsKey(item.id())) {
            throw new IllegalArgumentException("Item with id " + item.id() + " already exists.");
        }
        items.put(item.id(), item);
        save(item);
    }

    @Override
    public void removeTemplate(String id) {
        if (!itemTemplates.containsKey(id)) {
            throw new IllegalArgumentException("Template with id " + id + " does not exist.");
        }
        itemTemplates.remove(id);
    }

    @Override
    public void removeDynamicPriceItem(String id) {
        if (!items.containsKey(id)) {
            throw new IllegalArgumentException("Item with id " + id + " does not exist.");
        }
        items.remove(id);
        deletePriceData(id);
    }

    private int getCurrentStock(String id) {
        DynamicPriceItem item = items.get(id);
        if (item == null) {
            return 0;
        }
        return item.stock();
    }
}
