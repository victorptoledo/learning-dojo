Auditors need to trust that what the ledger said yesterday it still says today. So the ledger
becomes **append-only**, and a mistake is corrected the way accountants do it: with a new
entry that reverses the wrong one. Write `history.sql`.

**Append-only.** No `UPDATE` and no `DELETE` of `journal` or `postings` rows, and no
`TRUNCATE` of either table, whoever tries (the table's owner too): each fails with the
ledger's own SQLSTATE **`LD002`**. Inserting stays as it was.

**Reversals.** A new column `journal.reverses` references the entry a reversal cancels
(`NULL` for ordinary entries).

`reverse_entry(p_journal bigint, p_memo text) RETURNS bigint` writes a new entry with that
memo, `reverses = p_journal`, and the same postings as the original with the opposite sign,
and returns its id. After it, every balance is back where it was before the original entry.

- An entry can be reversed **once**: a second reversal of it fails with `23505`, a unique
  violation from the table itself (two sessions reversing at once included).
- A reversal cannot be reversed, and an entry that does not exist cannot either: both fail
  with `22023`.

> [!TIP]
> A `BEFORE UPDATE OR DELETE ... FOR EACH ROW` trigger whose function raises an exception
> stops the statement before any row changes. `TRUNCATE` does not fire row triggers: it needs
> a `BEFORE TRUNCATE ... FOR EACH STATEMENT` trigger. Raise with `USING ERRCODE = 'LD002'`.
> A superuser could still switch triggers off (`session_replication_role = replica`), which
> is exactly how the tests clean up between scenarios; that is what privileges are for, and
> stage 5 gives customers none.
