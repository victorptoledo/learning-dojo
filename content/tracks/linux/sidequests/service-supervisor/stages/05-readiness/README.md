A process that exists is not yet a server that answers: it may still be loading data or
opening its port. Until now `svc.sh start` said `started` as soon as the process existed;
whoever depends on the service (a load balancer, the next deploy step) needs "ready".

Two new `[Service]` keys:

- `ReadyCheck`: a command run by `bash -c` (with `MAINPID` set), whose success means ready,
  for example `ReadyCheck=curl -fs http://localhost:8080/health`;
- `ReadyTimeoutSec`: how long to wait for it (seconds, decimals allowed, default `10`).

With `ReadyCheck`, `svc.sh start` (and so `restart`) runs the check every 0.1 s after the
service starts and prints `started NAME (pid PID)` only once it succeeds. If it still fails
after `ReadyTimeoutSec`:

- `start` stops the service (its whole process group, as `stop` does);
- prints `svc.sh: NAME not ready after Ts` on stderr (`T` as written in the unit) and exits `1`;
- `status` then shows `NAME: failed (not ready)` and exits `3`.

Without `ReadyCheck`, nothing changes. This is what systemd's `Type=notify` and Kubernetes'
readiness probes are for.
