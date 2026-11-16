# Your First Fitness Function

The shop in `dojo.shop` has three packages and one rule, written on a wiki page nobody reads:

> **The domain (`dojo.shop.domain`) uses nothing from `dojo.shop.application` or
> `dojo.shop.infra`.** Infrastructure depends on the domain, never the other way round.

The rule is already broken twice. You will make it a test that fails, then fix the code.

## Part 1: the rule as code

`dojo.fitness.ClassGraph` is given. `ClassGraph.scan("dojo.shop")` reads the compiled classes
with the JDK's Class-File API and returns every class mapped to the classes it uses
(`"dojo.shop.domain.Order" -> {"dojo.shop.infra.OrderRow", "java.util.List", ...}`). It sees
imports, fully qualified names, generic signatures and static calls.

Write `dojo.fitness.Rules.violations(graph, fromPackage, forbiddenPackages)`:

- for every class **in** `fromPackage`, every class it uses that is **in** one of the
  `forbiddenPackages` is a violation, written `"<from class> -> <used class>"`;
- a class is in a package when its name starts with the package name followed by a dot, so
  subpackages count, and `dojo.shop.infrastructure.X` is **not** in `dojo.shop.infra`;
- the result is sorted, without duplicates, and empty when the rule holds.

`ArchitectureTest` (in `src/test/java/dojo`) applies it to the shop. Run it: it lists what is
wrong.

## Part 2: make it green

- `Order.toRow` builds an `infra.OrderRow`. Remove it; `application.PlaceOrder` builds the row
  (`new OrderRow(order.id(), order.lines().size(), totalEuroCents)`).
- `PriceConverter` creates an `infra.FxApiClient`. Give the domain a **port**:

| Type | What it does |
| --- | --- |
| `domain.ExchangeRates` | an interface with `BigDecimal toEuro(String currency)`: how many euros one unit of `currency` is worth |
| `domain.PriceConverter` | `PriceConverter(ExchangeRates rates)`; `long toEuroCents(long cents, String currency)`: `EUR` is returned as is **without** asking the rates; otherwise `cents × rate`, rounded `HALF_EVEN` to whole cents |
| `infra.FxApiRates` | `FxApiRates(FxApiClient client)` implements `ExchangeRates` with `client.quote(currency + "EUR")` |
| `dojo.shop.App` | `placeOrder(OrderTable table)` wires the real adapter into `PlaceOrder` |

`PlaceOrder.place(order)` keeps its behaviour: it converts the order total to euro cents,
inserts the row and returns the euro total. `FxApiClient`, `OrderRow` and `OrderTable` stay as
they are.

## What the grading tests check

- `Rules.violations` on small graphs: subpackages, the `infra` / `infrastructure` trap, sorting,
  duplicates, a clean graph.
- The shop's own graph, read from the compiled classes: the domain uses nothing from
  `application` or `infra`. `ExchangeRates` is an interface in the domain and `FxApiRates`
  implements it.
- The prices: conversions with a fake `ExchangeRates`, `HALF_EVEN` rounding, euros that never ask
  for a rate, and the row the table receives.

Keep your tests in the package `dojo`: `ClassGraph` scans `dojo.shop` and skips test classes.
