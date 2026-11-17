package dojo.wallet;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The body of a deposit or a withdrawal. */
public record Amount(@NotNull @Min(1) @Max(100_000_000) Long amountCents) {
}
