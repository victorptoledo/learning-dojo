---
from: go
status: draft
concept: "Problem details (RFC 9457)"
from_way: "`http.Error(w, \"bad request\", 400)`, which sends `text/plain`."
native_way: "A `Problem` struct with `json` tags and a helper that sets `Content-Type: application/problem+json`, the status, and encodes it."
sensei_tip: "Keep `Extensions map[string]any` and marshal it flat with a custom `MarshalJSON`, or embed typed fields per problem type; RFC 9457 extensions live at the top level."
---

| Go | Problem details |
| --- | --- |
| `type Problem struct{ Type, Title string; Status int; ... }` | a problem |
| `w.Header().Set("Content-Type", "application/problem+json")` | media type |
| custom `MarshalJSON` | flat extension members |
| `errors.As(err, &notFound)` | mapping domain errors |
| `slog` with a request id | safe 500s |
