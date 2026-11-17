# Sidequest: LRU cache with metrics

Build `lru_metrics`, a library crate with one type, `Cache<K, V>`: a fixed-size cache that
throws out the **least recently used** entry when it is full, counts how well it works, and is
safe to share between threads. Then ship it like a service would be: a CI workflow and an
alert on a falling hit ratio.

The contract tests use only the public API that each stage describes. How you store entries
is yours. Two things worth aiming for, which the AI review (`[A]`) can discuss with you:
`get` and `put` in O(1) (a `HashMap` plus an order that is cheap to update), and a lock held
for as short a time as possible.

- Keep the package name `lru_metrics`; use the standard library only (the checks run offline).
- Every method takes `&self`, never `&mut self`: from stage 3 on, one cache is shared by many
  threads through an `Arc`. Use interior mutability from the start (a `Mutex` around the
  state is fine).
