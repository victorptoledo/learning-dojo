---
from: python
status: draft
concept: "Paths, the program search path and documentation"
from_way: "os.getcwd(), os.chdir() and pathlib.Path, with shutil.which() to find programs and help() or the docs site for answers."
native_way: "The shell's working directory, cd and PATH lookup, with man pages and --help installed with the system."
sensei_tip: "pathlib.Path('config.yml') is relative to the working directory, not to the script. Under systemd or cron that directory is often /, so build paths from Path(__file__).parent or WorkingDirectory=."
---

| Python | Linux |
| --- | --- |
| os.getcwd() | pwd |
| os.chdir('/var/log') | cd /var/log |
| glob.glob('*.log') | *.log, expanded by the shell |
| shutil.which('git') | type git or command -v git |
| help(str) | man, --help, help |
