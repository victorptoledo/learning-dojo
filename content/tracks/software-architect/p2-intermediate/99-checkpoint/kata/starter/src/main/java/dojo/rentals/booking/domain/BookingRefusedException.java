package dojo.rentals.booking.domain;

public class BookingRefusedException extends RuntimeException {
    public BookingRefusedException(String message) {
        super(message);
    }
}
