# Page Table

Build the translation hardware of the previous lesson: a two-level page table that creates
second-level tables only where memory is mapped, permission checks for reads, writes, execution
and user mode, accessed and dirty bits, and a TLB with LRU replacement. The hidden tests try the
bug that has bitten real kernels: a translation still cached in the TLB after its page was
unmapped or made read-only. Press **Enter** to create the kata folder and open it.
