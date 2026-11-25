package dojo.travel.domain;

public record TripId(String value) {
    public TripId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("trip id is required");
        }
    }
}
