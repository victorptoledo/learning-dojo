The dictionary first, then the measure of how wrong a word is.

| Method | Does |
| --- | --- |
| `void add(String word)`, `void add(String word, long frequency)` | Adds the word, in lower case, with how often it is used (1 by default). Adding it again adds to its frequency. A `null` or empty word, a word with anything but letters `a`-`z` (in any case), or a frequency below 1 is an `IllegalArgumentException`. |
| `boolean contains(String word)`, `long frequency(String word)` | Ignoring case; `0` and `false` for unknown words and for strings that cannot be words. `null` is an `IllegalArgumentException`. |
| `int size()` | Distinct words. |
| `List<Misspelling> check(String text)` | The words of the text that are not in the dictionary, in order, as written, with the index where each starts. A word is a maximal run of ASCII letters; everything else separates words. |
| `static int distance(String a, String b)` | The Levenshtein distance: the fewest insertions, deletions and substitutions of one character that turn `a` into `b`. `null` is an `IllegalArgumentException`. |

```java
var spell = new SpellChecker();
spell.add("the", 500);
spell.add("cat");
spell.check("The catt sat.");              // [Misspelling[word=catt, offset=4], Misspelling[word=sat, offset=9]]
SpellChecker.distance("kitten", "sitting"); // 3: k→s, e→i, +g
```

**The distance in two rows.** `d[i][j]`, the distance between the first `i` letters of `a` and
the first `j` of `b`, is the smallest of `d[i-1][j] + 1` (delete), `d[i][j-1] + 1` (insert) and
`d[i-1][j-1] + (a[i-1] == b[j-1] ? 0 : 1)` (keep or substitute), with `d[i][0] = i` and
`d[0][j] = j`. Row `i` needs only row `i - 1`, so keep two rows of the shorter word's length: the
tests compare strings of 15,000 characters, whose full table of 225 million `int`s does not fit in
the heap.

Store the words in a **trie** (a node per prefix, children by letter, the frequency where a word
ends): the next stage walks it.
