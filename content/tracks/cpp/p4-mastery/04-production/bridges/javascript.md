---
from: javascript
status: draft
concept: "C++ in production"
from_way: "npm with lockfiles, a memory-safe runtime, N-API addons or WebAssembly for native code."
native_way: "vcpkg or Conan with pinned baselines, memory safety through tooling, N-API addons, WebAssembly via Emscripten, or C interfaces."
sensei_tip: "C++ compiled to WebAssembly keeps its bugs but runs in a sandbox; native addons run with full access, so harden and fuzz them."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| npm + lockfile | vcpkg baseline / Conan lockfile |
| `package.json` scripts | CMake presets |
| native addon (N-API) | `extern "C"` + node-addon-api |
| WebAssembly | Emscripten builds of C++ |
| memory-safe runtime | sanitizers, fuzzing, hardening flags |
