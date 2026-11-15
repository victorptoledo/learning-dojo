# Performance and Troubleshooting

When a server is slow, the worst thing to do is guess. A short **method** and a dozen
standard commands find most problems in minutes.

## Load average

```text
$ uptime
 10:42:07 up 12 days,  3:01,  2 users,  load average: 7.92, 6.10, 2.35
```

The three numbers are the average number of tasks **running or waiting to run** (plus, on
Linux, tasks in uninterruptible sleep, usually waiting for disk) over 1, 5 and 15 minutes.
Compare them with the **number of CPUs** (`nproc`): 7.9 on 4 CPUs means about half the work
is waiting. Rising numbers (1 minute > 15 minutes) mean the problem started recently.

## The USE method

For every **resource** (CPUs, memory, disks, network interfaces), check:

| | Question | Example |
| --- | --- | --- |
| **U**tilisation | how busy is it? | CPU 95%, disk busy 100% of the time |
| **S**aturation | is work queueing? | run queue longer than the CPU count, swapping, disk queue |
| **E**rrors | is anything failing? | OOM kills, disk errors, dropped packets |

It turns "the server is slow" into a checklist you can finish, and it finds bottlenecks
that a single "CPU %" graph hides.

## The first 60 seconds

```bash
uptime                      # load: rising or falling?
journalctl -k -p warning --since "1 hour ago"   # or dmesg -T | tail: OOM kills, I/O errors, drops
vmstat 1 5                  # r = run queue, si/so = swap in/out, wa = CPU waiting for I/O
mpstat -P ALL 1 3           # per CPU: one core at 100% means a single-threaded bottleneck
pidstat 1 3                 # which processes use the CPU
iostat -xz 1 3              # per disk: %util, r_await/w_await (ms), aqu-sz (queue)
free -h                     # look at "available", not "free"
sar -n DEV 1 3              # network throughput per interface
sar -n TCP,ETCP 1 3         # TCP connections and retransmits
top                         # the overall picture, then press 1 (per CPU), M (by memory)
```

`mpstat`, `pidstat`, `iostat` and `sar` come with the **sysstat** package. Reading the
results:

- High load, high CPU **user/system** time, a long run queue: **CPU saturation**; find the
  process with `pidstat` or `top`.
- High load but CPUs mostly **idle with high `wa`**, processes in state `D`: they wait for
  **disk** (or network storage); `iostat -x` shows which device.
- `si`/`so` not zero for long, `available` memory near zero: **memory pressure**; the
  system is swapping or about to kill something.
- Nothing saturated locally but requests are slow: look **outside** (a slow database or
  API, DNS, packet loss: retransmits in `sar -n ETCP`).

## Memory and the OOM killer

```text
$ free -h
               total   used   free   shared  buff/cache  available
Mem:            15Gi   9.1Gi  310Mi  220Mi    6.2Gi       6.1Gi
```

Linux uses idle memory as **page cache** for files, so `free` is almost always low; the
number that matters is **available**. When memory runs out, the kernel's **OOM killer**
picks a process (usually the biggest) and kills it with `SIGKILL`:

```text
$ journalctl -k | grep -i 'out of memory'
Out of memory: Killed process 4121 (java) total-vm:9123456kB, anon-rss:7650000kB ...
```

In a container or a systemd unit with `MemoryMax=`, the limit is the **cgroup's**, so a
process can be killed while the host has free memory; that shows as exit status 137.

## Following one process

```bash
top -p 4121; ps -o pid,stat,rss,etime,cmd -p 4121
cat /proc/4121/status | grep -E 'State|VmRSS|Threads'
sudo strace -f -tt -p 4121          # every system call it makes, with times (ctrl+c stops)
sudo strace -c -p 4121              # a summary: which calls, how many, how long
sudo lsof -p 4121                   # its open files and sockets
sudo lsof -i :8080                  # who uses port 8080
ls /proc/4121/fd | wc -l            # how many file descriptors are open
```

`strace` answers "what is it doing right now?": waiting in `read()` on a socket, retrying
`connect()`, or looping on `open()` of a missing file. It slows the process down, so use it
briefly in production. For CPU profiling, `perf top` and `perf record` sample the whole
system with little overhead.

## Resource limits

Each process has **limits** (`ulimit -a`, `/proc/PID/limits`), the best known being the
maximum number of open files:

```text
java.io.IOException: Too many open files
```

means the process hit its `nofile` limit (often 1024 by default). Raise it where the
process is started: `LimitNOFILE=65536` in its systemd unit, `--ulimit` in Docker, or
`/etc/security/limits.conf` for login sessions. Then find out whether the program really
needs that many, or leaks connections.

Kernel-wide settings live in `/proc/sys` and are changed with **sysctl**
(`sysctl net.core.somaxconn`, persistent in `/etc/sysctl.d/*.conf`). Change them only with
a measurement that says you need to.

## Summary

- Load average counts runnable and waiting tasks; compare it with `nproc`.
- USE: utilisation, saturation and errors for every resource.
- First minute: `uptime`, kernel log, `vmstat`, `mpstat`, `pidstat`, `iostat -xz`, `free`, `sar`, `top`.
- Read `available` memory; OOM kills are in the kernel log; cgroup limits kill too (137).
- `strace`, `lsof` and `/proc/PID` show what one process is doing.
- "Too many open files" is a `nofile` limit: `LimitNOFILE=` in the unit.

## Further reading

- Brendan Gregg, "The USE Method": <https://www.brendangregg.com/usemethod.html>
- Brendan Gregg, "Linux Performance" (tools and the 60-second checklist): <https://www.brendangregg.com/linuxperf.html>
- `proc(5)` for `/proc/PID/status` and `limits`: <https://man7.org/linux/man-pages/man5/proc.5.html>
- `strace(1)`: <https://man7.org/linux/man-pages/man1/strace.1.html>
