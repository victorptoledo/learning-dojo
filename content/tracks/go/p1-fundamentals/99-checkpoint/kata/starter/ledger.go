// Package ledger keeps accounts in cents and moves money between them.
package ledger

import (
	"errors"
	"fmt"
)

// ErrUnknownAccount is wrapped when an operation names an account that was never opened.
var ErrUnknownAccount = errors.New("unknown account")

// ErrInsufficientFunds is wrapped when a transfer needs more than the balance.
var ErrInsufficientFunds = errors.New("insufficient funds")

// Account is an open account. Balance is in cents.
type Account struct {
	ID      string
	Owner   string
	Balance int64
}

// LineError is the failure of one line given to ApplyLines.
type LineError struct {
	Line int
	Err  error
}

func (e *LineError) Error() string { return fmt.Sprintf("line %d: %v", e.Line, e.Err) }

// Unwrap lets errors.Is and errors.As see the cause.
func (e *LineError) Unwrap() error { return e.Err }

// Ledger holds the accounts. Create it with NewLedger.
type Ledger struct {
	accounts map[string]*Account
}

// NewLedger returns an empty ledger.
func NewLedger() *Ledger {
	return &Ledger{accounts: make(map[string]*Account)}
}

// Open adds an account with an opening balance in cents.
func (l *Ledger) Open(id, owner string, cents int64) error {
	panic("todo: write Open")
}

// Balance returns the balance of an account in cents.
func (l *Ledger) Balance(id string) (int64, error) {
	panic("todo: write Balance")
}

// Transfer moves cents from one account to another. On error nothing changes.
func (l *Ledger) Transfer(from, to string, cents int64) error {
	panic("todo: write Transfer")
}

// ParseCents reads an amount such as "10", "10.5" or "10.50" into cents.
func ParseCents(text string) (int64, error) {
	panic("todo: write ParseCents")
}

// FormatCents writes cents as units and two decimals: 1050 is "10.50".
func FormatCents(cents int64) string {
	return fmt.Sprintf("%d.%02d", cents/100, cents%100)
}

// ApplyLines runs commands, one per line: "open ID OWNER AMOUNT" and
// "transfer FROM TO AMOUNT". Blank lines and lines starting with # are skipped. A failing
// line does not stop the others; the result joins one *LineError per failing line.
func (l *Ledger) ApplyLines(lines []string) error {
	panic("todo: write ApplyLines")
}

// Statement returns one line per account, "ID OWNER BALANCE", sorted by id.
func (l *Ledger) Statement() []string {
	panic("todo: write Statement")
}
