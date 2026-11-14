package dojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DiscountRequest(@Min(1) @Max(100) int percent) {}
