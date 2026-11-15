---
from: java
status: draft
concept: "Paths, the program search path and documentation"
from_way: "On Windows, paths start with a drive letter and use backslashes, programs are found through PATH and file extensions (.exe, .bat), and help is on a website."
native_way: "One tree from /, forward slashes, case-sensitive names, executables found through PATH without extensions, and the manual installed with the system."
sensei_tip: "java.nio.file.Path works the same on both systems, but a hard-coded C:\\ or a file named Config.yml read as config.yml breaks on a Linux server. Test on Linux early."
---

| Windows / Java | Linux |
| --- | --- |
| `C:\Users\ada\app\config.yml` | `/home/ada/app/config.yml` |
| `D:\` for another disk | another disk is mounted somewhere, such as `/data` |
| Names are case-insensitive | `Config.yml` and `config.yml` are different files |
| `where java` | `type java` or `command -v java` |
| `%APPDATA%`, the registry | `~/.config`, files in `/etc` |
| Javadoc in the browser | `man`, `--help` and `help` in the terminal |
