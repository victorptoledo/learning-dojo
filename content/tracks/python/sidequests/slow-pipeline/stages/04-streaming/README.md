**`read_orders(path)`** reads a JSON Lines file. Tonight's file is bigger than the machine's
memory, so this one is about **memory**, not time.

The contract test writes 200 000 orders (about 20 MB) and walks over them with
`tracemalloc` running: the peak must stay under **5 MB**. Measure the current version the same
way (`tracemalloc.start()`, then `tracemalloc.get_traced_memory()`).

Make it a generator that keeps one line in memory at a time. The behaviour must not change:
blank lines are skipped, and a bad line raises `ValueError` starting with `line <n>:` when
the reader reaches it (the orders before it are still handed out).
