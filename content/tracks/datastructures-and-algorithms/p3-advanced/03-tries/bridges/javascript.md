---
from: javascript
status: draft
concept: "Tries and prefix search"
from_way: "Filtering an array of strings on every keystroke in a search box."
native_way: "A trie of plain objects or Maps (`node.children.get(ch)`), built once and walked per keystroke. Routers such as find-my-way (used by Fastify) are radix trees of URL segments."
sensei_tip: "Use a `Map` rather than a plain object for children: keys like 'constructor' or '__proto__' are ordinary there. Debounce the input as well, so you do not query on every single key press."
---

| JavaScript | Java |
| --- | --- |
| `{ children: new Map(), end: false }` | `Node` with `HashMap<Character, Node>` |
| `for (const ch of word)` | `for (char c : word.toCharArray())` |
| Fastify's radix-tree router | Spring's path pattern matching |
| sorted array + binary search | `TreeMap.subMap` |
