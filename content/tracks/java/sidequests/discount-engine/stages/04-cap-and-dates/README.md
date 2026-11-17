Two safety nets for the marketing team.

- `cap <n>`: the whole discount is at most `n`% of the subtotal (rounded down). When the cap
  cuts the discount, the `cap` rule is in `applied` too.
- Any rule may end with `until <yyyy-mm-dd>`: it is valid up to that day, **inclusive**.
  A bad date is a parse error (`line <n>: ...`).
- A new method prices a cart on a given day, ignoring rules past their date:

```java
public Receipt price(List<Line> lines, String tier, LocalDate day);
```

  The old `price(lines, tier)` keeps every rule, as if there were no dates.

`applied` shows a rule as written, `until` part included.
