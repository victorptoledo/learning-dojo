package dojo.tickets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** The body of POST /events/{id}/holds. */
public record HoldRequest(@NotEmpty List<@NotBlank String> seats) {
}
