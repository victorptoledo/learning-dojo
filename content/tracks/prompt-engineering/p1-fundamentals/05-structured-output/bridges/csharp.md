---
from: csharp
status: draft
concept: "System.Text.Json with strict contracts"
from_way: "JsonSerializer.Deserialize<T> and data annotations, with a model state that collects validation errors."
native_way: "Deserialise the model's answer into a record, validate enums, required fields and lengths, reject unknown fields, and feed the errors into a retry."
sensei_tip: "JsonUnmappedMemberHandling.Disallow makes System.Text.Json reject unknown fields, the same as additionalProperties false in the schema you send."
---

| C# | Structured-output concept |
| --- | --- |
| `record Ticket(...)` | the output schema |
| `enum Priority` with a string converter | an enum |
| `[Required]`, `[MaxLength(200)]` | required fields and limits |
| `JsonUnmappedMemberHandling.Disallow` | no unknown fields |
| `ModelState` errors | the problem list for the retry |
