---
from: go
status: draft
concept: "Compiling and building"
from_way: "go build compiles packages with no headers; go.mod pins modules; one static binary comes out, quickly."
native_way: "C++ uses headers for declarations, compiles each .cpp separately, links them, and relies on CMake and a separate package manager."
sensei_tip: "Builds are slower and more manual than Go's; keep headers small and include only what you use, and let CMake do the bookkeeping."
---

| Go | C++ |
| --- | --- |
| `go build ./...` | `cmake -B build && cmake --build build` |
| package import | `#include` + linking the library target |
| `go.mod` | `CMakeLists.txt` + vcpkg.json or conanfile |
| exported = capitalised | declared in a header (and visible to the linker) |
| `go vet`, gofmt | warnings, clang-tidy, clang-format |
| `-race` | `-fsanitize=thread` |
