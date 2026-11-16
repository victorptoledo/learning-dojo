use kata::{Access, Cache, CacheConfig, Outcome};

fn direct_mapped() -> Cache {
    Cache::new(CacheConfig { sets: 4, ways: 1, line_size: 16 }).unwrap()
}

#[test]
fn an_address_splits_into_tag_index_and_offset() {
    // 0x1234 = tag 0x48 | index 0b11 | offset 0b0100 with 4 sets of 16-byte lines
    assert_eq!(direct_mapped().split(0x1234), (0x48, 3, 4));
}

#[test]
fn the_second_read_of_a_line_hits() {
    let mut cache = direct_mapped();
    assert_eq!(cache.access(Access::Read(0x100)), Outcome::Miss);
    assert_eq!(cache.access(Access::Read(0x10C)), Outcome::Hit);
}
