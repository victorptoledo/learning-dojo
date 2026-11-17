package dojo.secure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The body of POST /api/projects. */
public record ProjectRequest(@NotBlank @Size(max = 60) String name) {
}
