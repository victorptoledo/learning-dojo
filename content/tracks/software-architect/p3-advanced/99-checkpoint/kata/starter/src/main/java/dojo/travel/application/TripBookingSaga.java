package dojo.travel.application;

import dojo.travel.adapters.InMemoryTrips;
import dojo.travel.domain.Leg;
import dojo.travel.domain.Trip;
import dojo.travel.domain.TripStatus;
import dojo.travel.messaging.CommandBus;
import dojo.travel.messaging.SupplierCommand;
import dojo.travel.messaging.SupplierReply;
import java.util.List;

/** Books a flight, a hotel and a car, one after the other. */
public class TripBookingSaga {
    private final InMemoryTrips trips;
    private final CommandBus bus;

    public TripBookingSaga(InMemoryTrips trips, CommandBus bus) {
        this.trips = trips;
        this.bus = bus;
    }

    public void start(String tripId, String traveller, List<Leg> legs) {
        Trip trip = new Trip();
        trip.setId(tripId);
        trip.setTraveller(traveller);
        trip.setLegs(legs);
        trips.save(trip);
        bus.send(SupplierCommand.reserve(tripId, legs.get(0)));
    }

    public void handle(SupplierReply reply) {
        Trip trip = trips.find(reply.tripId());
        trip.apply(reply);
        // When a supplier refuses, the trip is cancelled. What about the legs already reserved?
        if (trip.getStatus() == TripStatus.PENDING) {
            int next = trip.getLegs().indexOf(reply.leg()) + 1;
            bus.send(SupplierCommand.reserve(trip.getId(), trip.getLegs().get(next)));
        }
    }
}
