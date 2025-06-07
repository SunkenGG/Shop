package gg.sunken.shop.controller;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import lombok.extern.java.Log;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Log
public class RedisPriceSyncController implements PriceSyncController {

    private final ShopPlugin plugin = ShopPlugin.instance();
    private final Executor REDIS_POOL = Executors.newFixedThreadPool(1);
    private final Jedis subscriber;
    private final Jedis publisher;

    public RedisPriceSyncController(String uri) {
        this.subscriber = new Jedis(uri);
        this.publisher = new Jedis(uri);
        subscribe();
    }

    public void updateStock(String id, int delta) {
        DynamicPriceItem item = plugin.shopService().item(id);
        if (item != null) {
            REDIS_POOL.execute(() -> {
                publisher.publish("price-updates", id + ":" + item.stock());
            });
        } else {
            log.warning("Tried updating stock for non-existent item: " + id);
        }
    }

    private void subscribe() {
        Thread thread = new Thread(() -> {
            subscriber.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    if (!channel.equals("price-updates")) return;
                    String[] parts = message.split(":");
                    if (parts.length != 2) return;

                    String id = parts[0];
                    int stock;
                    try {
                        stock = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        log.warning("Received malformed message on price-updates channel: " + message);
                        return;
                    }

                    DynamicPriceItem item = plugin.shopService().item(id);
                    if (item != null) {
                        item.stock(stock);
                    }
                }
            }, "price-updates");
        });
        thread.setName("PriceSyncManager-Subscribe");
        thread.setDaemon(true);
        thread.start();
    }
}
