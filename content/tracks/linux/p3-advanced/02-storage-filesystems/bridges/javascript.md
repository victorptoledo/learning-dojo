---
from: javascript
status: draft
concept: "Disks and free space"
from_way: "fs.statfs (Node 18+) and ENOSPC errors from fs.writeFile."
native_way: "df, du and lsblk, and fstab for permanent mounts."
sensei_tip: "ENOSPC from a Node watcher is often not the disk but the inotify watch limit (fs.inotify.max_user_watches). Check df -h first, then sysctl."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| fs.statfsSync('/') | df -h / |
| ENOSPC | df -h, df -i, or inotify limits |
| node_modules size | du -sh node_modules |
| Docker volume | a mount under /var/lib/docker/volumes |
