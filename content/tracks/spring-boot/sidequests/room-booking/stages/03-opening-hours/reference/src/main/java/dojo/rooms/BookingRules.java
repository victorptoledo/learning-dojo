package dojo.rooms;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** The rules of the building, from the booking.* properties. */
@Validated
@ConfigurationProperties("booking")
public record BookingRules(@NotNull LocalTime opens, @NotNull LocalTime closes, @NotNull Duration maxDuration) {

    @AssertTrue(message = "opens must be before closes")
    public boolean isOpenBeforeClose() {
        return opens == null || closes == null || opens.isBefore(closes);
    }
}
