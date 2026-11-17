Rewrite trees. Add to `expr.hpp`:

```cpp
// The same expression with constant parts computed and neutral operations removed.
Expr simplify(const Expr& expr);

// The names of the variables the expression reads, sorted, each once.
std::vector<std::string> variables(const Expr& expr);
```

`simplify` builds a **new** tree (the argument does not change), working from the leaves up:

- A negation, binary operation or **builtin** call whose operands are all numbers becomes one
  number, unless computing it would be an error (`1 / 0`, `sqrt(-4)`, `sqrt(1, 2)`,
  `10 ^ 400` stay as they are). Calls of other functions keep their call, with their
  arguments simplified: `f(1 + 1)` is `f(2)`.
- Neutral operations go away: `x + 0`, `0 + x`, `x - 0`, `x * 1`, `1 * x`, `x / 1`, `x ^ 1`
  are `x`, and `--x` is `x`. Nothing else changes: `x * 0` stays, because `x` might be unknown
  and evaluating it would be an error.
- A folded number keeps the position of the node it replaces, so errors found when the
  simplified tree is evaluated still point at the right place.

The simplified tree evaluates to the same result as the original in every environment where
the builtins are not hidden. `variables` walks the tree too: collect, then
`std::ranges::sort` and `std::ranges::unique`.
