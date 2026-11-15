# Running Linux in Production: Packages, Logs, Scheduling and Backups

Installing a server takes an hour; operating it takes years. This lesson collects the
habits that keep servers boring.

## Packages and repositories

```bash
apt list --upgradable
sudo apt full-upgrade                      # also installs or removes packages when dependencies change
apt policy nginx                           # installed and candidate versions, and from which repository
apt-cache rdepends --installed libssl3t64  # what depends on a library
sudo apt-mark hold postgresql-17           # never upgrade this one automatically
apt-mark showhold
sudo apt autoremove --purge                # remove dependencies nobody needs any more
```

Repositories are listed in `/etc/apt/sources.list.d/` (Debian 13 uses the **deb822**
format, `debian.sources`). Third-party repositories (PostgreSQL, Docker, Grafana) come with
a signing key: put it in `/etc/apt/keyrings/` and reference it with `Signed-By:`, so it is
trusted for that repository only.

Rules that prevent surprises:

- Upgrade **staging first**, then production, and read what `apt` is going to remove.
- Hold or pin software whose upgrades need planning (databases, Kubernetes components).
- Upgrade between Debian releases (12 to 13) following the official release notes, one
  release at a time, after a backup.

## Logs and rotation

Programs that write their own files need **rotation**, or they fill the disk. `logrotate`
runs daily (a systemd timer) and reads `/etc/logrotate.d/*`:

```text
/var/log/orders/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0640 orders adm
    postrotate
        systemctl kill -s HUP orders.service
    endscript
}
```

After renaming a file, the program must reopen it (`postrotate` sends a signal), or it
keeps writing to the renamed file, the "deleted but open" problem from the storage lesson.
`copytruncate` avoids the signal but may lose lines. Test with `sudo logrotate -d
/etc/logrotate.d/orders` (dry run). The journal rotates itself (`SystemMaxUse=` in
`/etc/systemd/journald.conf`).

In production, ship logs to a central system (Loki, Elasticsearch, a cloud service) so you
can search across machines and keep them when a machine dies.

## Scheduled jobs

You know systemd timers. **cron** is still everywhere:

```text
# crontab -e   (minute hour day-of-month month day-of-week command)
30 2 * * *   /usr/local/bin/backup.sh -k 7 /srv/data /srv/backups >> /var/log/backup.log 2>&1
*/5 * * * *  /usr/local/bin/health.sh || logger -t health "health check failed"
```

Cron runs with a minimal `PATH` and no terminal, so use full paths, redirect output, and
make jobs safe to run twice (`flock -n /run/backup.lock backup.sh` prevents overlaps).
Always know **where the output goes** and **who is alerted** when a job fails.

## Backups

A backup you have never restored is a hope, not a backup.

- **3-2-1**: three copies, on two kinds of storage, one off-site (and ideally one that the
  server itself cannot delete, against ransomware).
- Back up **data and configuration**: `/etc`, application data, the package list
  (`dpkg --get-selections`), not the OS itself if you can rebuild it.
- Databases need a **consistent** copy: `pg_dump` or the database's own backup tool, or a
  filesystem snapshot (LVM), not a copy of files being written.
- Tools: `rsync`, `restic` and `borg` (deduplicated, encrypted, with retention), your
  provider's volume snapshots.
- **Restore regularly**, on a schedule, and time it: that number is your real recovery time.

## Configuration as code

Hand-made servers drift apart and cannot be rebuilt. Describe them in versioned files:

- **cloud-init** configures a VM at its first boot (users, packages, files);
- **Ansible** applies the desired state over SSH, repeatably (idempotent tasks);
- images built with **Packer** or containers built from a Dockerfile replace patching
  running machines with replacing them.

The goal: any server can be destroyed and rebuilt from the repository in minutes. Then
upgrades, scaling and disaster recovery become the same routine.

## When things go wrong

1. **Stabilise first**: restore service (roll back, restart, fail over, add capacity)
   before you look for the root cause.
2. **Communicate**: one person coordinates, others investigate; keep a timeline.
3. **Investigate with the method**: what changed (deploys, `apt` history in
   `/var/log/apt/history.log`, config), then the USE checks and the logs.
4. **Write it down**: a blameless postmortem with the timeline, the cause, and actions that
   make it impossible or harmless next time; turn repeated steps into a **runbook**.

## Summary

- `apt policy`, `apt-mark hold`, keys in `/etc/apt/keyrings` with `Signed-By`, staging first.
- `logrotate` with a reopen signal (or `copytruncate`); central log collection.
- cron with full paths, redirected output, `flock`; or systemd timers.
- 3-2-1 backups of data and configuration, consistent database copies, regular restore tests.
- Configuration as code: rebuild instead of repair.
- Incidents: stabilise, communicate, investigate what changed, write the postmortem.

## Further reading

- The Debian Administrator's Handbook: <https://debian-handbook.info/browse/stable/>
- Debian 13 release notes (upgrading): <https://www.debian.org/releases/trixie/releasenotes>
- Arch Wiki, "Logrotate": <https://wiki.archlinux.org/title/Logrotate>
- `crontab(5)`: <https://man7.org/linux/man-pages/man5/crontab.5.html>
- Google SRE book, "Postmortem Culture": <https://sre.google/sre-book/postmortem-culture/>
