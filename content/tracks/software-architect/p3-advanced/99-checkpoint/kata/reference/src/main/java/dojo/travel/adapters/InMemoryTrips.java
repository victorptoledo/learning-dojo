package dojo.travel.adapters;

import dojo.travel.domain.Trip;
import dojo.travel.domain.TripId;
import dojo.travel.domain.Trips;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryTrips implements Trips {
    private final Map<TripId, Trip> trips = new HashMap<>();

    @Override
    public void save(Trip trip) {
        trips.put(trip.id(), trip);
    }

    @Override
    public Optional<Trip> find(TripId id) {
        return Optional.ofNullable(trips.get(id));
    }
}
