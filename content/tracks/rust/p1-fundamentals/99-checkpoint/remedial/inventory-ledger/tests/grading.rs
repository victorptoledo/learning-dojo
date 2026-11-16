use kata::{Change, Inventory};

fn add(item: &str, quantity: u32) -> Change {
    Change::Add {
        item: item.to_string(),
        quantity,
    }
}

fn remove(item: &str, quantity: u32) -> Change {
    Change::Remove {
        item: item.to_string(),
        quantity,
    }
}

#[test]
fn a_new_inventory_is_empty() {
    let inventory = Inventory::new();
    assert_eq!(inventory.quantity("anything"), 0);
    assert!(inventory.low_stock(100).is_empty());
}

#[test]
fn additions_accumulate() {
    let mut inventory = Inventory::new();
    inventory.apply(add("apples", 5));
    inventory.apply(add("apples", 3));
    assert_eq!(inventory.quantity("apples"), 8);
}

#[test]
fn removing_some_stock_works() {
    let mut inventory = Inventory::new();
    inventory.apply(add("apples", 5));
    assert!(inventory.apply(remove("apples", 2)));
    assert_eq!(inventory.quantity("apples"), 3);
}

#[test]
fn removing_too_much_changes_nothing() {
    let mut inventory = Inventory::new();
    inventory.apply(add("apples", 3));
    assert!(!inventory.apply(remove("apples", 10)));
    assert_eq!(inventory.quantity("apples"), 3);
}

#[test]
fn removing_an_unknown_item_fails() {
    let mut inventory = Inventory::new();
    assert!(!inventory.apply(remove("pears", 1)));
    assert_eq!(inventory.quantity("pears"), 0);
    assert!(inventory.low_stock(5).is_empty(), "a failed removal adds no item");
}

#[test]
fn removing_everything_keeps_the_item_at_zero() {
    let mut inventory = Inventory::new();
    inventory.apply(add("salt", 2));
    assert!(inventory.apply(remove("salt", 2)));
    assert_eq!(inventory.quantity("salt"), 0);
    assert_eq!(inventory.low_stock(1), ["salt"]);
}

#[test]
fn low_stock_is_sorted_and_strictly_below() {
    let mut inventory = Inventory::new();
    for (item, quantity) in [("tea", 1), ("beans", 10), ("coffee", 4), ("oats", 5)] {
        inventory.apply(add(item, quantity));
    }
    assert_eq!(inventory.low_stock(5), ["coffee", "tea"]);
}
