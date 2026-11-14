use kata::{Inventory, Removal};

#[test]
fn adding_twice_sums_the_quantity() {
    let mut inventory = Inventory::new();
    inventory.add("apple", 3);
    inventory.add("apple", 2);
    assert_eq!(inventory.quantity("apple"), 5);
}

#[test]
fn removing_part_of_the_stock_reports_what_is_left() {
    let mut inventory = Inventory::new();
    inventory.add("pear", 4);
    assert_eq!(inventory.remove("pear", 1), Removal::Done { left: 3 });
}

#[test]
fn the_report_is_sorted_by_name() {
    let mut inventory = Inventory::new();
    inventory.add("pear", 1);
    inventory.add("apple", 2);
    assert_eq!(inventory.report(), ["apple: 2", "pear: 1"]);
}
