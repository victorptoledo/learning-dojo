---
from: java
status: draft
concept: "File-level access control"
from_way: "Managing permissions only in application code, ignoring the operating system's."
native_way: "Every file has three sets of bits, read (4), write (2) and execute (1), for owner, group and others."
sensei_tip: "Never `chmod 777` 'to make it work'. Find out which user runs the process and grant only what it needs (e.g. 644 for files, 755 for binaries)."
---
