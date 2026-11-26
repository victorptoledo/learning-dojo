---
from: go
status: draft
concept: "Running in production"
from_way: "log/slog, flags or env for configuration, and a static binary in a scratch image."
native_way: "tracing, serde config, explicit errors, and the same small-image story."
sensei_tip: "Go and Rust ship the same way: one binary in a tiny image. Rust adds compile-time memory safety without a GC, at the cost of longer builds; cache cargo's target directory in CI."
---

| Go | Rust |
| --- | --- |
| log/slog | tracing |
| envconfig / viper | envy / config |
| errors.Join, %w | anyhow context, thiserror |
| CGO_ENABLED=0 static binary | musl target or distroless |
| pprof endpoint | tokio-console, perf |
