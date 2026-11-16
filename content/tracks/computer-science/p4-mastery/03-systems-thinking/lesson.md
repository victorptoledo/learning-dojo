# Systems Thinking Across the Layers

You have now opened every layer of the stack: bits, gates, the CPU, caches, processes, virtual
memory, files, networks, compilers, automata, complexity, concurrency and databases. Real problems
do not respect those boundaries: a slow request might be a cache miss, a page fault, a TCP
retransmit, a lock, an `fsync` or an exponential regex. This last lesson ties the layers together
and gives you the habits that let you move between them.

## One request, every layer

A user taps "Pay" and the app sends `POST /orders`:

1. **Application and runtime**: the handler runs as bytecode JIT-compiled to machine code; it
   allocates objects (heap pages backed on demand by the kernel) and may pause for garbage
   collection.
2. **Data representation**: the amount must not be a binary float; the JSON body is UTF-8 that
   must be decoded with the right encoding.
3. **Concurrency**: the request runs on a pool thread or a virtual thread; shared state (a cache, a
   counter) needs a lock, an atomic or a single owner.
4. **Operating system**: `write` on a socket is a system call; the scheduler decides when the thread
   runs; under a cgroup quota it may be throttled.
5. **Network**: the database query leaves through TCP to another machine: a round trip of ~0.5 ms,
   more if a packet is lost and retransmitted.
6. **Database**: the query is parsed and planned, an index B+tree is walked through the buffer
   pool, the change is logged to the WAL and the commit waits for an `fsync`, shared with other
   commits by group commit.
7. **Storage and hardware**: the SSD's controller writes the log page; the CPU spends most of its
   time waiting on memory, and the caches decide how much.

Each step has a cost you can estimate, and each step has its own failure modes.

## Back-of-the-envelope estimates

Estimate before you measure, then measure to check:

- **Latency numbers** from the first lesson: nanoseconds for caches, ~100 ns for RAM, tens of
  microseconds for an SSD read, ~0.5 ms within a data centre, ~100 ms across an ocean.
- **Count round trips**, not bytes, for small messages: ten sequential queries cost ten round
  trips.
- **Bandwidth** for large data: 1 GB over 1 Gbit/s is 8 s; reading 1 GB sequentially from an NVMe
  SSD is well under a second, from RAM a fraction of that.
- **Throughput limits**: one `fsync` of 1 ms per request limits a single writer to ~1,000 requests
  per second, unless commits are batched.

## Three laws worth knowing

- **Amdahl's law**: if a fraction *p* of the work can be sped up by a factor *s*, the total speed-up
  is 1 / ((1 − p) + p / s). The part you cannot speed up dominates: with p = 0.9, even infinitely
  many cores give at most 10×.
- **Little's law**: in a stable system, the average number of items in it equals the arrival rate
  times the average time each spends: **L = λW**. 1,000 requests per second at 50 ms each means
  50 requests in flight on average, so a pool of 20 threads must queue. It holds for thread pools,
  connection pools, queues and checkout lines alike.
- **Utilisation and queueing**: as a resource approaches 100% utilisation, waiting time grows
  without bound (for a simple queue, the wait is proportional to ρ / (1 − ρ)). Running servers at
  95% CPU is a latency decision.

## Tail latency

Averages hide what users feel. Look at **percentiles**: p50, p99, p99.9. Tails come from rare
events in every layer: a GC pause, a page fault, a context switch, a retransmit, a lock convoy, a
cache miss storm, a compaction, a slow disk. And they **amplify**: a page that calls 100 services
in parallel is slower than the p99 of each service for about 63% of page views (1 − 0.99¹⁰⁰).
Defences: timeouts, hedged or backup requests, load shedding and back-pressure, and removing the
slow path from the common case.

## Debugging across layers

Form a hypothesis about **which layer** is responsible, then use that layer's tool:

| Layer | Questions | Tools |
| --- | --- | --- |
| Application, runtime | where is the time? GC? locks? | profilers and flame graphs, JFR, `py-spy`, `pprof`, thread dumps, GC logs |
| OS | CPU, run queue, throttling, faults, syscalls | `top`, `vmstat`, `pidstat`, `perf`, `strace`, cgroup stats |
| Memory | RSS, page faults, OOM | `/proc/<pid>/status`, `smaps`, `dmesg` |
| Storage | latency, queue depth, fsync | `iostat`, `strace -T`, database I/O statistics |
| Network | round trips, loss, DNS | `ping`, `mtr`, `dig`, `ss`, `tcpdump`, retransmit counters |
| Database | plans, locks, waits | `EXPLAIN ANALYZE`, `pg_stat_activity`, lock views, slow-query logs |
| Hardware | cache misses, branch misses, IPC | `perf stat`, hardware counters |

The **USE method** (Brendan Gregg): for every resource, check **U**tilisation, **S**aturation and
**E**rrors. And change one thing at a time.

## The ideas that keep coming back

Look back over the track and the same few ideas solve problem after problem:

- **Abstraction** with a contract: the ISA, system calls, sockets, SQL. Know the contract, and
  know where it leaks.
- **Caching** wherever there is locality: registers, CPU caches, the TLB, the page cache, the
  buffer pool, DNS, CDNs. And its two hard problems: invalidation (the TLB after `unmap`) and
  coherence.
- **Indirection**: virtual addresses, file descriptors, inodes, DNS names, slot numbers in a VM.
  It buys flexibility for a lookup.
- **Logging before acting**: journals, the WAL, replication logs, event sourcing. Append is cheap
  and makes recovery possible.
- **Batching and amortisation**: group commit, buffered I/O, TCP windows, dynamic arrays.
- **Trading time for space** and **precomputation**: tables, indexes, DFAs built from NFAs,
  compiled bytecode.
- **Limits are real**: undecidability says some tools must approximate; NP-hardness says some
  problems need heuristics; physics says the network is slow.
- **Concurrency needs a discipline**: one owner, or locks with an order, or atomics with a model.

## Summary

- A single request crosses every layer of this track; each layer has a cost and failure modes.
- Estimate with latency numbers and round trips; reason with Amdahl's and Little's laws and
  queueing.
- Watch percentiles, not averages; tails amplify across fan-out.
- Debug by layer with that layer's tools, and check utilisation, saturation and errors.
- Abstraction, caching, indirection, logging, batching and precomputation recur everywhere.

## Further reading

- Brendan Gregg, *Systems Performance: Enterprise and the Cloud*, 2nd ed., and "The USE Method": <https://www.brendangregg.com/usemethod.html>
- Dean and Barroso, "The Tail at Scale" (Communications of the ACM, 2013): <https://research.google/pubs/the-tail-at-scale/>
- Bryant and O'Hallaron, *Computer Systems: A Programmer's Perspective*, 3rd ed., chapter 1 and chapter 5 "Optimizing Program Performance"
- Saltzer and Kaashoek, *Principles of Computer System Design: An Introduction*: <https://ocw.mit.edu/courses/res-6-004-principles-of-computer-system-design-an-introduction-spring-2009/>
- Butler Lampson, "Hints for Computer System Design" (1983)
- MIT OCW 6.033 Computer System Engineering: <https://ocw.mit.edu/courses/6-033-computer-system-engineering-spring-2018/>
