# Sales CSV Report

## 1. `SalesParser.parse(text)`

Each line is one sale: `date,region,product,units,unitCents`.

```text
# exported 2026-11-14
2026-11-01,north,tea,3,250
2026-11-01, south , mug ,1,1200

2026-11-02,north,tea,2,250
```

- Blank lines and lines starting with `#` are skipped. Line numbers start at 1 and count
  **every** line, skipped ones included.
- Fields are trimmed. `region` and `product` must not be empty.
- Return the sales in input order. At the **first** bad line, throw `BadLineException`
  (already written) with that line number and one of these reasons:

  | Problem | Reason (exact text) |
  | --- | --- |
  | not exactly 5 fields | `expected 5 fields but found 4` |
  | date is not `yyyy-MM-dd` | `bad date '2026-13-01'` |
  | empty region or product | `missing region or product` |
  | units not a whole number ≥ 1 | `bad units 'x'` |
  | price not a whole number ≥ 0 | `bad price '-5'` |

  The message becomes `line 3: bad units 'x'`. When the problem came from a parse exception
  (`DateTimeParseException`, `NumberFormatException`), pass it as the **cause**.

## 2. `SalesReport` (use streams)

- `totalCents(sales)`: the sum of every sale's `totalCents()`.
- `centsByRegion(sales)`: region → total cents, **sorted by region**.
- `topProducts(sales, n)`: the `n` product names with the most **units**, most first; ties
  in alphabetical order.
- `bestDay(sales)`: the date with the highest total cents, the **earliest** on a tie;
  `Optional.empty()` when there are no sales.

> Tip: `text.lines()`, `split(",", -1)` (keeps empty trailing fields), `LocalDate.parse`,
> `Integer.parseInt`, `Collectors.groupingBy(key, TreeMap::new, Collectors.summingLong(...))`,
> `Map.Entry.comparingByValue()`.
