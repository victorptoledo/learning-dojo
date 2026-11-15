package logs_test

import (
	"strings"
	"testing"

	"dojo/logs"
)

func TestParseReadsOneEntry(t *testing.T) {
	entries, err := logs.Parse(strings.NewReader("2026-11-15T10:00:00Z ERROR payments card declined\n"))
	if err != nil {
		t.Fatalf("Parse = %v, want nil", err)
	}
	if len(entries) != 1 {
		t.Fatalf("Parse returned %d entries, want 1", len(entries))
	}
	e := entries[0]
	if e.Level != logs.Error || e.Service != "payments" || e.Message != "card declined" {
		t.Errorf("entry = %+v, want level ERROR, service payments, message \"card declined\"", e)
	}
}
