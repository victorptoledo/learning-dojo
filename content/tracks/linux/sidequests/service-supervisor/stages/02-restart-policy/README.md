A supervisor earns its name when the service crashes at 3 a.m. New `[Service]` keys:

| Key | Values | Default |
| --- | --- | --- |
| `Restart` | `no`, `on-failure` (a non-zero exit status or a signal), `always` | `no` |
| `RestartSec` | seconds to wait before restarting, decimals allowed | `1` |
| `StartLimitBurst` | starts allowed within the interval (at least 1) | `5` |
| `StartLimitIntervalSec` | the interval, in seconds | `10` |

- When the service ends by itself and `Restart` says so, the supervisor waits `RestartSec`
  and starts it again (a new main pid). A service stopped by `svc.sh stop` is never restarted.
- **Start limit.** When a restart is due, if `StartLimitBurst` starts already happened in
  the last `StartLimitIntervalSec` seconds, the supervisor gives up instead: a service that
  crashes at once is not restarted forever. With `StartLimitBurst=3`, the service runs three
  times (two restarts).
- `svc.sh start` on a service that gave up starts it again, with a fresh count.

`status` gains states and a second line, `restarts: N` (how many times the supervisor
restarted the service since `svc.sh start`; `0` when it never did), in every state:

| State | First line | Exit |
| --- | --- | --- |
| waiting `RestartSec` before a restart | `NAME: activating (auto-restart)` | `3` |
| gave up | `NAME: failed (start limit hit)` | `3` |

```text
$ bash svc.sh status worker.service
worker: active (running), pid 4242
restarts: 2
```

A `svc.sh stop` while the supervisor waits to restart stops it at once: the wait must not
block the signal (`sleep N & wait $!`).
