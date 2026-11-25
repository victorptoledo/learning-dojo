# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a model checker, in Python, that explores every
   interleaving of a small concurrent program. It ties the phase together: the program is a
   **finite automaton** whose states are snapshots of the machine, the search is a plain graph
   walk, the number of states grows **exponentially** with the threads (which is why real
   checkers need bounds and reductions), and what it finds are the **races, deadlocks and
   broken locks** of the concurrency lesson, each with the shortest schedule that shows it.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Automata, Regular Languages and Regular Expressions | What are the states and transitions? Why does a DFA never need to backtrack? |
| Regex Engine (kata) | How does simulating a set of states avoid exponential time? |
| Turing Machines, Grammars and Computability | Why can a model checker decide properties of *this* program but no tool can for every program? |
| Complexity Classes | How many states do n threads of k steps have? What does that make exhaustive checking cost? |
| Concurrency Theory | Which interleaving loses an update? Which Coffman condition does lock ordering break? Why is Peterson's algorithm correct? |

> [!TIP]
> A finite program with finite data has finitely many states, so "does it ever deadlock?" is
> decidable for it: you can look at every state. Rice's theorem says this cannot work for
> arbitrary programs, because their state spaces are unbounded. Model checkers live in that
> gap, and their real enemy is not undecidability but the state explosion.
