---
from: java
status: draft
concept: "Native code and raw memory"
from_way: "JNI or the Foreign Function and Memory API (Panama), `MemorySegment`, and `sun.misc.Unsafe` for raw access."
native_way: "`unsafe` blocks, raw pointers, `MaybeUninit`, and `extern \"C\"` with no glue code."
sensei_tip: "Panama's `Linker` and `MemorySegment` are Java's safe-ish FFI; in Rust an `extern \"C\"` declaration is enough, and the safety contract is written in `# Safety`."
---

| Java | Rust |
| --- | --- |
| JNI / Panama `Linker` | `unsafe extern "C" { fn ... }` |
| `MemorySegment` | `*const T`, `*mut T`, slices from raw parts |
| `sun.misc.Unsafe` | `unsafe { ... }` with `// SAFETY:` comments |
| `native` method | `#[unsafe(no_mangle)] pub extern "C" fn` |
