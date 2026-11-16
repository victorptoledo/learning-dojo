# Browser Storage, URL State and Client-Side State

A page forgets everything on reload unless you keep it somewhere. The browser offers several
places, each with its own size, lifetime and risks, and choosing **where each piece of state
lives** is one of the main design decisions of any frontend.

## Where state can live

| Place | Lifetime | Size | Shared with | Good for |
| --- | --- | --- | --- | --- |
| A variable in memory | Until reload | RAM | This tab | UI state: open menus, current input |
| The **URL** (path, `?query`, `#hash`) | As long as the link | ~2 KB safe | Anyone with the link | Filters, search terms, the selected tab, pagination |
| `sessionStorage` | Until the tab closes | ~5 MB | This tab only | A wizard's progress |
| `localStorage` | Until cleared | ~5 MB per origin | Every tab of the origin | Drafts, preferences, the last query |
| **IndexedDB** | Until cleared (can be evicted) | Large (a share of the disk) | Every tab of the origin | Offline data, big caches, files |
| **Cache API** | Until cleared | Large | Every tab, service workers | Offline copies of responses |
| **Cookies** | Set by `Expires`/`Max-Age` | ~4 KB each | Sent to the server on every request | Sessions (HttpOnly), server-side needs |
| The server | Permanent | Any | Every device of the user | Anything that must survive a new device |

## Web Storage: localStorage and sessionStorage

```js
localStorage.setItem("last-query", "dune");          // strings only
const query = localStorage.getItem("last-query");    // null when missing
localStorage.setItem("draft", JSON.stringify(values));
const draft = JSON.parse(localStorage.getItem("draft") ?? "null");
localStorage.removeItem("draft");
window.addEventListener("storage", (event) => { /* another tab changed event.key */ });
```

- Only **strings**: serialize with JSON and parse defensively (`try/catch`), because stored
  data can be old, edited or broken.
- **Synchronous**: every call blocks the main thread; fine for small values, not for megabytes.
- It can **throw**: quota exceeded, storage disabled, some private modes. Wrap writes in
  `try/catch` and keep the page working without it.
- **Never store secrets** (tokens, personal data you do not need): any script on the origin,
  including an injected one, can read it. Session tokens belong in `HttpOnly` cookies.
- Version what you store (`{ version: 2, ... }`) so a new release can migrate or drop old data.

## IndexedDB in brief

An asynchronous, transactional database of JavaScript objects with indexes, available to pages,
workers and service workers. Its raw API is verbose; small wrappers such as `idb` make it
pleasant. Use it for offline-first apps, large lists and binary data. Browsers may evict it
under storage pressure unless you ask for persistence with `navigator.storage.persist()`.

## The URL is state too

```js
const params = new URLSearchParams(location.search);
const q = params.get("q") ?? "";
const url = new URL(location.href);
url.searchParams.set("q", "dune");
history.replaceState(null, "", url);   // update the address bar without a reload or a new entry
history.pushState(null, "", url);      // a new history entry: Back returns to the old state
window.addEventListener("popstate", render); // Back and Forward
```

State in the URL can be bookmarked, shared and restored, and Back works. Put there what a
user would want to send to a colleague: the search, the filters, the page, the selected item.
Use `replaceState` for every keystroke and `pushState` for real navigation steps.

## Designing client-side state

- **Single source of truth.** Each piece of state lives in one place; everything else is
  derived. Do not keep a `count` variable next to the list it counts: compute it.
- **Server state vs UI state.** Data from the server (products, orders) is a **cache** of
  something you do not own: it can be stale, it needs loading and error states, refetching and
  invalidation. UI state (open, selected, typed) is yours. Libraries split them the same way
  (TanStack Query for server state; framework state or a small store for UI state).
- **Immutable updates.** Replace objects instead of mutating them
  (`state = { ...state, items: [...state.items, item] }`): changes become easy to detect, undo
  and debug.
- **Render from state.** `render(state)` rebuilds the view from data, rather than patching
  the DOM in a dozen places; frameworks automate exactly that.
- **Persist on purpose.** Choose for each value: memory, URL, storage or server, and what
  happens when the stored version is old.

## Privacy and limits

Storage is per **origin** and, in modern browsers, partitioned by the top-level site for
third-party frames. Users and browsers clear it; Safari deletes script-writable storage of
sites not visited for seven days. Treat everything client-side as a cache that can disappear.

## Summary

- Memory, URL, Web Storage, IndexedDB, Cache API, cookies and the server each have a job.
- Web Storage holds small strings synchronously: parse defensively, catch errors, no secrets.
- Shareable state goes in the URL with `URLSearchParams` and `history.replaceState`.
- Keep one source of truth, separate server state from UI state, and update immutably.
- Anything stored in the browser can vanish: design for it.

## Further reading

- MDN, "Web Storage API": <https://developer.mozilla.org/en-US/docs/Web/API/Web_Storage_API>
- MDN, "Using IndexedDB": <https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API/Using_IndexedDB>
- MDN, "Storage quotas and eviction criteria": <https://developer.mozilla.org/en-US/docs/Web/API/Storage_API/Storage_quotas_and_eviction_criteria>
- MDN, "History API": <https://developer.mozilla.org/en-US/docs/Web/API/History_API>
- WHATWG HTML, "Web storage": <https://html.spec.whatwg.org/multipage/webstorage.html>
- web.dev, "Storage for the web": <https://web.dev/articles/storage-for-the-web>
