---
from: go
status: draft
concept: "encoding/json since Go 1.21"
from_way: "omitempty that never omits a zero struct or a zero time.Time, MarshalJSON written by hand for string-shaped types, and reflect-heavy third-party decoders for speed or strictness."
native_way: "omitzero (1.24) that uses IsZero, TextMarshaler for string-shaped types, errors.As for *json.SyntaxError and *json.UnmarshalTypeError, and encoding/json/v2 (an experiment since 1.25, behind GOEXPERIMENT=jsonv2)."
sensei_tip: "omitempty on a time.Time field never omitted anything, because a struct is never empty. Switch those fields to omitzero, which calls IsZero, and check the release notes of your Go version before relying on json/v2."
---

| Older Go | Go 1.27 |
| --- | --- |
| `omitempty` on a struct or `time.Time` (no effect) | `omitzero` (1.24) |
| a hand-written `MarshalJSON` adding quotes | `MarshalText`, quoted by the encoder |
| strings.Contains on the error message | `errors.As(err, &syntaxErr)` |
| `interface{}` | `any` |
| third-party strict decoders | `DisallowUnknownFields` + a check for trailing data |
| encoding/json only | encoding/json/v2 as an experiment (`GOEXPERIMENT=jsonv2`) |
