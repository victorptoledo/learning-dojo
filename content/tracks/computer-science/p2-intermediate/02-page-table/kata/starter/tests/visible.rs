use kata::{split, AccessKind, Flags, Mmu, Mode};

#[test]
fn an_address_splits_ten_ten_twelve() {
    assert_eq!(split(0x0040_3A7C), (1, 3, 0xA7C));
}

#[test]
fn a_mapped_page_translates_and_keeps_the_offset() {
    let mut mmu = Mmu::new(4);
    let rw = Flags { writable: true, user: true, executable: false };
    mmu.map(0x0040_3000, 0x1F2E0, rw).unwrap();
    assert_eq!(mmu.translate(0x0040_3A7C, AccessKind::Read, Mode::User), Ok(0x1F2E_0A7C));
}
