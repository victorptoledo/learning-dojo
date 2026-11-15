# Shopping Cart

Model a shopping cart in `cart.py` with dataclasses. Prices are exact money: `Decimal` with
two decimal places, never `float`.

## `Category` and `Product`

`Category` is a `StrEnum` with the members `FOOD`, `BOOKS` and `OTHER` (values `"food"`,
`"books"`, `"other"`); it is already written.

`Product(sku, name, price, category=Category.OTHER)` is an **immutable value**: assigning a
field raises `dataclasses.FrozenInstanceError`, two products with the same fields are equal,
and products can be put in a set. When it is created:

- `sku` is stripped and turned to upper case (`" tea-01 "` becomes `"TEA-01"`); a blank
  sku raises `ValueError`;
- `price` may be given as a `Decimal`, a `str` or an `int`, and is stored as a `Decimal`
  with exactly two places (`"3.5"` becomes `Decimal("3.50")`);
- a `float` price raises `TypeError`; a negative price, a price with more than two decimal
  places or a text that is not a number raises `ValueError`.

## `LineItem` and `Cart`

`LineItem(product, quantity)` has a `total` property: price times quantity.

`Cart()` starts empty; two carts never share their lines. It has:

| Member | Behaviour |
| --- | --- |
| `lines` | The `LineItem`s, in the order the products were first added |
| `add(product, quantity=1)` | Adds a line, or increases the quantity of the line with the same sku. A quantity below 1 raises `ValueError`; a **different** product with a sku already in the cart raises `ValueError` |
| `remove(sku, quantity=None)` | Removes the whole line (`None`, or a quantity at least the line's), or decreases it. The sku is normalised like a product's. An unknown sku raises `KeyError`; a quantity below 1 raises `ValueError` |
| `total` | Sum of the line totals, a `Decimal` (`Decimal("0.00")` when empty) |
| `total_for(category)` | The same, only for products of that category |
| `receipt()` | One row per line, then the total, joined by newlines (format below) |
| `len(cart)` | The number of items: the sum of the quantities |
| `sku in cart` | Whether a line has that sku (normalised like a product's) |

```python
tea = Product("tea-01", "Green tea", "3.5", Category.FOOD)
dune = Product("BK-7", "Dune", Decimal("9.99"), Category.BOOKS)
cart = Cart()
cart.add(tea, 2)
cart.add(dune)
print(cart.receipt())
```

```text
2 x Green tea (TEA-01) @ 3.50 = 7.00
1 x Dune (BK-7) @ 9.99 = 9.99
TOTAL 16.99
```

An empty cart's receipt is `TOTAL 0.00`.

Tools: `@dataclass(frozen=True)`, `field(default_factory=list)`, `__post_init__`,
`object.__setattr__` (to normalise a frozen field), `Decimal.quantize`,
`decimal.InvalidOperation`, `@property`, `__len__`, `__contains__`.

Run the visible tests with `uv run pytest`. There are 18 tests: 1 in
`tests/test_visible.py` and 17 hidden ones, plus ruff on `cart.py`.
