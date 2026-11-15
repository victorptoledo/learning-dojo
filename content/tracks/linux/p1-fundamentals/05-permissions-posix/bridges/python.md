---
from: python
status: draft
concept: "Who may do what with a file"
from_way: "os.chmod and os.stat with stat module constants, usually only to make a script executable or a secret private."
native_way: "Owner, group and others with r, w and x, set with chmod and chown, and services running as their own users."
sensei_tip: "os.chmod(path, 0o600) uses the same octal digits as chmod 600; note the 0o prefix. Create secrets with the right mode from the start: os.open(path, os.O_CREAT | os.O_WRONLY, 0o600)."
---

| Python | Linux |
| --- | --- |
| os.chmod(p, 0o644) | chmod 644 p |
| os.stat(p).st_mode | stat -c %a p |
| os.chown(p, uid, gid) | chown user:group p |
| os.umask(0o027) | umask 027 |
| running as Administrator | sudo for one command |
