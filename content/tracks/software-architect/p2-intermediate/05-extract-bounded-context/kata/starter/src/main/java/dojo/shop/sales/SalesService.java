package dojo.shop.sales;

import dojo.shop.catalog.Product;
import dojo.shop.catalog.ProductRepository;

public class SalesService {
    private final ProductRepository products;

    public SalesService(ProductRepository products) {
        this.products = products;
    }

    public Quote quote(String sku, int quantity) {
        return new Quote(sku, quantity, product(sku).priceCents() * quantity);
    }

    public void reprice(String sku, long priceCents) {
        product(sku).setPriceCents(priceCents);
    }

    public Order placeOrder(String orderId, String sku, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("bad quantity " + quantity);
        }
        Product product = product(sku);
        if (product.discontinued()) {
            throw new IllegalStateException("not for sale: " + sku);
        }
        if (product.stockOnHand() < quantity) {
            throw new IllegalStateException("out of stock: " + sku);
        }
        product.setStockOnHand(product.stockOnHand() - quantity);
        return new Order(orderId, sku, quantity, product.priceCents() * quantity);
    }

    private Product product(String sku) {
        return products.find(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
    }
}
