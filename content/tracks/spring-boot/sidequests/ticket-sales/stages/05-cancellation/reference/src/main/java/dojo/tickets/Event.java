package dojo.tickets;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;

/** Something people buy seats for. Its row is the first lock of every change to its sale. */
@Entity
public class Event {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private Instant startsAt;

    private long priceCents;

    private int seatCount;

    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.ON_SALE;

    protected Event() {
    }

    public Event(String name, Instant startsAt, long priceCents, int seatCount) {
        this.name = name;
        this.startsAt = startsAt;
        this.priceCents = priceCents;
        this.seatCount = seatCount;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public EventStatus getStatus() {
        return status;
    }

    public boolean isCancelled() {
        return status == EventStatus.CANCELLED;
    }

    public void cancel() {
        status = EventStatus.CANCELLED;
    }
}
