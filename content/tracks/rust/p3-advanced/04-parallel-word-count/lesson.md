# Parallel Word Count

Counting words is easy to split: each thread counts a slice of the texts, then the partial maps
are merged. `std::thread::scope` lets those threads **borrow** the texts without `Arc` or
`'static`, because the scope guarantees they finish before the data is dropped.
