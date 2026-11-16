# Debounced Search

## 1. `useDebouncedValue(value, delayMs)` (`useDebouncedValue.ts`)

Returns `value`, but only after it has stopped changing for `delayMs` milliseconds. On the
first render it returns `value` right away. Each change restarts the wait, so typing
`r`, `re`, `rea` quickly gives `"rea"` once, `delayMs` after the last key. Clean up the timer
when the value changes and when the component unmounts.

## 2. `useSearch(query, search, delayMs = 300)` (`useSearch.ts`)

Runs `search(text, signal)` for the **debounced, trimmed** query and returns a `SearchState`:

| State | When |
| --- | --- |
| `{ status: "idle" }` | the debounced query is blank: `search` is **not** called |
| `{ status: "loading" }` | a search for the current debounced query is running |
| `{ status: "success", results }` | it resolved |
| `{ status: "error", message }` | it rejected (`error.message`, or `"Search failed"` for a non-Error) |

Rules:

- When the debounced query changes, or the component unmounts, **abort** the running search
  (the `AbortSignal` you passed) and ignore whatever it returns later. An old query must never
  overwrite the state of a newer one.
- `search` may be a new function on every render (an inline arrow in the parent). That must
  **not** start a new search: only a change of the debounced query does. Always call the
  latest `search` the component received.

## 3. `SearchBox` (`SearchBox.tsx`)

`<SearchBox search={...} delayMs={...} />` renders an input labelled **Search** and, below it:

| State | Shows |
| --- | --- |
| idle | nothing |
| loading | `<p role="status">Searching…</p>` |
| success, no results | `<p>No results</p>` |
| success | `<ul aria-label="Results">` with one `<li>` per result |
| error | `<p role="alert">` with the message |

The tests use real timers with short delays and promises they resolve themselves. There are
11 tests: 1 in `tests/visible.test.tsx` and 10 hidden ones. Run them with `npm install` once,
then `npm test`.
