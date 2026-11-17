# Sidequest: Discount engine

Shops change their promotions every week, so the rules live in text, not in code:

```text
# books week
percent 10 category books
buy 2 get 1 sku PEN
over 10000 off 1500
```

Build `dojo.discounts.DiscountEngine`: `DiscountEngine.parse(rules)` reads the rules once,
and `engine.price(lines, tier)` prices a cart. The starter gives you the two records of the
contract, `Line` (a cart line) and `Receipt` (the result), and the method signatures.
Everything else is yours, and this is where Java's newer features shine: a `sealed interface
Rule` with one `record` per kind of rule, `switch` with patterns to apply them, streams to add
things up.

Money is always a `long` number of **cents**. When a percentage gives a fraction of a cent,
round **down**.

General rules of the language:

- One rule per line; blank lines and lines starting with `#` are ignored; words are separated
  by spaces.
- `Receipt.applied()` lists the rules that changed the price, each written exactly as its
  trimmed line in the rules text, once, in the order of the rules text.
- `subtotalCents` is the sum of `unitCents × quantity`; `totalCents` is
  `subtotalCents − discountCents`.
