package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record NewOrder(@NotBlank String customer, @Positive long totalCents) {}
