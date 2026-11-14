use kata::{Inventory, Removal};

fn stocked() -> Inventory {
    let mut inventory = Inventory::new();
    inventory.add("pear", 4);
    inventory.add("apple", 10);
    inventory.add("fig", 1);
    inventory
}

#[test]
fn a_new_inventory_is_empty() {
    let inventory = Inventory::new();
    assert!(inventory.report().is_empty());
    assert_eq!(inventory.quantity("anything"), 0);
}

#[test]
fn removing_too_much_changes_nothing() {
    let mut inventory = stocked();
    assert_eq!(inventory.remove("pear", 5), Removal::NotEnough { available: 4 });
    assert_eq!(inventory.quantity("pear"), 4);
}

#[test]
fn removing_an_unknown_item_is_reported() {
    let mut inventory = stocked();
    assert_eq!(inventory.remove("kiwi", 1), Removal::Unknown);
    assert_eq!(inventory.report().len(), 3);
}

#[test]
fn an_item_that_reaches_zero_disappears() {
    let mut inventory = stocked();
    assert_eq!(inventory.remove("fig", 1), Removal::Done { left: 0 });
    assert_eq!(inventory.report(), ["apple: 10", "pear: 4"]);
    assert_eq!(inventory.remove("fig", 1), Removal::Unknown);
}

#[test]
fn names_are_case_sensitive() {
    let mut inventory = stocked();
    inventory.add("Apple", 1);
    assert_eq!(inventory.quantity("apple"), 10);
    assert_eq!(inventory.quantity("Apple"), 1);
}

#[test]
fn low_stock_is_strictly_below_the_threshold_and_sorted() {
    let inventory = stocked();
    assert_eq!(inventory.low_stock(5), ["fig", "pear"]);
    assert_eq!(inventory.low_stock(4), ["fig"]);
    assert!(inventory.low_stock(1).is_empty());
}

#[test]
fn removing_zero_is_a_no_op() {
    let mut inventory = stocked();
    assert_eq!(inventory.remove("pear", 0), Removal::Done { left: 4 });
}
