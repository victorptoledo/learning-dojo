# Page Table

Write the memory management unit of the previous lesson in `src/lib.rs`: a **two-level page
table** for 32-bit virtual addresses with 4 KiB pages, permission checks, accessed and dirty bits,
and a small **TLB** with LRU replacement in front of it. The public types are declared; you add
the fields of `Mmu` and write the functions.

## Addresses

```text
 31          22 21          12 11            0
| directory (10) | table (10)  |  offset (12)  |
```

`split(vaddr)` returns `(directory index, table index, offset)`. The **virtual page number** is
`vaddr >> 12`; a translation replaces it with the page's **frame** number and keeps the offset:
`physical = frame << 12 | offset`. Frames have 20 bits.

## The page table

The directory has 1,024 slots, each empty or pointing to a **second-level table** of 1,024 entries.
A table is created when the first page in its 4 MiB range is mapped and **freed** when its last
page is unmapped (`table_count()` tells the tests how many exist).

| Method | Does |
| --- | --- |
| `Mmu::new(tlb_capacity)` | an empty page table and an empty TLB of that many entries (0 = no TLB) |
| `map(vaddr, frame, flags)` | maps the page at `vaddr`. Errors: `Misaligned(vaddr)` if `vaddr` is not a multiple of 4,096, `BadFrame(frame)` if the frame does not fit in 20 bits, `AlreadyMapped(vaddr)` |
| `unmap(vaddr)` | removes the page containing `vaddr` and returns its frame, or `None` |
| `protect(vaddr, flags)` | changes the flags of the page containing `vaddr`; `false` if it is not mapped |
| `accessed(vaddr)`, `dirty(vaddr)` | the bits of the page containing `vaddr`, or `None` if it is not mapped |
| `clear_accessed()` | clears every accessed bit |
| `flush_tlb()` | empties the TLB |
| `stats()` | `tlb_hits`, `tlb_misses` and `faults` |

## Translation

`translate(vaddr, kind, mode)` with `kind` `Read`, `Write` or `Execute` and `mode` `User` or
`Kernel`:

1. Look the page number up in the **TLB**. A hit makes the entry the most recently used. On a
   miss, walk the tables; if the page is mapped, insert its translation (frame and flags) at the
   front of the TLB, evicting the least recently used entry when it is full.
2. If the page is not mapped: `Err(Fault::NotMapped { vaddr })`.
3. Check the flags, on hits as on walks: a `Write` needs `writable`, an `Execute` needs
   `executable` (reading is always allowed), and `User` mode needs `user` (the kernel ignores the
   `user` flag but obeys the others). Otherwise: `Err(Fault::Protection { vaddr, kind })`.
4. On success, set the page's **accessed** bit, and its **dirty** bit for a `Write`, then return
   the physical address.

Every lookup counts as a TLB hit or miss, and every `Err` as a fault.

**Keep the TLB consistent**: `unmap` and `protect` must remove the page's TLB entry. Real kernels
do exactly this (`invlpg` on x86); forgetting it is a classic and dangerous bug.

## What the hidden tests check

Address splitting, map errors, translations, every protection rule on walks and on TLB hits,
accessed and dirty bits, TLB hits, LRU eviction, a zero-sized TLB, invalidation on `unmap` and
`protect`, `flush_tlb`, tables created and freed on demand, 50,000 random operations against a
`HashMap` model, and a million translations within a time limit.

Run the visible tests with `cargo test`.
