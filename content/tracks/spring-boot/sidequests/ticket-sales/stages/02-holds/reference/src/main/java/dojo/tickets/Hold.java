package dojo.tickets;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Seats kept for one buyer for a while. */
@Entity
public class Hold {
    @Id
    @GeneratedValue
    private Long id;

    private long eventId;

    private String buyer;

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    private List<String> seats = new ArrayList<>();

    private long totalCents;

    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    private HoldStatus status = HoldStatus.HELD;

    protected Hold() {
    }

    public Hold(long eventId, String buyer, List<String> seats, long totalCents, Instant expiresAt) {
        this.eventId = eventId;
        this.buyer = buyer;
        this.seats.addAll(seats);
        this.totalCents = totalCents;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public void setStatus(HoldStatus status) {
        this.status = status;
    }
}
