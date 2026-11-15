---
from: java
status: draft
concept: "Lists, stacks and queues"
from_way: "Using java.util.Stack for stacks and LinkedList for queues because the names match."
native_way: "ArrayDeque is the stack, queue and deque of choice: a growable circular array with O(1) operations at both ends and good cache behaviour. LinkedList allocates a node per element; Stack is a synchronized Vector from Java 1.0."
sensei_tip: "Use the Deque method names consistently: `push`/`pop`/`peek` for a stack (they work on the front), `offer`/`poll`/`peek` for a queue. Mixing `push` with `poll` silently turns your stack into something else."
---

| Need | Java | Note |
| --- | --- | --- |
| stack | `Deque<T> s = new ArrayDeque<>(); s.push(x); s.pop();` | LIFO at the front |
| queue | `Queue<T> q = new ArrayDeque<>(); q.offer(x); q.poll();` | FIFO |
| deque | `addFirst`, `addLast`, `pollFirst`, `pollLast` | both ends |
| bounded, thread-safe | `ArrayBlockingQueue` | a ring buffer with locks |
| LRU list | `LinkedHashMap(cap, 0.75f, true)` + `removeEldestEntry` | hash map + doubly linked list |
| your own node | `final class Node { int val; Node next; }` | interview style |
