---
from: python
status: draft
concept: "json, dataclasses and csv"
from_way: "json.loads into dicts, dataclasses or pydantic models for typed data, a default= function or custom encoder for other types, and csv.DictReader for files with a header."
native_way: "json.Unmarshal into a struct whose tags name the fields, methods on your types for custom forms, and csv.Reader returning []string records that you map by header yourself."
sensei_tip: "json.loads gives you whatever the input had. In Go you decode into a struct, so the shape is checked for you; reach for map[string]any only when the shape is really unknown."
---

| Python | Go |
| --- | --- |
| `json.loads(s)` | `json.Unmarshal(data, &v)` |
| `json.dumps(obj, indent=2)` | `json.MarshalIndent(v, "", "  ")` |
| `json.load(f)` on a stream | `json.NewDecoder(r).Decode(&v)` |
| pydantic `extra="forbid"` | `dec.DisallowUnknownFields()` |
| `Optional[int] = None` | `*int` with `omitempty` |
| `default=` encoder function | `MarshalJSON` / `MarshalText` methods |
| `csv.DictReader(f)` | `csv.NewReader(r)` + a map from header to index |
| `decimal.Decimal` for money | integer minor units, or a type with `MarshalText` |
