---
from: python
status: draft
concept: "Input checks and error responses"
from_way: "Pydantic validation in FastAPI returning 422, and exception handlers."
native_way: "Bean Validation annotations, @Valid, and a @RestControllerAdvice answering with Problem Details."
sensei_tip: "FastAPI answers invalid bodies with 422; Spring with 400. Both can be customised: extend ResponseEntityExceptionHandler to shape the Problem Details body."
---

| Python | Spring Boot 4 |
| --- | --- |
| Field(min_length=1) | @NotBlank |
| EmailStr | @Email |
| @app.exception_handler(NotFound) | @ExceptionHandler in a @RestControllerAdvice |
| 422 with error list | 400 Problem Details with field errors |
