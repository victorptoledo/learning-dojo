---
from: python
status: draft
concept: "Parse, validate, retry"
from_way: "json.loads(answer) and hoping, or catching json.JSONDecodeError and giving up."
native_way: "Strip fences, take the object, json.loads it, validate every field (with Pydantic or by hand), and send all the problems back in a bounded retry."
sensei_tip: "Pydantic's model_json_schema() gives you the schema to send to the provider, and model_validate_json() with a ValidationError gives you the full error list for the retry. One class, both directions."
---

| Python | Structured-output concept |
| --- | --- |
| `class Ticket(BaseModel)` with `Literal[...]` | schema with enums |
| `Ticket.model_json_schema()` | the JSON Schema for the provider |
| `model_config = ConfigDict(extra="forbid")` | no unknown fields |
| `ValidationError.errors()` | all the problems, for the retry message |
| `str \| None = None` | an explicit null for missing values |
