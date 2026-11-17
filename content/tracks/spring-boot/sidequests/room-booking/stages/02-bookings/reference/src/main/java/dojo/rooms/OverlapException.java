package dojo.rooms;

/** The time is taken by another booking: 409 with its id. */
public class OverlapException extends RuntimeException {
    private final long conflictsWith;

    public OverlapException(Booking other) {
        super("room " + other.roomId() + " is booked from " + other.start() + " to " + other.end());
        this.conflictsWith = other.id();
    }

    public long conflictsWith() {
        return conflictsWith;
    }
}
