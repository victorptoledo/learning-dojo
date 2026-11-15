---
from: go
status: draft
concept: "Implicit interfaces and method sets"
from_way: "Interfaces satisfied implicitly, small interfaces such as io.Reader and fmt.Stringer, sort.Interface with Len, Less and Swap, no operator overloading, and struct tags read by reflection."
native_way: "Protocols satisfied implicitly, special methods such as __str__, __len__ and __lt__, operator overloading through __add__ and friends, and descriptors and __set_name__ instead of struct tags."
sensei_tip: "Go's io.Reader is a Protocol. fmt.Stringer is __str__, and sort.Interface becomes __len__ plus __lt__ (or just a key function)."
---

| Go | Python |
| --- | --- |
| `type Reader interface { Read(p []byte) (int, error) }` | `class SupportsRead(Protocol): def read(self, n: int) -> bytes: ...` |
| `String() string` (fmt.Stringer) | `__str__` |
| `Len`, `Less`, `Swap` | `__len__`, `__lt__` (or `sorted(key=...)`) |
| `a.Add(b)` | `a + b` with `__add__` |
| `func (s Set) Contains(x T) bool` | `__contains__`, used by `in` |
| struct tags plus reflection | descriptors with `__set_name__` |
| embedding to reuse methods | inheritance from an ABC or a `collections.abc` mixin |
