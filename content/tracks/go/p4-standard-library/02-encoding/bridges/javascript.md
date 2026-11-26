---
from: javascript
status: draft
concept: "JSON.parse, JSON.stringify and CSV libraries"
from_way: "JSON.parse and JSON.stringify with a replacer or toJSON method, any object shape accepted, validation libraries such as zod for checks, and csv-parse for CSV."
native_way: "json.Unmarshal into typed structs, MarshalJSON instead of toJSON, struct tags for names and omission, DisallowUnknownFields for strictness, and encoding/csv in the standard library."
sensei_tip: "JSON numbers become float64 when you decode into any, so an ID like 9007199254740993 loses precision, just as in JavaScript. Decode into a typed int64 field, or use Decoder.UseNumber."
---

| JavaScript | Go |
| --- | --- |
| `JSON.parse(text)` | `json.Unmarshal([]byte(text), &v)` |
| `JSON.stringify(v, null, 2)` | `json.MarshalIndent(v, "", "  ")` |
| `toJSON()` | `MarshalJSON()` or `MarshalText()` |
| a reviver function | `UnmarshalJSON` on the field's type |
| `zod.object({...}).strict()` | a struct + `DisallowUnknownFields` + your own `Validate` |
| `undefined` fields are dropped | `omitempty` / `omitzero` |
| csv-parse | `encoding/csv` |
