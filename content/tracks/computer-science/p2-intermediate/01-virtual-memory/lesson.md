# Virtual Memory, Paging and the TLB

Two processes can both store a value at address `0x7ffd5a3c1000` without interfering, a process
can reserve 100 GiB on a machine with 16 GiB of RAM, and `fork()` of a 4 GiB process takes
milliseconds. All of this comes from one idea: the addresses a program uses are **virtual**, and
hardware translates them to **physical** addresses on every single memory access, using tables
the kernel controls.

## Why virtual memory

- **Isolation**: each process has its own address space; it cannot even name another process's
  memory.
- **Simplicity**: every program sees a large, contiguous address space starting at the same
  layout (code, data, heap growing up, stack growing down), whatever RAM is free.
- **Overcommit and laziness**: memory is reserved cheaply and only backed by RAM when touched.
- **Sharing**: the same physical pages (a shared library, a file) can appear in many processes.
- **Protection**: pages can be read-only, non-executable, or kernel-only.

## Pages and frames

Memory is managed in fixed-size blocks: **pages** in virtual memory and **frames** in physical
memory, usually **4 KiB** (with optional 2 MiB and 1 GiB *huge pages*). A virtual address splits
into a **virtual page number** (VPN) and an **offset** inside the page; translation replaces the
VPN with a **physical frame number** (PFN) and keeps the offset.

```text
virtual  0x0040_3A7C  →  VPN 0x00403, offset 0xA7C
page table: VPN 0x00403 → PFN 0x1F2E0
physical 0x1F2E_0A7C
```

## Page tables

The kernel keeps, per process, a **page table** mapping VPNs to **page table entries** (PTEs). A
PTE holds the frame number and flag bits:

| Bit | Meaning |
| --- | --- |
| Present (valid) | the page is in RAM; if 0, access causes a page fault |
| Writable | writes allowed; otherwise a write faults |
| User | accessible in user mode, not only by the kernel |
| No-execute (NX) | instructions cannot be fetched from it (stops code injection) |
| Accessed | set by hardware on any access (used by replacement) |
| Dirty | set by hardware on a write (the page must be saved before reuse) |

A flat table for a 48-bit address space with 4 KiB pages would need 2³⁶ entries per process.
So tables are **multi-level** (a tree): x86-64 uses four levels of 512 entries (9 bits of VPN
each) and newer chips five. Only the parts of the tree that cover used memory exist. A 32-bit
example with two levels of 10 bits:

```text
| 10 bits: directory index | 10 bits: table index | 12 bits: offset |
```

The CPU's **memory management unit** (MMU) walks this tree in hardware, starting from a register
the kernel sets per process (`CR3` on x86). Switching processes means switching that register.

## The TLB

A walk costs up to four extra memory accesses per access, which would be ruinous. The **TLB**
(translation lookaside buffer) is a small, very fast cache of recent translations: tens of
entries in L1, a thousand or two in L2. With 4 KiB pages, 1,536 entries cover only 6 MiB, so
programs that touch memory randomly over gigabytes suffer **TLB misses**; huge pages (2 MiB) let
one entry cover 512 times more. Switching address spaces invalidates TLB entries, unless they
are tagged with an address-space id (PCID, ASID).

## Page faults and demand paging

When the MMU finds a non-present page or a forbidden access, it raises a **page fault** and the
kernel decides:

- **Minor fault**: the page is valid but not mapped yet (first touch of a heap page, a shared
  library already in RAM). The kernel picks a free frame, zeroes or maps it, and resumes the
  instruction: microseconds.
- **Major fault**: the data must be read from disk (a file-backed page, or a page that was
  swapped out): from tens of microseconds on an SSD to milliseconds on a disk.
- **Invalid access**: no mapping at all, or a write to a read-only page. The process gets a
  **segmentation fault** (`SIGSEGV`); in Java a `NullPointerException` is in fact often produced
  this way, from a fault on page 0.

Allocating memory (`malloc`, `mmap`, a JVM reserving its heap) only creates mappings; RAM is
assigned **on demand** at the first touch. That is why the **virtual size** of a process (VSZ) can
be huge and harmless, while the **resident set size** (RSS) is what uses RAM.

## Page replacement and thrashing

When RAM is full, the kernel must evict a page to make room: write it to swap if it is dirty and
anonymous, or simply drop it if it is a clean file page (it can be read again). Choosing the
victim is the same problem as cache replacement: **LRU** is ideal but expensive, so kernels use
approximations such as the **clock** (second-chance) algorithm over the accessed bits, and Linux's
active and inactive lists. **Belady's** optimal algorithm (evict the page used furthest in the
future) is the theoretical bound.

If the working sets of the running processes do not fit in RAM, the system spends its time
faulting pages in and out: **thrashing**. Throughput collapses. The fixes are more RAM, fewer or
smaller processes, or killing something: Linux's **OOM killer** does the last, and in a
container the **cgroup memory limit** triggers it for the container alone.

## Tricks built on paging

- **Copy-on-write**: `fork()` copies only the page tables and marks every page read-only in both
  processes; a page is physically copied when one of them first writes to it. Redis uses this to
  snapshot its whole dataset while it keeps serving writes.
- **Memory-mapped files** (`mmap`): a file appears as a range of memory; reads fault pages in from
  the **page cache**, and writes mark them dirty for the kernel to write back. Databases (LMDB,
  older MongoDB), search engines (Lucene) and the JVM's class data sharing use it.
- **Shared libraries**: one copy of `libc` in RAM, mapped into every process.
- **Guard pages**: an unmapped page below each thread stack turns a stack overflow into a clean
  fault instead of silent corruption.
- **Zero pages and overcommit**: fresh anonymous pages all map to one shared zero frame until
  written.

## Summary

- Programs use virtual addresses; the MMU translates them per page through page tables the kernel
  controls.
- A PTE holds a frame number and present, writable, user, no-execute, accessed and dirty bits.
- Multi-level tables keep translation compact; the TLB caches translations; huge pages extend
  its reach.
- Page faults implement demand paging; replacement and thrashing mirror caching at a larger scale.
- Copy-on-write, `mmap`, shared libraries and guard pages are all paging tricks.
- RSS, not VSZ, is the memory that counts against RAM and container limits.

## Further reading

- Arpaci-Dusseau and Arpaci-Dusseau, *Operating Systems: Three Easy Pieces*, chapters 13–23 (address spaces, paging, TLBs, smaller tables, swapping): <https://pages.cs.wisc.edu/~remzi/OSTEP/>
- Bryant and O'Hallaron, *Computer Systems: A Programmer's Perspective*, 3rd ed., chapter 9 "Virtual Memory"
- Silberschatz, Galvin and Gagne, *Operating System Concepts*, 10th ed., chapters 9 "Main Memory" and 10 "Virtual Memory"
- Mel Gorman, *Understanding the Linux Virtual Memory Manager*: <https://www.kernel.org/doc/gorman/>
- Linux kernel documentation, "Control Group v2", memory controller: <https://docs.kernel.org/admin-guide/cgroup-v2.html>
