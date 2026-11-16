package dojo.shop.sales;

import dojo.shop.warehouse.api.Stock;

public class SalesService {
    private final Products products;
    private final Stock stock;

    public SalesService(Products products, Stock stock) {
        this.products = products;
        this.stock = stock;
    }

    public Quote quote(String sku, int quantity) {
        return new Quote(sku, quantity, product(sku).priceCents() * quantity);
    }

    public void reprice(String sku, long priceCents) {
        products.save(product(sku).withPrice(priceCents));
    }

    public Order placeOrder(String orderId, String sku, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("bad quantity " + quantity);
        }
        Product product = product(sku);
        if (product.discontinued()) {
            throw new IllegalStateException("not for sale: " + sku);
        }
        if (!stock.reserve(sku, quantity)) {
            throw new IllegalStateException("out of stock: " + sku);
        }
        return new Order(orderId, sku, quantity, product.priceCents() * quantity);
    }

    private Product product(String sku) {
        return products.find(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
    }
}
