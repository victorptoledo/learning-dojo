# Live Search: Debounce, Abort and Cache

`index.html` and `style.css` are done. Write **`app.js`**: search a library catalog as the user
types. The tests run a fake API on the same origin, `GET /api/search?q=...`, which answers with
JSON `[{ id, title, author }]` (status 500 when the search fails). They also control the page's
clock, so the timing checks are exact.

| Code | What it does |
| --- | --- |
| `debounce(fn, ms)` | Returns a function that calls `fn` with the **last** arguments, `ms` after the **last** call |
| `searchUrl(query)` | `"/api/search?q=..."`, encoded with `URLSearchParams` (`"c & c++"` → `q=c+%26+c%2B%2B`) |
| `search(query, { signal })` | `fetch`es `searchUrl(query)` with the signal and returns the parsed JSON. `fetch` does not reject on 404 or 500: check `response.ok` and throw an `Error` whose message holds the status |
| `resultLabel(n)` | `"No results"`, `"1 result"`, `"2 results"` |
| `runSearch(query)` | One search, described below |

`runSearch(query)`, with the query trimmed:

1. **Abort** the search still running, if any (keep its `AbortController`).
2. Put the query in the URL (`?q=dune`, with `history.replaceState`) and in `localStorage`
   under `"last-query"`.
3. Blank query: empty `#results` and `#status`, and send **no request**.
4. Already answered: show the remembered books (a `Map` from query to books) without a request.
5. Otherwise set `aria-busy="true"` on `#results` and `"Searching…"` in `#status`, then fetch.
   - Success: one `<li>` per book, `"Title — Author"` (with an em dash), set with `textContent`;
     `#status` is `resultLabel(count)`; remember the answer.
   - Aborted (`error.name === "AbortError"`): change nothing. A newer search owns the screen.
   - Any other error: empty `#results`, `#status` is `"Search failed. Try again."`.
   - Either way, set `aria-busy="false"` when the search that is still current ends.

Wiring: every `input` event calls a **debounced** `runSearch(input.value)` with a 300 ms delay
(`DELAY_MS`). On load, if the URL has `?q=`, or else `localStorage` has `"last-query"`, fill the
field and search for it at once.

To try it in your own browser, the `serve` command has no API, so the search will fail; the
tests are the way to check it:

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```

There are 16 tests: 1 in `tests/visible.test.js` and 15 hidden ones.
