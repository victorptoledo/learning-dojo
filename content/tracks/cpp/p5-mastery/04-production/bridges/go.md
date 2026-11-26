---
from: go
status: draft
concept: "C++ in production"
from_way: "Go modules with checksums, one static binary, a memory-safe language (except data races), and cgo for C."
native_way: "vcpkg or Conan, CMake presets, a fragile ABI, memory safety through tooling and hardening, and a C interface that cgo can call."
sensei_tip: "cgo calls C, not C++: expose an `extern \"C\"` API with opaque handles, and never let an exception reach Go."
---

| Go | C++ |
| --- | --- |
| go.mod + go.sum | vcpkg.json + baseline / Conan lockfile |
| one static binary | static or shared linking; mind the ABI |
| cgo | `extern "C"` API |
| `go test -fuzz` | libFuzzer, AFL++ |
| `-race` in CI | ASan, UBSan, TSan in CI |
| govulncheck | dependency scanning, hardening flags |
