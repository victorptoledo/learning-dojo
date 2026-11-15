package words_test

import (
	"slices"
	"testing"

	"dojo/words"
)

func TestWordsSplitsAndLowercases(t *testing.T) {
	got := words.Words("The cat and the HAT.")
	want := []string{"the", "cat", "and", "the", "hat"}
	if !slices.Equal(got, want) {
		t.Errorf("Words(...) = %q, want %q", got, want)
	}
}
