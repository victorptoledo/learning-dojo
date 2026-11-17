package dojo.secure;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** The body of PUT /api/admin/users/{username}/roles. */
public record RolesRequest(@NotEmpty List<@NotNull @Pattern(regexp = "USER|ADMIN") String> roles) {
}
