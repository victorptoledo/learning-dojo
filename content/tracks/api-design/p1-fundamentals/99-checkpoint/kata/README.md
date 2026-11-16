# Checkpoint 1: Orders API

Design and build the orders API of a small shop: a paginated, filterable, sortable collection,
one resource per order, a cancellation, and problem details for every error. `Order`,
`OrderStatus`, `OrderRequest` (with its constraints), `OrderExceptions` and `OrderStore` (in
memory, 30 seeded orders) are written. Write `OrderController` and `ApiProblems`.

## The resources

| Request | Answer |
| --- | --- |
| `GET /orders` | `200`, one page of orders (below) |
| `GET /orders/{id}` | `200`, the order |
| `POST /orders` (validated `OrderRequest`) | `201 Created`, `Location: .../orders/{id}`, the new order (`PENDING`) |
| `POST /orders/{id}/cancellation` | `200`, the cancelled order. Cancelling a cancelled order is fine (same answer); a `SHIPPED` order cannot be cancelled |

`OrderStore.cancel` already knows the rules; the API decides how they look on the wire.

## The collection

| Query parameter | Meaning | Default | Invalid when |
| --- | --- | --- | --- |
| `status` | only orders in this status | all | not one of the enum names |
| `customer` | only orders of this customer | all | never |
| `sort` | comma-separated fields, `-` in front for descending: `totalCents,-id` | `id` | a field other than `id`, `createdAt`, `totalCents` |
| `page` | 1-based page number | `1` | below 1 |
| `size` | items per page | `10` | below 1 or above 100 |

After the requested keys, ties are broken by ascending `id`, so pages are stable. A page past the
end is an empty page (`200`), not an error.

```json
GET /orders?status=PAID&size=3&page=2
→ 200
Link: <http://localhost/orders?status=PAID&size=3&page=1>; rel="first",
      <http://localhost/orders?status=PAID&size=3&page=1>; rel="prev",
      <http://localhost/orders?status=PAID&size=3&page=3>; rel="next",
      <http://localhost/orders?status=PAID&size=3&page=3>; rel="last"
{"items": [...3 orders...], "page": 2, "size": 3, "totalItems": 8, "totalPages": 3}
```

The `Link` header (RFC 8288) always has `first` and `last`, has `prev` except on page 1 and
`next` except on the last page, and every link keeps the other query parameters.

## Errors (`application/problem+json`)

Types start with `https://api.dojo.example/problems/`; every problem has `type`, `title`,
`status`, `detail` and `instance` (the request path).

| Cause | Status | `type` suffix | `title` | Extra |
| --- | --- | --- | --- | --- |
| `OrderNotFoundException` | 404 | `order-not-found` | `Order not found` | |
| `OrderNotCancellableException` | 409 | `order-not-cancellable` | `Order cannot be cancelled` | |
| `InvalidParameterException` (throw it for bad query parameters) | 400 | `invalid-parameter` | `Invalid query parameter` | `parameter`: its name |
| invalid `OrderRequest` | 400 | `validation-error` | `Invalid request` | `errors`: `[{"pointer": "#/field", "detail": "..."}]` sorted by pointer; `detail` is `the request has N invalid field(s)` |

The `detail` of the first three is the exception's message.
