# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): the search core of a fare site. It answers
   "same region?" with union-find while routes arrive, the cheapest fare with Dijkstra, the
   cheapest fare with at most k flights with a dynamic programme over the number of flights,
   code suggestions with a trie and a bounded heap, and the cheapest network that joins every
   airport with Kruskal.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Shortest Paths, Union-Find and Spanning Trees | Why is the smallest tentative distance final in Dijkstra? What do path compression and union by size each prevent? Why does the cut property make Kruskal correct? |
| Route Finder (kata) | How do I skip an outdated heap entry? Why does Kruskal sort a copy of the edges? |
| Tries and Prefix Search | What does a prefix query cost, and what does it not depend on? How do I rank only the best k? |
| Autocomplete (kata) | What does each node have to store so the question stops at the prefix? |
| Dynamic Programming | What is the state, what is the recurrence, and in which order are the states ready? |
| DP Toolkit (kata) | When does one row of the table replace the whole table, and in which direction must I fill it? |

> [!TIP]
> "Cheapest with at most k flights" is Bellman-Ford stopped after k rounds, and each round may
> only read the previous one. If a round reads values it wrote itself, one round can add many
> flights and the limit stops meaning anything.
