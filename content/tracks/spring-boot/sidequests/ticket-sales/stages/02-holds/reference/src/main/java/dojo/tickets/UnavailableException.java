package dojo.tickets;

import java.util.List;

/** Some of the seats asked for are taken: 409 with their labels. */
public class UnavailableException extends RuntimeException {
    private final List<String> seats;

    public UnavailableException(List<String> seats) {
        super("some seats are not available");
        this.seats = List.copyOf(seats);
    }

    public List<String> seats() {
        return seats;
    }
}
