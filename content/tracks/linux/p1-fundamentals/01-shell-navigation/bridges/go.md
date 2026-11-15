---
from: go
status: draft
concept: "Paths, the program search path and documentation"
from_way: "os.Getwd, filepath.Join and exec.LookPath, with go doc for documentation."
native_way: "The shell's working directory, PATH lookup in order, and man pages for the system tools."
sensei_tip: "exec.LookPath searches PATH exactly like the shell does. Under systemd the PATH is short, so call programs by absolute path or set Environment=PATH=... in the unit."
---

| Go | Linux |
| --- | --- |
| os.Getwd() | pwd |
| os.Chdir(dir) | cd dir |
| filepath.Glob("*.log") | *.log, expanded by the shell |
| exec.LookPath("git") | command -v git |
| go doc os.Open | man 2 open |
