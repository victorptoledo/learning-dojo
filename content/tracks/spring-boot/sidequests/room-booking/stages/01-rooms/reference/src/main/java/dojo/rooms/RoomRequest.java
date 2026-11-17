package dojo.rooms;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The body of POST /rooms. */
public record RoomRequest(
        @NotBlank @Size(max = 40) String name,
        @NotNull @Min(1) @Max(100) Integer capacity) {
}
