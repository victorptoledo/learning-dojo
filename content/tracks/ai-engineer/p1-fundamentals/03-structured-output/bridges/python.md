---
from: python
status: draft
concept: "Structured output is deserialisation plus validation"
from_way: "json.loads and manual dict checks, or Pydantic models for request bodies."
native_way: "A Pydantic model (or a JSON Schema) given to the SDK for constrained output, then model_validate on what comes back, with a bounded repair loop."
sensei_tip: "With Pydantic v2, `Ticket.model_json_schema()` gives the schema and `ValidationError.errors()` gives paths and messages you can feed straight back to the model."
---

| Python | Structured output |
| --- | --- |
| `class Ticket(BaseModel)` | the schema |
| `Literal["billing", "shipping"]` | an enum |
| `str | None` | a nullable field |
| `Ticket.model_validate_json(text)` | parse plus validate |
| `ValidationError.errors()` | errors to send back in the repair loop |
