---
from: go
status: draft
concept: "Structs and pointers in recent releases"
from_way: "Helper functions such as func intPtr(i int) *int for optional fields, spelling out the embedded struct in literals, and a temporary variable before taking an address."
native_way: "new(expr) creates an initialised pointer (1.26), struct literals may name promoted fields (1.27), and encoding/json/v2 offers stricter, faster JSON for tagged structs (1.27)."
sensei_tip: "Delete your ptr helpers: new(v) does the same, works for any type and reads clearly at the call site."
---

| Older Go | Go 1.27 |
| --- | --- |
| `func intPtr(i int) *int { return &i }` | `new(42)` |
| `age := yearsSince(born); p.Age = &age` | `p.Age = new(yearsSince(born))` |
| `Invoice{Audit: Audit{CreatedBy: "ada"}, Number: "F-1"}` | `Invoice{CreatedBy: "ada", Number: "F-1"}` |
| `encoding/json` only | `encoding/json/v2` and `jsontext` (v1 now runs on v2) |
| `interface{}` fields | `any` |
