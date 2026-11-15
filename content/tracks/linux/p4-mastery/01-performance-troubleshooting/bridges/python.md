---
from: python
status: draft
concept: "Finding why a service is slow"
from_way: "cProfile, py-spy and memory_profiler, with the GIL limiting CPU work to one core per process."
native_way: "Look at the machine first with the USE method and the standard tools, then zoom into the process."
sensei_tip: "py-spy top --pid PID shows where a running Python process spends its time without restarting it. One core at 100% in mpstat with others idle is the GIL: scale with processes."
---

| Python | Linux |
| --- | --- |
| cProfile, py-spy | top, pidstat, perf top |
| tracemalloc | free -h, /proc/PID/status VmRSS |
| MemoryError | the OOM killer in journalctl -k |
| too many open files | ulimit -n, LimitNOFILE= |
| asyncio debug mode | strace -p PID |
