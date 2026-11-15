# systemd: Daemons, Unit Files and journalctl

**systemd** is PID 1 on Debian, Ubuntu, Fedora and RHEL. It starts the system, supervises
services (restarts them, collects their output, limits their resources) and replaces several
older tools: init scripts, much of cron, syslog for most uses.

## Units and systemctl

Everything systemd manages is a **unit**: `nginx.service`, `ssh.socket`, `backup.timer`,
`data.mount`, `multi-user.target` (a group of units, like a runlevel).

```bash
systemctl status nginx              # state, PID, memory, the last log lines
sudo systemctl start nginx          # start now
sudo systemctl stop nginx
sudo systemctl restart nginx        # stop + start
sudo systemctl reload nginx         # re-read the config without stopping (if supported)
sudo systemctl enable nginx         # start at boot (enable --now: and start now)
sudo systemctl disable nginx
systemctl is-active nginx; systemctl is-enabled nginx
systemctl --failed                  # what is broken
systemctl list-units --type=service --state=running
systemctl cat nginx                 # the unit file(s) in effect
```

`enable` and `start` are separate on purpose: one is about boot, the other about now.

## Writing a service

Your own units go in **`/etc/systemd/system/`** (packages put theirs in
`/usr/lib/systemd/system/`; never edit those, override them).

```ini
# /etc/systemd/system/orders.service
[Unit]
Description=Orders API
After=network-online.target
Wants=network-online.target

[Service]
User=orders
Group=orders
WorkingDirectory=/opt/orders
EnvironmentFile=/etc/orders/orders.env
ExecStart=/usr/bin/java -Xmx512m -jar /opt/orders/orders.jar
Restart=on-failure
RestartSec=5
TimeoutStopSec=30

[Install]
WantedBy=multi-user.target
```

```bash
sudo useradd --system --no-create-home --shell /usr/sbin/nologin orders
sudo systemctl daemon-reload          # after creating or changing a unit file
sudo systemctl enable --now orders
```

Key directives:

| Directive | Means |
| --- | --- |
| `ExecStart=` | the command, with a **full path**; no shell, so no pipes or `&` (wrap in `bash -c` if you must) |
| `Type=` | `simple` (default: running = started), `exec`, `notify` (the program tells systemd it is ready), `oneshot` (runs and ends) |
| `User=`, `Group=` | run as a dedicated user, never root without a reason |
| `Environment=`, `EnvironmentFile=` | variables; keep secrets in a file readable only by root (or use `LoadCredential=`) |
| `Restart=` | `on-failure` restarts after a crash or a non-zero exit; `always` also after a clean exit |
| `RestartSec=`, `StartLimitBurst=`, `StartLimitIntervalSec=` | wait between restarts, and give up after too many in a period |
| `TimeoutStopSec=` | how long to wait after **SIGTERM** before **SIGKILL** |

The program should run in the **foreground** and log to **stdout/stderr**: systemd is the
one that daemonises and collects the logs. That is why a worker that stops cleanly on
SIGTERM (the last kata) is exactly what systemd needs.

To change a packaged unit, use a **drop-in**: `sudo systemctl edit nginx` creates
`/etc/systemd/system/nginx.service.d/override.conf` with only your changes.

## The journal

```bash
journalctl -u orders                  # logs of one unit
journalctl -u orders -f               # follow, like tail -f
journalctl -u orders --since "1 hour ago" -p warning   # since a time, warnings and worse
journalctl -b -p err                  # errors since this boot
journalctl -k                         # kernel messages (like dmesg)
journalctl --disk-usage; sudo journalctl --vacuum-time=14d
```

Debian 13 keeps the journal on disk in `/var/log/journal`. Traditional text logs (such as
`/var/log/nginx/`) are still written by programs that do their own logging.

## Timers instead of cron

```ini
# /etc/systemd/system/backup.service
[Service]
Type=oneshot
ExecStart=/usr/local/bin/backup.sh -k 7 /srv/data /srv/backups

# /etc/systemd/system/backup.timer
[Timer]
OnCalendar=*-*-* 02:30:00
Persistent=true
RandomizedDelaySec=10m

[Install]
WantedBy=timers.target
```

`sudo systemctl enable --now backup.timer`, then `systemctl list-timers`. Compared with
cron you get the journal, `Persistent=` (catch up on runs missed while the machine was off),
no overlapping runs, and the same sandboxing as services. `systemd-analyze calendar
'Mon *-*-* 09:00'` checks an expression. Cron (`crontab -e`, `/etc/cron.d/`) still works and
is everywhere; know both.

## Sandboxing and limits

systemd can take privileges away from a service in a few lines:

```ini
[Service]
NoNewPrivileges=yes          # setuid programs cannot raise privileges
ProtectSystem=strict         # the whole filesystem read-only...
StateDirectory=orders        # ...except /var/lib/orders, created and owned for the service
PrivateTmp=yes               # its own /tmp
ProtectHome=yes              # no access to /home
MemoryMax=1G                 # cgroup limits (Phase 4)
CPUQuota=200%
```

`systemd-analyze security orders` scores a unit and lists what else you could restrict.

## Summary

- Units, targets and `systemctl start|stop|restart|reload|enable|status`.
- Your units in `/etc/systemd/system/`, then `daemon-reload`; drop-ins with `systemctl edit`.
- A service runs in the foreground, logs to stdout, as its own `User=`, with `Restart=on-failure`.
- `TimeoutStopSec` is the grace period between SIGTERM and SIGKILL.
- `journalctl -u NAME -f`, `-p`, `--since`, `-b`, `-k`.
- Timers replace cron with logging and `Persistent=`; sandbox services with `ProtectSystem`, `NoNewPrivileges`, `PrivateTmp`.

## Further reading

- `systemd.service(5)`: <https://www.freedesktop.org/software/systemd/man/latest/systemd.service.html>
- `systemd.exec(5)` (users, environment, sandboxing): <https://www.freedesktop.org/software/systemd/man/latest/systemd.exec.html>
- `systemd.timer(5)`: <https://www.freedesktop.org/software/systemd/man/latest/systemd.timer.html>
- Arch Wiki, "systemd": <https://wiki.archlinux.org/title/Systemd>
