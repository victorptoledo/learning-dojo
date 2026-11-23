package dojo.rooms;

/** A meeting to place in a room, over {@code [start, end)}. */
public record Request(String id, long start, long end) {
}
