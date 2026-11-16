---
from: go
status: draft
concept: "Evolving an API without breaking clients"
from_way: "Changing a struct field's type and redeploying all callers at once (impossible for external clients)."
native_way: "Additive fields, `encoding/json` ignoring unknown fields by default, a `/v2` mux subtree when necessary, and oasdiff on the OpenAPI document."
sensei_tip: "`json.Decoder.DisallowUnknownFields()` is great for strict server-side input validation and wrong for clients reading your responses."
---

| Go | Evolution concept |
| --- | --- |
| new struct field with `omitempty` | compatible change |
| default `json.Unmarshal` | tolerant reader |
| `mux.Handle("/v2/", v2)` | path version |
| `w.Header().Set("Sunset", ...)` | retirement notice |
| oasdiff | breaking-change check |
