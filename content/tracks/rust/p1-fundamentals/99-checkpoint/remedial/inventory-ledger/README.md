# Inventory Ledger

Practice for *Checkpoint 1*. A small shop keeps its stock in an `Inventory`. Every change
is a `Change`: `Add` or `Remove` an item, with a quantity. Implement the methods in
`src/lib.rs`.

1. `Inventory::new()` is an empty inventory.
2. `apply(change)` adds to or removes from the stock and returns `true`. Removing an item
   that is not in stock, or more than there is, changes nothing and returns `false`.
   Removing everything keeps the item at `0`.
3. `quantity(item)` is how many there are; `0` for an item never added.
4. `low_stock(threshold)` lists the items whose quantity is below `threshold`, sorted by
   name, borrowed from the inventory.

Save a file to run the checks. Passing gives practice XP; it does not change the checkpoint.
