---
from: java
status: draft
concept: "Packages, visibility and tests"
from_way: "Packages follow folders, `public`/`protected`/package-private, JUnit tests in a mirrored `src/test/java` tree."
native_way: "Modules are declared with `mod`, items are private to their module unless `pub`, unit tests live in the same file under `#[cfg(test)]`."
sensei_tip: "Keep `main.rs` thin and put the logic in `lib.rs`: everything there is testable from `tests/`."
---

| Java / Maven | Rust / Cargo |
| --- | --- |
| `pom.xml` | `Cargo.toml` |
| a jar / module | a crate |
| a package | a module |
| `import a.b.C;` | `use a::b::C;` |
| `mvn test` | `cargo test` |
| `@Test` | `#[test]` |
| `assertThrows` | `#[should_panic]` or a `Result` assertion |
