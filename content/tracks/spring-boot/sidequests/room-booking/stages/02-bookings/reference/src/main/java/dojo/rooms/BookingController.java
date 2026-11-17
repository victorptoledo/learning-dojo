package dojo.rooms;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class BookingController {
    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @PostMapping("/rooms/{roomId}/bookings")
    public ResponseEntity<Booking> book(@PathVariable long roomId, @Valid @RequestBody BookingRequest request,
            UriComponentsBuilder uri) {
        Booking booking = bookings.book(roomId, request);
        return ResponseEntity.created(uri.path("/bookings/{id}").build(booking.id())).body(booking);
    }

    @GetMapping("/rooms/{roomId}/bookings")
    public List<Booking> ofRoom(@PathVariable long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookings.ofRoom(roomId, date);
    }

    @GetMapping("/bookings/{id}")
    public Booking get(@PathVariable long id) {
        return bookings.get(id);
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancel(@PathVariable long id) {
        bookings.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
