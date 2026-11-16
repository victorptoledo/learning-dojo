---
from: java
status: draft
concept: "Deserialise, then validate"
from_way: "Jackson's ObjectMapper.readValue into a record, with Bean Validation (@NotNull, @Pattern) checked before the data is used."
native_way: "The model's answer is parsed into an object, then validated against a schema with enums, required fields and no unknown fields; failures are sent back as a retry message."
sensei_tip: "Configure your parser like FAIL_ON_UNKNOWN_PROPERTIES and collect every ConstraintViolation, not just the first: the full list is exactly what the retry message needs."
---

| Java | Structured-output concept |
| --- | --- |
| `record Ticket(Category category, ...)` | the output schema |
| `enum Category` | an enum in the schema |
| `FAIL_ON_UNKNOWN_PROPERTIES` | `additionalProperties: false` |
| `Validator.validate(obj)` returns all violations | collect every problem for the retry |
| `Optional<LocalDate>` | an explicit `null` for missing values |
