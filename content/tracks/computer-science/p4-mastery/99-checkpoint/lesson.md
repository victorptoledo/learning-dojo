# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards database
   internals and systems thinking. You need 7 right.
2. **The integration kata** (press **Enter**): a B+tree stored in 4 KiB pages of a file, behind
   an LRU buffer pool that pins the pages in use and writes dirty ones back. The tests read
   your file byte by byte and count every page you read, so the layout and the I/O matter as
   much as the answers.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Data Representation | How do you put a `long` at byte 7 of a page, and read it back? |
| Memory Hierarchy and Cache Simulator (kata) | What are a hit, a miss, an eviction and a write-back? Why LRU? |
| File Systems, Storage and I/O | Why do pages have a fixed size? What does `fsync` promise, and what does it not? |
| Database Internals | Why is a B+tree shallow? What is a buffer pool for? Why are leaves linked? |
| Storage Engine (kata) | Which problems does this tree leave to a write-ahead log? |
| Systems Thinking Across the Layers | How many page reads does a lookup cost, cold and warm? |

> [!TIP]
> Before you write the tree, predict its numbers. With 255 entries per leaf and 341 children
> per internal page, how many keys fit in two levels, and in three? How many pages does a
> cold lookup read? When the tests print a count, check it against your prediction: a gap
> between the two is a bug, or a lesson.
