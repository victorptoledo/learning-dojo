package dojo.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The body of POST /accounts. */
public record NewAccount(
        @NotBlank @Size(max = 40) String owner,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {
}
