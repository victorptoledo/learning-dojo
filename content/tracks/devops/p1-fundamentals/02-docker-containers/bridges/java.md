---
from: java
status: draft
concept: "Packaging applications"
from_way: "Shipping a large artefact on a heavy base image that carries the full toolchain."
native_way: "Multi-stage build: the first stage compiles with the toolchain; the final stage copies only the binary into a tiny image (Alpine or distroless)."
sensei_tip: "Order Dockerfile instructions from least to most frequently changed to make the most of the layer cache."
---
