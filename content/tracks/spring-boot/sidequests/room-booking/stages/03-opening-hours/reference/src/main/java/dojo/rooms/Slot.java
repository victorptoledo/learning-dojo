package dojo.rooms;

import java.time.LocalDateTime;

/** A free stretch of a room. */
public record Slot(LocalDateTime start, LocalDateTime end) {
}
