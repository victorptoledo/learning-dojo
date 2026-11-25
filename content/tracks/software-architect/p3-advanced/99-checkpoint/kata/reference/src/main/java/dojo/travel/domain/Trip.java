package dojo.travel.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** The aggregate root: a trip owns its reservations and decides what must be released. */
public final class Trip {
    private final TripId id;
    private final String traveller;
    private final List<Leg> legs;
    private final Map<Leg, String> confirmations = new EnumMap<>(Leg.class);
    private final List<TripEvent> events = new ArrayList<>();
    private TripStatus status = TripStatus.PENDING;

    private Trip(TripId id, String traveller, List<Leg> legs) {
        this.id = id;
        this.traveller = traveller;
        this.legs = List.copyOf(legs);
    }

    public static Trip request(TripId id, String traveller, List<Leg> legs) {
        if (id == null) {
            throw new IllegalArgumentException("trip id is required");
        }
        if (traveller == null || traveller.isBlank()) {
            throw new IllegalArgumentException("traveller is required");
        }
        if (legs == null || legs.isEmpty()) {
            throw new IllegalArgumentException("a trip needs at least one leg");
        }
        if (legs.stream().anyMatch(Objects::isNull) || new HashSet<>(legs).size() != legs.size()) {
            throw new IllegalArgumentException("each leg at most once");
        }
        return new Trip(id, traveller, legs);
    }

    public TripId id() {
        return id;
    }

    public String traveller() {
        return traveller;
    }

    public TripStatus status() {
        return status;
    }

    public List<Leg> legs() {
        return legs;
    }

    public Optional<String> confirmation(Leg leg) {
        return Optional.ofNullable(confirmations.get(leg));
    }

    /** The leg to reserve next, while the trip is pending. */
    public Optional<Leg> nextLeg() {
        if (status != TripStatus.PENDING) {
            return Optional.empty();
        }
        return legs.stream().filter(leg -> !confirmations.containsKey(leg)).findFirst();
    }

    public void reserved(Leg leg, String confirmation) {
        requirePending();
        Leg next = nextLeg().orElseThrow();
        if (leg != next) {
            throw new IllegalStateException("expected " + next + ", got " + leg);
        }
        if (confirmation == null || confirmation.isBlank()) {
            throw new IllegalArgumentException("confirmation is required");
        }
        confirmations.put(leg, confirmation);
        if (confirmations.size() == legs.size()) {
            status = TripStatus.CONFIRMED;
            events.add(new TripConfirmed(id));
        }
    }

    /** Cancels the trip and returns the legs to release, the latest reservation first. */
    public List<Leg> cancel(String reason) {
        requirePending();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        status = TripStatus.CANCELLED;
        events.add(new TripCancelled(id, reason));
        return legs.stream().filter(confirmations::containsKey).toList().reversed();
    }

    public List<TripEvent> pullEvents() {
        List<TripEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    private void requirePending() {
        if (status != TripStatus.PENDING) {
            throw new IllegalStateException("trip is " + status);
        }
    }
}
