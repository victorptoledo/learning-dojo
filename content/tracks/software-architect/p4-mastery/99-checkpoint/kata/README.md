# Strangle the Pricing Engine (final exam)

The shop prices every cart line with `dojo.legacy.pricing.PriceEngine`, written in 2009: string
codes for categories (`"BK"`, `"EL"`, `"GR"`) and customers (`"M"`, `"G"`), magic numbers, and
integer rounding that every invoice since depends on. `Checkout` calls it directly. Nobody dares
to touch it, and a rewrite in one go would have to be right on its first day.

You migrate it **incrementally**, the way the whole track has taught: put pricing behind a port
(branch by abstraction), grow a modern implementation next to the old one, route each category
through a **strangler** that can run both in parallel, and cut a category over only when the
parallel run is clean. Then guard the new shape with **fitness functions and a ratchet**, and
**record the decision**.

`PriceEngine` and `dojo.shop.reports.SalesReport` are given: do not change them. The report
still uses the engine; migrating it is next quarter's work.

## What the engine does

| Category | Price of a line (`base = unit × quantity`, in cents) |
| --- | --- |
| `BK` books | members get 5 % off the base; from 10 items, 10 % off (after the member discount) |
| `EL` electronics | from 3 items, 3 % off the base; then an eco fee of 150 cents **per item** |
| `GR` grocery | a base under 1000 cents gets 300 cents of handling; then members get 10 % off (the handling included) |

Every percentage is `cents * percent / 100` in `long` arithmetic, rounded down at each step. A
quantity below 1 or a unit price below 0 throws `IllegalArgumentException`.

## The target design

| Type | What it is |
| --- | --- |
| `shop.pricing.api.Category` | enum `BOOKS`, `ELECTRONICS`, `GROCERY` |
| `shop.pricing.api.PriceRequest` | **record** `(Category category, long unitPriceCents, int quantity, boolean member)` |
| `shop.pricing.api.Pricing` | the port: `long priceCents(PriceRequest request)` |
| `shop.pricing.legacy.LegacyPricing` | `LegacyPricing(PriceEngine engine)` implements `Pricing`: the only adapter that speaks the engine's codes |
| `shop.pricing.modern.ModernPricing` | `ModernPricing()` implements `Pricing` with the same results **to the cent**, and the same exceptions for bad lines |
| `shop.pricing.migration.Mode` | enum `LEGACY`, `SHADOW`, `MODERN` |
| `shop.pricing.migration.Mismatch` | **record** `(PriceRequest request, long legacyCents, String modern)` |
| `shop.pricing.migration.StranglerPricing` | `StranglerPricing(Pricing legacy, Pricing modern)` implements `Pricing` |
| `shop.checkout.CartLine` | **record** `(String sku, Category category, long unitPriceCents, int quantity)` |
| `shop.checkout.Checkout` | `Checkout(Pricing pricing)`; `long totalCents(List<CartLine> lines, boolean member)`: one request per line |
| `dojo.shop.Shop` | **record** `(Checkout checkout, StranglerPricing pricing, SalesReport report)` |
| `dojo.shop.App` | `static Shop create()`: the composition root, which wires legacy and modern into the strangler |

### The strangler

- `Mode mode(Category)`: every category starts on `LEGACY`.
- `priceCents(request)` follows the category's mode:
  - `LEGACY`: only the legacy pricing answers; the modern one is not called;
  - `SHADOW`: the legacy pricing answers, and then the modern one is asked the same question.
    When its price differs, or it throws (`RuntimeException`), the strangler logs a `Mismatch`
    whose `modern` is the price as text (`"1234"`) or `"error: " + message`, and still returns
    the legacy price. A modern failure must never fail the customer. When the legacy pricing
    throws, the exception goes to the caller and the modern one is not asked;
  - `MODERN`: only the modern pricing answers.
- `route(Category, Mode)`: `LEGACY` and `SHADOW` are always allowed (rolling back is a
  configuration change). Entering `SHADOW` from another mode starts a **new shadow run** for
  that category; routing to `SHADOW` again while in it changes nothing. `MODERN` is allowed only
  while the category is in `SHADOW` and its current run has compared **at least 3** prices with
  **no** mismatch; otherwise `IllegalStateException(category + " needs a clean shadow run
  first")` and nothing changes.
- `List<Mismatch> mismatches()`: every mismatch so far, oldest first, as a copy.

### The fitness functions

`dojo.fitness.ArchitectureRules.violations(classGraph)` takes the graph of `ClassGraph.scan` and
returns the sorted set of violations of three rules. The **root** is the classes of the package
`dojo.shop` itself (`App`, `Shop`), not its subpackages.

| Rule | Violation, one per class and forbidden target |
| --- | --- |
| only the legacy adapter (and the root) uses the engine | `"legacy: <class> -> <target>"` for a class outside `dojo.shop.pricing.legacy` and the root that uses a class of `dojo.legacy` |
| the new code does not lean on the old | `"modern: <class> -> <target>"` for a class of `dojo.shop.pricing.modern` that uses `dojo.shop.pricing.legacy` or `dojo.shop.pricing.migration` |
| callers use the port | `"api: <class> -> <target>"` for a class outside `dojo.shop.pricing` and the root that uses a class of `dojo.shop.pricing` outside `dojo.shop.pricing.api` |

`SuiteTest` ratchets the shop against `src/test/resources/fitness-baseline.txt`: a violation that
is not in the baseline fails the build, and a baseline line that is no longer a violation must be
removed. The baseline you are given lists today's two violations. When `Checkout` stops using
the engine, its line has to go; the report's line stays.

### The decision record

Write `docs/adr/0001-strangle-the-pricing-engine.md` (any `NNNN-short-title.md` name works)
from `docs/adr/template.md`: a `# ` title, then `## Status` (`Accepted` or `Proposed`),
`## Context`, `## Decision` and `## Consequences`, in that order, each with a few real sentences.
Say how the new pricing is checked against the old one.

## What the grading tests check

- `ModernPricing` and `LegacyPricing` against the engine on every category, 10 unit prices, 8
  quantities and both customer types; the bad lines.
- The strangler: modes per category, who is asked in each mode, the mismatch log, failures on
  either side, cut-over rules, rollbacks and new shadow runs.
- `Checkout` and the wired shop, before and after a full migration.
- The graph, independently of your rules: checkout knows only the port; the modern code knows
  neither the engine nor the strangler; the strangler and the port know no engine.
- Your rules on small graphs, the real shop against the baseline (no new, no fixed violations),
  and the decision record.

The visible tests describe the target design, so they do not compile until it exists. Keep your
tests in the package `dojo`.

> Tip: this is the order of a real migration. Each step leaves the shop working: the port and
> the legacy adapter first (nothing changes for the customer), then the strangler on `LEGACY`,
> then the modern rules one category at a time, checked in `SHADOW`.
