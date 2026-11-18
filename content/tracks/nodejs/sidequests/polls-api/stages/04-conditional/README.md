Let clients skip downloads they already have, and stop two editors from overwriting each
other: entity tags.

## ETag and If-None-Match

- `GET /polls/:id` and `GET /polls/:id/results` send an `ETag` header: a **strong** tag (a
  quoted string, `"..."`, no `W/`) that changes whenever the JSON they answer changes,
  including when the status flips with the clock or a vote arrives. A hash of the body text
  is the simplest way.
- With `If-None-Match` holding the current tag, they answer `304` with the `ETag` and **no
  body**. The header may list several tags (`"a", "b"`), a tag may come weak (`W/"a"` matches
  `"a"`), and `*` matches any.

## PATCH /polls/:id

Changes `question` and `closesAt` with a JSON merge patch: content type
`application/merge-patch+json`, fields that are absent stay as they were, and
`"closesAt": null` removes the closing time.

1. `404` problem when the poll does not exist.
2. `428` problem (Precondition Required) without an `If-Match` header.
3. The body: `415` for another content type (plain `application/json` too), `413`, `400`.
4. `412` problem (Precondition Failed) when `If-Match` is not the poll's current ETag (`*`
   matches any), with the current `ETag` in the answer so the client can reload.
5. `422` problem listing every invalid field: `question` as for a new poll, `closesAt` a date
   in the future or `null`, `options` cannot be changed, anything else is not allowed. A body
   that is not an object has the error path `""`. Nothing changes.
6. `200` with the new poll and its new `ETag`.

Ten editors sending the same `If-Match` at the same time: exactly one wins, the others get
`412`. Like the votes, the tests open every request before sending the bodies, so compare
the tag at the moment you change the poll, not when the request arrives.

`/polls/:id` now allows `GET, PATCH`.
