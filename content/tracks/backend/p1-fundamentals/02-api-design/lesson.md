# REST API Design

An API is a product for other developers. Once clients depend on it, every mistake in it is
expensive to fix. Good APIs are **predictable** (the same patterns everywhere), **hard to misuse**
and **able to evolve** without breaking anyone.

## Resources and URLs

Model **nouns** (resources), and let HTTP methods be the verbs:

```text
GET    /projects                     list projects
POST   /projects                     create one
GET    /projects/7                   read one
PATCH  /projects/7                   change some fields
DELETE /projects/7                   delete it
GET    /projects/7/issues            issues of project 7 (a sub-collection)
GET    /issues/31                    an issue on its own, when it has a global identity
```

- Plural nouns for collections, ids for items; lowercase with hyphens (`/billing-accounts`).
- Nest only one level, for real containment. `/projects/7/issues/31/comments/5` is a sign the
  comment should be reachable as `/comments/5`.
- Actions that do not fit CRUD become a **resource of their own**:
  `POST /orders/42/cancellation` instead of `POST /cancelOrder?id=42`, or
  `POST /payments/9/refunds` for a refund.

## Requests and responses

- JSON in and out, with one naming style (`camelCase` or `snake_case`) everywhere.
- Return the resource after `POST`, `PUT` and `PATCH`, so the client does not need another `GET`.
- Dates as ISO-8601 with an offset (`2026-11-14T10:00:00Z`), money as an integer in the smallest
  unit or a decimal string plus a currency, ids as strings if they may ever exceed 2^53.
- Never expose internal details: database ids that leak growth, stack traces, column names.

## Collections: pagination, filtering, sorting

Never return an unbounded list.

| Style | Request | Good for |
| --- | --- | --- |
| Offset | `?page=3&size=20` (or `offset`/`limit`) | small, stable data; "jump to page 7" |
| Cursor (keyset) | `?after=eyJpZCI6MTIzfQ&limit=20` | large or fast-changing data, infinite scroll |

Offsets get slow on deep pages (the database still skips the rows) and shift when rows are
inserted. A **cursor** encodes the position of the last item returned (`id > 123`), so every
page is an indexed range scan.

```json
{
  "items": [ ... ],
  "page": 3, "size": 20, "total": 412,
  "next": "/issues?page=4&size=20&status=open"
}
```

Filters and sorting as query parameters: `?status=open&assignee=ada&sort=-createdAt`. Validate
them: unknown sort fields are `400`, not a silent default.

## Partial updates

- `PUT` replaces the whole resource: missing fields become empty. Fine for small resources.
- `PATCH` changes some fields. The simplest format is **JSON Merge Patch**
  (`application/merge-patch+json`): fields present are set, `null` removes, absent stays.

```json
PATCH /issues/31   {"status": "closed", "assignee": null}
```

Combine updates with `If-Match` and ETags so two clients do not overwrite each other.

## Errors

Use the status code for the kind of error and one body shape for all of them, Problem Details
(RFC 9457):

```json
{
  "type": "https://api.example.com/problems/insufficient-stock",
  "title": "Insufficient stock",
  "status": 409,
  "detail": "only 2 of TEA-001 left, 5 requested",
  "sku": "TEA-001"
}
```

`type` is a stable identifier clients can branch on; `detail` is for humans. Validation errors
list every invalid field at once, so the client can fix them in one go.

## Versioning and evolution

Most changes can be **additive** and need no version:

- adding a field to a response (clients must ignore unknown fields),
- adding an optional request field or a new endpoint.

These **break** clients and need a new version or a migration period: removing or renaming a
field, changing its type or meaning, making an optional field required, changing status codes.

Version in the path (`/v2/orders`) or a header; keep the old version running while clients move,
announce deprecation (`Deprecation` and `Sunset` headers), and remove it only when traffic is gone.

## Contracts: OpenAPI

Describe the API in an **OpenAPI** document: endpoints, parameters, schemas, errors. Generate it
from code (springdoc-openapi for Spring) or write it first and generate code from it. Either way it
gives clients documentation, mock servers and generated SDKs, and gives you a contract to test
against.

## Summary

- Resources as plural nouns, methods as verbs; non-CRUD actions become resources.
- Return the resource after writes; ISO dates, explicit money, no internals.
- Always paginate: offsets for small data, cursors for large; validate filters and sorts.
- `PUT` replaces, `PATCH` with merge patch changes fields; protect both with `If-Match`.
- One error format (Problem Details) with stable types.
- Evolve additively; version only for breaking changes, with a deprecation period; describe the
  contract with OpenAPI.
