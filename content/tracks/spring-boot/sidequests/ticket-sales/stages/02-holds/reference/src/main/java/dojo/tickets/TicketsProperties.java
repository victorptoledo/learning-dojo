package dojo.tickets;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** The tickets.* settings. */
@Validated
@ConfigurationProperties("tickets")
public record TicketsProperties(@NotNull Duration holdTtl, @Min(1) @Max(50) int maxSeatsPerHold) {
}
