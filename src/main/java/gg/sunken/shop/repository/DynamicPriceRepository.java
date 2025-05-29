package gg.sunken.shop.repository;

import gg.sunken.shop.entity.DynamicPriceItem;

public interface DynamicPriceRepository {

    void save(DynamicPriceItem item);

    DynamicPriceItem findById(String id);

    void deletePriceData(String id);

    void addHistory(String id, int amount);

    void removeHistory(String id, int amount);

}
