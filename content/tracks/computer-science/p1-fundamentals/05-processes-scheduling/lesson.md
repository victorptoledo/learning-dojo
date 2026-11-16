# Processes, Threads and CPU Scheduling

A laptop runs hundreds of programs on 8 or 16 cores, and each of them behaves as if it had the
machine to itself. That illusion is the job of the **operating system kernel**. This lesson covers
the two abstractions it gives every program, the **process** and the **thread**, and the
**scheduler** that decides which of them runs on each core.

## User mode, kernel mode and system calls

The CPU has (at least) two privilege levels:

- **User mode**: your code. It cannot touch devices, page tables or other processes' memory.
- **Kernel mode**: the kernel, which can do anything.

A program asks the kernel for a service with a **system call** (`read`, `write`, `open`,
`mmap`, `clone`, …): a special instruction (`syscall` on x86-64) switches to kernel mode and
jumps to a fixed entry point. A system call costs from about a hundred nanoseconds to
microseconds, much more than a function call, which is why buffered I/O exists.

Control also enters the kernel through **interrupts** (a timer, a network card, a disk finishing)
and **exceptions** (a division by zero, a page fault). The **timer interrupt**, every few
milliseconds, is what lets the kernel take the CPU back from a program that never makes a system
call.

## Processes

A **process** is a running program: an address space (code, data, heap, stacks), open file
descriptors, credentials, and one or more threads. The kernel keeps it in a **process control
block** (`task_struct` on Linux).

```text
            admitted         scheduled
   New ─────────────► Ready ─────────► Running ──── exit ───► Terminated
                        ▲   ◄─────────    │
                        │   preempted     │ waits for I/O, a lock, sleep
                        └──── Blocked ◄───┘
                           event happened
```

On Unix, `fork()` copies the calling process (cheaply, with copy-on-write; see Virtual Memory),
and `exec()` replaces the copy's program with a new one. A shell runs a command with fork + exec
+ wait. The parent collects the child's exit status with `wait`; a child that has exited but not
been waited for is a **zombie**.

Processes are **isolated**: one cannot read another's memory, and a crash kills only itself. They
communicate through pipes, sockets, files, shared memory or signals.

## Threads

A **thread** is a sequence of execution inside a process. Threads of one process **share** the
address space, heap, globals and open files; each has its **own** stack, registers and program
counter.

| | Processes | Threads |
| --- | --- | --- |
| Memory | separate | shared |
| Create | expensive (new address space) | cheaper |
| Communicate | IPC: pipes, sockets | read and write shared memory (with locks) |
| A crash | kills one process | kills the whole process |
| Examples | browser tabs, PostgreSQL backends, Python multiprocessing | a Java server's request threads, Go's runtime threads |

**Green threads**, coroutines and virtual threads (Java's virtual threads, Go's goroutines, async
tasks in Python, JavaScript and C#) are scheduled by the **language runtime** on top of a few OS
threads. They are cheap enough to create one per request, because switching between them does not
enter the kernel.

## Context switches

Moving a core from one thread to another is a **context switch**: save the registers and program
counter, load the other thread's, and, between processes, switch address spaces (which may flush
the TLB). The direct cost is a few microseconds; the indirect cost, cold caches, is often larger.
Thousands of threads competing for a few cores spend real time switching.

## Scheduling

The **scheduler** decides which ready thread runs next on each core. What it optimises:

- **Throughput**: jobs finished per unit of time.
- **Turnaround time**: completion − arrival.
- **Waiting time**: time spent ready but not running (turnaround − CPU burst, ignoring I/O).
- **Response time**: first time on the CPU − arrival. What interactive users feel.
- **Fairness**: everyone gets a share; nobody **starves**.

The classic policies:

| Policy | How | Good at | Bad at |
| --- | --- | --- | --- |
| FCFS (first come, first served) | run in arrival order to completion | simple, no switches | a long job delays all short ones (**convoy effect**) |
| SJF / SRTF | shortest job (or shortest remaining time, preemptive) first | provably minimal average waiting time | needs to know job lengths; long jobs can starve |
| Round robin | each job runs for a **quantum** (time slice), then goes to the back of the queue | good response time, fair | more switches; poor turnaround if the quantum is too small |
| Priority | highest priority first | important work first | starvation, fixed by **aging** |
| MLFQ (multi-level feedback queue) | several RR queues; a job that uses its whole slice moves down, interactive jobs stay high | approximates SJF without knowing lengths | tuning, gaming |

The choice of quantum matters: much larger than the context switch cost (otherwise the CPU
switches more than it works) but small enough for interactive response, typically 1–100 ms.

### Real schedulers

- **Linux** used the **CFS** (Completely Fair Scheduler) from 2007: each thread accumulates
  "virtual runtime" weighted by its `nice` value, and the one with the least runs next, kept in a
  red-black tree. Since kernel 6.6 the default is **EEVDF**, which adds a deadline to also bound
  latency.
- **Windows** and **macOS** use priority-based, preemptive schedulers with boosts for interactive
  and foreground threads.
- On multicore machines the scheduler also chooses **which core**: keeping a thread on the same
  core keeps its cache warm (**affinity**), and load balancing moves threads between cores.
- Containers get CPU through **cgroups**: shares (weights) and quotas. A container with a quota of
  one CPU that runs eight threads is **throttled** when it uses its quota early in a period.

## Summary

- The kernel runs in a privileged mode; programs enter it with system calls, devices with
  interrupts.
- A process is an isolated address space with resources; threads share a process's memory but
  have their own stacks.
- Context switches cost microseconds plus cold caches.
- Scheduling trades throughput, turnaround, response time and fairness: FCFS, SJF, round robin,
  priorities and MLFQ are the building blocks; Linux uses fair-share schedulers.

## Further reading

- Arpaci-Dusseau and Arpaci-Dusseau, *Operating Systems: Three Easy Pieces*, chapters 4–10 (processes, limited direct execution, scheduling, MLFQ, lottery scheduling): <https://pages.cs.wisc.edu/~remzi/OSTEP/>
- Silberschatz, Galvin and Gagne, *Operating System Concepts*, 10th ed., chapters 3 "Processes", 4 "Threads" and 5 "CPU Scheduling"
- Tanenbaum and Bos, *Modern Operating Systems*, 4th ed., chapter 2 "Processes and Threads"
- MIT 6.1810 Operating System Engineering (xv6 book): <https://pdos.csail.mit.edu/6.1810/>
- Linux kernel documentation, "EEVDF Scheduler": <https://docs.kernel.org/scheduler/sched-eevdf.html>
