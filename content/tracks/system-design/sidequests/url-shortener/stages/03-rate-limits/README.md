The service now faces the internet. A `Gateway` turns requests into responses and keeps
each client to its share with **token buckets**.

```java
public Gateway(Shortener shortener, Clock clock, Limits limits)
public Response post(String client, String url, String alias)   // alias may be null
public Response get(String client, String code)
public int trackedClients()
```

`Response(int status, String body, Map<String, String> headers)` and
`Limits(int writeBurst, Duration writeRefill, int readBurst, Duration readRefill)` are new in
the starter.

**Responses**

- `post`: `201` with the code as the body and the header `Location: /<code>`; `400` with the
  error message as the body for an invalid URL or alias; `409` for an alias that is taken.
- `get`: `302` with `Location: <the long URL>` and an empty body; `404` for an unknown code.
  (A `301` would be cached by browsers for good: deleted links would keep working and no
  click would reach you again.)

**Token buckets**

- Each client has a write bucket for `post` (up to `writeBurst` tokens, one more every
  `writeRefill`) and a read bucket for `get` (`readBurst`, `readRefill`). Refill is
  continuous: half a period gives half a token. A new client starts with full buckets.
- A request takes one token from its bucket, whatever its answer (`400`, `404` and `409`
  too: abuse with bad requests is still abuse). Without a whole token it is refused with
  `429`, an empty body and `Retry-After` set to the seconds until the next whole token,
  rounded up and at least 1. A refused request takes nothing.
- One client never uses the tokens of another.

**Memory**

A client whose buckets are full again is no different from one never seen, so its buckets
must not stay in memory forever: once enough time has passed to refill any bucket from
empty, the next request to the gateway forgets every client that is full by then.
`trackedClients()` says how many clients have buckets in memory. Forgetting a client that is
not full would hand it free tokens.

**Threads**

The gateway and the shortener behind it are called from many threads at once: a client
never gets more than its tokens, and every link created gets its own code.
