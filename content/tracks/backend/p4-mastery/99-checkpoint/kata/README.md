# Checkpoint 4: Final Exam — Moving the Checkout out of the Monolith

The shop is moving its checkout out of the monolith. Stock, payments and shipping are now three
services, and a checkout that spans them can no longer be one database transaction. The new
checkout goes to production behind a feature flag, a few users at a time, while the old one keeps
serving everyone else. At the same time, the `orders` table is renaming its `customer_name` column
to `customer` without downtime.

The entities, the saga record, the three services and their fakes (`FakeInventory`,
`FakePayments`, `FakeShipping`), the flag store and the request and result records are written.
You write `CheckoutSaga`, `Rollout`, `OrderBackfill` (and its queries in `OrderRepository`) and
`CheckoutController`, and you fix `LegacyCheckout`.

## 1. The saga: `CheckoutSaga`

`start(request)` saves a `PENDING` order and a `SagaRecord` (step `RESERVING`), then runs it:

```text
RESERVING ──reserve──▶ CHARGING ──charge──▶ SHIPPING ──schedule──▶ DONE (order CONFIRMED)
     │                     │                    │
     └────── a step throws StepFailedException: fail(reason) ──▶ COMPENSATING ──▶ COMPENSATED
                                                                (order CANCELLED with the reason)
```

- **Save the saga after every step** (`moveTo(next)` then `sagas.save(...)`). Do **not** put a
  transaction around the saga: the remote calls cannot be rolled back, and a crash must leave the
  last step that was saved.
- Pass the saga's id to every service. It is their **idempotency key**: running a step again with
  the same id has no extra effect.
- On a `StepFailedException`, call `saga.fail(reason)` (it remembers `failedAt`) and save. While
  `COMPENSATING`, undo in reverse order what ran **before** the failed step: a failure at shipping
  refunds and releases, at charging it releases, at reserving there is nothing to undo. Then the
  order is `CANCELLED` with the reason and the saga is `COMPENSATED`.
- A `CrashException` stands for the process dying. Do not catch it.
- `resume(sagaId)` runs a saga on from its saved step. A finished saga only returns its result.
  `resumeStuck()` resumes every saga that is not finished and returns how many it resumed.
- The result is `CheckoutResult(orderId, order status, order reason, "saga")`.

## 2. Expand and contract

The `orders` table has both columns now (**expand**). Until the old column can be dropped
(**contract**):

- **Every writer fills both.** The saga does, and `LegacyCheckout` must too: as written, it fills
  only `customer_name`.
- **`OrderBackfill.runBatch(batchSize)`** copies `customer_name` into `customer` for up to
  `batchSize` rows that have no `customer`, lowest ids first, in one transaction, and returns how
  many rows it changed. It never overwrites a `customer` that is already there, so it is safe to run
  again. `remaining()` counts the rows that still have no `customer`.
- **Reads** (`GET /orders/{id}` → `OrderView`) use `customer`, and `customer_name` only when
  `customer` is null. Unknown ids are `404`.

## 3. The rollout: `Rollout.enabled(flag, user)`

- A flag the `FlagStore` does not know is **off**.
- A user in the flag's `allow` set always has it.
- Otherwise the user's **bucket** decides: the CRC-32 (`java.util.zip.CRC32`) of
  `flag + ":" + user` in UTF-8, modulo 100. The flag is on when `bucket < percent`. The same user
  always gets the same answer, and raising the percentage only adds users. `0` with no allow list is
  the kill switch.

## 4. The endpoint

`POST /checkout` with the `X-User` header and a validated `CheckoutRequest` goes to the saga when
`saga-checkout` is enabled for that user, and to `LegacyCheckout` otherwise. It answers the
`CheckoutResult`: `201` when the order is `CONFIRMED`, `422` when it is `CANCELLED`.

> Tip: drive the saga with a `while (!saga.getStep().finished())` loop around a `switch` on the
> step, with the `try`/`catch (StepFailedException e)` inside the loop, so that `start`, `resume`
> and a crash halfway all share the same code. For the backfill, select the ids first
> (`@Query` with a `Pageable`), then `@Modifying @Query("update ShopOrder o set o.customer = ...
> where o.id in :ids and o.customer is null")`.
