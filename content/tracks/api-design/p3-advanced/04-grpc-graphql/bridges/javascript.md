---
from: javascript
status: draft
concept: "gRPC, GraphQL and REST"
from_way: "One Express app answering REST to browsers and to other services."
native_way: "`@grpc/grpc-js` with generated types (or Connect RPC, which also speaks to browsers), and Apollo Server or GraphQL Yoga with DataLoader."
sensei_tip: "Connect RPC (connectrpc.com) speaks gRPC, gRPC-Web and a simple JSON-over-HTTP protocol from one `.proto`: an easy way to let browsers call your RPC services."
---

| JavaScript / TypeScript | API style |
| --- | --- |
| `@grpc/grpc-js` | gRPC |
| Connect RPC | RPC for browsers too |
| Apollo Server / GraphQL Yoga | GraphQL server |
| `dataloader` | N+1 fix |
| persisted queries | query allow-list |
