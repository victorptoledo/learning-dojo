---
from: python
status: draft
concept: "Metaprogramming"
from_way: "Decorators, metaclasses and `exec` change code at run time."
native_way: "Macros expand at compile time; the result is type-checked like hand-written code."
sensei_tip: "A Python decorator wraps a function when the module loads; a Rust attribute macro rewrites it before it compiles, so mistakes are compile errors."
---

| Python | Rust |
| --- | --- |
| `@dataclass` | `#[derive(...)]` |
| decorator `@app.route` | attribute macro `#[get("/")]` |
| `*args` | `$( $x:expr ),*` |
| `f"{x}"` | `format!("{x}")` |
