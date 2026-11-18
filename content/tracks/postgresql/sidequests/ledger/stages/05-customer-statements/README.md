The customer portal reads the ledger directly, with a role of its own, and each request says
which customer is logged in. A bug in the portal's SQL must never show one customer another
customer's money. Write `customers.sql`.

**The role.** `ledger_customer` (no login needed here). It may **read** `accounts`, `journal`
and `postings`, and nothing else: an `INSERT`, `UPDATE` or `DELETE` by it fails with `42501`.

**Who is asking.** Every portal request runs `SET LOCAL app.customer = 'alice'` (the
`owner` of the accounts). As `ledger_customer`, a query then sees only:

- the accounts of that owner;
- the postings of those accounts;
- the journal entries that have a posting on one of those accounts.

No `app.customer` (never set, or `''`) means no rows, not an error. The rule holds whatever
query the portal writes, `balance()` included: alice asking for bob's balance gets `0`.

**The statement.** `statement(p_account bigint, p_from date, p_to date) RETURNS TABLE
(posted_at timestamptz, memo text, amount numeric, balance numeric)`: the postings of the
account whose entry was posted from `p_from` to `p_to`, **both days included**, in the order
they were posted (then by posting `id`), each with the account's **running balance** after
it, counting everything posted before `p_from` too. Called for an account the caller cannot
see, it returns no rows.

> [!TIP]
> Row-level security: `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` and `CREATE POLICY ... FOR
> SELECT TO ledger_customer USING (...)`. A policy may use a subquery, and the subquery's
> tables apply their own policies, so "postings of my accounts" can lean on the policy of
> `accounts`. `current_setting('app.customer', true)` returns `NULL` instead of failing when
> the setting does not exist. Superusers, and table owners unless you `FORCE` it, bypass
> RLS: the tests switch with `SET LOCAL ROLE ledger_customer`. And a function runs with the
> caller's rights (`SECURITY INVOKER`, the default): a `SECURITY DEFINER` one owned by a
> superuser would see everything. A running balance is a window function: `sum(amount) OVER
> (ORDER BY ...)`.
