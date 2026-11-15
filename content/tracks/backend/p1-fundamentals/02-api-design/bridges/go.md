---
from: go
status: draft
concept: "From RPC endpoints to resources"
from_way: "Handlers registered per path, often designed endpoint by endpoint."
native_way: "A resource model and conventions for every endpoint."
sensei_tip: "Go's 1.22 routing patterns (\"GET /orders/{id}\") make resource-style APIs natural; keep the same pagination and error format across handlers."
---

| Go | In practice |
| --- | --- |
| /getOrder?id=1 | GET /orders/{id} |
| custom error structs | Problem Details |
| ad hoc query params | consistent filters and pagination |
| breaking changes | additive evolution |
