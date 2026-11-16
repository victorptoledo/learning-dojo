# gRPC, GraphQL and REST: Choosing a Style

REST over HTTP is a good default, not a law. Two other styles are common enough that every API
designer must know when they win: **gRPC** for typed, fast service-to-service calls, and
**GraphQL** for clients that need flexible reads over a rich graph. Many companies use all
three: REST for public APIs, gRPC inside, GraphQL behind their own apps.

## gRPC and Protocol Buffers

The contract is a `.proto` file; code generators produce clients and servers in a dozen
languages.

```proto
syntax = "proto3";
package shop.orders.v1;

service OrderService {
  rpc GetOrder(GetOrderRequest) returns (Order);
  rpc ListOrders(ListOrdersRequest) returns (ListOrdersResponse);
  rpc WatchOrder(WatchOrderRequest) returns (stream OrderEvent);   // server streaming
}

message Order {
  string id = 1;
  OrderStatus status = 2;
  int64 total_cents = 3;
  reserved 4;              // a removed field: the number is never reused
  google.protobuf.Timestamp created_at = 5;
}
```

- **Binary and compact** over HTTP/2, with deadlines, cancellation and metadata built in.
- **Streaming** in both directions (server, client, bidirectional).
- **Evolution rules** are explicit: fields are identified by **number**, not name. Add fields
  with new numbers; never reuse or change the type of a number; `reserved` removed ones. Old
  clients ignore unknown fields. The package carries the major version (`orders.v1`).
- **Errors** use status codes such as `NOT_FOUND`, `INVALID_ARGUMENT`, `FAILED_PRECONDITION`,
  `UNAVAILABLE`, with rich details (`google.rpc.Status`, `ErrorInfo`, `BadRequest`).
- Costs: browsers cannot call it directly (gRPC-Web or a gateway is needed), payloads are not
  human-readable, HTTP caches and many tools do not understand it. Google AIP applies
  resource-oriented design to gRPC too, so methods still look like `GetOrder`, `ListOrders`.

## GraphQL

One endpoint (`POST /graphql`); the server publishes a typed **schema**, and each client query
names exactly the fields it needs:

```graphql
query {
  order(id: "ord_8f2kq") {
    status
    customer { name }
    lines(first: 5) { edges { node { sku quantity } } }
  }
}
```

- **No over- or under-fetching**: one round trip per screen, shaped by the client.
- **Strongly typed, introspectable schema**; deprecation per field (`@deprecated(reason: ...)`)
  and usage metrics per field make "versionless" evolution realistic.
- Costs you must design for:
  - **Query cost**: one innocent-looking nested query can touch millions of rows. Limit depth
    and complexity, use persisted (allow-listed) queries for your own apps.
  - **N+1 resolvers**: batch with DataLoader-style loaders.
  - **HTTP caching** mostly disappears (everything is a `POST` to one URL); persisted queries
    via `GET` bring some back.
  - **Errors** arrive with status `200` in an `errors` array, partial data next to them.
  - **Authorisation per field and per object**, not per endpoint.
- Pagination conventions: the Relay "connections" spec (`edges`, `node`, `pageInfo`,
  `endCursor`): a cursor design, like the one in phase 2.

## Choosing

| Need | Lean towards |
| --- | --- |
| public API for unknown developers, cacheable resources, curl-friendly | REST |
| internal calls between services, low latency, streaming, many languages | gRPC |
| your own web and mobile apps with many screens over a connected domain | GraphQL (often as a BFF over REST/gRPC services) |
| pushing events to subscribers | webhooks or a broker (next lesson); GraphQL subscriptions or gRPC streams for live connections |

Questions that decide it: who are the clients and do you control them? Do you need HTTP
caching? How do you want to evolve the contract? What does your team already operate well?

## Further reading

- gRPC documentation, "Core concepts" and "Status codes": <https://grpc.io/docs/what-is-grpc/core-concepts/>, <https://grpc.io/docs/guides/status-codes/>
- Protocol Buffers, "Proto Best Practices" and "Updating a message type": <https://protobuf.dev/best-practices/dos-donts/>
- Google AIP-127, "HTTP and gRPC Transcoding": <https://google.aip.dev/127>
- GraphQL specification: <https://spec.graphql.org/>
- GraphQL.org, "Best Practices" (pagination, versioning, caching): <https://graphql.org/learn/best-practices/>
- GraphQL Cursor Connections Specification (Relay): <https://relay.dev/graphql/connections.htm>
- OWASP, "GraphQL Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/GraphQL_Cheat_Sheet.html>
