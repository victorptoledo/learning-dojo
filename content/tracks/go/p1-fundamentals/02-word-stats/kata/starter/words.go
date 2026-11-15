// Package words computes statistics about the words of a text.
package words

// WordCount is a word and how many times it appears.
type WordCount struct {
	Word  string
	Count int
}

// Words returns every word of text, in order and in lower case. A word is a run of letters
// or digits; an apostrophe between two of them belongs to the word.
func Words(text string) []string {
	panic("todo: write Words")
}

// Count returns how many times each word appears.
func Count(words []string) map[string]int {
	panic("todo: write Count")
}

// Top returns the n most frequent words of text, most frequent first and equal counts in
// alphabetical order.
func Top(text string, n int) []WordCount {
	panic("todo: write Top")
}

// FirstSeen returns each different word of text once, in the order it first appears.
func FirstSeen(text string) []string {
	panic("todo: write FirstSeen")
}

// ByLength maps each word length, in runes, to the sorted different words of that length.
func ByLength(text string) map[int][]string {
	panic("todo: write ByLength")
}
