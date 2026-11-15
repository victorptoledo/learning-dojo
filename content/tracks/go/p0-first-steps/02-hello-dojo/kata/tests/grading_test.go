package greet_test

import (
	"testing"

	"dojo/greet"
)

func check(t *testing.T, call, got, want string) {
	t.Helper()
	if got != want {
		t.Errorf("%s = %q, want %q", call, got, want)
	}
}

func TestGreetsAStrangerWhenTheNameIsEmpty(t *testing.T) {
	check(t, `Greet("", false)`, greet.Greet("", false), "Welcome to the dojo, stranger!")
}

func TestGreetsAStrangerWhenTheNameIsBlank(t *testing.T) {
	check(t, `Greet("  \t ", false)`, greet.Greet("  \t ", false), "Welcome to the dojo, stranger!")
}

func TestTrimsSpacesAroundTheName(t *testing.T) {
	check(t, `Greet("  Grace Hopper ", false)`, greet.Greet("  Grace Hopper ", false), "Welcome to the dojo, Grace Hopper!")
}

func TestShoutsTheWholeMessage(t *testing.T) {
	check(t, `Greet(" Grace ", true)`, greet.Greet(" Grace ", true), "WELCOME TO THE DOJO, GRACE!")
	check(t, `Greet("", true)`, greet.Greet("", true), "WELCOME TO THE DOJO, STRANGER!")
}

func TestInitialsOfEachWord(t *testing.T) {
	check(t, `Initials("ada lovelace")`, greet.Initials("ada lovelace"), "AL")
	check(t, `Initials("  grace   brewster  hopper ")`, greet.Initials("  grace   brewster  hopper "), "GBH")
	check(t, `Initials("élodie durand")`, greet.Initials("élodie durand"), "ÉD")
}

func TestInitialsOfABlankName(t *testing.T) {
	check(t, `Initials("")`, greet.Initials(""), "")
	check(t, `Initials("   ")`, greet.Initials("   "), "")
}
