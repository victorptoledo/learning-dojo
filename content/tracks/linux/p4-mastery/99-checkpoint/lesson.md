# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole Linux track. It has two parts, and
you need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards
   performance, the kernel and containers, and production operation. You need 7 right.
2. **The integration kata** (press **Enter**): the triage script you run in the first minute
   of an incident. It does not guess. It reads what the kernel already counts: how long tasks
   waited for CPU, memory and disk, which processes the OOM killer chose, which service
   cgroup is at its memory limit or throttled, which process is out of file descriptors or
   holds a deleted log, and whether the server still runs an old kernel.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Performance and Troubleshooting | What does the USE method ask of each resource? How do I tell CPU saturation from waiting on I/O? Where do I see an OOM kill, and what does `Too many open files` mean? |
| Boot, the Kernel, cgroups and Namespaces | What is in `/sys/fs/cgroup/system.slice/NAME.service`? What happens when a cgroup reaches `memory.max`? Where do kernel parameters come from? |
| Running Linux in Production | When does an upgrade need a reboot, and how do I know? Why rotate logs with a signal, and what happens to a log deleted while it is open? |
| Disks, Filesystems, Mounts and LVM | Why can `df` and `du` disagree? |
| Processes and Signals | Where in `/proc/PID` are a process's name, limits and open files? |

## What the kata adds

**Pressure stall information** (PSI, `/proc/pressure/*`) answers the question that load
average only hints at: *what share of time did tasks wait, and for which resource?* It
covers the "saturation" part of the USE method in one file per resource. Brendan Gregg's
`vmstat` and `iostat` checks still apply. PSI just gives you the answer sooner. On
Debian it is compiled in but off by default, and `psi=1` on the kernel command line
(`GRUB_CMDLINE_LINUX` in `/etc/default/grub`, then `update-grub`) turns it on.

The cgroup files are the same ones `systemd-cgtop` and `docker stats` read. `memory.events`
counts the OOM kills **inside** a service's limit, which is why a service can be killed
while `free` shows plenty of memory on the host.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
