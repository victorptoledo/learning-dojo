---
from: python
status: draft
concept: "Packages, visibility and tests"
from_way: "Packages and modules from the file layout, _private by convention, and pytest in a tests folder."
native_way: "Modules declared with mod, pub for visibility, unit tests in the same file and integration tests in tests/."
sensei_tip: "Rust's unit tests live next to the code in #[cfg(test)] mod tests, so they can reach private functions. Python's leading underscore is only a convention; Rust's privacy is enforced."
---

| Python | Rust |
| --- | --- |
| package/__init__.py | mod.rs or a module file |
| _private | no pub (private by default) |
| from x import y | use crate::x::y; |
| pytest tests/ | cargo test |
| assert a == b | assert_eq!(a, b) |
| doctest | doc tests in /// comments |
