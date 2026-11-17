package dojo.tickets;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** The body of POST /events. */
public record NewEvent(
        @NotBlank @Size(max = 80) String name,
        @NotNull Instant startsAt,
        @NotNull @Min(1) @Max(26) Integer rows,
        @NotNull @Min(1) @Max(50) Integer seatsPerRow,
        @NotNull @Min(0) @Max(10_000_000) Long priceCents) {
}
