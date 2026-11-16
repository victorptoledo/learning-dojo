---
from: javascript
status: draft
concept: "Structured output is deserialisation plus validation"
from_way: "JSON.parse and a zod schema to check request bodies."
native_way: "A zod schema converted to JSON Schema for the model, then safeParse on the reply, with the issues fed back on failure."
sensei_tip: "zod 4 can emit JSON Schema (z.toJSONSchema), and both the OpenAI and Anthropic TypeScript SDKs have zod helpers, so one schema drives the model and the validation."
---

| JavaScript | Structured output |
| --- | --- |
| `z.object({...})` | the schema |
| `z.enum([...])` | an enum |
| `.nullable()` | a nullable field |
| `schema.safeParse(JSON.parse(text))` | parse plus validate |
| `result.error.issues` | errors to send back |
