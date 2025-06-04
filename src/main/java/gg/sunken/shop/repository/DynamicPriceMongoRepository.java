package gg.sunken.shop.repository;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import org.bson.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class DynamicPriceMongoRepository implements DynamicPriceRepository {

    private final static ShopPlugin PLUGIN = ShopPlugin.instance();
    private final static ReplaceOptions UPSERT = new ReplaceOptions().upsert(true);
    private final MongoCollection<Document> itemCollection;
    private final MongoCollection<Document> dayCollection;

    public DynamicPriceMongoRepository(MongoCollection<Document> itemCollection, MongoCollection<Document> dayCollection) {
        this.itemCollection = itemCollection;
        this.dayCollection = dayCollection;

        dayCollection.createIndex(new Document("itemId", 1).append("hourEnd", 1));
    }

    public void save(DynamicPriceItem item) {
        Document doc = item.serialize();
        itemCollection.replaceOne(new Document("_id", item.id()), doc, UPSERT);
    }

    public DynamicPriceItem findById(String id) {
        Document doc = itemCollection.find(new Document("_id", id)).first();
        if (doc == null) return null;
        DynamicPriceItem item = new DynamicPriceItem(doc.getString("_id"), ShopPlugin.instance().itemTemplates().get(id));
        item.stock(doc.getInteger("stock", 0));
        return item;
    }

    public void deletePriceData(String id) {
        itemCollection.deleteOne(new Document("_id", id));
    }

    public void addHistory(String id, int amount) {
        long hour = Instant.now().truncatedTo(ChronoUnit.HOURS).getEpochSecond();

        Document existingDoc = dayCollection.find(
                Filters.and(
                        Filters.eq("itemId", id),
                        Filters.eq("hourEnd", hour)
                )
        ).first();

        if (existingDoc == null) {
            int currentStock = getCurrentStock(id) + amount;
            try {
                dayCollection.insertOne(
                        new Document("itemId", id)
                                .append("hourEnd", hour)
                                .append("amount", currentStock)
                );
            } catch (MongoWriteException ignored) {
                addHistory(id, amount);
            }
        } else {
            dayCollection.updateOne(
                    Filters.and(
                            Filters.eq("itemId", id),
                            Filters.eq("hourEnd", hour)
                    ),
                    new Document("$inc", new Document("amount", amount))
            );
        }
    }

    public void removeHistory(String id, int amount) {
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
    }

    private int getCurrentStock(String id) {
        return PLUGIN.items().get(id).stock();
    }
}
