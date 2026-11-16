---
from: go
status: draft
concept: "Compile-time programming"
from_way: "const declarations for simple values and go generate for code generation."
native_way: "constexpr functions compute arbitrary values at compile time, and static_assert turns assumptions into build errors."
sensei_tip: "Many `go generate` tables become a constexpr function; a `var _ = [1]int{}[unsafe.Sizeof(x)-8]` trick becomes `static_assert(sizeof(X) == 8)`."
---

| Go | C++ |
| --- | --- |
| `const Size = 64` | `constexpr int kSize = 64;` |
| `go generate` lookup tables | `constexpr std::array` |
| size checks via array tricks | `static_assert(sizeof(T) == N)` |
| `init()` functions | `constinit` / `constexpr` |
| type switches | `if constexpr` + traits |
