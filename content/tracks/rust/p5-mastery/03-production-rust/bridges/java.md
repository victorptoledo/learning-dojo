---
from: java
status: draft
concept: "Running in production"
from_way: "Checked and unchecked exceptions with stack traces, SLF4J/Logback, Spring `application.yml` profiles, a fat jar on a JVM."
native_way: "`Result` with `thiserror`/`anyhow` and context chains, `tracing` with `RUST_LOG`, config from args/env (`clap`), one static binary."
sensei_tip: "Add `.context(...)` at every I/O boundary: it gives you the readable \"caused by\" chain you are used to from Java stack traces."
---

| Java | Rust |
| --- | --- |
| custom `Exception` classes | an error enum with `thiserror` |
| `throws Exception` in `main` | `fn main() -> anyhow::Result<()>` |
| SLF4J + Logback | `tracing` + `tracing-subscriber` |
| MDC fields | span fields (`#[instrument]`) |
| `application.yml` + `@Value` | `clap` with `env = "..."`, or `serde` + a TOML file |
| `mvn package` → jar | `cargo build --release` → native binary |
