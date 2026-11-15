---
from: python
status: draft
concept: "Pydantic and zod"
from_way: "Pydantic models validate and coerce data and are also the type (BaseModel subclasses); mypy checks the rest."
native_way: "zod schemas validate and the type is inferred with z.infer; there is no class, the result is a plain object."
sensei_tip: "zod is TypeScript's Pydantic: same idea (the schema is the type), same place (the boundary). One difference: zod does not coerce strings to numbers unless you ask with z.coerce."
---

| Pydantic v2 | zod 4 |
| --- | --- |
| `class User(BaseModel): name: str` | `const User = z.object({ name: z.string() })` |
| `User.model_validate(data)` | `User.parse(data)` |
| `ValidationError.errors()` with `loc` | `error.issues` with `path` |
| `Field(default=10)` | `.default(10)` |
| `@field_validator` | `.refine(...)` / `.transform(...)` |
| `User.model_json_schema()` | `z.toJSONSchema(User)` |
| `pydantic-settings` | A zod schema over `process.env` |
