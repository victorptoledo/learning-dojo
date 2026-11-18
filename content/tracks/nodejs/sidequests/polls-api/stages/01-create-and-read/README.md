Create polls and read them back, and answer every mistake with a useful error.

```ts
export interface AppOptions { now?: () => Date; maxBodyBytes?: number }  // 16384 by default
export interface FieldError { path: string; message: string }
export function createApp(options?: AppOptions): Server;
```

## Problem details

Every error answer is an RFC 9457 problem: content type `application/problem+json` and a
body `{ type, title, status, detail }`, where `type` is `"about:blank"`, `title` is the
status's standard text (`STATUS_CODES[status]` from `node:http`), `status` repeats the HTTP
status and `detail` explains this case. Validation problems add `errors`, a list of
`{ path, message }`. An unexpected exception is a `500` problem that does not leak the stack.

## POST /polls

The body must be JSON: another content type is a `415` (a `charset` parameter is fine),
a body over `maxBodyBytes` is a `413` (read and throw away the rest, so the connection stays
usable), and text that does not parse is a `400`. Then validate, and report **every** invalid
field at once in a `422`:

| Field | Rule | Error path |
| --- | --- | --- |
| the body | an object (not an array, `null` or a string) | `""` (and nothing else) |
| `question` | required, a string of 1 to 200 characters after trimming | `question` |
| `options` | required, an array of 2 to 10 items | `options` |
| each option | a string of 1 to 100 characters after trimming; not a repeat of an earlier one, ignoring case | `options[<index>]` |
| `closesAt` | optional: a date and time `Date.parse` understands, later than `now()` | `closesAt` |
| anything else | not allowed | its own name |

The messages are yours; each path appears once per problem found. A valid poll answers
`201`, with `Location: /polls/<id>` and the poll:

```json
{ "id": "a unique string", "question": "Tabs or spaces?",
  "options": [{ "id": 1, "text": "Tabs" }, { "id": 2, "text": "Spaces" }],
  "status": "open", "closesAt": "2026-03-02T12:00:00.000Z", "createdAt": "2026-03-01T12:00:00.000Z" }
```

Texts are stored trimmed, option ids count from 1, dates are ISO strings from
`toISOString()`, `closesAt` is `null` when absent, and `status` is `"closed"` once `now()`
has reached `closesAt` and `"open"` before.

## GET /polls/:id, and the rest

- `GET /polls/:id` answers `200` with the poll, or a `404` problem.
- Any other path is a `404` problem. A known path with a method it does not have is a `405`
  problem with an `Allow` header listing its methods (for now `POST` on `/polls` and `GET` on
  `/polls/:id`; later stages add more).
