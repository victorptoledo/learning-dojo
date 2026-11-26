---
from: go
status: draft
concept: "unsafe and cgo"
from_way: "`unsafe.Pointer`, `unsafe.Slice` and cgo with `import \"C\"`; cgo calls are slow and pin goroutines."
native_way: "`unsafe` blocks, raw pointers and `extern \"C\"` calls at the cost of a normal function call."
sensei_tip: "Go's `unsafe` package breaks the type system; Rust's `unsafe` keeps it and adds only five powers."
---

| Go | Rust |
| --- | --- |
| `unsafe.Pointer(&x)` | `&raw const x` |
| `unsafe.Slice(ptr, n)` | `slice::from_raw_parts(ptr, n)` |
| `import "C"`, `C.strlen` | `unsafe extern "C" { fn strlen(...); }` |
| `//export Name` | `#[unsafe(no_mangle)] pub extern "C" fn name` |
