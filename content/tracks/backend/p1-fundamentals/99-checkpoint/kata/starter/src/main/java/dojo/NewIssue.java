package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewIssue(@NotBlank @Size(max = 200) String title, String assignee) {}
