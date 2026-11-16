//! A two-level page table for 32-bit addresses with 4 KiB pages, and a TLB in front of it.

pub const PAGE_SIZE: u32 = 4096;
pub const ENTRIES: usize = 1024;

/// What a page allows. Reading is always allowed on a mapped page.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Flags {
    pub writable: bool,
    pub user: bool,
    pub executable: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AccessKind {
    Read,
    Write,
    Execute,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Mode {
    User,
    Kernel,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Fault {
    /// No page is mapped at this address.
    NotMapped { vaddr: u32 },
    /// The page is mapped but does not allow this access.
    Protection { vaddr: u32, kind: AccessKind },
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MapError {
    Misaligned(u32),
    AlreadyMapped(u32),
    BadFrame(u32),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Stats {
    pub tlb_hits: u64,
    pub tlb_misses: u64,
    pub faults: u64,
}

pub struct Mmu {
    // Add your fields: the page directory (1,024 optional second-level tables of 1,024
    // optional entries), the TLB with its capacity, and the statistics.
    tlb_capacity: usize,
}

/// (directory index, table index, offset) of a virtual address.
pub fn split(vaddr: u32) -> (usize, usize, u32) {
    todo!("split {vaddr:#010x} into 10 + 10 + 12 bits")
}

impl Mmu {
    pub fn new(tlb_capacity: usize) -> Mmu {
        todo!("an empty page table and a TLB of {tlb_capacity} entries")
    }

    pub fn map(&mut self, vaddr: u32, frame: u32, flags: Flags) -> Result<(), MapError> {
        todo!("map the page at {vaddr:#x} to frame {frame} with {flags:?}")
    }

    /// Removes the page containing `vaddr`; returns its frame. Frees an emptied table.
    pub fn unmap(&mut self, vaddr: u32) -> Option<u32> {
        todo!("unmap {vaddr:#x}")
    }

    /// Changes the flags of the page containing `vaddr`; false if it is not mapped.
    pub fn protect(&mut self, vaddr: u32, flags: Flags) -> bool {
        todo!("set {flags:?} on {vaddr:#x}")
    }

    pub fn translate(&mut self, vaddr: u32, kind: AccessKind, mode: Mode) -> Result<u32, Fault> {
        todo!("translate {vaddr:#x} for {kind:?} in {mode:?} mode (TLB size {})", self.tlb_capacity)
    }

    pub fn accessed(&self, vaddr: u32) -> Option<bool> {
        todo!("the accessed bit of {vaddr:#x}")
    }

    pub fn dirty(&self, vaddr: u32) -> Option<bool> {
        todo!("the dirty bit of {vaddr:#x}")
    }

    /// Clears the accessed bit of every page, as a clock algorithm sweep would.
    pub fn clear_accessed(&mut self) {
        todo!("clear every accessed bit")
    }

    /// Empties the TLB, as a switch to another address space does.
    pub fn flush_tlb(&mut self) {
        todo!("empty the TLB")
    }

    pub fn stats(&self) -> Stats {
        todo!("the statistics")
    }

    /// How many second-level tables exist.
    pub fn table_count(&self) -> usize {
        todo!("count the second-level tables")
    }
}
