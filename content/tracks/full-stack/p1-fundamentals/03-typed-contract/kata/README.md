# A Typed Contract Between Client and Server

A to-do API and its client keep drifting apart: the server accepts titles the UI cannot show,
and the UI crashes when the server changes a field. Put **one contract** in `contract.ts`, written
with [zod](https://zod.dev), and make both sides use it: the server validates what comes in, the
client validates what comes back, and TypeScript derives every type from the same schemas.

## The contract (`contract.ts`)

The schemas compile already, but they are too loose. Tighten them:

| Schema | Rules |
| --- | --- |
| `NewTodo` | `title`: a string, trimmed, 1 to 120 characters after trimming; `due`: optional, an ISO date `YYYY-MM-DD` that exists (`2026-02-30` does not). **Unknown keys are refused** |
| `Todo` | `id` a positive integer, `title` a string, `due` an ISO date or `null`, `done` a boolean |
| `Problem` | [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details: `type`, `title` (strings), `status` (integer), optional `errors`: a list of `{ path, message }` |

and write `toFieldErrors(error)`: one `{ path, message }` per zod issue, where `path` is the
issue's path joined with `.` (`""` for an issue about the whole object).

## The server (`server.ts`)

`handleTodos(request, store)` gets `{ method, path, contentType, body }` (the body as text) and
returns `{ status, headers, body }` (the body as a value, sent as JSON). `serve()` already turns it
into a real `node:http` server.

| Request | Answer |
| --- | --- |
| `GET /todos` | `200`, every todo in the store, oldest first |
| `POST /todos` whose `Content-Type` is not `application/json` (parameters such as `; charset=utf-8` are fine) | `415`, problem `type: "/problems/unsupported-media-type"` |
| `POST /todos` whose body is not JSON | `400`, problem `type: "/problems/malformed-json"` |
| `POST /todos` that does not match `NewTodo` | `422`, problem `type: "/problems/validation"` with `errors` from `toFieldErrors` |
| a valid `POST /todos` | `201`, `Location: /todos/<id>`, the stored `Todo`: the parsed title, `due` or `null`, `done: false`, ids from 1 |
| anything else | `404`, problem `type: "/problems/not-found"` |

Every problem has `title` (a short sentence of your choice) and `status` equal to the HTTP
status, and its `Content-Type` is `application/problem+json`. JSON answers use
`application/json`.

## The client (`client.ts`)

| Function | Does |
| --- | --- |
| `createTodo(baseUrl, input, fetchFn?)` | `POST`s the input as JSON. Returns `{ ok: true, todo }` on a 2xx, `{ ok: false, problem }` on an error answer |
| `listTodos(baseUrl, fetchFn?)` | `GET /todos`, returns the todos |

Both **parse what the server sent** with the contract. When an answer does not match it (a
`Todo` without `done`, an error body that is not a `Problem`), they throw a `ContractError`
instead of returning bad data to the UI.

## How the kata is checked

`tsc` checks every `.ts` file; `tests/*.types.ts` sections are type-level tests (the types come
from the schemas, so `Todo` and `NewTodoInput` must match the table above); then `node:test`
runs the server on `127.0.0.1` and calls it. There are 18 tests: 2 in `tests/visible.test.ts`
and 16 hidden ones. The functions that still throw `new Error("todo ...")` are yours to write.
