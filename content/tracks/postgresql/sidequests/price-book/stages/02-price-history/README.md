Prices change, and the shop must answer "what did this cost on 3 March?" for invoices,
refunds and reports. Write `prices.sql`.

**The table `prices`**

| Column | Rule |
|---|---|
| `product_id` | required, references `products` |
| `currency` | required, of type `currency` |
| `amount` | `numeric(10,2)`, required, greater than zero |
| `valid` | `tstzrange`, required: when this price applies |

The rules of `valid`:

- it includes its start and excludes its end, `[start, end)`: `'[2026-01-01, 2026-02-01)'`
  is accepted, `'(2026-01-01, 2026-02-01]'` is not;
- it has a start (`tstzrange(NULL, '2026-02-01')` is refused) and it is not empty; it may
  have no end, which means "until further notice";
- **no two prices of the same product and currency overlap**: the database refuses the
  second one with an exclusion violation (`23P01`). Prices in different currencies, or
  ranges that only touch (`[Jan, Feb)` and `[Feb, Mar)`), are fine.

**The functions**

- `price_at(p_product bigint, p_currency currency, p_at timestamptz) RETURNS numeric`: the
  amount valid at that moment, or `NULL` when there is none.
- `set_price(p_product bigint, p_currency currency, p_amount numeric, p_from timestamptz)
  RETURNS void`: from `p_from` on, until further notice, the price is `p_amount`.
  - the price valid at `p_from`, if any, now ends at `p_from`, so the history has **no gap**;
  - a price that starts exactly at `p_from` is replaced;
  - a price that starts **after** `p_from` means someone is rewriting history out of order:
    raise an error with SQLSTATE `22023` and change nothing.

**What a customer pays**

`final_price(p_product bigint, p_currency currency, p_qty integer, p_at timestamptz)
RETURNS numeric`: what a customer pays for `p_qty` units at `p_at`.

- it starts from `price_at(...) × p_qty`, and returns `NULL` when there is no price;
- a `percent` rule always applies; a `bulk` rule applies when `p_qty >= min_qty`;
- discounts never add up: only the **largest** applicable `value` is taken off;
- the result is rounded to cents with `round(x, 2)`;
- `p_qty` below 1 raises an error with SQLSTATE `22023`.

A kettle at 29.90 with a `percent` 10 and a `bulk` 3 → 15: one costs 26.91 (10% off), three
cost 76.25 (15% off 89.70, rounded).

> [!TIP]
> An exclusion constraint generalises `UNIQUE`: "no two rows where these columns are equal
> **and** these ranges overlap", `EXCLUDE USING gist (a WITH =, b WITH =, valid WITH &&)`.
> GiST indexes ranges but not plain equality on integers or enums: the `btree_gist`
> extension adds that (`CREATE EXTENSION IF NOT EXISTS btree_gist`). The functions
> `lower`, `upper`, `lower_inc`, `upper_inc`, `lower_inf` and `isempty`, and the operator
> `@>` (the range contains a moment), are all you need for the rest.
