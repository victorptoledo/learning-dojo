package dojo.shop.warehouse;

import dojo.shop.catalog.Product;
import dojo.shop.catalog.ProductRepository;

public class WarehouseService {
    private final ProductRepository products;

    public WarehouseService(ProductRepository products) {
        this.products = products;
    }

    public void receive(String sku, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("bad quantity " + quantity);
        }
        Product product = product(sku);
        product.setStockOnHand(product.stockOnHand() + quantity);
    }

    public String pickLocation(String sku) {
        return product(sku).binLocation();
    }

    public int stock(String sku) {
        return product(sku).stockOnHand();
    }

    private Product product(String sku) {
        return products.find(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
    }
}
