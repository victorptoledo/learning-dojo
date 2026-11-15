---
from: go
status: draft
concept: "Disks and free space"
from_way: "syscall.Statfs and ENOSPC errors from os.WriteFile."
native_way: "df, du, mount and LVM, which read and change the same kernel structures."
sensei_tip: "syscall.Statfs(path, &st) gives the numbers df prints: st.Bavail * uint64(st.Bsize) is the space available to unprivileged users, which excludes ext4's reserved blocks."
---

| Go | Linux |
| --- | --- |
| syscall.Statfs("/", &st) | df / |
| syscall.ENOSPC | df -h and df -i |
| filepath.WalkDir summing sizes | du -sh |
| os.ReadFile("/proc/mounts") | findmnt |
