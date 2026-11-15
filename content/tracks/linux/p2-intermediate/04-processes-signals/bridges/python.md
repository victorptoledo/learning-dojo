---
from: python
status: draft
concept: "Processes and stopping cleanly"
from_way: "subprocess for child processes, threading and multiprocessing, and the signal module for handlers."
native_way: "fork and exec, a PID per program, signals to talk to them, and trap in bash."
sensei_tip: "signal.signal(signal.SIGTERM, handler) is Python's trap. Python turns SIGINT into KeyboardInterrupt by default but ignores nothing on SIGTERM, which kills it at once unless you install a handler."
---

| Python | Linux |
| --- | --- |
| subprocess.Popen(['ls']) | fork + exec |
| os.getpid() | $$ |
| proc.wait() | wait |
| signal.signal(SIGTERM, h) | trap h TERM |
| proc.terminate() / kill() | kill PID / kill -9 PID |
