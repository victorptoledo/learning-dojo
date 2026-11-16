package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** What a client sends to create or replace a product. Every field is required. */
public record ProductRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @PositiveOrZero Long priceCents) {}
