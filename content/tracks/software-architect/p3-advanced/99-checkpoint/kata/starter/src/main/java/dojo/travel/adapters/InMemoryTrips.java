package dojo.travel.adapters;

import dojo.travel.domain.Trip;
import java.util.HashMap;
import java.util.Map;

public class InMemoryTrips {
    private final Map<String, Trip> trips = new HashMap<>();

    public void save(Trip trip) {
        trips.put(trip.getId(), trip);
    }

    public Trip find(String id) {
        return trips.get(id);
    }
}
