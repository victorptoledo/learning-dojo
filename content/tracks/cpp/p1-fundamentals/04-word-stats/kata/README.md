# Word Stats

Write the five functions declared in `words.hpp`, in `words.cpp`. `WordCount` is already
declared (with a defaulted `operator==`, so tests can compare it).

**A word** is a run of ASCII letters or digits (`std::isalnum`). An apostrophe `'` between a
word character and another word character belongs to the word (`don't`, `rock'n'roll`); any
other character, `_` and `-` included, separates words. Words are returned in lower case.

| Function | Returns |
| --- | --- |
| `words(text)` | Every word, in order, in lower case |
| `count(words)` | A `std::map` from each word to how many times it appears |
| `top(text, n)` | The `n` most frequent words, most frequent first; equal counts in alphabetical order. Fewer if there are fewer words; empty when `n` is 0 |
| `first_seen(text)` | Each different word once, in the order it first appears |
| `by_length(text)` | Each word length mapped to the different words of that length, sorted |

```cpp
const auto* text = "The cat and the hat. THE END, don't stop!";
dojo::words(text);       // {the, cat, and, the, hat, the, end, don't, stop}
dojo::top(text, 2);      // {{the, 3}, {and, 1}}
dojo::first_seen(text);  // {the, cat, and, hat, end, don't, stop}
dojo::by_length(text);   // {3: {and, cat, end, hat, the}, 4: {stop}, 5: {don't}}
```

Tools: `std::isalnum` and `std::tolower` (cast to `unsigned char` first), `std::map` and its
`operator[]`, structured bindings (`for (const auto& [word, n] : counts)`),
`std::ranges::sort` with a lambda, `std::set` and `std::unordered_set`.

The dojo runs 10 hidden tests and the visible one, plus the warnings, clang-format and
clang-tidy checks on `words.cpp` and `words.hpp`.
