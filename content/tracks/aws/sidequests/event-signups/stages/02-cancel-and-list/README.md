Plans change: people cancel, and the organisers want the list of who is coming.

| Route | Answer |
| --- | --- |
| `DELETE /events/{id}/signups/{email}` | `204` with no body; the sign-up is gone and the seat is free again. `404` `not signed up` when there is no such sign-up (or no such event). The email in the path is matched trimmed and in lower case |
| `GET /events/{id}/signups?limit=50&cursor=...` | `200` and `{"items": ["ana@example.com", ...], "next": "<cursor>" or null}`: the emails in alphabetical order, at most `limit` of them (default 50, from 1 to 100, else `400`); `next` is the cursor of the following page, and `null` when there are no more sign-ups. `404` `no such event` |

Rules:

- **Cancel in one transaction**, like the sign-up: a `Delete` of the sign-up on condition it
  exists, and an `Update` of the event that subtracts 1 from `taken`. A cancel that finds
  nothing must not free a seat: two people cancelling the same sign-up at once free one seat.
- **List with `query`**, never `scan`: the partition `EVENT#<id>`, sort keys that begin with
  `SIGNUP#` (the event's own `META` item is not a sign-up).
- **The cursor is opaque** to the client: any text of your choosing that your handler can
  read back (base64 of a small JSON is common). A cursor your handler cannot read answers
  `400`. Following `next` until it is `null` must visit every sign-up exactly once.
- **`next` is `null` on the last page**, even when the last page is exactly full. DynamoDB
  returns a `LastEvaluatedKey` whenever it stopped at `Limit`, more items or not: ask for one
  more than you need to know whether there is a next page.
- Requests may now arrive with `isBase64Encoded: true` (API Gateway does that for some
  content types): decode the body first.

> [!TIP]
> `query(..., Limit=n)` stops after reading `n` items; `ExclusiveStartKey` takes a full key
> (`pk` and `sk`) and starts right after it. The cursor only has to remember the last email.
