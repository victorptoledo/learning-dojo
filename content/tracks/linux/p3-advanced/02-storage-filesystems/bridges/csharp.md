---
from: csharp
status: draft
concept: "Disks and free space"
from_way: "DriveInfo with drive letters, and Disk Management on Windows."
native_way: "One tree with filesystems mounted on directories, listed by df and lsblk."
sensei_tip: "On Linux, DriveInfo.GetDrives() returns mount points such as / and /data, not letters. Use new DriveInfo(path) for the filesystem that holds a given path."
---

| C# | Linux |
| --- | --- |
| D:\ | a filesystem mounted on /data |
| DriveInfo.AvailableFreeSpace | df -h |
| Disk Management | lsblk, fdisk, mkfs, mount |
| Extend Volume | lvextend -r |
