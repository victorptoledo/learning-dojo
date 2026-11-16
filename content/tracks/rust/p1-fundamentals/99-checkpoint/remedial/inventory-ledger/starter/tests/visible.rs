use kata::{Change, Inventory};

#[test]
fn adding_stock_is_counted() {
    let mut inventory = Inventory::new();
    assert!(inventory.apply(Change::Add {
        item: "rice".into(),
        quantity: 4,
    }));
    assert_eq!(inventory.quantity("rice"), 4);
}
