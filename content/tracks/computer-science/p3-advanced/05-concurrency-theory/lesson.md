# Concurrency Theory: Races, Mutual Exclusion, Deadlock and Memory Models

Sequential code has one execution for each input. Concurrent code has an astronomical number:
every way the steps of the threads can **interleave**, plus the reorderings the compiler and the
CPU perform. A bug that shows up in one interleaving in a million is still a bug, and it will
appear in production. This lesson gives the concepts and the vocabulary to reason about all
interleavings at once instead of hoping.

## Interleavings and atomicity

`count++` looks like one step but is three: **read** count, **add** 1, **write** it back. Two
threads doing it once each:

```text
T1: read 0          T1: add → 1      T1: write 1
           T2: read 0          T2: add → 1      T2: write 1      result: 1, not 2
```

An operation is **atomic** if no other thread can observe it half done. A **race condition** is a
bug whose outcome depends on the timing of threads. A **data race** is the specific case of two
threads accessing the same memory location concurrently, at least one writing, without
synchronisation. Every data race is a bug in practice; not every race condition is a data race (a
"check then act" on a thread-safe map is a race condition without a data race).

## Critical sections and mutual exclusion

A **critical section** is code that must not run in two threads at once. A correct solution needs
**mutual exclusion** (at most one thread inside), **progress** (if nobody is inside, someone who
wants to enter eventually does) and **bounded waiting** (no thread waits forever).

**Peterson's algorithm** achieves it for two threads with only loads and stores:

```text
flag[i] = true          # I want to enter
turn = j                # but you go first
while flag[j] and turn == j: wait
... critical section ...
flag[i] = false
```

It is correct on paper but **fails on modern hardware** unless the variables are accessed with
sequentially consistent operations: CPUs and compilers reorder the store to `flag[i]` and the
load of `flag[j]`. That is why real locks are built on **hardware atomics**:

- **Test-and-set**, **fetch-and-add** (`lock xadd` on x86), and above all **compare-and-swap**:
  `CAS(addr, expected, new)` writes `new` only if the value is still `expected`, atomically, and
  reports whether it did. A CAS loop builds almost anything: `do { old = x; } while
  (!CAS(&x, old, old + 1))`.
- A **spinlock** loops on CAS; a **mutex** (Java's `synchronized`, `ReentrantLock`, Go's
  `sync.Mutex`, C#'s `lock`) spins briefly and then asks the OS to put the thread to sleep
  (`futex` on Linux).

## Coordination primitives

- **Semaphore** (Dijkstra): a counter with `P` (wait until > 0, then decrement) and `V`
  (increment). A semaphore of N limits concurrency to N; of 1 it is a lock.
- **Monitor**: a lock plus **condition variables**. A thread `wait`s on a condition (releasing the
  lock atomically) until another `signal`s it. Always wait in a **loop** that re-checks the
  condition, because of spurious wake-ups and other threads getting there first. Java's
  `synchronized` + `wait`/`notify` and `Lock` + `Condition` are monitors.
- **Read-write locks**, **barriers**, **latches**, **futures** and **channels** are built on these.

The classic exercises (bounded buffer producer-consumer, readers-writers, dining philosophers)
are each a pattern you will meet in real code: work queues, caches, resource pools.

## Deadlock, livelock and starvation

A **deadlock**: threads wait for each other forever. It needs all four **Coffman conditions**:

1. **Mutual exclusion**: resources are held exclusively.
2. **Hold and wait**: a thread holds one resource while waiting for another.
3. **No preemption**: resources cannot be taken away.
4. **Circular wait**: T1 waits for T2, which waits for … T1.

Break any one to prevent it. The most practical is a **global lock order**: always acquire locks
in the same order (by id, by address), which makes a cycle impossible. Others: acquire all locks at
once, `tryLock` with a timeout and back-off, or avoid shared locks altogether. Databases detect
cycles in their **wait-for graph** and abort one transaction.

- **Livelock**: threads keep reacting to each other without progress (two people stepping aside
  in a corridor in the same direction forever). Randomised back-off fixes it.
- **Starvation**: a thread never gets the resource because others always win. Fair locks and
  aging fix it.
- **Priority inversion**: a low-priority thread holds a lock that a high-priority thread needs,
  while medium-priority threads keep the low one off the CPU. It hung the Mars Pathfinder lander in
  1997; **priority inheritance** fixed it.

## Memory models and happens-before

Compilers reorder and cache variables in registers; CPUs have store buffers and reorder memory
operations (x86 a little, ARM a lot). Without rules, one thread could see another's writes in any
order, or never. A **memory model** is the contract that says which values a read may return.

- **Sequential consistency** (Lamport): the result is as if all operations ran in some single
  interleaving that respects each thread's program order. Intuitive, and expensive to guarantee for
  every access.
- Modern languages (Java since JSR-133, C++11, C#, Go, Rust) promise **sequential consistency for
  data-race-free programs** (SC-DRF): if your program has no data races, you may reason as if it
  were sequentially consistent.
- "No data race" is defined through **happens-before**: an unlock happens-before the next lock of
  the same lock; a write to a `volatile` (Java) or an atomic happens-before a read that sees it;
  starting a thread happens-before its first action; finishing it happens-before `join` returns. A
  write is guaranteed visible to a read only if it happens-before it.
- Practical consequences: a flag read in a loop without `volatile` or an atomic may never be seen
  to change (the compiler can hoist the read); double-checked locking needs a `volatile` field;
  in C and C++ a data race is undefined behaviour.

## Correctness for concurrent objects

- **Linearizability** (Herlihy and Wing): every operation appears to take effect instantly at some
  point between its call and its return, so the object behaves like a sequential one. It is the
  usual correctness condition for concurrent data structures (`ConcurrentHashMap`, atomics) and,
  in distributed systems, the strongest single-object consistency model.
- **Lock-free** algorithms guarantee that some thread always makes progress (no thread can block
  the others by being paused); **wait-free** ones that every thread does. They are built from CAS
  loops: the Treiber stack, the Michael-Scott queue.
- The **ABA problem**: a CAS sees A, the value changes to B and back to A, and the CAS wrongly
  succeeds. Fixes: version counters (tagged pointers) or safe memory reclamation; garbage
  collectors avoid most of it.

## Shared memory or message passing

Two families of models:

- **Shared memory with locks**: threads share objects and protect them (Java, C#, C++).
- **Message passing**: no shared mutable state; processes communicate by sending messages.
  **CSP** (Hoare) with channels, as in Go ("do not communicate by sharing memory; share memory by
  communicating"), and the **actor model** (Erlang, Akka, Orleans), where each actor owns its state
  and processes one message at a time.

Message passing removes data races by construction but not deadlock (two goroutines each waiting
to receive from the other) or races on the order of messages. Go's race detector, Java's
jcstress and model checkers such as TLA+ help find what reasoning misses.

## Summary

- Concurrency means many interleavings; `count++` is read-modify-write and not atomic.
- Mutual exclusion needs hardware atomics in practice; CAS is the universal building block.
- Semaphores, monitors and condition variables coordinate threads; always wait in a loop.
- Deadlock needs four conditions; a global lock order removes the circular wait.
- Memory models guarantee sequential consistency only to data-race-free programs, defined by
  happens-before.
- Linearizability defines correct concurrent objects; lock-free code uses CAS and must handle ABA.
- Message passing (CSP, actors) avoids shared state but not every concurrency bug.

## Further reading

- Herlihy, Shavit, Luchangco and Spear, *The Art of Multiprocessor Programming*, 2nd ed.
- Arpaci-Dusseau and Arpaci-Dusseau, *Operating Systems: Three Easy Pieces*, chapters 26–32 (concurrency, locks, condition variables, semaphores, common bugs): <https://pages.cs.wisc.edu/~remzi/OSTEP/>
- Goetz et al., *Java Concurrency in Practice*, chapters 2, 3, 10 and 16
- Leslie Lamport, "How to Make a Multiprocessor Computer That Correctly Executes Multiprocess Programs" (1979)
- Herlihy and Wing, "Linearizability: A Correctness Condition for Concurrent Objects" (1990)
- Russ Cox, "Memory Models" series: <https://research.swtch.com/mm>
- Allen B. Downey, *The Little Book of Semaphores*: <https://greenteapress.com/wp/semaphores/>
