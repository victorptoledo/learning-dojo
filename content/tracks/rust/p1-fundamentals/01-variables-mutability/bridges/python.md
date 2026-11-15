---
from: python
status: draft
concept: "Immutability by default"
from_way: "Every name can be rebound and most objects can be changed; immutability is a convention (tuples, frozen dataclasses)."
native_way: "Bindings are immutable unless declared mut, and shadowing with let creates a new binding."
sensei_tip: "Rust's let x = x.trim(); looks like Python's rebinding, but it is a new variable that may even have a new type. Mutating without mut is a compile error, not a runtime surprise."
---

| Python | Rust |
| --- | --- |
| x = 5; x = 6 | let mut x = 5; x = 6; |
| x = x.strip() | let x = x.trim(); |
| @dataclass(frozen=True) | the default: fields cannot change without mut |
| MAX = 10 (by convention) | const MAX: u32 = 10; |
