---
from: java
status: draft
concept: "I/O-bound vs CPU-bound concurrency"
from_way: "Many OS threads running truly in parallel on several cores."
native_way: "The GIL lets only one thread run Python bytecode at a time, but for I/O (network, database) `asyncio` handles thousands of connections on one cooperative event loop."
sensei_tip: "For CPU-bound work (heavy maths, image processing), use `multiprocessing` or native extensions, for example in Rust via PyO3."
---
