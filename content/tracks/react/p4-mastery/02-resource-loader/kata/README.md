# Resource Loader

## 1. The cache (`resource.ts`)

`createResourceCache(load)` returns `{ read, evict }`:

- `read(key)` calls `load(key)` the first time and returns **the same promise** for that key
  afterwards (`use` needs a stable promise);
- `evict(key)` forgets the key, so the next `read` calls `load` again.

## 2. The error boundary (`ErrorBoundary.tsx`)

`<ErrorBoundary fallback={(error, reset) => ...}>children</ErrorBoundary>` renders its children
until one of them throws while rendering (a rejected promise read with `use` counts). Then it
renders `fallback(error, reset)`; calling `reset()` renders the children again. A thrown
non-`Error` value is wrapped in `new Error(String(value))`. When the optional `resetKey` prop
changes (for example the id of what is shown), the boundary resets by itself.

## 3. The page (`ProfilePage.tsx`)

`UserCard({ userPromise })` reads the user with `use` and renders
`<article aria-label={user.name}>` with the name in an `<h2>` and the bio in a `<p>`.

`<ProfilePage ids={["ada", "linus"]} cache={cache} />` renders:

- a `<nav aria-label="Users">` with one button per id (its text is the id), the current one
  with `aria-pressed="true"`; the first id is current at the start;
- a `<section aria-label="Profile">` with `aria-busy="true"` while a switch is pending and
  `"false"` otherwise, containing the card of the current user.

Behaviour:

| Situation | Shows |
| --- | --- |
| first load of a user | `<p role="status">Loading profile…</p>` (a Suspense fallback) |
| loading failed | `<div role="alert">` with `Could not load <id>: <message>` and a **Try again** button |
| Try again | evicts that user from the cache and renders the page again (the card must get a fresh promise), so the request is made again |
| switching user | in a **transition**: the previous card stays visible, `aria-busy="true"`, no fallback, until the new card is ready |

A failure for one user must not stay on screen after switching to another user.

There are 9 tests: 1 in `tests/visible.test.ts` and 8 hidden ones. Rendering that suspends
must happen inside an awaited `act()` in React 19, so the hidden tests wrap renders and clicks
in `await act(async () => ...)`. Run them with
`npm install` once, then `npm test`.
