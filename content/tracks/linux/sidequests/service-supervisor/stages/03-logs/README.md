A log nobody can read, or one that fills the disk, is worse than none. The supervisor now
owns the log, like `journald` does for systemd.

## Stamped lines and supervisor events

Every line of `NAME.log` starts with the time it was written, in UTC, then a space:

```text
2026-11-17T03:12:44Z svc: started (pid 4242)
2026-11-17T03:12:44Z listening on :8080
2026-11-17T03:14:02Z panic: out of memory
2026-11-17T03:14:02Z svc: exited (status 2)
2026-11-17T03:14:02Z svc: restarting in 1s
2026-11-17T03:14:03Z svc: started (pid 4300)
```

- The service's stdout and stderr lines get the stamp. A last line without a final newline is
  still written, as a line of its own: the event that follows never lands on it.
- The supervisor writes its own events in the same log, as lines starting with `svc: `:
  `started (pid PID)`, `exited (status S)`, `restarting in RESTARTSECs` (the value as written
  in the unit), `start limit hit`, `killed after TIMEOUTSTOPSECs` and `stopped`, in the order
  they happen. After `svc.sh stop` returns, `svc: stopped` is already in the log.

One process should write the log: the service's output goes through a pipe to a small
reader loop (`while IFS= read -r line`) in the supervisor, which stamps each line
(`printf '%(%Y-%m-%dT%H:%M:%SZ)T' -1` with `TZ=UTC`) and writes it. The supervisor's events
go through the same pipe, so lines never interleave in the middle.

## Rotation

Two new `[Service]` keys: `LogMaxBytes` (at least 1, default `1048576`) and `LogKeep`
(0 or more, default `3`). After writing a line that brings `NAME.log` to `LogMaxBytes` bytes
or more, the writer rotates: `NAME.log.(K-1)` becomes `NAME.log.K` ... `NAME.log` becomes
`NAME.log.1`, the file beyond `LogKeep` is deleted, and a new, empty `NAME.log` is opened.
With `LogKeep=0` the full log is simply deleted. So every file holds at most `LogMaxBytes`
plus one line, and no more than `LogKeep` old files exist.

## `svc.sh logs [-n N] UNIT`

Prints the last `N` lines (default `10`) of the log, old files included, oldest first: what
`journalctl -u NAME -n N` would show. A service that never logged prints nothing.
