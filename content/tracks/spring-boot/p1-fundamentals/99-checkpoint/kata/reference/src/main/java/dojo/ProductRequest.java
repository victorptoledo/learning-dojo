package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** The body of POST /products. Add the constraints from the README. */
public record ProductRequest(
        @NotNull @Pattern(regexp = "[A-Z]{3}-\\d{3}") String sku,
        @NotBlank String name,
        @NotNull @Positive Long priceCents,
        @NotBlank String category) {}
