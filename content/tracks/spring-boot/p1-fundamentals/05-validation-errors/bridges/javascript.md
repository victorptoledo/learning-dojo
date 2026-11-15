---
from: javascript
status: draft
concept: "Input checks and error responses"
from_way: "class-validator decorators with NestJS's ValidationPipe, or zod schemas in Express."
native_way: "Bean Validation on records, @Valid on the parameter, and an advice for errors."
sensei_tip: "class-validator's @IsEmail is Bean Validation's @Email; NestJS exception filters are @RestControllerAdvice classes."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| @IsNotEmpty() | @NotBlank |
| zod.parse(body) | @Valid @RequestBody |
| ExceptionFilter | @RestControllerAdvice |
| { statusCode, message } | RFC 9457 Problem Details |
