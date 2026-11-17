package dojo.secure;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The body of POST /api/signup. */
public record SignupRequest(
        @NotNull @Pattern(regexp = "[a-z0-9_]{3,20}") String username,
        @NotNull @Size(min = 12, max = 72) String password) {
}
