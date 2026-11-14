package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    private long priceCents;

    private int stock;

    protected Product() {}

    public Product(String sku, long priceCents, int stock) {
        this.sku = sku;
        this.priceCents = priceCents;
        this.stock = stock;
    }

    /** Takes {@code quantity} units out of stock, or refuses without changing anything. */
    public void reserve(int quantity) {
        if (quantity > stock) {
            throw ShopException.outOfStock(sku, quantity, stock);
        }
        stock -= quantity;
    }

    public void release(int quantity) {
        stock += quantity;
    }

    public String getSku() {
        return sku;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public int getStock() {
        return stock;
    }
}
