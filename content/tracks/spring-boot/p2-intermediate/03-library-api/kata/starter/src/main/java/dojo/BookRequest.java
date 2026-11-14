package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookRequest(@NotBlank String isbn, @NotBlank String title, @NotNull Integer year, @NotBlank String author) {}
