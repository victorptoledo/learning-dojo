# fetch, async Code and Cancellation in the Browser

A page talks to servers with `fetch`. Network calls take tens to thousands of milliseconds and
can fail in many ways, so they are **asynchronous**: the page keeps reacting while it waits.
Getting the waiting, the errors and the cancellation right is most of the work.

## The event loop in one paragraph

The browser runs your JavaScript on **one main thread**, together with layout, painting and
input handling. Each task (a script, an event handler, a timer callback) runs to completion;
then **microtasks** (promise callbacks) run; then the browser may render a frame. A long task
blocks everything, including scrolling and typing. `await` does not block: it ends the current
task and resumes your function later, when the promise settles.

## fetch

```js
const response = await fetch("/api/search?q=dune", {
  headers: { accept: "application/json" },
});
if (!response.ok) {                         // fetch only rejects on network failures
  throw new Error(`HTTP ${response.status}`);
}
const books = await response.json();       // also .text(), .blob(), .formData()
```

- `fetch` **resolves** for 404 and 500: check `response.ok` (status 200–299) yourself.
- It **rejects** with a `TypeError` for network errors, DNS failures, CORS refusals, and with
  an `AbortError` when aborted.
- Build query strings with `URLSearchParams`, never by gluing strings:
  `` `/api/search?${new URLSearchParams({ q: "c & c++" })}` `` gives `q=c+%26+c%2B%2B`.
- Sending JSON: `method: "POST"`, a `content-type: application/json` header and
  `body: JSON.stringify(data)`.
- Cookies are sent automatically to the same origin (`credentials: "same-origin"`, the
  default); cross-origin requests follow CORS (Phase 3).

## Promises and async/await

```js
async function loadAll() {
  const [user, orders] = await Promise.all([getUser(), getOrders()]); // in parallel
  return { user, orders };
}
const settled = await Promise.allSettled(requests);  // never rejects; a status per promise
const fastest = await Promise.race([fetch(a), timeout(3000)]);
const first = await Promise.any([mirror1(), mirror2()]); // first success
```

- `await` in a loop runs requests **one after another**; collect the promises and
  `Promise.all` them to run in parallel.
- Always handle rejections (`try/catch` around `await`, or `.catch`): an unhandled one shows
  up as an error in the console and the user sees nothing.

## Cancellation with AbortController

```js
const controller = new AbortController();
fetch(url, { signal: controller.signal })
  .catch((error) => { if (error.name !== "AbortError") throw error; });
controller.abort();                                  // cancels the request
fetch(url, { signal: AbortSignal.timeout(5000) });   // a deadline
fetch(url, { signal: AbortSignal.any([controller.signal, AbortSignal.timeout(5000)]) });
```

Abort what the user no longer needs: the search for "du" once they typed "dune", the page
data when they navigate away.

## Race conditions: stale responses

Responses do not come back in the order the requests left. If the request for "du" is slow and
the one for "dune" fast, a naive handler shows "du" last, over the right answer. Two fixes,
best combined:

1. Abort the previous request when starting a new one.
2. Ignore results that are not from the latest request (compare a counter or the controller).

## Debounce and throttle

```js
function debounce(fn, ms) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
}
input.addEventListener("input", debounce(() => search(input.value), 300));
```

**Debounce** waits until the calls stop (search as you type); **throttle** runs at most once
per interval (scroll and resize handlers). Both protect the server and the main thread.

## Loading, empty and error states

Every asynchronous view has at least four states: **idle**, **loading**, **success** (maybe
**empty**) and **error**. Design each one: a "Searching…" message in a `role="status"`
region, `aria-busy="true"` on the region being replaced, "No results", and an error that
says what to do ("Search failed. Try again."). Keep the previous content or clear it, but on
purpose.

## Caching

The HTTP cache (`Cache-Control`, `ETag`) handles a lot for `GET` requests. On top of it, an
in-memory `Map` from query to result avoids repeat requests in one session; libraries such as
TanStack Query add expiry, background refresh and deduplication.

## Summary

- One main thread: `await` frees it while waiting; long synchronous work blocks it.
- `fetch` resolves on HTTP errors: check `response.ok`; it rejects on network errors and aborts.
- Use `URLSearchParams` and JSON with an explicit `content-type`.
- Abort what is no longer wanted and never let a stale response overwrite a newer one.
- Debounce input, and design loading, empty and error states with live regions.

## Further reading

- MDN, "Using the Fetch API": <https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch>
- MDN, `AbortController`: <https://developer.mozilla.org/en-US/docs/Web/API/AbortController>
- MDN, "Using promises": <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Using_promises>
- MDN, "The event loop": <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Execution_model>
- WHATWG, Fetch Standard: <https://fetch.spec.whatwg.org/>
- WHATWG, URL Standard, `URLSearchParams`: <https://url.spec.whatwg.org/#urlsearchparams>
- web.dev, "Learn JavaScript: asynchronous JavaScript": <https://web.dev/learn/javascript>
