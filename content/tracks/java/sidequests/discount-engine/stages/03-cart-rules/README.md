Rules on the whole cart, applied **after** the line discounts, in this order:

| Rule | Effect |
|---|---|
| `over <threshold> off <cents>` | when the cart after line discounts is at least `threshold`, `cents` off |
| `tier <name> percent <n>` | for customers of that tier (ignoring case), `n`% off what is left after `over` |

- Of several `over` rules, only the best one that applies counts; the same for several `tier`
  rules of the customer's tier.
- The total never goes below 0: the discount is at most the subtotal.
- `applied` keeps the order of the rules text, line rules and cart rules mixed.

Example: a 12 000 cart with 1 000 of line discounts, `over 10000 off 1500` and
`tier gold percent 10` for a gold customer: 11 000 → 9 500 → 8 550, so the discount is 3 450.
