# Sidequest: Bill splitter

Dinner is over and the bill is on the table. Build `BillSplitter.tsx`, one React component
that lists who is at the table and what was ordered, lets you tick who had each item, adds a
tip and tells each person what they owe, so that the amounts always add up to the total, to
the cent.

The component is checked from the outside, by contract tests written with Testing Library:
they type into fields found by their labels, click buttons found by their names and read
what a user would see. The labels, button names and texts each stage gives are the
contract; how you split the component, which state you keep and what you derive is yours.

```tsx
import { BillSplitter } from "./BillSplitter.tsx";

// main.tsx in a Vite app; the tests render it with no props.
createRoot(document.getElementById("root")!).render(<BillSplitter />);
```

Two rules make this sidequest worth doing:

- **Money is counted in cents**, as whole numbers. `0.1 + 0.2` is not `0.3` in JavaScript,
  and a bill that is off by one cent is wrong.
- **Store as little as you can.** People and items are state; subtotals, shares and the tip
  are computed from them during render. Keeping a "total" in state that you update by hand
  is how amounts drift apart.

No libraries beyond React: plain React 19 and TypeScript in strict mode. `npm test` compiles
the project and runs the tests in `tests/`, which are yours too: add your own.
