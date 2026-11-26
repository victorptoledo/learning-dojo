---
from: java
status: draft
concept: "Code generation"
from_way: "Annotation processors (Lombok, MapStruct) and reflection at run time; no macros."
native_way: "`macro_rules!` for pattern-based expansion and procedural macros for derives and attributes."
sensei_tip: "`#[derive(Debug, Clone)]` is Lombok's `@ToString` and copy constructor done by the compiler, with no run-time reflection."
---

| Java | Rust |
| --- | --- |
| Lombok `@Data` | `#[derive(Debug, Clone, PartialEq)]` |
| annotation processor | procedural macro crate |
| varargs `List.of(a, b)` | `vec![a, b]` |
| `String.format` checked at run time | `format!` checked at compile time |
