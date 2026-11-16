---
from: javascript
status: draft
concept: "Never trust JSON.parse"
from_way: "JSON.parse on a fetch response and treating the result as the type you expected."
native_way: "Parse the model's answer defensively, validate it against a schema (Zod, Ajv), and retry with the list of issues when it does not match."
sensei_tip: "A Zod schema can be converted to JSON Schema for the provider and also used with safeParse on the answer, so the contract you send and the contract you check are the same object."
---

| JavaScript / TypeScript | Structured-output concept |
| --- | --- |
| `z.object({...}).strict()` | schema with no unknown fields |
| `z.enum(["low", "medium", "high"])` | an enum |
| `schema.safeParse(data)` | validation that returns all issues |
| `as Ticket` casts | a bug: no runtime check |
| `z.string().nullable()` | an explicit null for missing values |
