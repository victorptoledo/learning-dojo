---
from: csharp
status: draft
concept: "Pass by reference vs aliasing"
from_way: "Reference types shared freely, plus ref, in and out for value types."
native_way: "& and &mut, with the compiler enforcing one writer or many readers."
sensei_tip: "C#'s in parameter is close to &T and ref to &mut T, but Rust checks them everywhere, also across fields and loops, and forbids a reference outliving its value."
---

| C# | Rust |
| --- | --- |
| void F(List<int> items) | fn f(items: &mut Vec<i32>) |
| in ReadOnlySpan<int> | &[i32] |
| ref int x | &mut i32 |
| InvalidOperationException: collection was modified | a compile error |
