Make it pleasant to run.

| Command | Reply |
|---|---|
| `STATS` | `VALUE keys=<n> clients=<n> commands=<n>` |
| `SHUTDOWN` | `OK`, then the server stops |

- `keys` counts the keys that have not expired; `clients` the connected clients, the one
  asking included; `commands` every command the server has answered since it started, this
  `STATS` included.
- After `SHUTDOWN` replies, the server stops accepting connections, makes sure the data file
  is written, and the process **exits with code 0** within 2 seconds.

Configuration and errors, Unix style:

- When `--port` is not given, the port comes from the environment variable `KVD_PORT`.
- A bad command line (an unknown flag, a flag without its value, a port that is not a number,
  or no port at all) prints a line starting with `usage: kvd` on stderr and exits with code 2.
- A port that cannot be bound (already in use) prints `error: ...` on stderr and exits with
  code 1.
