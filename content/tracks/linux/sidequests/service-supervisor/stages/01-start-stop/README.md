A unit file describes the service. This stage knows two keys:

```ini
# web.service
[Unit]
Description=The web app

[Service]
ExecStart=python3 -m http.server 8080
TimeoutStopSec=3
```

- Lines are `KEY=VALUE` inside a `[Unit]` or `[Service]` section. Empty lines and comments
  (`#` or `;`) are ignored, and so is a `\r` at the end of a line (a unit saved on Windows).
- `[Unit]` knows `Description` (ignored). `[Service]` knows `ExecStart`, the command, run by
  `bash -c` (required), and `TimeoutStopSec` (seconds, decimals allowed, default `5`).
- The service's name is the file name without `.service`.

## `svc.sh start UNIT`

Starts a **supervisor** in the background and returns at once with
`started NAME (pid PID)`, where `PID` is the service's main process: the one `ExecStart`
runs. The supervisor starts the service and stays with it until it ends. It must outlive
`svc.sh start`, the terminal and the test that started it: give it a session of its own
(`setsid`) and point its stdin, stdout and stderr away (`< /dev/null > /dev/null 2>&1`),
otherwise whoever reads `svc.sh start`'s output waits forever.

The service's stdout and stderr are appended to `NAME.log` in the **state folder**,
`$SVC_STATE` (default `~/.local/state/svc`), created if needed. Keep your other files (pids,
status) there too, as you like.

Starting a running service prints `NAME is already running (pid PID)` and changes nothing.

## `svc.sh status UNIT`

The first line is one of:

| State | Line | Exit |
| --- | --- | --- |
| running | `NAME: active (running), pid PID` | `0` |
| never started | `NAME: inactive` | `3` |
| ended by itself with status `S` | `NAME: inactive (exited S)` | `3` |
| stopped by `svc.sh stop` | `NAME: inactive (stopped)` | `3` |

## `svc.sh stop UNIT`

Asks the supervisor to stop the service and waits until it is gone, then prints
`stopped NAME`. The supervisor sends `SIGTERM` to the service's **process group** (start it
with `setsid` too, so it has one and `kill -- -PID` reaches every process it started), and
`SIGKILL` to whatever is left `TimeoutStopSec` seconds later. Not running: `NAME is not
running`, exit `0`.

## Errors

Exit `2`, message on stderr: `svc.sh: cannot read UNIT`, `svc.sh: UNIT: not a .service file`,
`svc.sh: UNIT: missing ExecStart`, and, with the line number, `svc.sh: UNIT:LINE: unknown
section [X]`, `svc.sh: UNIT:LINE: unknown key KEY`, `svc.sh: UNIT:LINE: expected KEY=VALUE`,
`svc.sh: UNIT:LINE: bad value for KEY: VALUE`. Wrong arguments print
`usage: svc.sh start|stop|restart|status|reload|logs [-n N] UNIT` (the other commands come
later).
