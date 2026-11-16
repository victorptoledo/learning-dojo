---
from: go
status: draft
concept: "gRPC, GraphQL and REST"
from_way: "`net/http` JSON between every pair of services."
native_way: "`google.golang.org/grpc` with `protoc-gen-go` stubs for internal calls (Go is a first-class gRPC language), gqlgen for GraphQL."
sensei_tip: "Return errors with `status.Error(codes.NotFound, ...)`; a plain `error` becomes `codes.Unknown` and clients cannot react to it."
---

| Go | API style |
| --- | --- |
| `protoc-gen-go` + `protoc-gen-go-grpc` | gRPC contract |
| `status.Error(codes.NotFound, ...)` | gRPC error |
| server streaming `stream.Send` | streaming |
| gqlgen | GraphQL from a schema |
| `dataloadgen` | N+1 fix |
