# Memory Hierarchy: Registers, L1/L2/L3 Caches and RAM

A core can execute several instructions per nanosecond, but a trip to main memory takes about
100 ns. If every load went to RAM, the CPU would idle 99% of the time. It does not, because of
**caches**: small, fast memories that keep copies of recently used data close to the core. This
lesson explains how they work, because the speed of a lot of real code is decided by how well it
uses them.

## The hierarchy

| Level | Typical size | Latency | Technology |
| --- | --- | --- | --- |
| Registers | ~1 KiB | 0 cycles | flip-flops |
| L1 cache (per core, data + instructions) | 32–64 KiB each | ~4 cycles | SRAM |
| L2 cache (per core) | 1–2 MiB | ~12 cycles | SRAM |
| L3 cache (shared) | 16–128 MiB | ~40 cycles | SRAM |
| Main memory | 16 GiB–2 TiB | ~300 cycles | DRAM |
| SSD | TBs | ~100,000 cycles | flash |

Fast memory is expensive and must be physically close to the core, so it is small. The hierarchy
works because programs have **locality**:

- **Temporal locality**: data used now is likely to be used again soon (a loop counter, a hot
  object).
- **Spatial locality**: data near what is used now is likely to be used soon (the next array
  element, the next field).

## Cache lines

Caches do not move single bytes. They move **lines** (blocks) of 64 bytes on x86 and most ARM
cores. Reading one `int` from RAM brings its 15 neighbours along for free: that is how spatial
locality pays off. Hardware **prefetchers** also notice sequential or strided access and fetch the
next lines before you ask.

## Where does a line go? Tag, index, offset

A cache with *S* sets, *E* lines per set (the **associativity**, or "ways") and *B*-byte lines
splits each address into three fields:

```text
| tag                         | set index (log₂ S bits) | offset (log₂ B bits) |
```

- The **offset** selects the byte inside the line.
- The **index** selects the one set where the line may live.
- The **tag** is stored with the line to tell which of the many addresses that map to this set it
  is.

Kinds of cache:

- **Direct-mapped** (E = 1): each address has exactly one possible slot. Simple and fast, but two
  hot addresses with the same index keep evicting each other.
- **E-way set-associative**: a line may sit in any of the E ways of its set. L1 caches are
  typically 8- or 12-way.
- **Fully associative** (one set): any line anywhere; used for small structures such as the TLB.

Example: 32 KiB, 8-way, 64-byte lines → 512 lines, 64 sets → 6 offset bits, 6 index bits, and
the rest of the address is the tag.

## Hits, misses and replacement

On a **hit**, the data comes from the cache. On a **miss**, the line is fetched from the next
level, and if its set is full, one line must be **evicted**. The usual policy is **LRU** (least
recently used), or a cheap approximation of it in hardware.

The "three Cs" of misses:

- **Compulsory** (cold): the first access to a line ever.
- **Capacity**: the working set is larger than the cache.
- **Conflict**: the cache has room, but too many hot lines map to the same set. Higher
  associativity reduces them.

Average memory access time: `AMAT = hit time + miss rate × miss penalty`. With a 4-cycle hit, a
5% miss rate and a 100-cycle penalty, AMAT = 4 + 0.05 × 100 = 9 cycles: the misses dominate.

## Writes

- **Write-through**: every store also goes to the next level. Simple, lots of traffic.
- **Write-back**: stores only mark the line **dirty**; it is written when evicted. What modern
  caches use.
- On a write miss, **write-allocate** fetches the line first (usual with write-back).

## Several cores: coherence and false sharing

Each core has private L1 and L2 caches, so two cores can hold copies of the same line. A
**coherence protocol** (MESI and its variants: Modified, Exclusive, Shared, Invalid) keeps them
consistent: before a core writes a line, every other copy is invalidated.

**False sharing**: two threads write *different* variables that happen to sit in the *same* line.
Coherence moves the line back and forth between the cores on every write, and the program slows
down although the threads share nothing logically. The fix is to put hot per-thread data on
separate lines (padding, `@Contended` in the JDK) or to accumulate locally and combine.

## Writing cache-friendly code

- Walk arrays **sequentially**. In row-major languages (C, Java arrays of arrays, NumPy default),
  loop over the last index innermost.
- Prefer **contiguous** data to pointer chasing. An array of numbers beats a linked list of boxed
  numbers, often by 10× or more. This is the reason `ArrayList` beats `LinkedList` (see the Data
  Structures & Algorithms track).
- **Structure of arrays** instead of an array of structures when a loop uses one field of many
  objects.
- **Blocking** (tiling): process a matrix in tiles that fit in the cache.
- Keep hot data small: smaller types, fewer pointers, and compact object layouts.

## Summary

- Memory is ~100× slower than the core; a hierarchy of caches hides it thanks to locality.
- Caches move 64-byte lines; an address splits into tag, set index and offset.
- Associativity trades lookup cost for fewer conflict misses; LRU decides evictions.
- Misses are compulsory, capacity or conflict; AMAT shows how much they cost.
- Coherence keeps cores consistent, and false sharing is its performance trap.
- Sequential, contiguous, compact data is fast.

## Further reading

- Bryant and O'Hallaron, *Computer Systems: A Programmer's Perspective*, 3rd ed., chapter 6 "The Memory Hierarchy": <https://csapp.cs.cmu.edu/>
- Patterson and Hennessy, *Computer Organization and Design, RISC-V Edition*, chapter 5 "Large and Fast: Exploiting Memory Hierarchy"
- Ulrich Drepper, "What Every Programmer Should Know About Memory" (2007): <https://people.freebsd.org/~lstewart/articles/cpumemory.pdf>
- Igor Ostrovsky, "Gallery of Processor Cache Effects": <https://igoro.com/archive/gallery-of-processor-cache-effects/>
- Jeff Dean and Peter Norvig, "Latency numbers every programmer should know": <https://gist.github.com/jboner/2841832>
- CMU 15-213 Introduction to Computer Systems, lecture slides on caches: <https://www.cs.cmu.edu/~213/>
