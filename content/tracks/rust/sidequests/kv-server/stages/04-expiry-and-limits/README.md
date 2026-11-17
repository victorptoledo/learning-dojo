Keys that expire, and limits that keep one client from hurting the others.

| Command | Reply |
|---|---|
| `EXPIRE <key> <seconds>` | `OK`, or `NOT_FOUND`; the key disappears after that many seconds |
| `TTL <key>` | `VALUE <n>`: seconds left, rounded up; `VALUE -1` when it never expires; `NOT_FOUND` |

- `<seconds>` must be a positive integer, otherwise `ERROR not an integer`.
- An expired key behaves exactly like a missing one. `SET` on a key removes its expiry.
- Expiries do not have to survive a restart.

Limits:

- A line longer than 4096 bytes (without its `\n`) gets `ERROR line too long` and the
  server closes that connection.
- `--max-clients <n>` (default 64): a client that connects when `n` are already connected
  gets `ERROR too many clients` and is disconnected. When a client leaves, its place is free.

> Closing a socket that still holds unread input makes the system send a *reset*, and the
> client may then lose your error line. Shut down your side for writing first
> (`TcpStream::shutdown`), then read and drop what is left before closing.
