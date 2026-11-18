# Sidequest: Service supervisor

`systemd` starts your service, restarts it when it crashes, collects its logs and stops it
cleanly. Inside a container, on a machine without it, or just to understand what it does,
you build `svc.sh`, a small cousin of `systemd`, `runit` and `supervisord`, in five stages:

1. `start`, `status` and `stop` a service described by a unit file, under a supervisor that
   lives in the background and stops the service's whole process group;
2. restart policies (`Restart=on-failure`), the delay between restarts and a start limit, so a
   broken service does not restart forever;
3. a log the supervisor owns: every line stamped, its own events in it, rotated by size, and
   `svc.sh logs`;
4. `reload` with `ExecReload`, and `restart`;
5. a readiness check that makes `start` wait until the service really answers.

How it is checked:

- Your work is `svc.sh` at the top of the folder (bash, started as `bash svc.sh COMMAND UNIT`).
  You may add more `.sh` files next to it; every script must pass ShellCheck.
- The tests write small services (bash scripts) and their unit files in a temporary
  directory, set `SVC_STATE` to a temporary state folder, and check what `svc.sh` prints, its
  exit status, the log, and which processes are alive. The formats in each stage's statement
  are the contract; how you keep your state files is yours.
- Services run for real in the background, so the tests wait for things to happen (up to a
  few seconds) and kill everything left over after each test. A `svc.sh` that hangs is
  stopped after 20 seconds.
- The tests run as an ordinary user in Debian 13, without network and without systemd.

Read `man 5 systemd.service` and `man 5 systemd.kill` for the real keys this mimics. When
you are done, `[A]` copies a review prompt: ask an AI about your supervisor.
