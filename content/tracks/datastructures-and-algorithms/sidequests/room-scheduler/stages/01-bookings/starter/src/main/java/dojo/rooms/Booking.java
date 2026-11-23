package dojo.rooms;

/** A booking of {@code room} over the half-open interval {@code [start, end)}. */
public record Booking(String id, String room, long start, long end) {
}
