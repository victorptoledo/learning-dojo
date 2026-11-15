// Package ledger keeps accounts in cents and moves money between them.
package ledger

import (
	"errors"
	"fmt"
	"slices"
	"strconv"
	"strings"
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
	switch {
	case id == "":
		return errors.New("open: the account id is empty")
	case cents < 0:
		return fmt.Errorf("open %s: negative opening balance %d", id, cents)
	}
	if _, exists := l.accounts[id]; exists {
		return fmt.Errorf("open %s: the account already exists", id)
	}
	l.accounts[id] = &Account{ID: id, Owner: owner, Balance: cents}
	return nil
}

func (l *Ledger) account(id string) (*Account, error) {
	account, ok := l.accounts[id]
	if !ok {
		return nil, fmt.Errorf("account %s: %w", id, ErrUnknownAccount)
	}
	return account, nil
}

// Balance returns the balance of an account in cents.
func (l *Ledger) Balance(id string) (int64, error) {
	account, err := l.account(id)
	if err != nil {
		return 0, err
	}
	return account.Balance, nil
}

// Transfer moves cents from one account to another. On error nothing changes.
func (l *Ledger) Transfer(from, to string, cents int64) error {
	if cents <= 0 {
		return fmt.Errorf("transfer %d: the amount must be positive", cents)
	}
	if from == to {
		return fmt.Errorf("transfer from %s to itself", from)
	}
	source, err := l.account(from)
	if err != nil {
		return fmt.Errorf("transfer: %w", err)
	}
	target, err := l.account(to)
	if err != nil {
		return fmt.Errorf("transfer: %w", err)
	}
	if source.Balance < cents {
		return fmt.Errorf("transfer %s from %s to %s: %w", FormatCents(cents), from, to, ErrInsufficientFunds)
	}
	source.Balance -= cents
	target.Balance += cents
	return nil
}

// ParseCents reads an amount such as "10", "10.5" or "10.50" into cents.
func ParseCents(text string) (int64, error) {
	units, fraction, hasFraction := strings.Cut(text, ".")
	if units == "" || !digitsOnly(units) || (hasFraction && (len(fraction) == 0 || len(fraction) > 2 || !digitsOnly(fraction))) {
		return 0, fmt.Errorf("invalid amount %q", text)
	}
	whole, err := strconv.ParseInt(units, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("invalid amount %q: %w", text, err)
	}
	cents := whole * 100
	if hasFraction {
		part, _ := strconv.ParseInt(fraction, 10, 64)
		if len(fraction) == 1 {
			part *= 10
		}
		cents += part
	}
	return cents, nil
}

func digitsOnly(text string) bool {
	for _, r := range text {
		if r < '0' || r > '9' {
			return false
		}
	}
	return true
}

// FormatCents writes cents as units and two decimals: 1050 is "10.50".
func FormatCents(cents int64) string {
	return fmt.Sprintf("%d.%02d", cents/100, cents%100)
}

// ApplyLines runs commands, one per line: "open ID OWNER AMOUNT" and
// "transfer FROM TO AMOUNT". Blank lines and lines starting with # are skipped. A failing
// line does not stop the others; the result joins one *LineError per failing line.
func (l *Ledger) ApplyLines(lines []string) error {
	var failures []error
	for i, line := range lines {
		text := strings.TrimSpace(line)
		if text == "" || strings.HasPrefix(text, "#") {
			continue
		}
		if err := l.apply(strings.Fields(text)); err != nil {
			failures = append(failures, &LineError{Line: i + 1, Err: err})
		}
	}
	return errors.Join(failures...)
}

func (l *Ledger) apply(fields []string) error {
	if len(fields) != 4 {
		return fmt.Errorf("%q needs 3 arguments, got %d", fields[0], len(fields)-1)
	}
	cents, err := ParseCents(fields[3])
	if err != nil {
		return err
	}
	switch fields[0] {
	case "open":
		return l.Open(fields[1], fields[2], cents)
	case "transfer":
		return l.Transfer(fields[1], fields[2], cents)
	default:
		return fmt.Errorf("unknown command %q", fields[0])
	}
}

// Statement returns one line per account, "ID OWNER BALANCE", sorted by id.
func (l *Ledger) Statement() []string {
	ids := make([]string, 0, len(l.accounts))
	for id := range l.accounts {
		ids = append(ids, id)
	}
	slices.Sort(ids)
	lines := make([]string, 0, len(ids))
	for _, id := range ids {
		account := l.accounts[id]
		lines = append(lines, fmt.Sprintf("%s %s %s", account.ID, account.Owner, FormatCents(account.Balance)))
	}
	return lines
}
