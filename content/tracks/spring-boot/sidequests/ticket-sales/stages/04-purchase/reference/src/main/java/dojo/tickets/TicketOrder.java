package dojo.tickets;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

/** A paid hold. The buyer and the key are unique together: one key, one payment. */
@Entity
@Table(name = "ticket_order", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"buyer", "idempotency_key"}),
        @UniqueConstraint(columnNames = "hold_id")})
public class TicketOrder {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "hold_id")
    private long holdId;

    private long eventId;

    private String buyer;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    private List<String> seats = new ArrayList<>();

    private long totalCents;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PAID;

    protected TicketOrder() {
    }

    public TicketOrder(Hold hold, String idempotencyKey) {
        this.holdId = hold.getId();
        this.eventId = hold.getEventId();
        this.buyer = hold.getBuyer();
        this.idempotencyKey = idempotencyKey;
        this.seats.addAll(hold.getSeats());
        this.totalCents = hold.getTotalCents();
    }

    public Long getId() {
        return id;
    }

    public long getHoldId() {
        return holdId;
    }

    public long getEventId() {
        return eventId;
    }

    public String getBuyer() {
        return buyer;
    }

    public List<String> getSeats() {
        return seats;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
