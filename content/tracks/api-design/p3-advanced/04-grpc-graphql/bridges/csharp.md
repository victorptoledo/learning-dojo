---
from: csharp
status: draft
concept: "gRPC, GraphQL and REST"
from_way: "WCF-era habits: RPC everywhere, or REST controllers for internal streaming."
native_way: "`Grpc.AspNetCore` with code generated from `.proto` in the build; Hot Chocolate for GraphQL with DataLoaders and cost analysis."
sensei_tip: "Hot Chocolate has built-in query cost analysis and DataLoaders; enable both before exposing a GraphQL endpoint outside your own apps."
---

| C# / ASP.NET Core | API style |
| --- | --- |
| `Grpc.AspNetCore` + `<Protobuf Include=...>` | gRPC contract in the build |
| `RpcException(new Status(StatusCode.NotFound, ...))` | gRPC error |
| `IServerStreamWriter<T>` | streaming |
| Hot Chocolate `[Query]` | GraphQL |
| `DataLoader` | N+1 fix |
