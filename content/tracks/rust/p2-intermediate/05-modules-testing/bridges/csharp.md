---
from: csharp
status: draft
concept: "Packages, visibility and tests"
from_way: "Namespaces, assemblies, public/internal/private, and xUnit or NUnit projects."
native_way: "Crates and modules, pub and pub(crate), and tests in the same crate."
sensei_tip: "pub(crate) is C#'s internal. A Rust crate is an assembly; a test module inside it can test private items without InternalsVisibleTo."
---

| C# | Rust |
| --- | --- |
| namespace | mod |
| internal | pub(crate) |
| public | pub |
| using X; | use x; |
| [Fact] | #[test] |
| Assert.Equal | assert_eq! |
