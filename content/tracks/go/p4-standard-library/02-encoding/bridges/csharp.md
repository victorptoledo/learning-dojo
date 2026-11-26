---
from: csharp
status: draft
concept: "System.Text.Json and attributes"
from_way: "System.Text.Json with [JsonPropertyName], [JsonIgnore] and JsonIgnoreCondition.WhenWritingDefault, JsonConverter<T> for custom types, UnmappedMemberHandling.Disallow, and a CSV library such as CsvHelper."
native_way: "encoding/json with struct tags, omitempty and omitzero, MarshalJSON or MarshalText methods on the type itself, DisallowUnknownFields, and encoding/csv."
sensei_tip: "A JsonConverter is registered in options; in Go the type carries its own MarshalJSON, so it is used everywhere the type goes, with nothing to register."
---

| C# | Go |
| --- | --- |
| `[JsonPropertyName("user_id")]` | `json:"user_id"` |
| `[JsonIgnore]` | `json:"-"` |
| `WhenWritingNull` / `WhenWritingDefault` | `omitempty` / `omitzero` |
| `JsonConverter<T>` | `MarshalJSON` / `UnmarshalJSON` on T |
| `UnmappedMemberHandling.Disallow` | `dec.DisallowUnknownFields()` |
| `JsonDocument` / `JsonElement` | `json.RawMessage` or `map[string]any` |
| `TimeSpan` in JSON | a `Duration` type with `MarshalText` |
| CsvHelper | `encoding/csv` |
