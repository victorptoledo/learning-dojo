# Inventory

Write `parse_cents` and the members of `Inventory` declared in `inventory.hpp`, in
`inventory.cpp`. Money is an `std::int64_t` number of **cents** (`Cents`): never a `double`.

| Member | Does |
| --- | --- |
| `parse_cents(text)` | `"12.34"` → 1234, `"7"` → 700, `"0.5"` → 50, `"3.05"` → 305. `std::nullopt` for empty text, a sign, a missing part around the dot (`"1."`, `".5"`), more than two decimals, any other character, or an amount that does not fit in `Cents`. |
| `add(sku, name, quantity, price)` | Adds a new item. `false` (nothing changes) for an existing sku, an empty sku or name, or a negative quantity or price. |
| `restock(sku, amount)` | Adds units. `false` for an unknown sku or `amount <= 0`. |
| `sell(sku, amount)` | Removes units. `false` (nothing changes) for an unknown sku, `amount <= 0` or not enough units. |
| `find(sku) const` | A **copy** of the item, or `std::nullopt`. |
| `total_value() const` | The sum of `quantity * unit_price`. |
| `low_stock(threshold) const` | The skus with `quantity <= threshold`, in sku order. |
| `apply(script)` | Runs one command per line and returns the 1-based numbers of the lines that failed. |

The commands of `apply`, one per line, words separated by one or more spaces:

```text
# opening stock           <- comments and blank lines are skipped
add A-1 kettle 5 29.99     <- add SKU NAME QUANTITY PRICE
sell A-1 2
restock A-1 10
```

A line fails when the command is unknown, it has the wrong number of words, a quantity or
amount is not a whole non-negative number that fits in an `int`, a price does not parse, or
the operation itself returns `false`. Lines may end with `"\r\n"`.

The dojo runs 12 hidden tests and the visible one, plus the warnings, clang-format and
clang-tidy checks on both files.
