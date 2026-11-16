# Notes API

Write a small JSON API with `node:http` alone, no framework. Frameworks such as Express and
Fastify do this routing and parsing for you; doing it once by hand shows what they do and what
can go wrong (bodies that never end, wrong content types, unhandled errors).

`HttpError` and `sendJson` are given. Write three functions in `app.ts`; `server.ts` starts the
API with `npm start`.

| Function | Does |
| --- | --- |
| `readJson(req, limit?)` | Reads the body (`for await` over the request) and parses it. Throws `HttpError` **415** when `content-type` is not `application/json` (parameters such as `; charset=utf-8` are fine), **413** when the body is longer than `limit` bytes (default 16 KiB), **400** when it is empty or not valid JSON |
| `validateNote(body)` | Returns `body.text` trimmed when it is a string of 1 to 280 characters; otherwise throws `HttpError` **422** |
| `createApp()` | Returns an `http.Server` (not listening) with the routes below. Each app has its own notes, in memory, with ids from 1 |

When the body is too large, `readJson` keeps reading and throws the bytes away, so the client
can still read the 413. (A public server would rather close the connection at once.)

## Routes

| Method and path | Answer |
| --- | --- |
| `GET /health` | **200** `{ "status": "ok" }` |
| `GET /notes` | **200** `{ "notes": [...] }` in id order. With `?q=text`, only notes whose text contains it, ignoring case |
| `POST /notes` | Body `{ "text": "..." }`. **201** with the new note `{ id, text }` and the header `Location: /notes/<id>` |
| `GET /notes/:id` | **200** with the note, **404** if there is none |
| `DELETE /notes/:id` | **204** with no body, **404** if there is none |
| Another method on one of these paths | **405** with the header `Allow`: `GET` for `/health`, `GET, POST` for `/notes`, `GET, DELETE` for `/notes/:id` |
| Any other path (including `/notes/abc`) | **404** |

Every answer except 204 is JSON sent with `sendJson`, and every error is `{ "error": "<message>" }`.

## Errors

Route with an `async` function and handle its errors **in one place**: the request listener
calls it with `.catch(...)`. An `HttpError` becomes its status, its headers and `{ error }`; any
other error is a bug, so log it and answer **500** `{ "error": "internal error" }` without the
details. Without that `.catch`, a rejected promise in a request handler is an unhandled
rejection, and Node.js stops the whole process.

Run the tests with `npm test` after `npm install`. The tests start the server on a free port
(`listen(0)`) and call it with `fetch`. There are 12 tests: 1 in `tests/visible.test.ts` and
11 hidden ones.
