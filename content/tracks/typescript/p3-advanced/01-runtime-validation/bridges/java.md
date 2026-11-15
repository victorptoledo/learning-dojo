---
from: java
status: draft
concept: "Bean Validation vs schema-first types"
from_way: "Classes annotated with Jakarta Bean Validation (@NotNull, @Size, @Email), Jackson deserialising into typed objects that fail on wrong types."
native_way: "A zod schema is the single source of truth: it validates unknown data at runtime and the TypeScript type is inferred from it."
sensei_tip: "In Java, deserialisation already enforces types; in TypeScript, JSON.parse enforces nothing. A schema at the boundary is the equivalent of Jackson plus Bean Validation together."
---

| Java | TypeScript |
| --- | --- |
| `record User(@NotBlank String name, @Email String email)` | `z.object({ name: z.string().min(1), email: z.email() })` |
| `objectMapper.readValue(json, User.class)` | `User.parse(JSON.parse(json))` |
| `ConstraintViolation` with `propertyPath` | `issue.path` |
| `@Valid` on a controller parameter | `safeParse` at the start of the handler |
| A distinct `UserId` class | `.brand<"UserId">()` |
