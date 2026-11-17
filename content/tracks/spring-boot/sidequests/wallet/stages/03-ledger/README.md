A balance alone cannot be audited. From now on every change of a balance also writes an
**entry** in a ledger, in the same transaction:

```json
{"id": 41, "type": "TRANSFER_OUT", "amountCents": 300, "balanceAfterCents": 700, "transferId": 12, "at": "2099-11-03T10:15:30Z"}
```

- `type` is `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN` or `TRANSFER_OUT`; a transfer writes one
  entry on each account.
- `amountCents` is always positive; `balanceAfterCents` is the balance right after the change.
- `transferId` is the transfer of a `TRANSFER_*` entry, and `null` for the others.
- `at` is the moment of the change, an `Instant` taken from a `java.time.Clock` **bean**
  that you define and inject (the next stage replaces it in its tests).

| Request | Answer |
| --- | --- |
| `GET /accounts/{id}/entries?page=0&size=20` | `200`, a page of the entries of the account, **newest first** |
| `GET /accounts/{id}/entries?type=DEPOSIT` | `200`, the same, only that type |
| `GET /reports/balances?currency=EUR&limit=10` | `200`, the open accounts of that currency with the most money |

A page is `{"content": [...], "page": 0, "size": 20, "totalElements": 57, "totalPages": 3}`.
`page` starts at 0 (default 0) and `size` goes from 1 to 100 (default 20); anything else is
`400`. An unknown account is `404`.

A report row is `{"owner": "ada", "balanceCents": 5000, "entries": 12}`: the largest balance
first, then by owner; `limit` goes from 1 to 100 (default 10). Closed accounts are left out.

**Few queries.** The tests count the SQL statements:

- a page of entries runs **at most 3** (the account, the page and its count);
- the report runs **exactly 1**, however many accounts it lists. Counting the entries
  of each account in a loop is the N+1 problem: one query for the accounts, then one more per
  account. Let the database group and count, and select straight into the row.

The entries written while many requests run at once must still add up: after twenty
concurrent deposits of 100, the `balanceAfterCents` values are exactly 100, 200, ..., 2000.

> Tip: `Page<Entry> findByAccountIdOrderByIdDesc(long accountId, Pageable pageable)`,
> `PageRequest.of(page, size)`, `@Min`/`@Max` on `@RequestParam` (Spring validates them on its
> own), and a JPQL `select new dojo.wallet.BalanceRow(a.owner, a.balanceCents, count(e)) from
> Account a left join Entry e on e.account = a ... group by ...` with a `Limit` parameter.
