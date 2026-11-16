# Cursor Feed

`GET /events` is a feed of shop events, newest first, that clients poll and page through.
It pages with `?offset=`, so when new events arrive between two requests, every later page
shifts: clients see some events twice and, when events are removed, miss others. Replace the
offset with an opaque **keyset cursor**.

`Event`, `EventStore` (50 seeded events whose times are shuffled and sometimes equal),
`FeedExceptions` and `ApiProblems` (problem details for both exceptions) are written. Change
`FeedController` and add what you need (a cursor type, a page record).

## The contract

```text
GET /events?type=order.paid&limit=3
→ 200
Link: <http://localhost/events?type=order.paid&limit=3&cursor=djF8MjAy...>; rel="next"
{"data": [{"id": 31, "type": "order.paid", ...}, ...3 events...], "nextCursor": "djF8MjAy..."}

GET /events?type=order.paid&limit=3&cursor=djF8MjAy...
→ the next 3 events
```

| Parameter | Meaning |
| --- | --- |
| `type` | only events of this type (optional) |
| `limit` | events per page, 1 to 100, default 20; otherwise `InvalidParameterException("limit", ...)` |
| `cursor` | the `nextCursor` of the previous page (optional: without it, the first page) |

Rules:

1. Order is **newest first**: by `occurredAt` descending, then by `id` descending for equal
   times. The id alone is not enough: events arrive late.
2. A page holds the `limit` events that come **after** the cursor in that order. Events added
   since the previous page must not appear again or push older events out of sight.
3. `nextCursor` is `null` on the last page, and only pages with a next cursor carry a
   `Link: <...>; rel="next"` header, whose URL keeps the other parameters.
4. The cursor is **opaque**: clients must not build or parse it. Encode it (for example
   base64url of a small versioned string); a plain number or offset is not a cursor.
5. A cursor belongs to its query: used with a different `type` (or without one) it is an
   `InvalidCursorException`, and so is anything this API did not issue.

> Tip: ask the store for `limit + 1` matching events after the cursor: if you get the extra
> one, there is a next page, and the cursor points at the **last event you return**, not at
> the extra one. `Base64.getUrlEncoder().withoutPadding()` and `Base64.getUrlDecoder()`.
