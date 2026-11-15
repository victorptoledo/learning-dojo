# Split the Daily Report

`DailyReport.render()` prints today's sales per product. It works, but one method decides
**where the sales come from** (`new SaleArchive()`), **what day it is** (`LocalDate.now()`),
**how to add them up** and **how to print them**. You cannot test it without the real
database and the real clock, and a change to the layout means editing the same method as a
change to the arithmetic.

Refactor it so each reason to change lives in one place and the report depends on
abstractions it is given (the single responsibility and dependency inversion principles).
The output must stay exactly the same.

## The target design

| Type | What it does |
| --- | --- |
| `interface SaleSource` | `List<Sale> salesOn(LocalDate day)`: where sales come from. A lambda can implement it in a test. |
| `SaleArchive implements SaleSource` | keeps `all()` and its data; `salesOn(day)` returns only the sales of that day |
| `record DailyTotals(LocalDate day, SortedMap<String, Long> byProduct)` | `static DailyTotals of(LocalDate day, List<Sale> sales)` keeps the sales of `day` and totals them per product, sorted by name; `long grandTotal()`. The map cannot be modified. |
| `final class TextReport` | `static String format(DailyTotals totals)`: the text layout, unchanged |
| `DailyReport` | `DailyReport(SaleSource source, Clock clock)` and `render()`; `static DailyReport withDefaults()` wires the real `SaleArchive` and `Clock.systemDefaultZone()` |

The text format does not change:

```text
Sales on 2026-03-14
coffee: 7.05
tea: 3.00
TOTAL: 10.05
```

and a day with no sales gives `No sales on 2026-03-14`. Amounts are cents printed with two
decimals.

## What the grading tests check

- The behaviour above, with a fake `SaleSource` (a lambda) and a fixed `Clock`. The day comes
  from the clock, in the clock's zone.
- `DailyReport` holds only a `SaleSource` and a `Clock`, never calls `LocalDate.now()` without
  the clock, and creates a `SaleArchive` only in `withDefaults()`.
- `DailyReport` neither adds up nor formats: no loops, `merge`, `StringBuilder` or
  `String.format` in it. It asks, computes with `DailyTotals` and hands over to `TextReport`.

The visible tests in `src/test/java` describe the target design, so they do not compile
until `SaleSource`, `DailyTotals` and the new constructor exist. Start there.

> Tip: move in small steps. Extract `DailyTotals.of` first (pure arithmetic), then
> `TextReport.format`, then introduce `SaleSource` and the `Clock` and pass them in.
