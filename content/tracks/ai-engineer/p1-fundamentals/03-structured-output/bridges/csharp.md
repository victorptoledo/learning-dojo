---
from: csharp
status: draft
concept: "Structured output is deserialisation plus validation"
from_way: "System.Text.Json into a record, with data annotations for validation."
native_way: "A JSON Schema derived from a C# type guides the model; you deserialize, validate and repair on failure."
sensei_tip: "System.Text.Json's JsonSchemaExporter (.NET 9+) produces a schema from a type, and Microsoft.Extensions.AI's GetResponseAsync<T> asks for structured output and deserializes it for you."
---

| C# | Structured output |
| --- | --- |
| `record Ticket(Category Category, int Urgency)` | the schema |
| `enum Category` with `JsonStringEnumConverter` | an enum |
| `string?` | a nullable field |
| `JsonSerializer.Deserialize<Ticket>` | parse |
| `Validator.TryValidateObject` | validation errors to feed back |
