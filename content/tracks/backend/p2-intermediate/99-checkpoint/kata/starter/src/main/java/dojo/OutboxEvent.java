package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

/** An event waiting to be published. Written in the same transaction as the change it describes. */
@Entity
public class OutboxEvent {
    public static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 2000)
    private String payload;

    private Instant publishedAt;

    private int attempts;

    private boolean dead;

    protected OutboxEvent() {}

    public OutboxEvent(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public void published(Instant at) {
        this.publishedAt = at;
    }

    /** One more failed attempt; after MAX_ATTEMPTS the event is dead (parked for a human). */
    public void failed() {
        attempts++;
        if (attempts >= MAX_ATTEMPTS) {
            dead = true;
        }
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isDead() {
        return dead;
    }
}
