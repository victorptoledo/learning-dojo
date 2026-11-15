---
from: java
status: draft
concept: "Tries and prefix search"
from_way: "Filtering a List<String> with startsWith for every keystroke."
native_way: "Write a trie with a small Node class (HashMap<Character, Node> children, an end count, a subtree count), or, for read-mostly data, use TreeMap.subMap(prefix, prefix + Character.MAX_VALUE) to get the range of keys with a prefix."
sensei_tip: "For lowercase-only interview problems, `Node[] children = new Node[26]` indexed by `c - 'a'` is the fastest and shortest. In production, Lucene's term dictionary (FST) is a compressed trie-like automaton, which is what Elasticsearch prefix queries use."
---

| Need | Java |
| --- | --- |
| node | `final class Node { Map<Character, Node> children = new HashMap<>(); int words; long freq; }` |
| small alphabet | `Node[] children = new Node[26]` |
| prefix range, no trie | `treeMap.subMap(p, true, p + Character.MAX_VALUE, false)` |
| top k | `PriorityQueue` of size k |
| real-world | Lucene FST, Spring's `PathPatternParser` (segment tree) |
