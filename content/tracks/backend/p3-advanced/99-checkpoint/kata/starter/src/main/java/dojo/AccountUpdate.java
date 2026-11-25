package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The only thing a client may change on an account. */
public record AccountUpdate(@NotBlank @Size(max = 40) String nickname) {}
