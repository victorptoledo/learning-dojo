// Package words computes statistics about the words of a text.
package words

import (
	"cmp"
	"slices"
	"strings"
	"unicode"
	"unicode/utf8"
)

// WordCount is a word and how many times it appears.
type WordCount struct {
	Word  string
	Count int
}

func isWordRune(r rune) bool {
	return unicode.IsLetter(r) || unicode.IsDigit(r)
}

// Words returns every word of text, in order and in lower case. A word is a run of letters
// or digits; an apostrophe between two of them belongs to the word.
func Words(text string) []string {
	runes := []rune(strings.ToLower(text))
	var words []string
	var word strings.Builder
	for i, r := range runes {
		apostrophe := r == '\'' && i > 0 && i+1 < len(runes) &&
			isWordRune(runes[i-1]) && isWordRune(runes[i+1])
		if isWordRune(r) || apostrophe {
			word.WriteRune(r)
			continue
		}
		if word.Len() > 0 {
			words = append(words, word.String())
			word.Reset()
		}
	}
	if word.Len() > 0 {
		words = append(words, word.String())
	}
	return words
}

// Count returns how many times each word appears.
func Count(words []string) map[string]int {
	counts := make(map[string]int)
	for _, word := range words {
		counts[word]++
	}
	return counts
}

// Top returns the n most frequent words of text, most frequent first and equal counts in
// alphabetical order.
func Top(text string, n int) []WordCount {
	if n <= 0 {
		return nil
	}
	var ranking []WordCount
	for word, count := range Count(Words(text)) {
		ranking = append(ranking, WordCount{Word: word, Count: count})
	}
	slices.SortFunc(ranking, func(a, b WordCount) int {
		if c := cmp.Compare(b.Count, a.Count); c != 0 {
			return c
		}
		return strings.Compare(a.Word, b.Word)
	})
	return ranking[:min(n, len(ranking))]
}

// FirstSeen returns each different word of text once, in the order it first appears.
func FirstSeen(text string) []string {
	seen := make(map[string]bool)
	var order []string
	for _, word := range Words(text) {
		if !seen[word] {
			seen[word] = true
			order = append(order, word)
		}
	}
	return order
}

// ByLength maps each word length, in runes, to the sorted different words of that length.
func ByLength(text string) map[int][]string {
	groups := make(map[int][]string)
	for _, word := range FirstSeen(text) {
		length := utf8.RuneCountInString(word)
		groups[length] = append(groups[length], word)
	}
	for _, group := range groups {
		slices.Sort(group)
	}
	return groups
}
