# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a build planner like the core of Maven or Bazel.
   It orders tasks after their dependencies (Kahn's algorithm with a priority queue), finds a
   dependency cycle (an iterative DFS), computes when each task can finish and which chain sets
   the total time (longest paths in a DAG), and lists what a change affects (a BFS over reverse
   edges).

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Sorting and Binary Search | Stable or not? When is the library sort enough? What is the invariant of my binary search? |
| Recursion, Divide and Conquer and Backtracking | What is the base case? How deep can the recursion go? What does backtracking undo? |
| Search Toolkit (kata) | Why a random pivot and a three-way partition? How does merge sort count inversions? |
| Trees, Binary Search Trees and Heaps | Why does a plain BST degrade? What does a heap guarantee, and what not? |
| Heap and Trees (kata) | Why is bottom-up heapify O(n)? How do I traverse a tree without recursion? |
| Graphs: BFS, DFS and Topological Sort | Adjacency list or matrix? Why does BFS give shortest paths in unweighted graphs? How does Kahn's algorithm detect a cycle? |

> [!TIP]
> The kata's graphs reach 200,000 tasks. Anything recursive along a chain overflows the stack,
> and anything that scans every task to find the next ready one is quadratic. Kahn's algorithm
> touches each task and each dependency once.
