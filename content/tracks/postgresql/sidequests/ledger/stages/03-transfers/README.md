Customers move money between accounts, from the app, the website and the card terminal, all
at once. Write `transfers.sql`.

`transfer(p_from bigint, p_to bigint, p_amount numeric, p_memo text) RETURNS bigint` moves
`p_amount` from one account to another as one journal entry (`-p_amount` on `p_from`,
`+p_amount` on `p_to`) and returns its id.

- `p_amount` must be greater than zero, the two accounts must exist, be different and share
  a currency; otherwise it fails with `22023` and posts nothing.
- A **customer** account never goes below zero: a transfer that would take it there fails
  with **`LD001`** (insufficient funds) and posts nothing. **System** accounts (the bank's
  cash) may go negative: that is how money enters the ledger, `transfer(1, 2, 100, 'deposit')`.

**The races** (alice, account 2, has 100.00):

- Session A transfers 70 from alice to bob inside a transaction that stays open; session B
  transfers 70 from alice to carol. B **waits** for A. If A commits, B fails with `LD001`; if
  A rolls back, B goes through. Alice never ends below zero.
- Transfers that share no account do not wait for each other: while A's transfer from alice
  to bob is open, carol can pay the bank.

> [!TIP]
> "Read the balance, then post" has a gap: under `READ COMMITTED`, B reads alice's balance
> before A commits and both see 100. Something must make B's transfer wait for A's before it
> reads. A lock on alice's row in `accounts` does it: `SELECT ... FOR UPDATE`, or `FOR NO KEY
> UPDATE`, which does not block the foreign key checks of other postings. When one transfer
> locks two accounts, lock them in the same order every time (by `id`), or two transfers in
> opposite directions can deadlock.
