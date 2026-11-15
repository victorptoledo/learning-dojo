---
from: go
status: draft
concept: "Tries and prefix search"
from_way: "Looping over a slice with strings.HasPrefix for each query."
native_way: "A struct with `children map[rune]*node` (or `[26]*node`) and counters. Popular HTTP routers such as httprouter and the Gin framework are radix trees of path segments, the compressed tries of this lesson."
sensei_tip: "Ranging over a string yields runes, so `for _, r := range word` walks code points and a `map[rune]*node` trie handles any Unicode text; `[26]*node` only fits a-z."
---

| Go | Java |
| --- | --- |
| `children map[rune]*node` | `HashMap<Character, Node>` |
| `children [26]*node` | `Node[26]` |
| `strings.HasPrefix(s, p)` | `s.startsWith(p)` |
| httprouter / Gin radix tree | Spring path matching |
| `sort.SearchStrings` on a sorted slice | `TreeMap.subMap` |
