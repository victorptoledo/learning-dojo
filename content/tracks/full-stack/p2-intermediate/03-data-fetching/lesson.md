# Data Fetching and Caching: HTTP Caches, Client Caches and Optimistic Updates

Between a user and your database there are several caches: the browser's HTTP cache, a CDN,
maybe a reverse proxy, the client's own memory cache, and your server's caches. Used well, they
make the app feel instant and cut your costs. Used badly, they show stale prices or, worse,
one user's data to another.

## HTTP caching in one table

The server controls shared and browser caches with `Cache-Control`:

| Response | Header | Why |
| --- | --- | --- |
| hashed static asset (`app.3f9a1c.js`) | `public, max-age=31536000, immutable` | the name changes when the content does |
| `index.html` of an SPA | `no-cache` | stored, but revalidated every time, so a deploy is seen at once |
| public product page or API | `public, max-age=60, s-maxage=300, stale-while-revalidate=30` | browsers keep 1 minute, the CDN 5, and can serve slightly stale while refreshing |
| per-user data (cart, profile) | `private, no-cache` or `no-store` | never in a shared cache |
| secrets (tokens, bank details) | `no-store` | not written anywhere |

`no-cache` does **not** mean "do not cache"; it means "revalidate before using". `no-store` means
do not store. And a response that varies by a header must say so (`Vary: Accept-Encoding`,
`Vary: Origin` for CORS), or a cache may serve the wrong variant.

## Validators: cheap revalidation

A validator lets a cache ask "has it changed?" without downloading again:

```http
GET /api/products/1                          →  200, ETag: "a1b2c3", Cache-Control: no-cache
GET /api/products/1  If-None-Match: "a1b2c3"  →  304 Not Modified (no body)
```

An `ETag` is usually a hash or a version of the representation; `Last-Modified` with
`If-Modified-Since` is the older, coarser variant. Frameworks can add ETags for you (Spring's
`ShallowEtagHeaderFilter`, ASP.NET Core output caching), but a hash computed after rendering saves
bandwidth, not server work; a version column lets you answer 304 before loading everything.

## Lost updates and If-Match

Two admins open the same product. Alice changes the price, Bob changes the name from his old
copy and saves: Alice's price is gone. **Conditional writes** stop that:

```http
PUT /api/products/1   If-Match: "a1b2c3"   →  200 with a new ETag, or
                                            →  412 Precondition Failed (someone changed it)
PUT /api/products/1   (no If-Match)         →  428 Precondition Required
```

On a 412 the UI shows what changed and lets the user decide. The same idea in the database is
**optimistic locking** with a version column (`@Version` in JPA). You will build both sides in the
next kata.

## Client caches for server state

In an SPA, data from the server is **server state**: shared, asynchronous and possibly stale.
Libraries such as **TanStack Query**, **SWR**, RTK Query and Apollo manage it:

```ts
const { data, isPending, error } = useQuery({
  queryKey: ["product", id],
  queryFn: () => api.getProduct(id),
  staleTime: 30_000,          // fresh for 30 s, then refetched in the background
});
```

- **Stale-while-revalidate**: show the cached data at once, refetch in the background, update.
- **Deduplication**: ten components asking for the same key make one request.
- **Refetch on focus or reconnect**, retries with backoff, cancellation when a component unmounts.
- **Invalidation after writes**: after `addToCart` succeeds, invalidate (or update) the `["cart"]`
  query. Forgetting this is the classic "I added it but the cart still shows 2" bug.

Keep server state there and UI state (open dialogs, form drafts) in components or a store; do not
copy one into the other.

## Optimistic updates

For actions that almost always succeed (a like, ticking a task), update the UI **before** the
server answers, and roll back if it fails:

```ts
onMutate: async () => {
  const previous = queryClient.getQueryData(["review", id]);
  queryClient.setQueryData(["review", id], (r) => ({ ...r, liked: true, likes: r.likes + 1 }));
  return { previous };
},
onError: (_error, _vars, context) => queryClient.setQueryData(["review", id], context.previous),
onSettled: () => queryClient.invalidateQueries({ queryKey: ["review", id] }),
```

Do not be optimistic about payments, stock reservations or anything the user must be sure of.

## Server-side caches

Behind the API, caches such as Redis cache expensive results (the System Design and Backend
tracks cover cache-aside, TTLs and stampedes). Two full stack rules: **key per user** whatever is
per user, and decide how each cache is **invalidated** when the data changes, before adding it.

## Summary

- Set `Cache-Control` per kind of response: immutable assets, revalidated HTML, short public
  TTLs, private or no-store for user data.
- ETags make revalidation cheap (304) and writes safe (If-Match, 412, 428).
- Client caches handle stale-while-revalidate, deduplication and refetching; invalidate after
  every mutation.
- Optimistic updates for low-risk actions, with rollback.

## Further reading

- MDN, "HTTP caching": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Caching>
- RFC 9111, "HTTP Caching": <https://www.rfc-editor.org/rfc/rfc9111>
- MDN, "HTTP conditional requests": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Conditional_requests>
- TanStack Query documentation, "Important Defaults": <https://tanstack.com/query/latest/docs/framework/react/guides/important-defaults>
- web.dev, "Love your cache": <https://web.dev/articles/love-your-cache>
- In the dojo: the React track's *Data Fetching, Races and Server State* and *State Management at Scale*, and the API Design track's *HTTP Caching and Conditional Requests*.
