---
from: go
status: draft
concept: "OpenAPI as a contract"
from_way: "Comments that are supposed to describe the API."
native_way: "Design-first with `oapi-codegen` generating types and a `ServerInterface` from the YAML; the handler implements the interface."
sensei_tip: "`oapi-codegen` with `strict-server: true` generates request and response types per operation, so returning an undocumented status does not compile."
---

| Go | OpenAPI concept |
| --- | --- |
| `oapi-codegen` | types and server interface from the contract |
| `StrictServerInterface` | documented responses only |
| `kin-openapi` | load and validate documents |
| request validation middleware | runtime checks |
| oasdiff | breaking changes |
