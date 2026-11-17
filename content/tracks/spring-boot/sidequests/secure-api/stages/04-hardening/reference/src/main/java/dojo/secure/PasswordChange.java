package dojo.secure;

import jakarta.validation.constraints.NotNull;

/** The body of PUT /api/me/password. */
public record PasswordChange(@NotNull String currentPassword, @NotNull String newPassword) {
}
