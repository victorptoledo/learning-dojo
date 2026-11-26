---
from: go
status: draft
concept: "Generated code"
from_way: "`go generate` with external tools writes `.go` files; no macros by design."
native_way: "Macros expand inside the compiler, so nothing generated is checked in."
sensei_tip: "Where Go runs `stringer` to generate a `String()` method, Rust uses a derive or a few lines of `macro_rules!`."
---

| Go | Rust |
| --- | --- |
| `//go:generate stringer` | a derive or `macro_rules!` |
| struct tags + reflection (`json:"name"`) | `#[serde(rename = "name")]` |
| variadic `...T` | `$( $x:expr ),*` |
| `fmt.Sprintf` checked by vet | `format!` checked by the compiler |
