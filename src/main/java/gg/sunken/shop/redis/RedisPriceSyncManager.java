package gg.sunken.shop.redis;

import gg.sunken.shop.ShopPlugin;
import gg.sunken.shop.entity.DynamicPriceItem;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

@Log
public class RedisPriceSyncManager implements PriceSyncManager {

    private final ShopPlugin plugin = ShopPlugin.instance();
    private final Jedis subscriber;
    private final Jedis publisher;

    public RedisPriceSyncManager(String uri) {
        this.subscriber = new Jedis(uri);
        this.publisher = new Jedis(uri);
        subscribe();
    }

    public void updateStock(String id, int delta) {
        DynamicPriceItem item = plugin.items().get(id);
        if (item != null) {
            publisher.publish("price-updates", id + ":" + item.stock());
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
                    int stock = Integer.parseInt(parts[1]);

                    DynamicPriceItem item = plugin.items().get(id);
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
