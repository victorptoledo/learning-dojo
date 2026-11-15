# Linked Lists, Stacks, Queues and Deques

Arrays keep elements side by side; **linked lists** chain them with pointers. Linked lists are
less useful in everyday Java than textbooks suggest, but their techniques run through every
interview, and the **abstract types** built on them (stacks, queues and deques) are everywhere:
call stacks, undo, BFS, message queues, schedulers and buffers.

## Linked lists

A **singly linked list** node holds a value and a pointer to the next node; a **doubly linked**
node also points to the previous one.

```text
head → [3 | •] → [8 | •] → [5 | ∅]                 singly linked
∅ ← [3] ⇄ [8] ⇄ [5] → ∅                           doubly linked
```

| Operation | Singly | Doubly | Array list |
| --- | --- | --- | --- |
| access index i | O(i) | O(min(i, n − i)) | O(1) |
| insert/remove at the front | O(1) | O(1) | O(n) |
| insert/remove at the back | O(n), O(1) with a tail pointer for insert | O(1) | O(1) amortised |
| insert/remove next to a node you hold | O(1) | O(1) | O(n) |
| memory per element | value + 1 pointer + object header | + 2 pointers | value (or reference) |

The O(1) inserts only help when you **already hold the node**. Finding it is O(n), and walking a
linked list jumps around the heap, which is slow for CPU caches. That is why `ArrayList` beats
`LinkedList` for almost every Java workload, even for many middle insertions. Linked structures
win when nodes are referenced from elsewhere: an LRU cache's hash map points straight at list
nodes (`LinkedHashMap`), an OS scheduler moves tasks between queues, a memory allocator links
free blocks.

### Techniques interviewers love

- **Dummy head (sentinel)**: start with a fake node before the head, so inserting or deleting the
  first node is the same code as any other. Return `dummy.next`.
- **Reverse in place**: walk with `previous`, `current`, `next`; point `current.next` at
  `previous` and move on. O(n) time, O(1) memory.
- **Fast and slow pointers**: one moves two steps, one moves one. When fast reaches the end, slow
  is in the **middle**; if they ever **meet**, there is a **cycle** (Floyd's algorithm). The same
  trick finds where the cycle starts.
- **Two pointers with a gap**: move one pointer k steps ahead, then both together; when the first
  hits the end, the second is k from the end.
- **Merge two sorted lists**: repeatedly take the smaller head; with a dummy head it is ten lines.

## Stacks, queues and deques

These are **abstract data types**: they promise operations, not an implementation.

| Type | Order | Operations | Typical uses |
| --- | --- | --- | --- |
| Stack | last in, first out | push, pop, peek | call stack, undo, bracket matching, DFS, expression parsing |
| Queue | first in, first out | offer, poll, peek | BFS, task queues, buffering, rate limiting |
| Deque | both ends | add/remove first and last | sliding windows, work stealing, palindromes |
| Priority queue | smallest (or largest) first | add, poll | scheduling, Dijkstra, top-k: phase 2 |

### Implementing them on a circular array

A queue on a plain array would shift everything on each `poll`. A **circular buffer** keeps a
`head` index and a `size`, and wraps around:

```text
capacity 8, head = 6, size = 4:   [c d _ _ _ _ a b]
                                     ↑           ↑
                                  tail=(6+4)%8=2  head
offer → slot (head + size) % capacity      poll → slot head, then head = (head + 1) % capacity
```

All operations are O(1) and there is no allocation per element. When the buffer is full you
either grow it (copying into a bigger array, amortised O(1), as `ArrayDeque` does) or, for a
fixed-size **ring buffer**, overwrite or reject. Ring buffers power audio pipelines, logging
libraries, network cards and the LMAX Disruptor.

### Monotonic stacks and deques

A stack or deque whose contents stay sorted solves a family of "next greater" and "window
maximum" problems in linear time:

- **Next greater element**: scan left to right with a stack of indexes still waiting for an
  answer. A new value pops every smaller value on top (it is their answer), then is pushed.
- **Sliding window maximum**: a deque of candidates with decreasing values. A new value removes
  smaller ones from the back (they can never be the maximum again); the front leaves when it
  falls out of the window.

Each element is pushed once and popped at most once, so the whole scan is O(n) even though
there is a loop inside the loop. Both appear in the next two katas.

## In the JDK

| Need | Use | Avoid |
| --- | --- | --- |
| stack | `ArrayDeque` (`push`, `pop`, `peek`) | `Stack` (synchronized `Vector`) |
| queue | `ArrayDeque` (`offer`, `poll`) | `LinkedList` (a node per element) |
| deque | `ArrayDeque` | |
| bounded queue between threads | `ArrayBlockingQueue` (a ring buffer with locks) | unbounded queues under load |
| lock-free queue between threads | `ConcurrentLinkedQueue` | |
| linked list with O(1) node removal | `LinkedHashMap` for LRU, or your own nodes | `LinkedList.remove(Object)` (O(n)) |

`ArrayDeque` rejects `null` elements: `poll` uses `null` to say "empty".

## In real systems

- Every thread has a **call stack**; deep recursion overflows it (`StackOverflowError`), which is
  why the tree kata asks for an explicit stack.
- Message brokers (RabbitMQ, SQS) and Kafka partitions are queues with persistence.
- Thread pools take work from a `BlockingQueue`; `ForkJoinPool` gives each worker a deque and
  lets idle workers steal from the other end.
- Browsers keep the back/forward history as two stacks.

## Summary

- Linked lists give O(1) insert and remove only next to a node you already hold; arrays win most
  everyday workloads thanks to locality.
- Learn the pointer techniques: dummy head, in-place reversal, fast and slow pointers.
- Stacks are LIFO, queues FIFO, deques both; implement them on a circular array.
- Monotonic stacks and deques answer next-greater and window-maximum questions in O(n).
- In Java, `ArrayDeque` is the default stack, queue and deque.

## Further reading

- Sedgewick and Wayne, *Algorithms*, 4th ed., "Bags, Queues, and Stacks": <https://algs4.cs.princeton.edu/13stacks/>
- Morin, *Open Data Structures*, chapters on array-based lists and linked lists: <https://opendatastructures.org/>
- JDK 25 API, `ArrayDeque`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ArrayDeque.html>
- roadmap.sh, DSA topic "Linked Lists": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/linked-lists@bRu9u8pEAQ1TmKygjIjyI.md>
- roadmap.sh, DSA topic "Fast and Slow Pointers": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/fast-and-slow-pointers@J_No8GTa92DcwPtua30wL.md>
- roadmap.sh, DSA topic "Queues": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/queues@v0phjC75jhmE1z-f9ZMck.md>
