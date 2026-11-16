package dojo.shop.warehouse;

import dojo.shop.warehouse.api.Stock;

public class WarehouseService implements Stock {
    private final StockItems items;

    public WarehouseService(StockItems items) {
        this.items = items;
    }

    @Override
    public boolean reserve(String sku, int quantity) {
        StockItem item = items.find(sku).orElse(null);
        if (item == null || quantity < 1 || item.onHand() < quantity) {
            return false;
        }
        items.save(item.withOnHand(item.onHand() - quantity));
        return true;
    }

    public void receive(String sku, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("bad quantity " + quantity);
        }
        StockItem item = item(sku);
        items.save(item.withOnHand(item.onHand() + quantity));
    }

    public String pickLocation(String sku) {
        return item(sku).bin();
    }

    public int stock(String sku) {
        return item(sku).onHand();
    }

    private StockItem item(String sku) {
        return items.find(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
    }
}
