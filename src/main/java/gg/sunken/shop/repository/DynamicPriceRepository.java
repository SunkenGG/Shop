package gg.sunken.shop.repository;

import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.ItemTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DynamicPriceRepository {

    CompletableFuture<Void> save(DynamicPriceItem item);

    CompletableFuture<DynamicPriceItem> findById(String id);

    CompletableFuture<Void> deletePriceData(String id);

    CompletableFuture<Void> addHistory(String id, int amount);

    CompletableFuture<Void> removeHistory(String id, int amount);

    ItemTemplate templateFromCache(String id);

    DynamicPriceItem priceFromCache(String id);

    List<DynamicPriceItem> allPrices();

    List<ItemTemplate> allTemplates();

    void addTemplate(ItemTemplate template);

    void removeTemplate(String id);

    void addDynamicPriceItem(DynamicPriceItem item);

    void removeDynamicPriceItem(String id);
}
