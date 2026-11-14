package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NewProject(@Pattern(regexp = "[A-Z]{2,10}") @NotBlank String key, @NotBlank String name) {}
