Let people vote, once each, and make retries safe.

## POST /polls/:id/votes

The voter says who they are with a `voter-id` header: 1 to 64 letters, digits, `_` or `-`.
The body is JSON `{ "optionId": <number> }`. Check in this order:

1. `404` problem when the poll does not exist.
2. `400` problem when `voter-id` is missing or invalid.
3. The body, as for a new poll: `415`, `413`, `400`.
4. `422` problem with an error on the path `optionId` when it is not the id of one of the
   poll's options (a string `"1"` is not).
5. `409` problem when the poll is closed.
6. `409` problem when this voter already voted on this poll.

A vote answers `201` with `{ "pollId": "...", "optionId": 2 }`. Twenty requests of the same
voter **at the same time** must count once: the tests open all the requests, then send all
the bodies. Think about what can happen between reading a body and saving the vote.

## GET /polls/:id/results

`200` with the counts, options in their order:

```json
{ "pollId": "...", "status": "open", "total": 3,
  "options": [{ "id": 1, "text": "Red", "votes": 0 }, { "id": 2, "text": "Green", "votes": 2 }, { "id": 3, "text": "Blue", "votes": 1 }],
  "winners": [2] }
```

`winners` are the ids of the options with the most votes (several on a tie), and `[]` when
nobody voted.

## Idempotency-Key

A client whose connection dropped does not know whether its `POST` arrived, so it sends the
same request again. With an `Idempotency-Key` header (1 to 128 visible ASCII characters,
otherwise `400`) on `POST /polls` or `POST /polls/:id/votes`:

- The first request runs as usual, and its answer (status, headers, body), **errors
  included**, is stored under the key.
- The same key with the same request (method, path, `voter-id` and body text) gets the stored
  answer again, plus the header `idempotent-replayed: true`, and changes nothing: a retried
  vote is not a second vote and a retried `POST /polls` creates no second poll.
- The same key with a different request is a `422` problem.

Without the header nothing is stored. Allowed methods: `POST` on `/polls/:id/votes`, `GET` on
`/polls/:id/results`.
