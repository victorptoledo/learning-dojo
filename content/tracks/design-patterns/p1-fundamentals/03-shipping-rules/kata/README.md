# Shipping Rules

`ShippingCalculator.quoteCents(method, parcel)` prices a parcel with one long `if/else` chain,
one branch per shipping method. Adding a carrier means editing that method again, and the
rules of one method cannot be used or tested on their own.

Refactor it to the **Strategy** pattern, written the modern Java way: each method's pricing
is a `ShippingRule` (a functional interface, so a lambda is a rule), a small **factory**
names the built-in rules, and the calculator only looks rules up. The prices must stay
exactly the same: the visible tests pin them and pass before you start.

## The target design

| Type | What it does |
| --- | --- |
| `@FunctionalInterface interface ShippingRule` | `long quoteCents(Parcel parcel)`. A rule that cannot ship a parcel throws `IllegalArgumentException`. |
| `final class ShippingRules` | `standard()`, `express()`, `pickup()`, `freight()` each return one rule; `Map<String, ShippingRule> builtIn()` returns the four by name |
| `ShippingCalculator()` | the built-in rules |
| `ShippingCalculator(Map<String, ShippingRule> rules)` | any rules; keeps its own copy |
| `quoteCents(String method, Parcel parcel)` | applies the rule; an unknown or `null` method throws `IllegalArgumentException("unknown shipping method: " + method)` |
| `Set<String> methods()` | the method names, sorted |
| `ShippingCalculator with(String method, ShippingRule rule)` | a **new** calculator that also knows `method`; the original is unchanged |
| `String cheapest(Parcel parcel)` | the method with the lowest price, skipping methods whose rule throws `IllegalArgumentException`; ties go to the name that sorts first; none left throws `IllegalArgumentException` |

The prices, as the current code computes them:

| Method | Price in cents |
| --- | --- |
| standard | 499, plus 150 per started kilo above 2 kg, plus 700 outside `PT`, plus 300 if fragile |
| express | 1299 plus 250 per started kilo, doubled outside `PT`, then plus 300 if fragile |
| pickup | 199; refuses parcels over 10 kg and outside `PT` |
| freight | 4500 plus 80 per started kilo; refuses parcels under 30 kg |

## What the grading tests check

- The prices and refusals above, for each rule alone and through the calculator.
- A rule written as a lambda in the test (a "drone" service) works without changing any of
  your classes: the calculator is open for extension and closed for modification.
- `ShippingCalculator` holds only its map of rules and has no `equals`, `switch` or
  `instanceof`: it looks rules up instead of branching on names.

> Tip: move one branch at a time into `ShippingRules` as a lambda, run the visible tests,
> then repeat. The duplicated "fragile" and "abroad" checks become private helpers there.
