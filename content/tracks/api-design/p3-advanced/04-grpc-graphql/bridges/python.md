---
from: python
status: draft
concept: "gRPC, GraphQL and REST"
from_way: "Everything in REST, or GraphQL adopted only because it is fashionable."
native_way: "`grpcio` with generated `_pb2` modules for internal services; Strawberry or Graphene for GraphQL with DataLoaders."
sensei_tip: "Strawberry's `DataLoader` batches lookups within a request; without it, each list item resolves its relations with a separate query."
---

| Python | API style |
| --- | --- |
| `grpcio-tools` generated stubs | gRPC contract |
| `context.abort(grpc.StatusCode.NOT_FOUND, ...)` | gRPC error |
| Strawberry `@strawberry.type` | GraphQL schema |
| `DataLoader` | N+1 fix |
| query depth limit extension | query cost control |
