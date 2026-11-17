Three rules that discount cart lines:

| Rule | Effect on a matching line |
|---|---|
| `percent <n> category <category>` | `n`% off the line total, for lines of that category |
| `percent <n> sku <sku>` | `n`% off the line total, for that SKU |
| `fixed <cents> sku <sku>` | `cents` off **each unit** of that SKU, never below 0 |

- The line total is `unitCents × quantity`.
- A line gets **one** discount: the largest one among the rules that match it. On a tie, the
  rule that comes first in the text wins. Discounts are not stacked.
- SKUs and categories compare exactly (case-sensitive).

```java
var engine = DiscountEngine.parse("percent 10 category books");
var receipt = engine.price(List.of(new Line("B1", "books", 2500, 2)), "standard");
// subtotal 5000, discount 500, total 4500, applied ["percent 10 category books"]
```

Only valid rules are used in this stage. The `tier` argument is used from stage 3.
