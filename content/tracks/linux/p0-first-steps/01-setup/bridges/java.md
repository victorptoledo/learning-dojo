---
from: java
status: draft
concept: "Getting and managing a development machine"
from_way: "On Windows you install programs from installers or winget, configure them in dialogs, and your Java app runs on whatever JDK is on the PATH."
native_way: "On Linux you install from signed distribution repositories with apt, configure with text files, and reproduce a whole machine with a Docker image or a script."
sensei_tip: "Treat `docker run -it --rm debian:trixie bash` like jshell for the operating system: a disposable place to try a command before you run it on a server."
---

| Windows / Java habit | Linux way |
| --- | --- |
| `winget install` or an installer | `sudo apt install <package>` |
| Programs in `C:\Program Files` | Binaries in `/usr/bin`, config in `/etc` |
| Settings in dialogs or the registry | Plain text files you can version |
| Maven downloads libraries per project | apt installs system packages shared by every program |
| The JDK on the PATH | The distribution's packages, or a container image per app |
