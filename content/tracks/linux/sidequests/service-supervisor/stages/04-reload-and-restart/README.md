Two things an operator does every day: reload a configuration without a restart, and restart
after an upgrade.

## `svc.sh reload UNIT`

A new `[Service]` key, `ExecReload`, is a command run by `bash -c` with the service's main
pid in the environment variable `MAINPID`. The classic one is `ExecReload=kill -HUP $MAINPID`:
the service catches `SIGHUP` and reads its configuration again, keeping its pid and its
connections. `reload` runs it and prints `reloaded NAME`. Errors, exit `1`:

- `svc.sh: NAME has no ExecReload`;
- `svc.sh: NAME is not running`;
- `svc.sh: reload of NAME failed (status S)` when the command exits with `S`.

## `svc.sh restart UNIT`

`stop` (printing `stopped NAME`) if the service runs, then `start` (printing
`started NAME (pid PID)`): a new main process, and the old one gone.

The tests reuse everything from the earlier stages, so `reload` and `restart` must work with
services that log through the supervisor and restart themselves.
