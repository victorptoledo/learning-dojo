# Paged B+Tree (final exam)

Every database index you have used is this kata: a B+tree whose nodes are 4 KiB pages in a
file, reached through a **buffer pool** that keeps the hot pages in memory. It brings the track
together: bytes laid out by hand (data representation), an LRU write-back cache with hits,
misses and evictions (the memory hierarchy), page-aligned reads and writes forced to disk
(file systems), a tree whose fan-out comes from the page size (database internals), and I/O you
count and predict (systems thinking).

`PageFile` (the disk, which counts every page it reads and writes), `Page` (a page in a frame)
and `Entry` are given. Write `BufferPool` and `BPlusTree`.

## The buffer pool

`new BufferPool(file, capacity)` holds at most `capacity` pages (at least 1, or
`IllegalArgumentException`).

- `fetch(pageId)` returns the page **pinned**. A page already in the pool is a **hit** and
  reads nothing; otherwise it is a **miss**, the page is read from the file into a free frame,
  and a page that does not exist throws `IllegalArgumentException`. Two fetches of the same
  page return views of the same data.
- `allocate()` returns a new page, zeroed, **pinned and dirty**, numbered after the last page of
  the file (the first one in an empty file is page 0, then 1, ...), even before it is written.
- `unpin(page, dirty)` releases one pin and records whether the caller changed the page.
  Unpinning a page that is not pinned throws `IllegalStateException`.
- When a frame is needed and the pool is full, the **least recently fetched** page with no pins
  is evicted; if it is dirty it is written back first, and a clean page is just dropped. If
  every frame is pinned, `fetch` and `allocate` throw `IllegalStateException`.
- `flush()` writes every dirty page once, marks it clean and syncs the file.
- `pinned()` (frames with at least one pin), `hits()`, `misses()` and `file()`.

## The tree

`BPlusTree.open(pool)` creates a tree in an empty file, or opens the one in the file (a page 0
without the magic number throws `IllegalStateException`). Then `put(key, value)` (insert or
replace), `get(key)` (an `OptionalLong`), `remove(key)`, `range(from, to)` (the entries with
`from <= key < to`, in order), `size()`, `height()` (1 for a lone leaf) and `close()`, which
flushes the pool.

Rules: the tree reaches the file **only through the pool** (no `PageFile`, channels or
`java.io` in `BPlusTree`), keeps **no nodes in fields** (only the pool and primitives), leaves
**every page unpinned** when a method returns, works with a pool of **8 frames**, and has no
method longer than 25 lines (in `BufferPool` too).

## The page format

All numbers are big-endian (`ByteBuffer`'s default), at fixed offsets.

| Page | Offset | Field |
| --- | --- | --- |
| meta (page 0) | 0 | `int` magic `BPlusTree.MAGIC` |
| | 4 | `int` root page id |
| | 8 | `int` height |
| | 12 | `long` number of keys |
| leaf | 0 | `byte` 1 |
| | 1 | `short` number of entries *n* (at most `LEAF_CAPACITY` = 255) |
| | 3 | `int` id of the next leaf to the right, 0 for none |
| | 7 + 16·i | `long` key *i*, then `long` value *i*, sorted by key |
| internal | 0 | `byte` 2 |
| | 1 | `short` number of keys *n* (at most `INTERNAL_CAPACITY` = 340) |
| | 3 | `int` child 0 |
| | 7 + 12·i | `long` key *i*, then `int` child *i + 1* |

Child *i* of an internal node holds the keys below key *i*; child *i + 1* holds the keys from
key *i* up. A new tree is page 0 (meta) and page 1 (an empty root leaf).

**Splits.** A leaf that would hold 256 entries splits in two: the left keeps the first 128, a
new page gets the other 128, the new page takes over the old `next`, the left now points to
it, and the first key of the new page goes up as the separator. An internal node that would
hold 341 keys keeps the first 170, sends key 170 up, and moves the last 170 keys (with their
171 children) to a new page. When the root splits, a new root with one key is allocated and the
height grows by one. **Removing** a key only takes it out of its leaf: leaves are never merged,
so a leaf can become empty and a range scan must walk past it.

## What the grading tests check

- The pool: hits and misses, LRU order, pins, write-back of dirty pages only, `flush`,
  allocation numbers, the errors.
- The tree against a `TreeMap` over 60,000 random puts and removes, extreme keys, ranges,
  emptied leaves, and closing and reopening the file.
- The bytes in the file: the meta page, a sorted leaf, the first split, the leaf chain.
- I/O: 100,000 keys are 3 levels deep, a cold lookup reads exactly one page per level and a
  repeated one none, a range scan descends once and then follows the leaves, and a pool of
  8 frames is enough.
- The rules above, by reading your sources and by reflection.

> Tip: read a node into plain arrays (keys, values or children), change the arrays, and write
> them back. Two small functions, `read(page)` and `write(page, node)`, keep all the byte
> offsets in one place, and the tree logic never touches a `ByteBuffer`.
