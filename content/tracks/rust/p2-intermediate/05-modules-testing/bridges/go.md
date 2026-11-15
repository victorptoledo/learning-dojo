---
from: go
status: draft
concept: "Packages, visibility and tests"
from_way: "Packages per directory, exported names with a capital letter, and _test.go files."
native_way: "Modules, pub for export, and tests in #[cfg(test)] modules or tests/."
sensei_tip: "Go's capital letter is Rust's pub. Go's _test.go next to the code is Rust's tests module in the same file; Go's external test package is Rust's tests/ directory."
---

| Go | Rust |
| --- | --- |
| package foo | mod foo |
| func Exported() | pub fn exported() |
| func internal() | fn internal() |
| x_test.go | #[cfg(test)] mod tests |
| go test ./... | cargo test |
| t.Errorf | assert!/assert_eq! |
