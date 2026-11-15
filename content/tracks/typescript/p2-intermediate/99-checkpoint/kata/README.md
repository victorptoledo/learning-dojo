# Checkpoint 2: Order Pipeline

Two modules work together: `status.ts` (the statuses and the allowed moves) and `orders.ts`
(orders, events and a pipeline). Some types are yours to finish, as well as the functions.

## status.ts

1. **`OrderStatus`** is `string` in the starter. Derive the union
   `"pending" | "paid" | "shipped" | "delivered" | "cancelled"` from `ORDER_STATUSES` with an
   indexed access type, so the list and the type can never disagree.
2. **`TRANSITIONS`** is empty. Fill it in, keeping the literal types with `as const` and
   checking that every status is a key with `satisfies Record<OrderStatus, readonly OrderStatus[]>`:

   | From | May move to |
   | --- | --- |
   | `pending` | `paid`, `cancelled` |
   | `paid` | `shipped`, `cancelled` |
   | `shipped` | `delivered` |
   | `delivered`, `cancelled` | nothing |

3. `isOrderStatus(value: unknown)` is a type predicate; `canTransition(from, to)` reads the table.

## orders.ts

| Function | Does |
| --- | --- |
| `total(order)` | The sum of `quantity * priceCents` |
| `apply(order, event)` | Returns a **new** order: `pay` → `paid`, `ship` → `shipped` with `trackingId`, `deliver` → `delivered`, `cancel` → `cancelled` with `cancelReason`. Throws `new TransitionError(order, event)` when the table forbids the move |
| `summarize(orders)` | A count per status, with **every** status present |
| `processAll(ids, load, event, { concurrency })` | Loads each order with `load(id)`, at most `concurrency` (default 4) at a time, applies the event, and returns `{ updated, failed }`, both in the order of the ids. A failure (a rejected load, a `TransitionError`, even a rejected non-`Error` value) goes into `failed` as an `Error` and never stops the other ids |

Import from `./status.ts` with the `.ts` extension, and mark type-only imports with
`import type` (the dojo sets `verbatimModuleSyntax`).

Run the tests with `npm test` after `npm install`. There are 14 tests: 1 in
`tests/visible.test.ts` and 13 hidden ones, 3 of them type-level.
