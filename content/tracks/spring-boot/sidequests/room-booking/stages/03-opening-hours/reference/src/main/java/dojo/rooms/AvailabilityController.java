package dojo.rooms;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvailabilityController {
    private final BookingService bookings;

    public AvailabilityController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/rooms/{roomId}/availability")
    public List<Slot> availability(@PathVariable long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookings.freeSlots(roomId, date);
    }
}
