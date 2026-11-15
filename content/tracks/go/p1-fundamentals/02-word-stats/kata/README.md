# Word Stats

Write five functions in `words.go`. The `WordCount` type is already declared.

**A word** is a run of letters or digits, in any alphabet (`café`, `naïve`, `2026` and `日本`
are words; `_` and `-` are not part of one: use `unicode.IsLetter` and `unicode.IsDigit`). An
apostrophe `'` **between** two such characters belongs to the word (`don't`, `l'eau`); any
other character separates words. Words are returned in lower case (`strings.ToLower`).

| Function | Returns |
| --- | --- |
| `Words(text string) []string` | Every word, in order, in lower case |
| `Count(words []string) map[string]int` | How many times each word appears |
| `Top(text string, n int) []WordCount` | The `n` most frequent words, most frequent first; equal counts in alphabetical order. Fewer if there are fewer words; empty when `n <= 0` |
| `FirstSeen(text string) []string` | Each different word once, in the order it first appears |
| `ByLength(text string) map[int][]string` | Each word length **in runes** mapped to the different words of that length, sorted |

```go
text := "The cat and the hat. THE END, don't stop!"
Words(text)      // [the cat and the hat the end don't stop]
Top(text, 2)     // [{the 3} {and 1}]
FirstSeen(text)  // [the cat and hat end don't stop]
ByLength(text)   // map[3:[and cat end hat the] 4:[stop] 5:[don't]]
```

Tools: `[]rune(text)` to look at the neighbours of an apostrophe, `unicode.IsLetter`,
`unicode.IsDigit`, `strings.Builder`, `slices.SortFunc` with `cmp.Compare`, `slices.Sort`,
`utf8.RuneCountInString`, and a `map[string]bool` as a set.

Run the visible test with `go test`. The dojo runs 12 tests: 1 in `words_test.go` and 11
hidden ones, plus `go vet` and `gofmt` on `words.go`.
