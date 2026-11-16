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

#[derive(Debug, Clone, Copy)]
struct Entry {
    frame: u32,
    flags: Flags,
    accessed: bool,
    dirty: bool,
}

struct Table {
    entries: Vec<Option<Entry>>,
    used: usize,
}

#[derive(Debug, Clone, Copy)]
struct TlbEntry {
    vpn: u32,
    frame: u32,
    flags: Flags,
}

pub struct Mmu {
    directory: Vec<Option<Table>>,
    /// Most recently used first.
    tlb: Vec<TlbEntry>,
    tlb_capacity: usize,
    stats: Stats,
}

/// (directory index, table index, offset) of a virtual address.
pub fn split(vaddr: u32) -> (usize, usize, u32) {
    (
        (vaddr >> 22) as usize,
        ((vaddr >> 12) & 0x3FF) as usize,
        vaddr & 0xFFF,
    )
}

fn allowed(flags: Flags, kind: AccessKind, mode: Mode) -> bool {
    let kind_ok = match kind {
        AccessKind::Read => true,
        AccessKind::Write => flags.writable,
        AccessKind::Execute => flags.executable,
    };
    kind_ok && (mode == Mode::Kernel || flags.user)
}

impl Mmu {
    pub fn new(tlb_capacity: usize) -> Mmu {
        Mmu {
            directory: (0..ENTRIES).map(|_| None).collect(),
            tlb: Vec::with_capacity(tlb_capacity),
            tlb_capacity,
            stats: Stats::default(),
        }
    }

    pub fn map(&mut self, vaddr: u32, frame: u32, flags: Flags) -> Result<(), MapError> {
        if vaddr % PAGE_SIZE != 0 {
            return Err(MapError::Misaligned(vaddr));
        }
        if frame >= 1 << 20 {
            return Err(MapError::BadFrame(frame));
        }
        let (dir, index, _) = split(vaddr);
        let table = self.directory[dir].get_or_insert_with(|| Table {
            entries: vec![None; ENTRIES],
            used: 0,
        });
        if table.entries[index].is_some() {
            return Err(MapError::AlreadyMapped(vaddr));
        }
        table.entries[index] = Some(Entry {
            frame,
            flags,
            accessed: false,
            dirty: false,
        });
        table.used += 1;
        Ok(())
    }

    /// Removes the page containing `vaddr`; returns its frame. Frees an emptied table.
    pub fn unmap(&mut self, vaddr: u32) -> Option<u32> {
        let (dir, index, _) = split(vaddr);
        let table = self.directory[dir].as_mut()?;
        let entry = table.entries[index].take()?;
        table.used -= 1;
        if table.used == 0 {
            self.directory[dir] = None;
        }
        self.invalidate(vaddr);
        Some(entry.frame)
    }

    /// Changes the flags of the page containing `vaddr`; false if it is not mapped.
    pub fn protect(&mut self, vaddr: u32, flags: Flags) -> bool {
        match self.entry_mut(vaddr) {
            Some(entry) => {
                entry.flags = flags;
                self.invalidate(vaddr);
                true
            }
            None => false,
        }
    }

    pub fn translate(&mut self, vaddr: u32, kind: AccessKind, mode: Mode) -> Result<u32, Fault> {
        let vpn = vaddr >> 12;
        let offset = vaddr & 0xFFF;
        let cached = self.tlb.iter().position(|e| e.vpn == vpn);
        let (frame, flags) = match cached {
            Some(position) => {
                self.stats.tlb_hits += 1;
                let entry = self.tlb.remove(position);
                self.tlb.insert(0, entry);
                (entry.frame, entry.flags)
            }
            None => {
                self.stats.tlb_misses += 1;
                let Some(entry) = self.entry_mut(vaddr).copied() else {
                    self.stats.faults += 1;
                    return Err(Fault::NotMapped { vaddr });
                };
                if self.tlb_capacity > 0 {
                    if self.tlb.len() == self.tlb_capacity {
                        self.tlb.pop();
                    }
                    self.tlb.insert(
                        0,
                        TlbEntry {
                            vpn,
                            frame: entry.frame,
                            flags: entry.flags,
                        },
                    );
                }
                (entry.frame, entry.flags)
            }
        };
        if !allowed(flags, kind, mode) {
            self.stats.faults += 1;
            return Err(Fault::Protection { vaddr, kind });
        }
        let entry = self.entry_mut(vaddr).expect("a translated page is mapped");
        entry.accessed = true;
        if kind == AccessKind::Write {
            entry.dirty = true;
        }
        Ok(frame << 12 | offset)
    }

    pub fn accessed(&self, vaddr: u32) -> Option<bool> {
        self.entry(vaddr).map(|e| e.accessed)
    }

    pub fn dirty(&self, vaddr: u32) -> Option<bool> {
        self.entry(vaddr).map(|e| e.dirty)
    }

    /// Clears the accessed bit of every page, as a clock algorithm sweep would.
    pub fn clear_accessed(&mut self) {
        for table in self.directory.iter_mut().flatten() {
            for entry in table.entries.iter_mut().flatten() {
                entry.accessed = false;
            }
        }
    }

    /// Empties the TLB, as a switch to another address space does.
    pub fn flush_tlb(&mut self) {
        self.tlb.clear();
    }

    pub fn stats(&self) -> Stats {
        self.stats
    }

    /// How many second-level tables exist.
    pub fn table_count(&self) -> usize {
        self.directory.iter().flatten().count()
    }

    fn entry(&self, vaddr: u32) -> Option<&Entry> {
        let (dir, index, _) = split(vaddr);
        self.directory[dir].as_ref()?.entries[index].as_ref()
    }

    fn entry_mut(&mut self, vaddr: u32) -> Option<&mut Entry> {
        let (dir, index, _) = split(vaddr);
        self.directory[dir].as_mut()?.entries[index].as_mut()
    }

    fn invalidate(&mut self, vaddr: u32) {
        let vpn = vaddr >> 12;
        self.tlb.retain(|e| e.vpn != vpn);
    }
}
