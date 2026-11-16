---
from: java
status: draft
concept: "gRPC, GraphQL and REST"
from_way: "REST controllers for everything, including internal calls in a tight loop between services."
native_way: "grpc-java (or Spring gRPC) with generated stubs for internal calls and streams; Spring for GraphQL with `@QueryMapping` and `@BatchMapping` for client-facing graphs."
sensei_tip: "Spring for GraphQL's `@BatchMapping` solves N+1 by loading a field for a whole list of parents in one call."
---

| Java / Spring | API style |
| --- | --- |
| `.proto` + generated stubs | gRPC contract |
| `StreamObserver<T>` | streaming |
| `Status.NOT_FOUND` | gRPC error |
| `@QueryMapping` | GraphQL resolver |
| `@BatchMapping` | N+1 fix |
