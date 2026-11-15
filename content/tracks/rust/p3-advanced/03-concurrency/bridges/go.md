---
from: go
status: draft
concept: "Multithreading and synchronisation"
from_way: "Goroutines and channels, sync.Mutex, and the race detector at test time."
native_way: "OS threads (or async tasks), channels, and races prevented at compile time."
sensei_tip: "Rust's mpsc channels feel like Go channels, but the values sent move: the sender cannot use them afterwards, which removes the share-by-accident bug."
---

| Go | Rust |
| --- | --- |
| go f() | thread::spawn(f) |
| make(chan T) | mpsc::channel::<T>() |
| sync.WaitGroup | thread::scope or join handles |
| sync.Mutex | Mutex<T> (owns the data) |
| go test -race | compile-time Send/Sync checks |
