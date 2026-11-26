---
from: java
status: draft
concept: "Jackson, annotations and OpenCSV"
from_way: "Jackson's ObjectMapper with @JsonProperty, @JsonIgnore and @JsonInclude(NON_NULL), FAIL_ON_UNKNOWN_PROPERTIES, custom serializers, records as DTOs, and a CSV library such as OpenCSV or Jackson CSV."
native_way: "encoding/json in the standard library with struct tags, omitempty and omitzero, Decoder.DisallowUnknownFields, MarshalJSON or MarshalText methods on your types, and encoding/csv for CSV."
sensei_tip: "Jackson ignores unknown properties only when you tell it to; encoding/json ignores them by default. Turn on DisallowUnknownFields for configuration and for any input where a typo should be an error."
---

| Java | Go |
| --- | --- |
| `@JsonProperty("user_id")` | `json:"user_id"` |
| `@JsonIgnore` | `json:"-"` |
| `@JsonInclude(NON_NULL)` / `NON_DEFAULT` | `omitempty` / `omitzero` |
| `FAIL_ON_UNKNOWN_PROPERTIES = true` | `dec.DisallowUnknownFields()` |
| a custom `JsonSerializer<T>` | `func (t T) MarshalJSON() ([]byte, error)` |
| `@JsonValue` on a string-like type | `MarshalText` / `UnmarshalText` |
| `JsonNode` | `map[string]any` or `json.RawMessage` |
| OpenCSV `CSVReader` | `csv.NewReader(r)` |
