# Processes, Jobs and Signals

A **process** is a running program: its memory, open files, current directory, user and
environment, identified by a **PID**. Everything you run, from `ls` to a database, is one.

## fork and exec

A new process is born when an existing one calls **`fork()`**: the kernel makes a copy with
a new PID. The copy usually calls **`execve()`**, which replaces its program with another
one. That is what the shell does for every command:

```text
bash (PID 1200) --fork--> bash (PID 1300) --exec--> ls (PID 1300)
```

So every process has a **parent** (PPID). PID 1 is the first process started by the kernel,
**systemd** on Debian (in a container, the container's main command). When a parent dies,
its children are adopted by PID 1 (or the nearest "subreaper").

When a process ends, its exit status waits in the process table until the parent reads it
with `wait()`. Until then it is a **zombie** (state `Z`): it uses no memory, just a slot.
Many zombies mean a parent that never waits.

## Seeing processes

```bash
ps aux                     # every process: user, PID, %CPU, %MEM, state, command
ps -ef --forest            # with parent PIDs, as a tree
ps -o pid,ppid,stat,etime,cmd -p 1300
pgrep -a nginx             # PIDs and command lines of matching processes
pstree -p                  # the whole tree
top                        # live view: P sorts by CPU, M by memory, k kills, q quits
```

The `STAT` column: `R` running, `S` sleeping (waiting for something), `D` uninterruptible
sleep (usually waiting for disk or network storage), `Z` zombie, `T` stopped.

Each process is also a directory in `/proc`:

```bash
cat /proc/1300/status      # name, state, PPID, memory (VmRSS), threads
ls -l /proc/1300/fd        # its open files
tr '\0' ' ' < /proc/1300/cmdline; echo
cat /proc/1300/environ | tr '\0' '\n'
```

## Jobs: foreground and background

```bash
sleep 300 &        # run in the background; the shell prints [1] 1400
jobs               # list this shell's jobs
fg %1              # bring job 1 to the foreground
# Ctrl+Z           # stop (pause) the foreground job
bg %1              # continue it in the background
wait               # wait for all background jobs (in scripts)
```

Background jobs still die with your terminal session (they get `SIGHUP`). For something
that must survive logging out, use `nohup command &`, a terminal multiplexer such as `tmux`,
or better, a **systemd service** (next lessons).

## Priorities

The scheduler shares the CPUs between runnable processes. **nice** values from -20 (most
favoured) to 19 (least) change the share:

```bash
nice -n 10 tar -czf backup.tgz /srv     # start with lower priority
renice -n 5 -p 1300                     # change a running process
ionice -c3 rsync -a /data /backup       # idle disk priority
```

Only root may raise a priority (lower the nice value).

## Signals

A **signal** is a small asynchronous message from the kernel or another process. Each has a
default action, and a process may **catch** most of them.

| Signal | Number | Sent by / when | Default |
| --- | --- | --- | --- |
| `SIGINT` | 2 | Ctrl+C | terminate |
| `SIGTERM` | 15 | `kill PID`, systemd, Docker, Kubernetes: "please stop" | terminate |
| `SIGKILL` | 9 | `kill -9 PID` | terminate, **cannot be caught** |
| `SIGHUP` | 1 | terminal closed; many daemons: "reload your config" | terminate |
| `SIGSTOP` / `SIGCONT` | 19 / 18 | pause / resume (`SIGSTOP` cannot be caught) | stop / continue |
| `SIGCHLD` | 17 | a child ended | ignore |
| `SIGSEGV` | 11 | invalid memory access | terminate with a core dump |

```bash
kill 1300            # SIGTERM
kill -HUP 1300       # ask nginx (for example) to reload
pkill -f 'worker.sh' # by command line
kill -0 1300         # sends nothing: checks that the process exists and you may signal it
```

> [!WARNING]
> `kill -9` gives the process no chance to finish writes, release locks or delete
> temporary files. Send `SIGTERM`, wait a few seconds, and only then `SIGKILL`. systemd,
> Docker and Kubernetes all do exactly that: TERM, a grace period, then KILL.

A process killed by a signal exits with status **128 + the signal number**: 130 for Ctrl+C,
143 for SIGTERM, 137 for SIGKILL (which is what an out-of-memory kill in a container shows).

## trap: handling signals in bash

```bash
#!/usr/bin/env bash
stop=false
trap 'stop=true' TERM INT              # remember the request
trap 'rm -f "$pidfile"' EXIT           # always clean up

echo "$$" > "$pidfile"
while ! "$stop"; do
    process_next_job
    sleep 5 & wait $!                  # wait returns at once when a signal arrives
done
echo "stopping"
```

Bash runs a trap only **between** commands: while a foreground `sleep 5` runs, the TERM
waits until `sleep` finishes. Running `sleep` in the background and `wait`ing for it lets the
trap run immediately. Set a flag in the trap and stop at a safe point instead of exiting in
the middle of a job.

One more catch: a script started with `&` from **another script** ignores `SIGINT` and
`SIGQUIT`, and a signal ignored at start cannot be trapped. Stop background workers with
`SIGTERM`, which is what service managers send anyway.

## Summary

- `fork` copies a process, `exec` replaces its program; every process has a parent, PID 1 adopts orphans.
- Zombies are finished children nobody `wait`ed for.
- `ps aux`, `pgrep -a`, `top`, and `/proc/PID/` show what is running and why.
- `&`, `jobs`, `fg`, `bg`, `wait`; `nohup` or a service for things that must outlive you.
- `SIGTERM` asks, `SIGKILL` forces; exit status 128 + N; `trap` lets scripts stop cleanly.

## Further reading

- `signal(7)`: <https://man7.org/linux/man-pages/man7/signal.7.html>
- `proc(5)`: <https://man7.org/linux/man-pages/man5/proc.5.html>
- GNU Bash manual, "Job Control" and "Signals": <https://www.gnu.org/software/bash/manual/bash.html>
- BashFAQ on signals and traps: <https://mywiki.wooledge.org/SignalTrap>
