package dojo.travel.domain;

public record TripCancelled(TripId tripId, String reason) implements TripEvent {}
