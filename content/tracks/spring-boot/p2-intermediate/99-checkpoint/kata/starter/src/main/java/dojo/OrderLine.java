package dojo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private ShopOrder order;

    private String sku;

    private int quantity;

    private long unitCents;

    protected OrderLine() {}

    OrderLine(ShopOrder order, String sku, int quantity, long unitCents) {
        this.order = order;
        this.sku = sku;
        this.quantity = quantity;
        this.unitCents = unitCents;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitCents() {
        return unitCents;
    }
}
