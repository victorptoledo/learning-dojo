Three routes first. Bodies are JSON; errors answer `{"error": "<message>"}` with the message
given here.

| Route | Answer |
| --- | --- |
| `POST /events` with `{"id": "rust-meetup", "title": "Rust meetup", "capacity": 40}` | `201` and the event: `{"id", "title", "capacity", "taken": 0, "remaining": 40}`. `409` `event exists` if the id is taken (a conditional put, never a read first) |
| `GET /events/{id}` | `200` and the event as above, with the current `taken` and `remaining`; `404` `no such event` |
| `POST /events/{id}/signups` with `{"email": "Ana@Example.com "}` | `201` and `{"event": "rust-meetup", "email": "ana@example.com"}` (trimmed, lower case); `404` `no such event`; `409` `already signed up`; `409` `event is full` |
| any other route | `404` `no such route` |

Validation answers `400` (any message): the body is not a JSON object; `id` is not 3 to 40
characters of `a-z`, `0-9` and `-`; `title` is not a non-empty string of at most 200
characters; `capacity` is not an integer from 1 to 10,000 (`true` is not an integer); the
email does not look like `name@domain.tld`.

The sign-up is the heart of it. Do it in **one** `transact_write_items` call with two
operations:

- an `Update` of the event that adds 1 to `taken`, on condition that the event exists and
  `taken` is below the capacity;
- a `Put` of the sign-up item, on condition that it does not exist yet.

Either both happen or neither does. When DynamoDB cancels the transaction, its
`CancellationReasons` (one per operation, in order) say which condition failed: that tells
`event is full` (or `no such event`) from `already signed up`. No `GetItem` before the
transaction to "check first": between that read and the write, the last seat may go.

The tests check what the table holds after each request, that a full event signs no one up
even when the counter is the only thing that says so, and that you never `Scan`.

> [!TIP]
> `capacity` is a DynamoDB reserved word (so are `name`, `status`, `count` and hundreds
> more): write `#capacity` in the expression and pass
> `ExpressionAttributeNames={"#capacity": "capacity"}`.
