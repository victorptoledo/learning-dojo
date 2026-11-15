---
from: python
status: draft
concept: "Multithreading and synchronisation"
from_way: "threading limited by the GIL, multiprocessing for CPU work, queue.Queue between threads."
native_way: "Real parallel threads with data races ruled out at compile time by Send and Sync."
sensei_tip: "Python threads take turns on the GIL; Rust threads run on all cores. The price is proving to the compiler that shared data is synchronised."
---

| Python | Rust |
| --- | --- |
| threading.Thread(target=f).start() | thread::spawn(f) |
| thread.join() | handle.join() |
| queue.Queue | mpsc::channel() |
| threading.Lock | Mutex<T> |
| concurrent.futures pool | rayon or thread::scope |
