package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An order. The column {@code customer_name} is being renamed to {@code customer} by expand and
 * contract: both exist now (expand); every writer fills both; a backfill copies the old rows; then
 * reads switch to {@code customer}, and only later does a migration drop {@code customer_name}
 * (contract).
 */
@Entity
@Table(name = "orders")
public class ShopOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The old column. Still read by the version of the service that is running in production. */
    @Column(name = "customer_name")
    private String customerName;

    /** The new column. Null in the rows written before the migration started. */
    @Column(name = "customer")
    private String customer;

    @Column(nullable = false)
    private String sku;

    private int quantity;

    private long totalCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String reason;

    protected ShopOrder() {}

    public ShopOrder(String customerName, String customer, String sku, int quantity, long totalCents, OrderStatus status) {
        this.customerName = customerName;
        this.customer = customer;
        this.sku = sku;
        this.quantity = quantity;
        this.totalCents = totalCents;
        this.status = status;
    }

    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomer() {
        return customer;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
