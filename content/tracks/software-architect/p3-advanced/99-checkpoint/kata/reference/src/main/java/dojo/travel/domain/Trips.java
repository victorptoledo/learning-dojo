package dojo.travel.domain;

import java.util.Optional;

/** The repository port: one per aggregate. */
public interface Trips {
    void save(Trip trip);

    Optional<Trip> find(TripId id);
}
