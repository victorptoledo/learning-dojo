---
from: csharp
status: draft
concept: "Source generators"
from_way: "Roslyn source generators and attributes read by reflection; no macros."
native_way: "Procedural macros fill the role of source generators; `macro_rules!` covers the small cases."
sensei_tip: "C# source generators add files to the compilation; Rust derive macros add `impl` blocks next to your type."
---

| C# | Rust |
| --- | --- |
| source generator | procedural macro |
| `[Serializable]` + reflection | `#[derive(Serialize)]` |
| `params object[] args` | `$( $x:expr ),*` |
| `nameof(x)` | `stringify!(x)` |
