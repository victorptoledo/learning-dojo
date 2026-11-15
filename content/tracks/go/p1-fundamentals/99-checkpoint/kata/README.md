# Checkpoint 1: The Ledger

Build a small bank ledger in `ledger.go`. Money is always an `int64` number of **cents**,
never a float. `Account`, `LineError`, the two sentinel errors, `NewLedger` and `FormatCents`
are already written; you write the rest.

| Method or function | Does |
| --- | --- |
| `(l *Ledger) Open(id, owner string, cents int64) error` | Adds an account. Fails for an empty id, a negative opening balance or an id that already exists. |
| `(l *Ledger) Balance(id string) (int64, error)` | The balance in cents. For an unknown id, an error that **wraps `ErrUnknownAccount`** and names the id. |
| `(l *Ledger) Transfer(from, to string, cents int64) error` | Moves money. Fails when the amount is not positive, when both ids are the same, when an account is unknown (wrapping `ErrUnknownAccount`) or when `from` has too little money (wrapping `ErrInsufficientFunds`). **On any error nothing changes.** |
| `ParseCents(text string) (int64, error)` | `"10"` → 1000, `"10.5"` → 1050, `"10.50"` → 1050, `"0.07"` → 7. Anything else is an error: signs, more than two decimals, `"1."`, `".5"`, commas, letters, `""`. |
| `(l *Ledger) ApplyLines(lines []string) error` | Runs one command per line: `open ID OWNER AMOUNT` or `transfer FROM TO AMOUNT` (amounts as in `ParseCents`). Skips blank lines and lines starting with `#`, and trims spaces. A failing line does **not** stop the others: return `errors.Join` of one `*LineError{Line: n, Err: cause}` per failing line (lines count from 1), or `nil`. |
| `(l *Ledger) Statement() []string` | One line per account, `"ID OWNER BALANCE"`, sorted by id: `"a-1 Ada 1234.56"`. |

```go
book := ledger.NewLedger()
err := book.ApplyLines([]string{
	"open acc-1 Ada 1.00",
	"transfer acc-1 acc-9 0.50", // line 2: wraps ErrUnknownAccount
	"open acc-2 Alan 0",
	"transfer acc-1 acc-2 5.00", // line 4: wraps ErrInsufficientFunds
})
errors.Is(err, ledger.ErrInsufficientFunds) // true, through the *LineError and the join
var lineErr *ledger.LineError
errors.As(err, &lineErr)                    // true, lineErr.Line == 2
```

Wrap with `%w` so the sentinels stay visible through every layer: `Transfer` wraps the
error of the account lookup, `LineError` unwraps to its cause, and `errors.Join` keeps all
of them.

Run the visible test with `go test`. The dojo runs 12 tests: 1 in `ledger_test.go` and 11
hidden ones, plus `go vet` and `gofmt` on `ledger.go`.
