# Hello, Full Stack: a Guestbook

A guestbook has both halves of a web app: a server that keeps entries and answers HTTP, and a
client that calls it and turns the answer into HTML. Write both with Node.js's own `node:http`
and `fetch` (no framework).

## The server (`server.ts`)

`createGuestbookServer()` returns a `node:http` server (not listening yet) that keeps entries in
memory, oldest first, with ids `1`, `2`, `3`...

| Request | Answer |
| --- | --- |
| `GET /api/entries` | `200`, `Content-Type: application/json`, body `{"entries": [{"id": 1, "name": "Ada", "message": "Hi"}, ...]}` |
| `POST /api/entries` with a JSON body `{"name": "...", "message": "..."}` | `201`, `Location: /api/entries/<id>`, the new entry as JSON. Name and message are trimmed |
| `POST /api/entries` with a body that is not JSON, or a name or message that is missing, not a string or blank after trimming | `400`, JSON `{"error": "<what is wrong>"}`, and nothing is stored |
| `GET /` | `200`, `Content-Type: text/html; charset=utf-8`, a page whose body has `<ul id="entries">` with the entries inside (use `renderEntries`) |
| any other method on `/api/entries` | `405` with `Allow: GET, POST` |
| any other path | `404`, JSON `{"error": "not found"}` |

## The client (`client.ts`)

| Function | Does |
| --- | --- |
| `escapeHtml(text)` | Replaces `&`, `<`, `>`, `"` and `'` with `&amp;`, `&lt;`, `&gt;`, `&quot;` and `&#39;` |
| `renderEntries(entries)` | `<li><strong>NAME</strong>: MESSAGE</li>` per entry, joined with no separator, with name and message escaped |
| `fetchEntries(baseUrl)` | `GET baseUrl + "/api/entries"`, returns the entries array; throws an `Error` whose message has the status when the answer is not 2xx |
| `postEntry(baseUrl, input)` | `POST`s the input as JSON with `Content-Type: application/json` and returns the created entry; on a non-2xx answer throws an `Error` with the server's `error` text |

Both fetching functions take an optional last argument `fetchFn` (default: the global `fetch`),
so tests can pass their own.

```ts
const server = createGuestbookServer().listen(3000);
await postEntry("http://localhost:3000", { name: "Ada", message: "<b>hi</b>" });
renderEntries(await fetchEntries("http://localhost:3000"));
// '<li><strong>Ada</strong>: &lt;b&gt;hi&lt;/b&gt;</li>'
```

Try it: `npm start` serves the guestbook on port 3000; open <http://localhost:3000>.

## How the kata is checked

`tsc` type-checks every `.ts` file with strict settings, then the tests in `tests/` run with
`node:test`: they start your server on a free port of `127.0.0.1` and call it with `fetch`.
There are 12 tests: 2 in `tests/visible.test.ts` and 10 hidden ones. The functions that still
throw `new Error("todo ...")` are yours to write.
