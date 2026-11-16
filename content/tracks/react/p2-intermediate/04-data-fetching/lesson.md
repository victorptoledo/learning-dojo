# Data Fetching, Races and Server State

Most React apps show data from a server. React itself does not fetch; you combine `fetch`
with state and effects, or use a library or framework that does it for you. Knowing the
plain version shows you exactly what those tools solve.

## Fetching in an effect

```tsx
type State =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "success"; user: User };

function UserProfile({ id }: { id: string }) {
  const [state, setState] = useState<State>({ status: "loading" });

  useEffect(() => {
    const controller = new AbortController();
    setState({ status: "loading" });
    fetch(`/api/users/${id}`, { signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json() as Promise<User>;
      })
      .then((user) => setState({ status: "success", user }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;            // a newer request replaced this one
        setState({ status: "error", message: error instanceof Error ? error.message : "Failed" });
      });
    return () => controller.abort();
  }, [id]);

  if (state.status === "loading") return <p>Loading…</p>;
  if (state.status === "error") return <p role="alert">Could not load the user: {state.message}</p>;
  return <h1>{state.user.name}</h1>;
}
```

Things this code gets right:

- **A discriminated union** for the state: never `loading` and `error` at the same time.
- **`response.ok`**: `fetch` only rejects on network failures; a 404 or 500 is a resolved
  response you must check.
- **Cleanup with `AbortController`**: when `id` changes (or the component unmounts), the
  previous request is aborted.

## Race conditions

Without the cleanup, a user who clicks user 1 then user 2 quickly starts two requests. If the
first one is slower, it arrives **last** and overwrites user 2 with user 1. Aborting the old
request (or an `ignore` flag set in the cleanup) makes sure only the response for the current
`id` updates state. StrictMode's double mount in development makes you hit this case on every
page load, which is exactly the point.

## The problems that remain

Even correct effect-based fetching has costs:

- **Waterfalls.** A parent fetches, renders a child, and only then the child starts its own
  fetch. Start independent requests together (`Promise.all`), or load at the route level.
- **No cache.** Navigate away and back, and the data is fetched again with a spinner.
- **No deduplication.** Two components that need the same user make two requests.
- **No refresh.** Data goes stale while the tab stays open.
- **Fetch after render.** The request starts only after the component has rendered once.

## Server state is a cache

Data that lives on the server is not really *your* state: you hold a **copy** that can be out
of date. Treat it as a cache with keys, freshness and invalidation. That is what server-state
libraries do. With **TanStack Query**:

```tsx
const { data, error, isPending } = useQuery({
  queryKey: ["user", id],
  queryFn: ({ signal }) => fetchUser(id, signal),
  staleTime: 60_000,
});
```

The library caches by `queryKey`, deduplicates identical requests, retries failures, refetches
on window focus, cancels with the signal, and `useMutation` + `invalidateQueries` refresh the
cache after writes. SWR and RTK Query are similar.

## Loaders and Suspense

Frameworks move fetching **out of components**. React Router loaders and Next.js Server
Components start loading when the route is matched, before (or while) rendering, so there is
no fetch-after-render waterfall, and the data can be rendered on the server.

React's own primitives for this are **Suspense** and **`use`**: a component reads a promise
with `use(promise)`, and the nearest `<Suspense fallback>` shows a placeholder until it
resolves. The promise must be created *outside* the render (by a loader, a cache or a parent),
not inside the component, or every render would start a new request. You will build this in
the Mastery phase.

## Practical guidance

- In an app with a framework, use its data loading.
- In a client-only app, use TanStack Query (or similar) for server state.
- Write raw effect fetching only for small cases, and then always: union state, `response.ok`,
  abort in cleanup.
- Validate responses at the boundary (for example with zod) instead of `as User`.

## Key points

- Effect fetching needs loading/error/success states, `response.ok` checks and an abort in the
  cleanup to avoid races.
- Watch for waterfalls; start independent requests in parallel.
- Server state is a cache: libraries add keys, deduplication, retries and invalidation.
- Loaders and Suspense move fetching before render; `use` reads a promise created elsewhere.

## Further reading

- react.dev, "You Might Not Need an Effect", fetching data: <https://react.dev/learn/you-might-not-need-an-effect#fetching-data>
- react.dev, `useEffect`, fetching data with effects: <https://react.dev/reference/react/useEffect#fetching-data-with-effects>
- react.dev, `use`: <https://react.dev/reference/react/use>
- MDN, `AbortController`: <https://developer.mozilla.org/en-US/docs/Web/API/AbortController>
- TanStack Query, "Overview": <https://tanstack.com/query/latest/docs/framework/react/overview>
- React Router, "Data Loading": <https://reactrouter.com/start/framework/data-loading>
