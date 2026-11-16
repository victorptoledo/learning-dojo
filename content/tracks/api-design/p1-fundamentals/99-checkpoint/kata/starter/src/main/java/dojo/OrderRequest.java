package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** What a client sends to place an order. */
public record OrderRequest(@NotBlank String customer, @NotNull @Positive Long totalCents) {}
