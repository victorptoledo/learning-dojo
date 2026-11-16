# Resources and REST: the Richardson Maturity Model

REST (Representational State Transfer, Roy Fielding's 2000 dissertation) is an architectural
style, not a standard: client and server, stateless requests, cacheable responses, a
**uniform interface**, layers in between. In daily practice "a REST API" means an HTTP API
built around **resources** and the standard methods. Leonard Richardson's maturity model is a
useful ladder for seeing how far an API has climbed.

## The four levels

| Level | Name | What it looks like |
| --- | --- | --- |
| 0 | The swamp of POX | one URL, one method: `POST /api` with `{"action": "getOrder", "id": 42}` |
| 1 | Resources | many URLs, one per thing: `POST /orders/42` with `{"action": "cancel"}` |
| 2 | HTTP verbs | the method carries the action and the status code the outcome: `GET /orders/42`, `DELETE /orders/42` → `204` |
| 3 | Hypermedia controls | responses carry links to what the client can do next |

Most good production APIs live at **level 2** and borrow selected level-3 ideas (a `Location`
header, `Link` headers for pagination, a `self` link). Full hypermedia (HATEOAS) is powerful for
long-lived, generic clients and rarely worth its cost for typed SDK clients.

## Modelling resources

A **resource** is anything worth naming: an order, a customer's address, a search, a payment
attempt, a report export. Start from the domain's nouns and the client's tasks, not from your
tables.

- **Collections and items**: `/orders` (a collection) and `/orders/{orderId}` (an item). `POST`
  to a collection creates an item; `GET`, `PUT`, `PATCH`, `DELETE` act on the item.
- **Sub-resources** for strict ownership: `/orders/{orderId}/lines/{lineId}`. Keep nesting to
  one or two levels; deeper paths make clients carry ids they do not need. If a line item has a
  globally unique id, `/order-lines/{lineId}` is fine too.
- **Singleton sub-resources** for one-per-parent things: `/customers/{id}/address`,
  `/orders/{id}/shipping`.
- **Representations are not tables.** An order resource can join three tables, hide two
  columns and compute one field. Never expose foreign keys just because they exist.

## Actions that are not CRUD

Real domains have verbs: cancel, approve, ship, refund. Three accepted ways to model them:

| Approach | Example | When |
| --- | --- | --- |
| A state field changed by `PATCH` | `PATCH /orders/42 {"status": "CANCELLED"}` | simple state machines, no extra data |
| A **resource for the action or its result** | `POST /orders/42/cancellation` · `POST /refunds` | the action has its own data, rules or history |
| A custom method (Google AIP-136) | `POST /orders/42:cancel` | style guides that standardise it |

The middle option is the most REST-like and most common: a refund **is** a thing with an id, an
amount and a status. The checkpoint kata of this phase uses `POST /orders/{id}/cancellation`.

## PUT vs PATCH vs POST for updates

- `PUT` replaces the whole representation; missing fields mean "absent", not "unchanged". It is
  idempotent. Use it when clients own the full state (a settings document).
- `PATCH` with JSON Merge Patch changes only the fields sent; `null` removes a field. Use it for
  large resources where clients change one or two fields.
- `POST` to the item for updates loses idempotency and tells generic clients nothing. Avoid it.

## A level-2 example

```http
POST /orders HTTP/1.1
Content-Type: application/json

{"customerId": "c-17", "lines": [{"sku": "kettle", "quantity": 1}]}

HTTP/1.1 201 Created
Location: https://api.shop.example/orders/42
Content-Type: application/json

{"id": "42", "status": "PENDING", "lines": [...], "createdAt": "2026-03-01T10:00:00Z"}
```

## Further reading

- Roy T. Fielding, "Architectural Styles and the Design of Network-based Software Architectures", chapter 5 (REST): <https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm>
- Martin Fowler, "Richardson Maturity Model": <https://martinfowler.com/articles/richardsonMaturityModel.html>
- Google AIP-121, "Resource-oriented design": <https://google.aip.dev/121>
- Google AIP-136, "Custom methods": <https://google.aip.dev/136>
- Microsoft REST API Guidelines, "Azure" guidelines on resources and collections: <https://github.com/microsoft/api-guidelines/blob/vNext/azure/Guidelines.md>
