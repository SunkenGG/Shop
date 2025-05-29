package gg.sunken.shop.redis;

public interface PriceSyncManager {

    void updateStock(String id, int delta);
}