---
from: java
status: draft
concept: "Structured output is deserialisation plus validation"
from_way: "Jackson maps JSON to a record and Bean Validation (@NotNull, @Min) checks it."
native_way: "A JSON Schema constrains or guides the model, then your code parses, validates and, on failure, asks the model to repair."
sensei_tip: "Generate the schema from a Java record (Spring AI's BeanOutputConverter or a JSON Schema generator) so the record and the prompt never drift apart."
---

| Java | Structured output |
| --- | --- |
| `record Ticket(Category category, int urgency)` | the schema with an enum and an integer |
| `objectMapper.readValue(json, Ticket.class)` | parse the model's JSON |
| `@Min(1) @Max(5)` | `minimum` and `maximum` in the schema |
| `FAIL_ON_UNKNOWN_PROPERTIES` | `additionalProperties: false` |
| `ConstraintViolation` list | validation errors fed back to the model |
