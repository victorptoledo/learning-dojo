package dojo.secure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** The secure.* settings of application.properties. */
@Validated
@ConfigurationProperties("secure")
public record SecureProperties(@Valid @NotNull Admin admin, @Valid @NotNull Lockout lockout) {

    public record Admin(@NotBlank String username, @NotBlank String password) {
    }

    public record Lockout(@Min(1) int attempts, @NotNull Duration duration) {
    }
}
