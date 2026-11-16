---
from: csharp
status: draft
concept: "Naming and representation conventions"
from_way: "PascalCase properties leaking as PascalCase JSON, `double` for money, `DateTime` with unspecified kind."
native_way: "System.Text.Json's camelCase policy, `decimal` (or `long` cents), `DateTimeOffset` in UTC, `JsonStringEnumConverter` for enums."
sensei_tip: "Without `JsonStringEnumConverter`, enums serialise as numbers: a reordering of the enum then changes the API."
---

| C# / ASP.NET Core | Convention |
| --- | --- |
| `JsonNamingPolicy.CamelCase` | consistent field case |
| `DateTimeOffset.UtcNow` | RFC 3339 timestamp |
| `decimal` / `long` cents | money |
| `JsonStringEnumConverter` | named enum values |
| `record Page<T>(IReadOnlyList<T> Items, string? NextCursor)` | collection envelope |
