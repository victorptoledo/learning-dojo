package dojo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ShopOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customer;

    private long totalCents;

    protected ShopOrder() {}

    public ShopOrder(String customer, long totalCents) {
        this.customer = customer;
        this.totalCents = totalCents;
    }

    public Long getId() {
        return id;
    }

    public String getCustomer() {
        return customer;
    }

    public long getTotalCents() {
        return totalCents;
    }
}
