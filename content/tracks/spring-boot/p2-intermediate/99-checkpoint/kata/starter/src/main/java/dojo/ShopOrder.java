package dojo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;

/** An order ("order" itself is a reserved word in SQL). */
@Entity
public class ShopOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PLACED;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    public void addLine(String sku, int quantity, long unitCents) {
        lines.add(new OrderLine(this, sku, quantity, unitCents));
    }

    /** Marks the order cancelled, or refuses when it already is. */
    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw ShopException.alreadyCancelled(id);
        }
        status = OrderStatus.CANCELLED;
    }

    public long totalCents() {
        return lines.stream().mapToLong(line -> line.getQuantity() * line.getUnitCents()).sum();
    }

    public Long getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}
