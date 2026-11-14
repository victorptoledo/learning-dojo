# Checkpoint 1: Inventory

Build a small shop inventory in `src/lib.rs`. The types are already declared; implement the
methods.

- `Inventory::new()` starts empty.
- `add(&mut self, name, quantity)` adds stock. Adding an item that exists increases its quantity.
- `remove(&mut self, name, quantity) -> Removal` takes stock out:
  - `Removal::Done { left }` when there was enough; `left` is what remains. An item that reaches
    0 is **removed** from the inventory.
  - `Removal::NotEnough { available }` when there is less than asked; nothing changes.
  - `Removal::Unknown` when the item is not in the inventory.
- `quantity(&self, name) -> u32` returns the stock of an item, `0` if unknown.
- `report(&self) -> Vec<String>` returns one line per item, `"name: quantity"`, sorted by name.
- `low_stock(&self, threshold) -> Vec<&str>` returns the names with a quantity **below**
  `threshold`, sorted, borrowed from the inventory.

Names are case-sensitive and stored as given.

> This kata reviews the whole phase: a struct with a `HashMap`, an enum with data, `match`,
> `&self` and `&mut self`, `String` and `&str`, sorting a `Vec`, and returning borrowed data.
