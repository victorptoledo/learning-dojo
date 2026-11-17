package dojo.rooms;

import java.time.LocalDateTime;

/** A room taken from start (included) to end (excluded). */
public record Booking(long id, long roomId, String title, LocalDateTime start, LocalDateTime end, int attendees) {

    boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }
}
