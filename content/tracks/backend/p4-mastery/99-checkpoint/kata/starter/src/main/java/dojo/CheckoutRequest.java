package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CheckoutRequest(@NotBlank String customerName, @NotBlank String sku, @Positive int quantity,
                              @Positive long totalCents, @NotBlank String address) {}
