---
from: python
status: draft
concept: "Disks and free space"
from_way: "shutil.disk_usage and an OSError: [Errno 28] No space left on device in a traceback."
native_way: "Block devices mounted into one tree, df and du to see usage, and LVM to grow volumes."
sensei_tip: "Errno 28 can also mean no free inodes: df -i. A Python app that writes many small cache or session files is the usual cause."
---

| Python | Linux |
| --- | --- |
| shutil.disk_usage('/') | df -h / |
| OSError: [Errno 28] | df -h and df -i |
| os.path.getsize | du -sh |
| tempfile in /tmp | tmpfs or the root filesystem: check findmnt /tmp |
