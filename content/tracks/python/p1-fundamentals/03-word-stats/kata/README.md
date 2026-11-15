# Word Stats

Write four functions in `words.py`.

**A word** is a run of letters or digits, in any alphabet (`café`, `naïve`, `2026` and
`日本` are words; `_` is not part of one). An apostrophe **between** two such characters
belongs to the word (`don't`, `l'eau`); any other character separates words. Words are
compared without case and returned in lower case: use `str.casefold()`, which also folds
`ß` to `ss`.

| Function | Returns |
| --- | --- |
| `words(text) -> list[str]` | Every word, in order, case-folded |
| `top_words(text, n=3) -> list[tuple[str, int]]` | The `n` most frequent words as `(word, count)`, most frequent first; equal counts in alphabetical order. Fewer if there are fewer words; `[]` when `n <= 0` |
| `first_seen(text) -> list[str]` | Each different word once, in the order it first appears |
| `words_by_length(text) -> dict[int, list[str]]` | Each word length mapped to the different words of that length, sorted; the keys in increasing order |

```python
text = "The cat and the hat. THE END, don't stop!"
words(text)            # ['the', 'cat', 'and', 'the', 'hat', 'the', 'end', "don't", 'stop']
top_words(text, 2)     # [('the', 3), ('and', 1)]
first_seen(text)       # ['the', 'cat', 'and', 'hat', 'end', "don't", 'stop']
words_by_length(text)  # {3: ['and', 'cat', 'end', 'hat', 'the'], 4: ['stop'], 5: ["don't"]}
```

Tools: `re.findall`, `str.casefold`, `collections.Counter`, `collections.defaultdict`,
`dict.fromkeys`, `sorted(..., key=...)`, comprehensions.

Run the visible tests with `uv run pytest`. There are 12 tests: 1 in
`tests/test_visible.py` and 11 hidden ones, plus ruff on `words.py`.
