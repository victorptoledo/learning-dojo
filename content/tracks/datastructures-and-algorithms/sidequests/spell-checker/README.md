# Sidequest: Spell checker

Type `recieve` and the editor underlines it and offers `receive`. Behind that small feature are
two classic ideas: a **trie** that holds the dictionary by prefixes, and the **edit distance**, a
dynamic programming table that counts the insertions, deletions and substitutions between two
words. Put together, the trie lets the table be computed once per *prefix* instead of once per
word, and whole branches of the dictionary are skipped as soon as they cannot get close enough.

Build `dojo.spell.SpellChecker`. The starter declares the class, its first methods and the record
`Misspelling`; the structure inside is yours, and the contract tests only call the public
methods. They load dictionaries of tens of thousands of words and ask hundreds of questions under
a time limit, inside a 256 MB heap.

- **Stage 1:** the dictionary, a text checked word by word, and the edit distance in O(n·m) time
  and two rows of memory.
- **Stage 2:** suggestions within a distance, by walking the trie with one row of the table per
  node and pruning.
- **Stage 3:** swapped neighbouring letters (`teh` → `the`) as one typo, and a best correction.
- **Stage 4:** text without spaces (`thequickbrownfox`) split into the fewest dictionary words.

Words are letters `a`-`z`; the dictionary ignores case and stores them in lower case. Use the
JDK only. JUnit is there for your own tests.
