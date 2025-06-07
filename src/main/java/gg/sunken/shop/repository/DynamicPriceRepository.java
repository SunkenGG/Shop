package gg.sunken.shop.repository;

import gg.sunken.shop.entity.DynamicPriceItem;
import gg.sunken.shop.entity.ItemTemplate;

import java.util.List;

public interface DynamicPriceRepository {

    void save(DynamicPriceItem item);

    DynamicPriceItem findById(String id);

    void deletePriceData(String id);

    void addHistory(String id, int amount);

    void removeHistory(String id, int amount);

    ItemTemplate templateById(String id);

    DynamicPriceItem priceById(String id);

    List<DynamicPriceItem> allPrices();

    List<ItemTemplate> allTemplates();

    void addTemplate(ItemTemplate template);

    void removeTemplate(String id);

    void addDynamicPriceItem(DynamicPriceItem item);

    void removeDynamicPriceItem(String id);
}
