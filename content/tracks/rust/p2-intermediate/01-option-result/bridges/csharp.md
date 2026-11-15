---
from: csharp
status: draft
concept: "Null safety and errors"
from_way: "Nullable reference types as warnings, and exceptions for errors."
native_way: "Option<T> and Result<T, E> enforced by the compiler, with ? to propagate."
sensei_tip: "C#'s nullable annotations are warnings you can ignore; Rust's Option cannot be used as a T without handling None. Result replaces exceptions for expected failures."
---

| C# | Rust |
| --- | --- |
| string? | Option<String> |
| throw new ArgumentException() | Err(...) |
| try/catch | match or ? |
| x ?? y | x.unwrap_or(y) |
| int.TryParse | s.parse::<i32>() |
