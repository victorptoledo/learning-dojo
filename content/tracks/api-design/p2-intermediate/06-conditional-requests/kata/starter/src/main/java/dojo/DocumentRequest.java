package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** The new content of a document. */
public record DocumentRequest(@NotBlank String title, @NotNull String body) {}
