---
from: python
status: draft
concept: "Lists, stacks and queues"
from_way: "Using a list as a queue with pop(0), which shifts every element."
native_way: "A list is a fine stack (append and pop at the end are O(1)). For a queue or deque use collections.deque, a doubly linked list of blocks with O(1) operations at both ends; `deque(maxlen=n)` is a ready-made ring buffer."
sensei_tip: "`deque(maxlen=10_000)` drops the oldest item automatically when full: the keystroke buffer of this lesson's exercise in one line. For threads, `queue.Queue` is the BlockingQueue of Python."
---

| Python | Java | Note |
| --- | --- | --- |
| `lst.append(x)` / `lst.pop()` | `deque.push` / `pop` | stack |
| `collections.deque` | `ArrayDeque` | O(1) at both ends |
| `dq.append(x)` / `dq.popleft()` | `offer` / `poll` | queue |
| `deque(maxlen=n)` | ring buffer | drops the oldest |
| `queue.Queue(maxsize=n)` | `ArrayBlockingQueue` | thread safe, blocking |
| `lst.pop(0)` | `list.remove(0)` | O(n): avoid as a queue |
