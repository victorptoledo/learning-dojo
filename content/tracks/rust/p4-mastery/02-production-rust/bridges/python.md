---
from: python
status: draft
concept: "Running in production"
from_way: "logging, settings from environment variables, Docker images with the interpreter and dependencies."
native_way: "tracing for structured logs, typed configuration, thiserror or anyhow for errors, and a small static binary in the image."
sensei_tip: "A Rust service image can be a few megabytes: build in a rust image, copy the binary into debian-slim or distroless. There is no interpreter or virtualenv to ship."
---

| Python | Rust |
| --- | --- |
| logging + JSON formatter | tracing + tracing-subscriber |
| pydantic-settings | a config struct with serde |
| exceptions with tracebacks | anyhow with context |
| python:3.13-slim image | distroless image with one binary |
| prometheus_client | metrics crates (prometheus, metrics) |
