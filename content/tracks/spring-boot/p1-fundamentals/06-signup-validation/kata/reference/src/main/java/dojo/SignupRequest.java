package dojo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The body of POST /signups. Add the constraints from the README. */
public record SignupRequest(
        @NotBlank @Size(max = 40) String name,
        @NotBlank @Email String email,
        @NotNull @Min(13) Integer age) {}
