The first commands:

| Command | Reply |
|---|---|
| `PING` | `PONG` |
| `SET <key> <value>` | `OK` |
| `GET <key>` | `VALUE <value>` or `NOT_FOUND` |
| `DEL <key>` | `DELETED` or `NOT_FOUND` |
| anything else | `ERROR unknown command` |

- A key is one word. The value is **the rest of the line** after the key and its space, so it
  can hold spaces: `SET greeting hello there` stores `hello there`.
- Missing parts (`GET`, `SET key`) reply `ERROR wrong number of arguments`.
- A client can send many commands on one connection. When it disconnects, the server keeps
  running and the data stays for the next client.

In this stage the tests use one connection at a time.
