Two things a bank customer asks for: a monthly statement, and "please undo that transfer".

| Request | Answer |
| --- | --- |
| `GET /accounts/{id}/statement?month=2099-11` | `200`, the statement of that month |
| `POST /transfers/{id}/reversal` | `201`, `Location: .../transfers/{new id}`, a new transfer that undoes it |

**Statement.** Months are in UTC, from the first instant of the month (included) to the first
instant of the next one (excluded), and the entries count by their `at`:

```json
{"month": "2099-11", "openingBalanceCents": 1000, "closingBalanceCents": 550,
 "inCents": 50, "outCents": 500, "entries": 3}
```

- `openingBalanceCents` is the balance after the last entry **before** the month (0 if there
  is none); `closingBalanceCents` the balance after the last entry of the month (the opening
  balance if there is none).
- `inCents` adds up the `DEPOSIT` and `TRANSFER_IN` entries of the month, `outCents` the
  `WITHDRAWAL` and `TRANSFER_OUT` ones; `entries` counts them.
- A missing or malformed month is `400`; an unknown account is `404`.
- A statement runs **at most 4** SQL statements: let the database find the last entries and
  add up the month, instead of loading every entry into Java.

**Reversal.** The new transfer goes the other way, with the same amount, and has a
`reverses` field with the id of the transfer it undoes. Every transfer now has `reverses`
(`null` for ordinary ones).

- The transfer must exist: `404`, `transfer <id> not found`.
- A transfer is reversed **at most once**, even when ten reversal requests arrive at the same
  moment: the others are `409`, `transfer <id> is already reversed`.
- A reversal cannot be reversed: `422`, `a reversal cannot be reversed`.
- It follows the rules of any transfer: if the receiver has spent the money, it is `409`
  (`account <id> has <balance> cents, needs <amount>`) and nothing changes; a closed account
  is `409` too.

The tests now replace your `Clock` bean with one they move by hand, to write entries in
different months.

> Tip: a derived query like `findFirstByAccountIdAndAtBeforeOrderByAtDescIdDesc` reads one
> row; `sum(case when ... then ... else 0 end)` and `count` add up the month in one JPQL query;
> `@DateTimeFormat(pattern = "yyyy-MM") YearMonth month` reads the parameter. Lock the
> original transfer row before checking for an earlier reversal, and a unique column for
> `reverses` makes the database refuse a second one.
