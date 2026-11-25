package dojo.travel.domain;

/** What happened to a trip, for whoever listens. */
public sealed interface TripEvent permits TripConfirmed, TripCancelled {
    TripId tripId();
}
