The most common typo is two letters swapped: `teh`, `recieve`, `form` for `from`. Levenshtein
counts a swap as two substitutions, so `teh` is as far from `the` as from `ten`. The **optimal
string alignment** distance (a restricted Damerau-Levenshtein) adds one more move to the table:

```text
if i > 1 and j > 1 and a[i-1] == b[j-2] and a[i-2] == b[j-1]:
    d[i][j] = min(d[i][j], d[i-2][j-2] + 1)      // swap two neighbouring letters
```

Each cell now looks two rows up, so keep three rows. Add to `SpellChecker`:

| Method | Does |
| --- | --- |
| `static int typoDistance(String a, String b)` | The optimal string alignment distance. `null` is an `IllegalArgumentException`. |
| `List<String> suggestTypos(String word, int maxDistance, int limit)` | Like `suggest`, with `typoDistance` instead of `distance`. |
| `Optional<String> correct(String word)` | The word itself, in lower case, when it is in the dictionary; otherwise the first of `suggestTypos(word, 2, 1)`, or empty. A word that is not letters `a`-`z` is an `IllegalArgumentException`. |

```java
SpellChecker.typoDistance("teh", "the");     // 1 (distance gives 2)
SpellChecker.typoDistance("ca", "abc");      // 3: "optimal alignment" never edits a swapped pair again
spell.correct("Teh");                         // Optional[the]
spell.correct("xqzv");                        // Optional.empty
```

In the trie walk, the row of a node is built from its parent's row **and its grandparent's**, and
needs the parent's letter too: pass both down the recursion. The pruning still holds (a swap
costs 1 and moves two rows down, so a row's minimum still cannot fall). The tests run 500
corrections on 60,000 words.
