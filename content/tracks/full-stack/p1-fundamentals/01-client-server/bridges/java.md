---
from: java
status: draft
concept: "Typed contracts"
from_way: "Separate backend and frontend where renaming a DTO field silently breaks the UI."
native_way: "Types shared end to end: a backend change makes the frontend compiler complain immediately."
sensei_tip: "Generate TypeScript types from the OpenAPI spec, or use tRPC, to end type mismatches between front and back."
---
