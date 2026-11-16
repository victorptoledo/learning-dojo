package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** What a client sends to withdraw money. */
public record WithdrawalRequest(
        @NotNull @Positive Long amountCents,
        @NotBlank @Size(max = 140) String reference) {}
