Money now moves between accounts, and accounts can be closed.

| Request | Answer |
| --- | --- |
| `POST /transfers` `{"from": 1, "to": 2, "amountCents": 300}` | `201`, `Location: .../transfers/{id}`, the transfer `{"id", "from", "to", "amountCents"}` |
| `GET /transfers/{id}` | `200`, the transfer |
| `POST /accounts/{id}/close` | `200`, the account, now with `"closed": true` |

Every account now has a `closed` field (`false` until it is closed).

Transfer rules, checked in this order:

1. The body is valid: `from`, `to` and `amountCents` present, the amount from 1 to
   100000000, and `to` different from `from` (otherwise the error is on `to`): `400`.
2. Both accounts exist: `404`, `account <id> not found`.
3. Neither account is closed: `409`, `account <id> is closed`.
4. Both use the same currency: `422`, `cannot move EUR to USD`.
5. `from` has the money: `409`, `account <id> has <balance> cents, needs <amount>`.

A transfer is **all or nothing**: when it is refused, no balance changes.

Closing: only an account with a balance of 0 closes (`409`, `account <id> still has <balance>
cents`); closing twice is `409` too. Deposits and withdrawals on a closed account are `409`
with the same `account <id> is closed`.

**Under load.** The tests now send many requests at the same moment, and the balances must
come out exact:

- twenty transfers of 100 from an account that has 1000: ten pass, ten are `409`, and the
  account ends at 0, never below;
- transfers from A to B and from B to A at the same time all pass, and no money appears
  or disappears;
- twenty deposits at once all count.

Reading a balance, changing it in Java and saving it back loses updates when two requests do
it at once. Lock the rows you are about to change, or let the database check and change the
balance in one statement, and when you lock two accounts, always lock them in the same
order (by id), or two opposite transfers wait for each other forever.

> Tip: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on a repository method runs `select ... for
> update`; `@Transactional` keeps the lock until the commit. A refused request must throw
> before the commit, so the transaction rolls back.
