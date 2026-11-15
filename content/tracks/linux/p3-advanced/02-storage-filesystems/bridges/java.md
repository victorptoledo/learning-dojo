---
from: java
status: draft
concept: "Disks and free space"
from_way: "Drive letters and Disk Management on Windows, and File.getUsableSpace() or an IOException: No space left on device in the logs."
native_way: "Block devices mounted into one tree through fstab, df and du to see usage, and LVM to grow volumes without downtime."
sensei_tip: "A JVM that writes heap dumps on OutOfMemoryError (-XX:HeapDumpPath) can fill a small disk in seconds. Point the dumps and logs at a volume with room, and watch both df and df -i."
---

| Windows / Java | Linux |
| --- | --- |
| `D:\` | a filesystem mounted on `/data` |
| Disk Management | `lsblk`, `fdisk`, `mkfs`, `mount` |
| Automatic drive letters | lines in `/etc/fstab` |
| `File.getUsableSpace()` | `df -h` |
| "Folder size" in Explorer | `du -sh dir` |
| Extending a volume in Disk Management | `lvextend -r` |
