package ledger_test

import (
	"testing"

	"dojo/ledger"
)

func TestOpenAndTransfer(t *testing.T) {
	book := ledger.NewLedger()
	if err := book.Open("acc-1", "Ada", 1000); err != nil {
		t.Fatalf("Open(acc-1) = %v", err)
	}
	if err := book.Open("acc-2", "Alan", 0); err != nil {
		t.Fatalf("Open(acc-2) = %v", err)
	}
	if err := book.Transfer("acc-1", "acc-2", 250); err != nil {
		t.Fatalf("Transfer = %v", err)
	}
	if got, _ := book.Balance("acc-1"); got != 750 {
		t.Errorf("Balance(acc-1) = %d, want 750", got)
	}
	if got, _ := book.Balance("acc-2"); got != 250 {
		t.Errorf("Balance(acc-2) = %d, want 250", got)
	}
}
