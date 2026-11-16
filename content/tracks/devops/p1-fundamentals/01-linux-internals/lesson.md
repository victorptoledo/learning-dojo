# Linux for Operations: Processes, Services, Networking and Triage

Every container, Kubernetes node and cloud VM is a Linux machine. When something is slow at
3 a.m., the dashboards tell you *which* machine; the shell tells you *why*. This lesson is the
operator's subset of Linux. The **Linux** track teaches each topic in depth, with katas.

## Processes and signals

A process has a PID, a parent, an owner, a state and resource usage:

```bash
ps -eo pid,ppid,user,stat,%cpu,%mem,etime,cmd --sort=-%cpu | head
pstree -p 1234          # the tree under a process
ls -l /proc/1234/fd     # its open files and sockets
```

States worth knowing: `R` running, `S` sleeping, `D` uninterruptible sleep (usually waiting
on disk or NFS: many `D` processes mean an I/O problem), `Z` zombie (exited, its parent has not
collected it).

**Signals** tell a process to do something. `SIGTERM` (15) asks it to stop cleanly; `SIGKILL`
(9) stops it without cleanup and cannot be caught; `SIGHUP` (1) often means "reload your
configuration". Always try `kill -TERM` first: containers and Kubernetes do the same, then
`SIGKILL` after a grace period.

## systemd services and the journal

On almost every distribution, **systemd** is PID 1 and starts, supervises and restarts
services described by unit files:

```ini
# /etc/systemd/system/api.service
[Unit]
Description=Orders API
After=network-online.target
Wants=network-online.target

[Service]
User=api
ExecStart=/usr/local/bin/api --port 8080
Restart=on-failure
RestartSec=2
Environment=LOG_LEVEL=info
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

```bash
systemctl status api                     # state, PID, last log lines
sudo systemctl restart api
sudo systemctl daemon-reload             # after editing a unit file
journalctl -u api --since "15 min ago"   # the service's logs
journalctl -u api -f                     # follow
journalctl -p err -b                     # errors since boot
```

## Networking from the host

| Question | Command |
| --- | --- |
| what listens on which port, and which process? | `ss -tlnp` |
| how many connections, in which state? | `ss -s`, `ss -tan state time-wait \| wc -l` |
| does the service answer locally? | `curl -sv http://127.0.0.1:8080/health` |
| what does this name resolve to? | `dig +short api.internal`, `getent hosts api.internal` |
| is the path open? | `nc -vz db.internal 5432`, `traceroute`, `mtr` |
| my addresses and routes | `ip -br addr`, `ip route` |

`getent` uses the same resolver path as applications (`/etc/hosts`, then DNS via
`/etc/resolv.conf` or systemd-resolved), so it can disagree with `dig`, which only asks DNS.
Many "network" incidents are DNS incidents.

## Disks and file systems

```bash
df -h            # space per file system
df -i            # inodes: a disk can be "full" with free space left
du -xh /var | sort -h | tail
lsof +L1         # deleted files still held open (space not freed until the process closes them)
iostat -xz 1     # per-device utilisation and latency (sysstat package)
```

A log file deleted while the process still writes to it keeps using space: restart or signal
the process, or truncate (`: > file`) instead of deleting.

## Memory

`free -h` shows that Linux uses spare memory for the page cache: look at **available**, not
"free". When memory runs out, the kernel's **OOM killer** stops a process; it leaves a trace:

```bash
dmesg -T | grep -i -E 'killed process|out of memory'
journalctl -k | grep -i oom
```

In containers the limit is the cgroup's, not the host's (`memory.max`), and the exit code of
an OOM-killed container is 137 (128 + 9, SIGKILL).

## The first five minutes on a sick host

Work from the whole machine down to one process, so you do not tunnel into the wrong problem:

```bash
uptime                 # load averages: 1, 5, 15 minutes, compared with the CPU count (nproc)
dmesg -T | tail        # kernel errors: OOM kills, disk errors, dropped packets
vmstat 1 5             # r (run queue), si/so (swapping), wa (I/O wait)
free -h; df -h         # memory, disk space
top or htop            # who uses CPU and memory
ss -s; ss -tlnp        # sockets and listeners
journalctl -p err --since "30 min ago"
```

Brendan Gregg's **USE method** gives the questions: for every resource (CPU, memory, disk,
network), check its **U**tilisation, **S**aturation (queueing) and **E**rrors.

## Summary

- Processes: `ps`, `/proc`, states (`D` means waiting on I/O), SIGTERM before SIGKILL.
- systemd runs services from unit files; `systemctl` controls them, `journalctl -u` reads logs.
- `ss`, `curl`, `dig` and `getent` answer most network questions; DNS is a common culprit.
- Watch disk space and inodes, deleted-but-open files, available memory and OOM kills.
- Triage from the whole host down: load, kernel log, memory, disk, network, service logs.

## Further reading

- Brendan Gregg, "Linux Performance Analysis in 60,000 Milliseconds": <https://netflixtechblog.com/linux-performance-analysis-in-60-000-milliseconds-accc10403c55>
- The USE method: <https://www.brendangregg.com/usemethod.html>
- systemd service units: <https://www.freedesktop.org/software/systemd/man/latest/systemd.service.html>
- `ss(8)` and `journalctl(1)` man pages: <https://man7.org/linux/man-pages/>
- *How Linux Works*, 3rd edition, Brian Ward (No Starch Press, 2021)
