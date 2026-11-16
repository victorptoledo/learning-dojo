# Layers of Abstraction: What Computer Science Studies

You can write useful software for years without knowing what a cache line, a page fault or a
context switch is. That is the whole point of the layers below you: they **hide** detail so you
can think about your problem. This track opens those layers one by one, because every one of
them eventually **leaks**, and when it does, the bug or the slowdown makes no sense until you
know what is underneath.

## The stack

From the top to the bottom, a line such as `total = price * quantity` passes through:

| Layer | What it gives the layer above | Covered in |
| --- | --- | --- |
| Application | your problem in your words | — |
| Language and runtime | types, functions, objects, garbage collection | Compilers and Interpreters |
| Operating system | processes, threads, files, sockets, virtual memory | Processes, Virtual Memory, File Systems |
| Instruction set (ISA) | registers, instructions such as `add`, `load`, `branch` | The CPU |
| Microarchitecture | pipelines, caches, branch predictors | The CPU, Memory Hierarchy |
| Digital logic | gates, adders, registers, a clock | Logic Gates |
| Physics | transistors as switches, voltages as 0 and 1 | — |

Beside the machine sits the **network**, which turns many machines into one system, and above
the operating system sit **databases**, which turn files into durable, queryable data. The track
covers both.

Each boundary is a **contract**. The ISA promises "`add r1, r2, r3` puts `r2 + r3` in `r1`",
whatever the pipeline does to get there. The OS promises "this process has its own memory",
whatever physical RAM is free. Understanding a system means knowing the contracts, and knowing
roughly how each one is kept.

## Abstractions leak

Joel Spolsky's *Law of Leaky Abstractions*: every non-trivial abstraction leaks somewhere. A few
leaks you will meet in this track:

- `0.1 + 0.2 == 0.3` is false in Java, Python, JavaScript, C# and Go. Real numbers are stored in
  binary floating point (next lesson).
- Summing a 2D array row by row can be several times faster than column by column. Same
  instructions, different **cache** behaviour.
- A container is killed with "OOMKilled" although `top` showed free memory: **virtual memory**
  and the kernel's accounting.
- A web request that runs 200 small queries is slow although each query takes 0.1 ms in the
  database: **network** round trips.
- A regular expression freezes a server on one input: a **backtracking** engine instead of an
  automaton.
- A counter updated by two threads loses increments: a **race condition**.

## The questions computer science asks

Engineering asks "how do I build this?". Computer science adds three deeper questions, and the
track has a part for each:

1. **What can be computed at all?** Some problems have no algorithm, for example deciding whether
   an arbitrary program halts (Computability).
2. **What can be computed efficiently?** Some problems seem to need exponential time however
   clever you are: the P versus NP question (Complexity Classes).
3. **How do we build computations that are correct and reliable** when they run concurrently,
   crash halfway or talk over unreliable networks? (Concurrency, Database Internals.)

Algorithms and data structures, the craft of solving problems efficiently, have their own track:
**Data Structures & Algorithms**. This track links to it instead of repeating it.

## Numbers to keep in your head

Approximate latencies on a current server, rounded so they are easy to remember:

| Operation | Time | Relative |
| --- | --- | --- |
| CPU cycle, register access | 0.3 ns | 1 |
| L1 cache hit | 1 ns | 3 |
| L2 cache hit | 4 ns | 12 |
| L3 cache hit | 15 ns | 50 |
| Main memory (RAM) | 80–100 ns | 300 |
| Read 4 KiB from an NVMe SSD | 20–100 µs | 100,000 |
| Round trip in a data centre | 0.5 ms | 1,500,000 |
| Read from a spinning disk (seek) | 5–10 ms | 25,000,000 |
| Round trip Europe ↔ US | 80–150 ms | 400,000,000 |

Two lessons follow from the table: **memory is far away from the CPU**, and **the network is far
away from memory**. Most performance work is about not going to the slower level more often than
necessary.

## How to use this track

- Every lesson ends with a quiz and a written exercise. The exercise is graded by a rubric; press
  `A` for an AI review prompt.
- The katas build the machinery: a float decoder, a cache simulator, a scheduler, a page table, a
  parser, a bytecode compiler, a regex engine and a storage engine. They run in Python, Rust and
  Java; you need no prior knowledge of Rust beyond the Rust track's first phase, and the starters
  give you the types.
- The **bridges** connect each idea to the language you already use: Java, Python,
  JavaScript/TypeScript, C# or Go.

## Summary

- A computer is a stack of layers, each with a contract that hides the one below.
- Abstractions leak: floating point, caches, memory limits, the network, regex engines, races.
- Computer science asks what is computable, what is efficiently computable, and how to compute
  correctly and reliably.
- Memorise the order of magnitude of the latency table; it makes estimates possible.

## Further reading

- Bryant and O'Hallaron, *Computer Systems: A Programmer's Perspective*, 3rd ed., chapter 1 "A Tour of Computer Systems": <https://csapp.cs.cmu.edu/>
- Patt and Patel, *Introduction to Computing Systems: From Bits and Gates to C/C++ and Beyond*, 3rd ed., chapter 1 (the levels of transformation)
- Nisan and Schocken, *The Elements of Computing Systems* (Nand to Tetris), 2nd ed.: <https://www.nand2tetris.org/>
- Joel Spolsky, "The Law of Leaky Abstractions": <https://www.joelonsoftware.com/2002/11/11/the-law-of-leaky-abstractions/>
- Harvard CS50, lecture notes: <https://cs50.harvard.edu/x/>
- roadmap.sh, Computer Science roadmap: <https://roadmap.sh/computer-science>
