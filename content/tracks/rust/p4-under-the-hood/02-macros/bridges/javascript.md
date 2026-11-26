---
from: javascript
status: draft
concept: "Build-time code generation"
from_way: "Babel plugins, tagged templates and decorators (stage 3); no macros in the language."
native_way: "Macros are part of the language and run inside the compiler."
sensei_tip: "A tagged template such as `` sql`SELECT ...` `` runs at run time; `sqlx::query!` checks the SQL against the database at compile time."
---

| JavaScript | Rust |
| --- | --- |
| tagged template `` html`...` `` | function-like macro `html! { ... }` |
| Babel plugin | procedural macro |
| rest parameters `...args` | `$( $x:expr ),*` |
| decorators | attribute macros |
