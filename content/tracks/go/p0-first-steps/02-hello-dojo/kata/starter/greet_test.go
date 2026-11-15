package greet_test

import (
	"testing"

	"dojo/greet"
)

func TestGreetsByName(t *testing.T) {
	got := greet.Greet("Ada", false)
	want := "Welcome to the dojo, Ada!"
	if got != want {
		t.Errorf("Greet(%q, false) = %q, want %q", "Ada", got, want)
	}
}
