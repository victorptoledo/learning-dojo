# Sidequest: Ledger

Every bank, wallet and marketplace keeps a ledger, and it has rules nobody may break: money
never appears or vanishes (each entry's postings add up to zero), history is never edited
(a mistake is fixed by a new entry that reverses it), and a customer never spends money they
do not have, not even with two payments at the same millisecond. You build that ledger in the
database, where every program that touches it has to follow the rules, in five stages:

1. journal entries whose postings must balance, checked when the transaction **commits**;
2. an append-only history, and reversals;
3. transfers that never overdraw a customer account, under concurrent sessions;
4. a month-end interest run over every account, committed in batches and safe to rerun;
5. account statements that a customer can read only for their own accounts.

```text
accounts (id, owner, currency, kind)                  -- fixture: 'customer' or 'system'
journal  (id, memo, posted_at, ...)                   -- stage 1
postings (id, journal_id → journal, account_id → accounts, amount)   -- stage 1
```

`fixture/01_accounts.sql` creates the accounts: the bank's cash in EUR (1) and USD (5), which
may go below zero, and customer accounts of alice (2 in EUR, 7 in USD), bob (3), carol (4)
and dave (6, USD).

How it is checked:

- You write one file per stage (`ledger.sql`, `history.sql`, `transfers.sql`,
  `interest.sql`, `customers.sql`). Each stage's statement gives the names, rules and
  signatures the tests use; how you write them is yours.
- The dojo loads every file once and **commits** it, then runs two kinds of tests:
  - ordinary tests call your functions inside a transaction that is rolled back (the rules
    checked at `COMMIT` are forced with `SET CONSTRAINTS ALL IMMEDIATE`);
  - **session tests** open real sessions (with `dblink`) that commit for real, race each other,
    and run your month-end procedure, which commits on its own.
- So the files must load in one go: no `BEGIN`/`COMMIT`, no `psql` commands.
- Errors the tests expect: `23514` (a broken rule), `23505` (unique), `22023` (an invalid
  argument), `42501` (no privilege), and two of the ledger's own, raised with
  `USING ERRCODE = '...'`: **`LD001`** insufficient funds and **`LD002`** history cannot change.
- Try things with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play` from
  this folder, and a second session with `docker exec -it <container> psql -d dojo`.

When you are done, `[A]` copies a review prompt: ask an AI where money could still appear,
vanish or be spent twice.
