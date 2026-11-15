# Autocomplete

A search box suggests completions as you type: "ca" → "cat", "car", "card". Write
`Autocomplete`, a **trie** (prefix tree) where each node is one character, each word ends at a
node, and every node knows how many words are below it. Do not keep a plain list of words and
scan it: the hidden tests store 200,000 words and run 200,000 prefix queries.

```java
var ac = new Autocomplete();
ac.add("card");
ac.add("care", 3);       // searched 3 times
ac.add("cat", 5);
ac.add("car", 3);
ac.countWithPrefix("car");   // 3: car, card, care
ac.suggest("ca", 3);         // [cat, car, care]   (5, then 3 and 3 alphabetically)
ac.remove("card");           // true
ac.nodeCount();              // 5: c, a, r, e, t
```

## Methods

| Method | Does |
| --- | --- |
| `add(word)` | adds one search of the word (frequency + 1) |
| `add(word, times)` | adds `times` searches; `times < 1` throws `IllegalArgumentException` |
| `frequency(word)` | how often it was added, 0 if absent |
| `contains(word)` | whether it is stored |
| `size()` | the number of distinct words |
| `countWithPrefix(prefix)` | distinct words starting with the prefix, in **O(prefix length)**; `""` counts all |
| `suggest(prefix, k)` | at most `k` words starting with the prefix: highest frequency first, ties in alphabetical (`String.compareTo`) order |
| `remove(word)` | removes the word completely and **prunes** nodes that no longer lead to any word; `false` if it was absent |
| `nodeCount()` | the number of nodes, not counting the root |

A `null` or empty word throws `IllegalArgumentException` in `add` and `remove`. A `null` prefix
throws `IllegalArgumentException`; `k < 0` too.

## Hints for the shape

- A node holds `Map<Character, Node> children`, `long frequency` (0 when no word ends here) and
  `int words` (the distinct words that end in this node's subtree, itself included).
- `add` walks or creates the path; only when the word is **new** does it add 1 to `words` along
  the path.
- `countWithPrefix` walks the prefix and reads `words`: that is what makes it O(prefix length).
- `suggest` walks the prefix, then visits the subtree and keeps the best `k` in a small heap (or
  collects them and sorts, when the subtree is small).
- `remove` walks the path, subtracts 1 from `words` along it, and drops the first child whose
  `words` becomes 0: everything below it is gone too.
