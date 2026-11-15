---
from: go
status: draft
concept: "Who may do what with a file"
from_way: "os.Chmod and the os.FileMode bits, with os.WriteFile taking a mode for new files."
native_way: "The same mode bits, shown by ls -l and changed by chmod, plus users and groups for services."
sensei_tip: "os.WriteFile(path, data, 0o600) applies the mode only when it creates the file, and the umask still applies. For secrets check the result with os.Stat or chmod afterwards."
---

| Go | Linux |
| --- | --- |
| os.Chmod(p, 0o644) | chmod 644 p |
| info.Mode().Perm() | stat -c %a p |
| os.Chown(p, uid, gid) | chown user:group p |
| os.FileMode(0o4755) | the setuid bit, chmod 4755 |
| syscall.Umask(0o027) | umask 027 |
