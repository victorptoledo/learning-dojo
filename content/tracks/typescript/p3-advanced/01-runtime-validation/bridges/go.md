---
from: go
status: draft
concept: "Decoding and validating input"
from_way: "encoding/json decodes into structs (wrong types fail, missing fields become zero values) and go-playground/validator checks struct tags."
native_way: "JSON.parse gives any; a zod schema checks types, required fields and rules in one step, and the type is inferred from it."
sensei_tip: "Go's zero values hide missing fields; zod reports them. Validate once in the handler, like you would call validate.Struct right after decoding."
---

| Go | TypeScript |
| --- | --- |
| `json.NewDecoder(r.Body).Decode(&u)` | `User.safeParse(await req.json())` |
| `validate:"required,email"` tags | `z.email()` |
| `DisallowUnknownFields()` | `z.strictObject({...})` |
| Zero value for a missing field | An issue for a missing field, or `.default(...)` |
| `envconfig.Process("app", &cfg)` | A schema over `process.env` |
