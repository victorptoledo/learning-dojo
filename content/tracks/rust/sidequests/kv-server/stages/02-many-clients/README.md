Serve many clients at the same time.

- A client that connects and sends nothing must not stop the others.
- Clients see each other's writes at once.
- A new command makes the shared state worth protecting:

| Command | Reply |
|---|---|
| `INCR <key>` | `VALUE <n>`: the integer value plus 1, stored back; a missing key counts as 0 |

  When the value is not an integer, the reply is `ERROR not an integer` and nothing changes.
  `INCR` is atomic: 8 clients sending 100 `INCR`s each to the same key end at exactly `800`.
