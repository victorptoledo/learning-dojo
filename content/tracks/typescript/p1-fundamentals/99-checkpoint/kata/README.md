# Checkpoint 1: Ledger

Build a small typed ledger in `ledger.ts`. The types are given (`Kind`, `Transaction`,
`Result`, `MonthSummary`, `LineError`); the functions and the `Ledger` class are yours.
Amounts are **integer cents**: `12.34` is stored as `1234`, so sums never meet float errors.

## Functions

| Function | Does |
| --- | --- |
| `parseAmount(text)` | `"12.34"` → `1234`, `"12"` → `1200`, `"0.5"` → `50`. `null` unless the text is digits with an optional `.` and 1 or 2 decimals, and the amount is greater than 0 (`"-5"`, `"0"`, `"1.234"`, `"1,5"`, `"1e3"`, `".5"` are all `null`) |
| `formatCents(cents)` | `123456` → `"1,234.56"`, `-50` → `"-0.50"`, `0` → `"0.00"` |
| `parseLine(line)` | Parses `id,date,kind,amount,category` into a `Result<Transaction>`, trimming each field |

`parseLine` returns `{ ok: true, value }` or `{ ok: false, error }`, checking in this order:

| Problem | `error` |
| --- | --- |
| Not exactly 5 fields | `"expected 5 fields, got N"` |
| Blank id | `"empty id"` |
| Not a real `YYYY-MM-DD` date (`2026-02-30` is not) | `"invalid date: <text>"` |
| Kind other than `income` or `expense` | `"invalid kind: <text>"` |
| Amount rejected by `parseAmount` | `"invalid amount: <text>"` |
| Blank category | `"empty category"` |

## The Ledger class

| Member | Does |
| --- | --- |
| `new Ledger(transactions?)` | Starts with the given transactions (any iterable) |
| `add(transaction)` | Adds it; throws an `Error` when the id is already there |
| `size` | A getter: the number of transactions |
| `balance()` | Income minus expenses, in cents |
| `totalsByCategory(kind)` | A `Map` from category to total cents for that kind, the largest total first, equal totals by category name |
| `monthly()` | One `MonthSummary` per month that has transactions (`month` is `YYYY-MM`), in month order, with `net = income - expense` |
| `Ledger.fromLines(lines)` | A static factory: skips blank lines and lines starting with `#`, adds each valid line and returns `{ ledger, errors }`. Each error has the **1-based line number** and the `parseLine` message, or `"duplicate id: <id>"` |

Keep the storage private (`#transactions`), narrow `Result` with `if (!parsed.ok)`, and do not
use `any`.

Run the tests with `npm test` after `npm install`. There are 14 tests: 1 in
`tests/visible.test.ts` and 13 hidden ones.
