---
from: javascript
status: draft
concept: "Least privilege at run time"
from_way: "Node's permission model (--permission, --allow-fs-read) restricts what a script can touch inside the process."
native_way: "Container runtime security restricts the whole process from the outside: limits, dropped capabilities, seccomp and a read-only file system."
sensei_tip: "Both layers combine well: a read-only container stops writes to the image, and Node's permission flags can stop a compromised dependency from reading the secrets mounted under /run/secrets."
---

| JavaScript | Runtime security |
| --- | --- |
| `--max-old-space-size` | below the `--memory` limit |
| `--permission` flags | the kernel-level equivalent: capabilities, seccomp |
| a cluster of workers | sized from `--cpus` |
| writing uploads into the app folder | a volume; the rest `--read-only` |
