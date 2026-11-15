# Tries and Prefix Search

A hash map tells you whether "cart" is a key. It cannot tell you which keys **start with**
"ca" without looking at all of them. A **trie** (from re*trie*val, also called a prefix tree)
stores keys character by character along paths from a root, so every prefix is a node and
prefix questions cost only the length of the prefix.

## The structure

```text
            (root)
           /      \
          c        d
          |        |
          a        o
        /   \      |
       r•    t•    g•          words: car, card, care, cat, dog
      / \                       • marks a node where a word ends
     d•  e•
```

Each node has children keyed by character and a flag (or a count) saying a word ends there.

| Operation | Cost | How |
| --- | --- | --- |
| insert word of length L | O(L) | walk, creating missing nodes |
| search word | O(L) | walk, check the end flag |
| starts-with prefix | O(L) | walk the prefix |
| count words with prefix | O(L) | read a counter kept at each node |
| list words with prefix | O(L + size of the subtree) | walk, then DFS |
| delete | O(L) | unmark, then prune nodes nothing needs |

None of these depend on how many words are stored. Compared with a balanced tree of strings
(O(L log n) because each comparison costs up to L), a trie also shares the work of common
prefixes.

## Node layouts

| Children stored as | Child lookup | Memory | Good for |
| --- | --- | --- | --- |
| `Node[26]` (or 128, 256) | O(1) index | 26 references per node, mostly empty | small fixed alphabets, interviews |
| `HashMap<Character, Node>` | O(1) expected | proportional to real children, but map overhead | any alphabet |
| sorted array of (char, node) | O(log k) | compact | read-mostly tries |
| compressed edges (radix / Patricia trie) | compare a label | far fewer nodes | real systems |

A **compressed trie** merges chains of single-child nodes into one edge labelled with a whole
string ("ca" → "r" → ...). Real implementations (Linux routing tables, HTTP routers, the Ethereum
Merkle-Patricia trie, many search engines' term dictionaries) use compression because naive tries
are memory hungry: a million words can mean tens of millions of nodes.

## Making prefix queries fast

- **Counts at nodes**: keep, at every node, the number of distinct words in its subtree. Update
  it along the path on insert (only when the word is new) and on delete. "How many words start
  with p" is then O(|p|).
- **Top-k suggestions**: walk to the prefix node, then explore its subtree keeping the best k in
  a min-heap of size k. For very short prefixes, whose subtrees are huge, **cache** the top k at
  each node and refresh the caches along the path when a frequency changes: production
  autocomplete does this.
- **Deletion with pruning**: remove the end mark; then, walking back up, remove each node that
  ends no word and has no children. With counts, the first child on the path whose count drops
  to 0 can be cut off with its whole subtree. You implement this in the next kata.

## Trie problems you will meet

- **Autocomplete and search suggestions**.
- **Spell checking**: walk the trie while allowing a few edits (Levenshtein automata).
- **Word search on a grid** ("find all dictionary words in this board"): DFS on the grid while
  walking the trie, pruning as soon as the path is not a prefix of any word. Much faster than
  searching for each word separately.
- **Longest common prefix** of many strings: walk from the root while there is one child.
- **Longest prefix match**: IP routing looks up the most specific route for an address by walking
  a binary trie over its bits and remembering the last node with a route.
- **Maximum XOR of two numbers**: a binary trie over the bits of the numbers, choosing the
  opposite bit at each level when possible.
- **HTTP routing**: frameworks match `/users/:id/orders` with a radix tree of path segments.

## Trie, hash map or sorted array?

| Need | Best fit |
| --- | --- |
| exact lookup only | hash map |
| prefix queries, autocomplete | trie |
| prefix range on static data, low memory | sorted array + binary search (the range of words between p and p + '￿') |
| ordered keys with floor/ceiling | `TreeMap` (`subMap(prefix, prefix + Character.MAX_VALUE)` gives a prefix range too) |

`TreeMap.subMap` gives a quick prefix range in Java without writing a trie, but counting the
entries in it is O(size of the range), not O(L).

## Summary

- A trie stores keys along root-to-node paths; every prefix is a node.
- Insert, search and prefix walks cost O(L), independent of the number of keys.
- Counts per node give O(L) prefix counts; cached top-k lists give fast suggestions.
- Delete must prune only nodes that no word needs.
- Compressed tries cut memory; binary tries over bits solve routing and XOR problems.

## Further reading

- Sedgewick and Wayne, *Algorithms*, 4th ed., "Tries": <https://algs4.cs.princeton.edu/52trie/>
- cp-algorithms, "Aho-Corasick algorithm" (a trie with failure links for multi-pattern search): <https://cp-algorithms.com/string/aho_corasick.html>
- Wikipedia, "Radix tree": <https://en.wikipedia.org/wiki/Radix_tree>
- JDK 25 API, `TreeMap.subMap` (prefix ranges without a trie): <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/TreeMap.html>
- roadmap.sh, DSA topic "Trie": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/trie@zy3wpb1MjizfUfx9_rZy2.md>
