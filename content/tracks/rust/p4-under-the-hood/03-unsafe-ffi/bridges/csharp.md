---
from: csharp
status: draft
concept: "unsafe code and P/Invoke"
from_way: "`unsafe` blocks with pointers, `fixed`, `Span<T>`, and `[DllImport]` / `[LibraryImport]` for P/Invoke."
native_way: "The same `unsafe` idea, with `MaybeUninit`, raw pointers and `extern \"C\"`."
sensei_tip: "C# `unsafe` also needs a compiler switch; in Rust it is per block, and the convention is a `// SAFETY:` comment on each one."
---

| C# | Rust |
| --- | --- |
| `unsafe { int* p = &x; }` | `let p = &raw mut x; unsafe { *p = 1 }` |
| `[LibraryImport("libc")]` | `unsafe extern "C" { ... }` |
| `fixed (byte* p = buffer)` | `buffer.as_ptr()` (no GC, nothing to pin) |
| `Span<T>` from a pointer | `slice::from_raw_parts(ptr, len)` |
| `[UnmanagedCallersOnly]` | `#[unsafe(no_mangle)] pub extern "C" fn` |
