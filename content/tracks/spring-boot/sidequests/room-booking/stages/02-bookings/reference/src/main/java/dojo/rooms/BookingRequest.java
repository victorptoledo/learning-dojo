package dojo.rooms;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** The body of POST /rooms/{roomId}/bookings. */
public record BookingRequest(
        @NotBlank @Size(max = 80) String title,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end,
        @NotNull @Min(1) Integer attendees) {
}
