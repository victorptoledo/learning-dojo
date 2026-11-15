package words_test

import (
	"maps"
	"slices"
	"testing"

	"dojo/words"
)

const sample = "The cat and the hat. THE END, don't stop!"

func equalStrings(t *testing.T, call string, got, want []string) {
	t.Helper()
	if len(got) == 0 && len(want) == 0 {
		return
	}
	if !slices.Equal(got, want) {
		t.Errorf("%s = %q, want %q", call, got, want)
	}
}

func TestWordsKeepsApostrophesInsideWords(t *testing.T) {
	equalStrings(t, "Words(sample)", words.Words(sample),
		[]string{"the", "cat", "and", "the", "hat", "the", "end", "don't", "stop"})
	equalStrings(t, `Words("'quoted' rock'n'roll l'eau")`, words.Words("'quoted' rock'n'roll l'eau"),
		[]string{"quoted", "rock'n'roll", "l'eau"})
}

func TestWordsSplitsOnPunctuationAndUnderscores(t *testing.T) {
	equalStrings(t, `Words("snake_case, kebab-case; x...y")`, words.Words("snake_case, kebab-case; x...y"),
		[]string{"snake", "case", "kebab", "case", "x", "y"})
}

func TestWordsHandlesUnicodeAndDigits(t *testing.T) {
	equalStrings(t, `Words("Café NAÏVE 2026 日本")`, words.Words("Café NAÏVE 2026 日本"),
		[]string{"café", "naïve", "2026", "日本"})
}

func TestWordsOfEmptyText(t *testing.T) {
	equalStrings(t, `Words("  ... ")`, words.Words("  ... "), nil)
}

func TestCount(t *testing.T) {
	got := words.Count([]string{"a", "b", "a", "c", "a", "b"})
	want := map[string]int{"a": 3, "b": 2, "c": 1}
	if !maps.Equal(got, want) {
		t.Errorf("Count(...) = %v, want %v", got, want)
	}
}

func TestTopMostFrequentFirst(t *testing.T) {
	got := words.Top("b a b c b a", 2)
	want := []words.WordCount{{Word: "b", Count: 3}, {Word: "a", Count: 2}}
	if !slices.Equal(got, want) {
		t.Errorf(`Top("b a b c b a", 2) = %v, want %v`, got, want)
	}
}

func TestTopBreaksTiesAlphabetically(t *testing.T) {
	got := words.Top(sample, 4)
	want := []words.WordCount{{Word: "the", Count: 3}, {Word: "and", Count: 1}, {Word: "cat", Count: 1}, {Word: "don't", Count: 1}}
	if !slices.Equal(got, want) {
		t.Errorf("Top(sample, 4) = %v, want %v", got, want)
	}
}

func TestTopWithFewerWordsOrNoN(t *testing.T) {
	if got := words.Top("one two", 10); len(got) != 2 {
		t.Errorf(`Top("one two", 10) = %v, want 2 entries`, got)
	}
	if got := words.Top(sample, 0); len(got) != 0 {
		t.Errorf("Top(sample, 0) = %v, want none", got)
	}
	if got := words.Top(sample, -1); len(got) != 0 {
		t.Errorf("Top(sample, -1) = %v, want none", got)
	}
}

func TestFirstSeen(t *testing.T) {
	equalStrings(t, "FirstSeen(sample)", words.FirstSeen(sample),
		[]string{"the", "cat", "and", "hat", "end", "don't", "stop"})
}

func TestByLengthGroupsDistinctSortedWords(t *testing.T) {
	got := words.ByLength(sample)
	want := map[int][]string{3: {"and", "cat", "end", "hat", "the"}, 4: {"stop"}, 5: {"don't"}}
	if !maps.EqualFunc(got, want, slices.Equal) {
		t.Errorf("ByLength(sample) = %v, want %v", got, want)
	}
}

func TestByLengthCountsRunesNotBytes(t *testing.T) {
	got := words.ByLength("été ete")
	want := map[int][]string{3: {"ete", "été"}}
	if !maps.EqualFunc(got, want, slices.Equal) {
		t.Errorf(`ByLength("été ete") = %v, want %v`, got, want)
	}
}
