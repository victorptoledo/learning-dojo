---
from: java
status: draft
concept: "Who may do what with a file"
from_way: "Windows ACLs edited in a Properties dialog, an administrator account you are often logged in as, and Java's File.setExecutable or Files.setPosixFilePermissions that you rarely need."
native_way: "Three permission triplets per file plus optional ACLs, a separate user per service, and root used only through sudo for one command at a time."
sensei_tip: "Run your Java service as its own user (User= in its systemd unit, USER in its Dockerfile), never as root, and keep its secrets at mode 600 owned by that user."
---

| Windows / Java | Linux |
| --- | --- |
| Properties > Security | `ls -l`, `chmod`, `chown`, `getfacl` |
| "Run as administrator" | `sudo command` |
| Local Administrators group | the `sudo` group (Debian) or `wheel` (Fedora, RHEL) |
| `Files.setPosixFilePermissions(p, fromString("rw-------"))` | `chmod 600 p` |
| `Files.getOwner(p)` | `stat -c %U p` |
| A service running as LocalSystem | a service running as its own user, such as `www-data` |
