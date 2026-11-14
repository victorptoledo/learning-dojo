---
from: java
status: draft
concept: "Binary serialisation vs JSON"
from_way: "Serialising and parsing verbose JSON text on every internal call between services."
native_way: "Protobuf encodes data in a compact binary format, much smaller than JSON, with types checked at build time."
sensei_tip: "Use REST or GraphQL for public, frontend-facing APIs; use gRPC for internal service-to-service traffic."
---
