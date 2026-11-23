Hashtags, domain names and text pasted without spaces: `thequickbrownfox`, `expertsexchange`.
Splitting them back into words is a dynamic programming problem on positions. Add to
`SpellChecker`:

```java
public Optional<List<String>> segment(String text)
```

Split `text` (letters `a`-`z`, any case) into dictionary words, in lower case, using the **fewest
words**; empty when it cannot be split at all. `""` gives an empty list. Among the splits with
the fewest words, keep the one whose first word is longest, then whose second word is longest,
and so on. `null`, or a text with anything but letters, is an `IllegalArgumentException`.

```java
// dictionary: a, an, and, android, droid, i, the, ant, is
spell.segment("TheAndroid");   // Optional[[the, android]]: fewer words than the, an, droid
spell.segment("anandroid");    // Optional[[an, android]]
spell.segment("antis");        // Optional[[ant, is]]: "an" leaves "tis", which cannot be split
spell.segment("xyz");          // Optional.empty
```

**From the end.** Let `best[i]` be the fewest words that split `text[i..]`, with `best[n] = 0`.
Then `best[i] = 1 + min(best[j])` over the `j` where `text[i..j)` is a word. Computing it from
`i = n - 1` down to `0` makes every `best[j]` known when it is needed, and remembering the `j` that
was chosen rebuilds the split. Walking down to the longest `j` that ties keeps the longest first
word, and the rest follows by itself.

**Find the words with the trie.** Checking every `text[i..j)` with a hash lookup is O(n²)
substrings. Walking the trie from position `i` finds every word that starts there in one pass, and
stops as soon as no word continues. The tests split a text of 200,000 letters, and texts built to
make plain recursion take forever.
