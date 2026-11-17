package dojo.wallet;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The body of POST /transfers. */
public record NewTransfer(@NotNull Long from, @NotNull Long to, @NotNull @Min(1) @Max(100_000_000) Long amountCents) {
}
