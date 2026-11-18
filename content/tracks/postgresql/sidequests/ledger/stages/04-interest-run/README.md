At the end of every month the bank pays interest on every customer account. There are
millions of them, and doing them all in one transaction would hold its locks and its
snapshot for an hour, keep VACUUM from cleaning anything, and lose all the work when the
run fails at 95%. Write `interest.sql`.

**The table `interest_runs`**: which accounts a month's run has already done.

| Column | Rule |
|---|---|
| `month` | `date`, the first day of the month, required |
| `account_id` | required, references `accounts` |
| `journal_id` | references `journal`: the interest entry, `NULL` when nothing was due |

At most one row per `month` and `account_id`.

**The procedure.** `apply_interest(p_month date, p_rate numeric, p_batch integer DEFAULT 100)`
is a `PROCEDURE`, called with `CALL apply_interest('2026-01-01', 0.01)` outside any
transaction block.

- It goes through the **customer** accounts in `id` order and skips those the month's run
  already did.
- For each one, the interest is `round(balance × p_rate, 2)`. When it is above zero, it posts
  one entry with the memo `interest 2026-01` (the month as `YYYY-MM`): `+interest` on the
  account, `-interest` on the bank's **system** account of the same currency. Either way, it
  records the account in `interest_runs`.
- It **commits after every `p_batch` accounts**, so a run of 250 accounts with batches of 100
  is three transactions (100, 100 and 50).
- If it fails halfway, the batches already committed stay, and calling it again for the same
  month does only the rest: **no account ever gets the month's interest twice**.

The tests run it in real sessions: on 250 new accounts with 100.00 each, they check the
entries, count the transactions that wrote `interest_runs` (their `xmin`), make it fail on the
180th account and run it again.

> [!TIP]
> Since PostgreSQL 11 a procedure (not a function) may `COMMIT` in the middle, even inside a
> `FOR` loop over a query, when it was called by a top-level `CALL`. A `COMMIT` also releases
> the locks and lets VACUUM move on. What the run has done must be **in the database**, in
> the same transaction as the postings it made, so that a crash and a rerun agree on it.
