package dojo.travel.application;

import dojo.travel.domain.Leg;
import dojo.travel.domain.Trip;
import dojo.travel.domain.TripEvent;
import dojo.travel.domain.TripId;
import dojo.travel.domain.TripStatus;
import dojo.travel.domain.Trips;
import dojo.travel.messaging.CommandBus;
import dojo.travel.messaging.DeadLetterChannel;
import dojo.travel.messaging.SupplierCommand;
import dojo.travel.messaging.SupplierReply;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The orchestrator: reserves the legs one by one, and when a supplier refuses or the traveller
 * cancels, releases what was reserved, latest first. Replies arrive at least once.
 */
public final class TripBookingSaga {
    private final Trips trips;
    private final CommandBus bus;
    private final Consumer<TripEvent> events;
    private final DeadLetterChannel deadLetters;
    private final Set<String> processed = new HashSet<>();

    public TripBookingSaga(Trips trips, CommandBus bus, Consumer<TripEvent> events, DeadLetterChannel deadLetters) {
        this.trips = trips;
        this.bus = bus;
        this.events = events;
        this.deadLetters = deadLetters;
    }

    public TripId start(String tripId, String traveller, List<Leg> legs) {
        TripId id = new TripId(tripId);
        if (trips.find(id).isPresent()) {
            throw new IllegalArgumentException("trip exists: " + tripId);
        }
        Trip trip = Trip.request(id, traveller, legs);
        trips.save(trip);
        reserveNext(trip);
        return id;
    }

    public void cancel(String tripId, String reason) {
        Trip trip = trips.find(new TripId(tripId))
                .orElseThrow(() -> new IllegalArgumentException("unknown trip " + tripId));
        cancel(trip, reason);
    }

    public void handle(SupplierReply reply) {
        if (!processed.add(reply.messageId())) {
            return;
        }
        Optional<Trip> found = trips.find(new TripId(reply.tripId()));
        if (found.isEmpty()) {
            deadLetters.deadLetter(reply, "unknown trip");
            return;
        }
        Trip trip = found.get();
        switch (trip.status()) {
            case PENDING -> onPending(trip, reply);
            case CANCELLED -> onCancelled(trip, reply);
            case CONFIRMED -> deadLetters.deadLetter(reply, "trip is confirmed");
        }
    }

    private void onPending(Trip trip, SupplierReply reply) {
        if (trip.nextLeg().orElseThrow() != reply.leg()) {
            deadLetters.deadLetter(reply, "unexpected reply for " + reply.leg());
        } else if (reply.reserved()) {
            trip.reserved(reply.leg(), reply.detail());
            trips.save(trip);
            reserveNext(trip);
            publish(trip);
        } else {
            cancel(trip, reply.leg() + " refused: " + reply.detail());
        }
    }

    /** A reservation that arrives after the cancellation would be orphaned: release it. */
    private void onCancelled(Trip trip, SupplierReply reply) {
        if (reply.reserved() && trip.confirmation(reply.leg()).isEmpty()) {
            bus.send(SupplierCommand.release(trip.id().value(), reply.leg()));
        }
    }

    private void cancel(Trip trip, String reason) {
        List<Leg> toRelease = trip.cancel(reason);
        trips.save(trip);
        toRelease.forEach(leg -> bus.send(SupplierCommand.release(trip.id().value(), leg)));
        publish(trip);
    }

    private void reserveNext(Trip trip) {
        if (trip.status() == TripStatus.PENDING) {
            trip.nextLeg().ifPresent(leg -> bus.send(SupplierCommand.reserve(trip.id().value(), leg)));
        }
    }

    private void publish(Trip trip) {
        trip.pullEvents().forEach(events);
    }
}
