---
from: go
status: draft
concept: "Input checks and error responses"
from_way: "Manual checks or go-playground/validator struct tags, and hand-written error JSON."
native_way: "Declarative validation and one place that turns exceptions into Problem Details."
sensei_tip: "validate:\"required,email\" struct tags are the same idea as @NotBlank @Email. Spring then turns failures into a consistent error body instead of each handler writing its own."
---

| Go | Spring Boot 4 |
| --- | --- |
| validate:"required" | @NotBlank |
| validate:"email" | @Email |
| if err := v.Struct(req) | @Valid |
| writeError(w, 400, ...) | @RestControllerAdvice |
