# Sidequest: Typed state machine

An order is open, then paid, then shipped; it can be cancelled while open but not once it
has shipped. Code like that usually lives in `if (order.status === ...)` scattered around,
and a typo or a forgotten case shows up in production. Build `machine.ts`, a small state
machine library where the transition table is the single source of truth and the compiler
refuses transitions that cannot happen.

```ts
import { createActor, defineMachine, transition } from "./machine.ts";
import { z } from "zod";

const order = defineMachine({
  initial: "open",
  states: {
    open: { on: { PAY: "paid", CANCEL: "cancelled" } },
    paid: { on: { SHIP: "shipped" } },
    shipped: {},
    cancelled: {},
  },
});

const next = transition(order, "open", "PAY"); // type "paid"
transition(order, "shipped", "CANCEL");        // does not compile

const actor = createActor(order, {
  context: { paid: 0 },
  events: { PAY: z.object({ amount: z.number().positive() }) },
  updates: { "open.PAY": (context, payload) => ({ paid: context.paid + payload.amount }) },
});
actor.send("PAY", { amount: 30 });
```

The contract tests call what each stage asks you to export, and type-level tests check what
compiles and what must not. The exported names are fixed; everything behind them is yours.
No `any`; zod (preinstalled in the image) is the only library. `tests/` is yours too.
