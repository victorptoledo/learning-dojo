---
from: javascript
status: draft
concept: "Native addons and WebAssembly"
from_way: "Node-API addons in C++, or WebAssembly with typed arrays over linear memory."
native_way: "Raw pointers and `extern \"C\"`; Rust is also a first-class WebAssembly language."
sensei_tip: "A `Uint8Array` view over WebAssembly memory is a raw slice: Rust's `slice::from_raw_parts` is the same idea with a written safety contract."
---

| JavaScript | Rust |
| --- | --- |
| Node-API addon | `extern "C"` functions, or the `napi` crate |
| `WebAssembly.Memory` + `Uint8Array` | `*mut u8` + `slice::from_raw_parts_mut` |
| `wasm-bindgen` glue | generated `extern "C"` on the Rust side |
