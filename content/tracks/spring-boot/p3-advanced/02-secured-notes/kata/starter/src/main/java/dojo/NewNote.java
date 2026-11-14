package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewNote(@NotBlank @Size(max = 500) String text) {}
