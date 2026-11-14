# Word Counter

Implement the three functions in `src/lib.rs`.

A **word** is a whitespace-separated piece of text, lowercased, with the punctuation at its
start and end removed (`"Dojo!"` → `"dojo"`, `"don't"` stays `"don't"`). Pieces that are only
punctuation, like `"--"`, are not words.

1. `words(text)` returns the words of `text` in order.
2. `word_counts(text)` returns a `HashMap` from each word to how many times it appears.
3. `top_words(text, n)` returns the `n` most frequent words with their counts, the highest
   count first. On a tie, words come in alphabetical order. If there are fewer than `n` words,
   return all of them.

```rust
top_words("the cat and the hat", 2) == vec![("the".to_string(), 2), ("and".to_string(), 1)]
```

> Tip: `char::is_alphanumeric`, `str::trim_matches`, `HashMap::entry`, `Vec::sort_by` and
> `Ordering::then_with`.
