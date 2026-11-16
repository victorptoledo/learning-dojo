# Pagination, Filtering and Sorting

Every collection grows. An endpoint that returns "all orders" works in the demo and times out
in production, and adding pagination later is a breaking change (clients that expected
everything now silently get the first page). **Paginate every collection from day one**, with a
default and a maximum page size.

## Offset pagination

```text
GET /orders?page=3&size=20        (or ?offset=40&limit=20)
→ {"items": [...], "page": 3, "size": 20, "totalItems": 1312, "totalPages": 66}
```

| Pros | Cons |
| --- | --- |
| simple; clients can jump to page 37 | `OFFSET 100000` makes the database read and throw away 100,000 rows |
| totals are easy to show in a UI | **unstable**: an insert before the current page shifts everything, so the next page repeats an item; a delete skips one |
| | `COUNT(*)` for totals can be as expensive as the query |

Offset is fine for small, slowly changing collections and admin UIs with page numbers.

## Cursor (keyset) pagination

```text
GET /events?limit=20
→ {"data": [...20 events...], "nextCursor": "djF8MjAyNi0wNS0wMVQwODoxODowMFp8NDE"}
GET /events?limit=20&cursor=djF8MjAyNi0wNS0wMVQwODoxODowMFp8NDE
```

The cursor encodes **where the previous page ended**: the sort key of its last item. The next
query is `WHERE (created_at, id) < (:lastCreatedAt, :lastId) ORDER BY created_at DESC, id DESC
LIMIT 21`, which an index answers directly however deep the client is.

- **Stable** under inserts and deletes: new items do not shift what comes "after" the cursor.
- **Fast** at any depth, but no "jump to page 37" and usually no cheap total.
- The sort key must be **unique**: add the id as a tie-breaker, or items with the same
  timestamp are skipped or repeated at page boundaries.
- Make cursors **opaque** (base64url of a small versioned structure, optionally signed) so you
  can change their content later and clients cannot forge them. Bind them to the query:
  a cursor from `?status=PAID` must not be accepted with `?status=SHIPPED`.
- Fetch `limit + 1` rows to know whether there is a next page without a count.

The Phase 2 kata "Cursor Feed" builds exactly this.

## Telling the client where the next page is

Either in the body (`nextCursor`, `nextPageToken`) or in a `Link` header (RFC 8288), or both:

```http
Link: <https://api.shop.example/orders?status=PAID&page=4&size=20>; rel="next",
      <https://api.shop.example/orders?status=PAID&page=2&size=20>; rel="prev"
```

Links must keep every other query parameter (filters, sort), so clients can follow them
blindly.

## Filtering

- **Simple equality per field**: `?status=PAID&customerId=c-17`. Covers most needs.
- **Ranges**: `?createdAfter=2026-01-01T00:00:00Z&createdBefore=...` or `createdAt[gte]=`.
- **A filter language** for advanced cases: `filter=status = "PAID" AND total > 1000`
  (Google AIP-160), or OData `$filter`. Powerful, but every operator you support is a
  permanent promise and a query-performance risk. Only allow fields you have indexes for.
- Validate every value and answer `400` with a problem detail that names the parameter.

## Sorting

`sort=-createdAt,totalCents`: a comma-separated list, `-` for descending. Allow only listed
fields, always append a unique tie-breaker internally, and document the default order.
Sorting by an arbitrary field is a database index you promise to keep.

## Sizes and limits

| Rule | Why |
| --- | --- |
| Default page size (for example 20) | clients that do not ask still get a bounded response |
| Maximum (for example 100) | protect the database; answer `400` above it, or clamp and say so |
| Same parameter names in every collection | consistency (phase 1, lesson 2) |

## Further reading

- RFC 8288, "Web Linking": <https://www.rfc-editor.org/rfc/rfc8288>
- Google AIP-158, "Pagination", and AIP-132, "Standard methods: List": <https://google.aip.dev/158>, <https://google.aip.dev/132>
- Google AIP-160, "Filtering": <https://google.aip.dev/160>
- Zalando RESTful API Guidelines, "Pagination": <https://opensource.zalando.com/restful-api-guidelines/#pagination>
- Markus Winand, "No Offset" (keyset pagination): <https://use-the-index-luke.com/no-offset>
- GitHub REST API docs, "Using pagination in the REST API": <https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api>
