---
from: javascript
status: draft
concept: "Running in production"
from_way: "pino or winston logs, dotenv configuration, and node images with node_modules."
native_way: "tracing, serde configuration, explicit error types, and one binary per image."
sensei_tip: "Where a Node image carries node_modules, a Rust image carries one file. Build with --release and set panic behaviour and logging before you ship."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| pino | tracing |
| dotenv | envy or config with serde |
| Error subclasses | thiserror enums |
| node:22-slim + node_modules | one binary in distroless |
| prom-client | prometheus / metrics crates |
