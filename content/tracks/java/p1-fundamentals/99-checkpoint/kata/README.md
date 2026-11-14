# Checkpoint 1: Order Pricing

Price a shop order. Amounts are in **cents** (`long`), so there is no rounding of money.
The discount types are already written; you write the order types and the pricing.

## 1. `Item` and `Order` (records that stay valid)

- `Item(String sku, String category, long unitCents, int quantity)`: its compact
  constructor throws `IllegalArgumentException` when `sku` or `category` is blank or `null`,
  `unitCents < 0` or `quantity < 1`, and **strips** `sku` and `category`.
  `subtotal()` returns `unitCents * quantity`.
- `Order(List<Item> items)`: keeps an **unmodifiable copy** of the list, so changing the
  caller's list later does not change the order. An empty order is allowed.

## 2. `Pricing`

- `subtotal(order)`: the sum of the items' subtotals.
- `discountFor(order, discount)`: how many cents one discount takes off. Use a `switch` over
  the sealed `Discount`, with **no `default`**:

  | Discount | Cents off |
  | --- | --- |
  | `PercentOff(category, percent)` | `percent`% of the subtotal of the items in `category`, rounded down |
  | `AmountOff(cents, minimumCents)` | `cents` when the order's subtotal is at least `minimumCents`, else 0 |
  | `BuyXGetY(sku, buy, free)` | for the item(s) with that `sku`: every complete group of `buy + free` units makes `free` units free |

  Example: `BuyXGetY("tea", 2, 1)` on 7 units of tea at 300 cents: 7 units hold 2 complete
  groups of 3, so 2 units are free: 600 cents off.
- `total(order, discounts)`: the subtotal minus every discount, **never below 0**.
- `subtotalByCategory(order)`: category → subtotal of its items, **sorted by category**.

```java
var order = new Order(List.of(
        new Item("tea", "drinks", 300, 7),
        new Item("mug", "kitchen", 1200, 1)));
Pricing.subtotal(order)                                  == 3300
Pricing.discountFor(order, new PercentOff("kitchen", 25)) == 300
Pricing.total(order, List.of(new BuyXGetY("tea", 2, 1), new AmountOff(500, 2000))) == 2200
Pricing.subtotalByCategory(order)                        == {drinks=2100, kitchen=1200}
```

> This kata reviews the whole phase: records with compact constructors and defensive copies,
> a sealed interface with a pattern `switch`, and collections (`TreeMap`, `merge`).
