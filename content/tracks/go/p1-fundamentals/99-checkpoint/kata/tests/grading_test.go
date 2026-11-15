package ledger_test

import (
	"errors"
	"slices"
	"strings"
	"testing"

	"dojo/ledger"
)

func opened(t *testing.T) *ledger.Ledger {
	t.Helper()
	book := ledger.NewLedger()
	for _, account := range []struct {
		id, owner string
		cents     int64
	}{{"acc-1", "Ada", 1000}, {"acc-2", "Alan", 500}} {
		if err := book.Open(account.id, account.owner, account.cents); err != nil {
			t.Fatalf("Open(%s) = %v", account.id, err)
		}
	}
	return book
}

func balance(t *testing.T, book *ledger.Ledger, id string, want int64) {
	t.Helper()
	got, err := book.Balance(id)
	if err != nil || got != want {
		t.Errorf("Balance(%s) = %d, %v; want %d, nil", id, got, err, want)
	}
}

func TestOpenRejectsEmptyIDNegativeBalanceAndDuplicates(t *testing.T) {
	book := opened(t)
	if err := book.Open("", "Nobody", 10); err == nil {
		t.Error("Open with an empty id returned nil, want an error")
	}
	if err := book.Open("acc-3", "Grace", -1); err == nil {
		t.Error("Open with a negative balance returned nil, want an error")
	}
	if err := book.Open("acc-1", "Impostor", 99); err == nil {
		t.Error("Open of an existing id returned nil, want an error")
	}
	balance(t, book, "acc-1", 1000)
}

func TestBalanceOfAnUnknownAccountWrapsErrUnknownAccount(t *testing.T) {
	_, err := opened(t).Balance("acc-9")
	if !errors.Is(err, ledger.ErrUnknownAccount) {
		t.Fatalf("Balance(acc-9) error = %v, want one that wraps ErrUnknownAccount", err)
	}
	if !strings.Contains(err.Error(), "acc-9") {
		t.Errorf("error %q should name the account acc-9", err)
	}
}

func TestTransferMovesMoney(t *testing.T) {
	book := opened(t)
	if err := book.Transfer("acc-2", "acc-1", 500); err != nil {
		t.Fatalf("Transfer of the whole balance = %v", err)
	}
	balance(t, book, "acc-1", 1500)
	balance(t, book, "acc-2", 0)
}

func TestTransferWithoutFundsWrapsErrInsufficientFundsAndChangesNothing(t *testing.T) {
	book := opened(t)
	err := book.Transfer("acc-2", "acc-1", 501)
	if !errors.Is(err, ledger.ErrInsufficientFunds) {
		t.Fatalf("Transfer(501 from a balance of 500) error = %v, want ErrInsufficientFunds", err)
	}
	balance(t, book, "acc-1", 1000)
	balance(t, book, "acc-2", 500)
}

func TestTransferToOrFromAnUnknownAccount(t *testing.T) {
	book := opened(t)
	for _, pair := range [][2]string{{"acc-1", "acc-9"}, {"acc-9", "acc-1"}} {
		err := book.Transfer(pair[0], pair[1], 100)
		if !errors.Is(err, ledger.ErrUnknownAccount) {
			t.Errorf("Transfer(%s -> %s) error = %v, want ErrUnknownAccount", pair[0], pair[1], err)
		}
	}
	balance(t, book, "acc-1", 1000)
}

func TestTransferRejectsNonPositiveAmountsAndTheSameAccount(t *testing.T) {
	book := opened(t)
	for _, cents := range []int64{0, -100} {
		if err := book.Transfer("acc-1", "acc-2", cents); err == nil {
			t.Errorf("Transfer(%d) returned nil, want an error", cents)
		}
	}
	if err := book.Transfer("acc-1", "acc-1", 100); err == nil {
		t.Error("Transfer from an account to itself returned nil, want an error")
	}
	balance(t, book, "acc-1", 1000)
	balance(t, book, "acc-2", 500)
}

func TestParseCentsAcceptsWholeAndDecimalAmounts(t *testing.T) {
	for text, want := range map[string]int64{"10": 1000, "10.5": 1050, "10.50": 1050, "0.07": 7, "0": 0, "1234.99": 123499} {
		got, err := ledger.ParseCents(text)
		if err != nil || got != want {
			t.Errorf("ParseCents(%q) = %d, %v; want %d, nil", text, got, err, want)
		}
	}
}

func TestParseCentsRejectsBadAmounts(t *testing.T) {
	for _, text := range []string{"", "abc", "-5", "1.234", "1.", ".5", "1,50", "1.5x", "+3"} {
		if got, err := ledger.ParseCents(text); err == nil {
			t.Errorf("ParseCents(%q) = %d, nil; want an error", text, got)
		}
	}
}

func TestApplyLinesRunsCommandsAndSkipsBlankAndCommentLines(t *testing.T) {
	book := ledger.NewLedger()
	err := book.ApplyLines([]string{
		"# opening balances",
		"open acc-1 Ada 10.00",
		"",
		"open acc-2 Alan 5",
		"   transfer acc-1 acc-2 2.50  ",
	})
	if err != nil {
		t.Fatalf("ApplyLines = %v, want nil", err)
	}
	balance(t, book, "acc-1", 750)
	balance(t, book, "acc-2", 750)
}

func TestApplyLinesReportsEveryFailingLine(t *testing.T) {
	book := ledger.NewLedger()
	err := book.ApplyLines([]string{
		"open acc-1 Ada 1.00",
		"transfer acc-1 acc-9 0.50",
		"withdraw acc-1 0.10 now",
		"open acc-2 Alan 0",
		"transfer acc-1 acc-2 5.00",
		"transfer acc-1 acc-2 0.25",
	})
	if err == nil {
		t.Fatal("ApplyLines = nil, want the failures of lines 2, 3 and 5")
	}
	var lineErr *ledger.LineError
	if !errors.As(err, &lineErr) || lineErr.Line != 2 {
		t.Errorf("first *LineError = %v, want the one for line 2", lineErr)
	}
	if !errors.Is(err, ledger.ErrUnknownAccount) || !errors.Is(err, ledger.ErrInsufficientFunds) {
		t.Errorf("ApplyLines error %q should wrap ErrUnknownAccount and ErrInsufficientFunds", err)
	}
	joined, ok := err.(interface{ Unwrap() []error })
	if !ok || len(joined.Unwrap()) != 3 {
		t.Errorf("ApplyLines should return errors.Join of 3 line errors, got %q", err)
	}
	if !strings.Contains(err.Error(), "line 3: ") {
		t.Errorf("error %q should mention line 3", err)
	}
	balance(t, book, "acc-1", 75)
	balance(t, book, "acc-2", 25)
}

func TestStatementIsSortedAndFormatted(t *testing.T) {
	book := ledger.NewLedger()
	_ = book.Open("b-2", "Grace", 5)
	_ = book.Open("a-1", "Ada", 123456)
	_ = book.Open("c-3", "Alan", 0)
	got := book.Statement()
	want := []string{"a-1 Ada 1234.56", "b-2 Grace 0.05", "c-3 Alan 0.00"}
	if !slices.Equal(got, want) {
		t.Errorf("Statement() = %q, want %q", got, want)
	}
}
