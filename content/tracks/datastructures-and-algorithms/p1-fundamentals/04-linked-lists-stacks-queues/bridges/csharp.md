---
from: csharp
status: draft
concept: "Lists, stacks and queues"
from_way: "Reaching for LinkedList<T> whenever insertions are involved."
native_way: "Stack<T> and Queue<T> are array-backed (Queue<T> is a circular buffer) and are the right defaults; LinkedList<T> is doubly linked and pays an allocation per node. .NET has no built-in deque, but PriorityQueue<TElement, TPriority> exists since .NET 6."
sensei_tip: "Unlike Java's Stack, C#'s Stack<T> is not synchronized and is perfectly fine to use. For producer/consumer queues, `Channel<T>` (bounded) is the modern equivalent of Java's ArrayBlockingQueue."
---

| C# | Java | Note |
| --- | --- | --- |
| `Stack<T>` | `ArrayDeque` as a stack | array-backed |
| `Queue<T>` | `ArrayDeque` as a queue | circular buffer |
| (no deque) | `ArrayDeque` | write one, or use LinkedList<T> |
| `LinkedList<T>` | `LinkedList<T>` | doubly linked |
| `Channel.CreateBounded<T>(n)` | `ArrayBlockingQueue` | producer/consumer |
| `ConcurrentQueue<T>` | `ConcurrentLinkedQueue` | lock-free |
