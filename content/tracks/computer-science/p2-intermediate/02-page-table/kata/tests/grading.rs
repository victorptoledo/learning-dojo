use std::collections::HashMap;
use std::time::{Duration, Instant};

use kata::{split, AccessKind, Fault, Flags, MapError, Mmu, Mode, Stats};

use AccessKind::{Execute, Read, Write};
use Mode::{Kernel, User};

const RW: Flags = Flags { writable: true, user: true, executable: false };
const RO: Flags = Flags { writable: false, user: true, executable: false };
const RX: Flags = Flags { writable: false, user: true, executable: true };
const KERNEL_RW: Flags = Flags { writable: true, user: false, executable: false };

#[test]
fn split_examples() {
    assert_eq!(split(0), (0, 0, 0));
    assert_eq!(split(0xFFFF_FFFF), (1023, 1023, 0xFFF));
    assert_eq!(split(0x0000_1000), (0, 1, 0));
    assert_eq!(split(0x0040_0000), (1, 0, 0));
    assert_eq!(split(0xC012_3456), (0x300, 0x123, 0x456));
}

#[test]
fn map_rejects_misaligned_addresses_bad_frames_and_double_mappings() {
    let mut mmu = Mmu::new(4);
    assert_eq!(mmu.map(0x1001, 5, RW), Err(MapError::Misaligned(0x1001)));
    assert_eq!(mmu.map(0x1000, 1 << 20, RW), Err(MapError::BadFrame(1 << 20)));
    assert_eq!(mmu.map(0x1000, (1 << 20) - 1, RW), Ok(()));
    assert_eq!(mmu.map(0x1000, 7, RW), Err(MapError::AlreadyMapped(0x1000)));
}

#[test]
fn translation_replaces_the_page_number_and_keeps_the_offset() {
    let mut mmu = Mmu::new(4);
    mmu.map(0x0000_0000, 0x00010, RW).unwrap();
    mmu.map(0xBFFF_F000, 0xABCDE, RW).unwrap();
    assert_eq!(mmu.translate(0x0000_0FFF, Read, User), Ok(0x0001_0FFF));
    assert_eq!(mmu.translate(0xBFFF_F004, Write, User), Ok(0xABCD_E004));
}

#[test]
fn unmapped_addresses_fault() {
    let mut mmu = Mmu::new(4);
    mmu.map(0x5000, 1, RW).unwrap();
    assert_eq!(mmu.translate(0x6000, Read, User), Err(Fault::NotMapped { vaddr: 0x6000 }));
    assert_eq!(mmu.translate(0x4FFF, Read, Kernel), Err(Fault::NotMapped { vaddr: 0x4FFF }));
    assert_eq!(
        mmu.translate(0x8040_0000, Write, User),
        Err(Fault::NotMapped { vaddr: 0x8040_0000 })
    );
}

#[test]
fn protection_bits_are_enforced() {
    let mut mmu = Mmu::new(4);
    mmu.map(0x1000, 1, RO).unwrap();
    mmu.map(0x2000, 2, RX).unwrap();
    mmu.map(0x3000, 3, KERNEL_RW).unwrap();
    mmu.map(0x4000, 4, RW).unwrap();
    assert!(mmu.translate(0x1010, Read, User).is_ok());
    assert_eq!(
        mmu.translate(0x1010, Write, User),
        Err(Fault::Protection { vaddr: 0x1010, kind: Write })
    );
    assert_eq!(
        mmu.translate(0x1010, Write, Kernel),
        Err(Fault::Protection { vaddr: 0x1010, kind: Write })
    );
    assert!(mmu.translate(0x2000, Execute, User).is_ok());
    assert_eq!(
        mmu.translate(0x4000, Execute, User),
        Err(Fault::Protection { vaddr: 0x4000, kind: Execute })
    );
    assert_eq!(
        mmu.translate(0x3008, Read, User),
        Err(Fault::Protection { vaddr: 0x3008, kind: Read })
    );
    assert!(mmu.translate(0x3008, Write, Kernel).is_ok());
    assert!(mmu.translate(0x4000, Write, Kernel).is_ok());
}

#[test]
fn protection_is_checked_on_tlb_hits_too() {
    let mut mmu = Mmu::new(4);
    mmu.map(0x1000, 1, RO).unwrap();
    assert!(mmu.translate(0x1000, Read, User).is_ok()); // now cached
    assert!(mmu.translate(0x1000, Write, User).is_err());
    assert_eq!(mmu.stats(), Stats { tlb_hits: 1, tlb_misses: 1, faults: 1 });
}

#[test]
fn accessed_and_dirty_bits() {
    let mut mmu = Mmu::new(4);
    mmu.map(0x1000, 1, RW).unwrap();
    mmu.map(0x2000, 2, RO).unwrap();
    assert_eq!((mmu.accessed(0x1000), mmu.dirty(0x1000)), (Some(false), Some(false)));
    assert_eq!(mmu.accessed(0x9000), None);
    mmu.translate(0x1234, Read, User).unwrap();
    assert_eq!((mmu.accessed(0x1000), mmu.dirty(0x1FFF)), (Some(true), Some(false)));
    mmu.translate(0x1234, Write, User).unwrap(); // a TLB hit still sets the dirty bit
    assert_eq!(mmu.dirty(0x1000), Some(true));
    let _ = mmu.translate(0x2000, Write, User); // a failed access sets nothing
    assert_eq!((mmu.accessed(0x2000), mmu.dirty(0x2000)), (Some(false), Some(false)));
    mmu.clear_accessed();
    assert_eq!((mmu.accessed(0x1000), mmu.dirty(0x1000)), (Some(false), Some(true)));
}

#[test]
fn the_tlb_caches_translations() {
    let mut mmu = Mmu::new(4);
    mmu.map(0x1000, 1, RW).unwrap();
    for offset in 0..10 {
        mmu.translate(0x1000 + offset * 100, Read, User).unwrap();
    }
    assert_eq!(mmu.stats(), Stats { tlb_hits: 9, tlb_misses: 1, faults: 0 });
}

#[test]
fn the_tlb_evicts_the_least_recently_used_entry() {
    let mut mmu = Mmu::new(2);
    for page in 1..=3 {
        mmu.map(page * 0x1000, page, RW).unwrap();
    }
    mmu.translate(0x1000, Read, User).unwrap(); // miss: [1]
    mmu.translate(0x2000, Read, User).unwrap(); // miss: [2, 1]
    mmu.translate(0x1000, Read, User).unwrap(); // hit:  [1, 2]
    mmu.translate(0x3000, Read, User).unwrap(); // miss, evicts 2: [3, 1]
    mmu.translate(0x1000, Read, User).unwrap(); // hit
    mmu.translate(0x2000, Read, User).unwrap(); // miss
    assert_eq!(mmu.stats(), Stats { tlb_hits: 2, tlb_misses: 4, faults: 0 });
}

#[test]
fn a_zero_sized_tlb_always_walks() {
    let mut mmu = Mmu::new(0);
    mmu.map(0x1000, 1, RW).unwrap();
    for _ in 0..5 {
        mmu.translate(0x1000, Read, User).unwrap();
    }
    assert_eq!(mmu.stats(), Stats { tlb_hits: 0, tlb_misses: 5, faults: 0 });
}

#[test]
fn unmap_invalidates_the_tlb_entry() {
    let mut mmu = Mmu::new(8);
    mmu.map(0x7000, 70, RW).unwrap();
    assert_eq!(mmu.translate(0x7010, Read, User), Ok(70 << 12 | 0x10));
    assert_eq!(mmu.unmap(0x7FFF), Some(70));
    assert_eq!(mmu.translate(0x7010, Read, User), Err(Fault::NotMapped { vaddr: 0x7010 }));
    mmu.map(0x7000, 71, RW).unwrap();
    assert_eq!(mmu.translate(0x7010, Read, User), Ok(71 << 12 | 0x10));
    assert_eq!(mmu.unmap(0x9000), None);
}

#[test]
fn protect_changes_flags_and_invalidates_the_tlb_entry() {
    let mut mmu = Mmu::new(8);
    mmu.map(0x7000, 70, RW).unwrap();
    assert!(mmu.translate(0x7000, Write, User).is_ok());
    assert!(mmu.protect(0x7123, RO));
    assert_eq!(
        mmu.translate(0x7000, Write, User),
        Err(Fault::Protection { vaddr: 0x7000, kind: Write })
    );
    assert!(mmu.protect(0x7000, RW));
    assert!(mmu.translate(0x7000, Write, User).is_ok());
    assert!(!mmu.protect(0x8000, RW));
}

#[test]
fn flush_tlb_forces_walks() {
    let mut mmu = Mmu::new(8);
    mmu.map(0x1000, 1, RW).unwrap();
    mmu.translate(0x1000, Read, User).unwrap();
    mmu.flush_tlb();
    mmu.translate(0x1000, Read, User).unwrap();
    assert_eq!(mmu.stats(), Stats { tlb_hits: 0, tlb_misses: 2, faults: 0 });
}

#[test]
fn second_level_tables_are_created_on_demand_and_freed_when_empty() {
    let mut mmu = Mmu::new(4);
    assert_eq!(mmu.table_count(), 0);
    mmu.map(0x0000_0000, 1, RW).unwrap();
    mmu.map(0x0000_1000, 2, RW).unwrap();
    assert_eq!(mmu.table_count(), 1);
    mmu.map(0x0040_0000, 3, RW).unwrap();
    mmu.map(0xFFFF_F000, 4, RW).unwrap();
    assert_eq!(mmu.table_count(), 3);
    mmu.unmap(0x0000_0000);
    assert_eq!(mmu.table_count(), 3);
    mmu.unmap(0x0000_1000);
    assert_eq!(mmu.table_count(), 2);
    assert_eq!(mmu.map(0x0040_0000, 5, RW), Err(MapError::AlreadyMapped(0x0040_0000)));
}

#[test]
fn matches_a_hash_map_model_on_random_operations() {
    let mut mmu = Mmu::new(16);
    let mut model: HashMap<u32, (u32, Flags)> = HashMap::new();
    let mut state = 0x9E37_79B9_7F4A_7C15u64;
    let mut next = || {
        state ^= state << 13;
        state ^= state >> 7;
        state ^= state << 17;
        state
    };
    let flag_sets = [RW, RO, RX, KERNEL_RW];
    for _ in 0..50_000 {
        let r = next();
        // 64 pages spread over a few directory entries
        let page = ((r >> 8) % 64) as u32;
        let vpn = (page % 8) | (page / 8) << 10;
        let vaddr = vpn << 12 | (r >> 20) as u32 & 0xFFF;
        match r % 10 {
            0 => {
                let flags = flag_sets[(r >> 40) as usize % 4];
                let frame = (r >> 44) as u32 & 0xFFFFF;
                let expected = if model.contains_key(&vpn) {
                    Err(MapError::AlreadyMapped(vpn << 12))
                } else {
                    model.insert(vpn, (frame, flags));
                    Ok(())
                };
                assert_eq!(mmu.map(vpn << 12, frame, flags), expected);
            }
            1 => assert_eq!(mmu.unmap(vaddr), model.remove(&vpn).map(|(frame, _)| frame)),
            2 => {
                let flags = flag_sets[(r >> 40) as usize % 4];
                let present = model.get_mut(&vpn).map(|entry| entry.1 = flags).is_some();
                assert_eq!(mmu.protect(vaddr, flags), present);
            }
            _ => {
                let kind = [Read, Write, Execute][(r >> 36) as usize % 3];
                let mode = if r & 0x80 == 0 { User } else { Kernel };
                let expected = match model.get(&vpn) {
                    None => Err(Fault::NotMapped { vaddr }),
                    Some(&(frame, flags)) => {
                        let ok = match kind {
                            Read => true,
                            Write => flags.writable,
                            Execute => flags.executable,
                        } && (mode == Kernel || flags.user);
                        if ok {
                            Ok(frame << 12 | vaddr & 0xFFF)
                        } else {
                            Err(Fault::Protection { vaddr, kind })
                        }
                    }
                };
                assert_eq!(mmu.translate(vaddr, kind, mode), expected, "{kind:?} {vaddr:#x}");
            }
        }
    }
}

#[test]
fn a_million_translations_run_quickly() {
    let mut mmu = Mmu::new(64);
    for page in 0..256u32 {
        mmu.map(page << 12, page + 1000, RW).unwrap();
    }
    let start = Instant::now();
    let mut sum = 0u64;
    for i in 0..1_000_000u32 {
        let vaddr = (i.wrapping_mul(2_654_435_761) % (256 << 12)) & !3;
        sum += mmu.translate(vaddr, Read, User).unwrap() as u64;
    }
    assert!(sum > 0);
    assert!(start.elapsed() < Duration::from_secs(5), "took {:?}", start.elapsed());
}
