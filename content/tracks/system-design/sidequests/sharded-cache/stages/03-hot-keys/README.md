A celebrity posts and one key gets a hundred thousand reads a second, all on the one node
that owns it. Consistent hashing spreads keys, not load. Spread the hot keys, and make sure
a burst of misses on one key reaches the database only once.

**Hot keys**

- Time is cut in windows of `config.hotWindow()`, aligned on the clock (the window of instant
  `t` is `t` in nanoseconds since the epoch divided by the window's length).
- A key read at least `config.hotThreshold()` times in one window is **hot** from the read
  that reaches the threshold until the end of the next window. A hot key read that often
  again stays hot.
- When a key becomes hot, its value is copied to `config.hotReplicas()` nodes in all: the
  owner and the next distinct nodes clockwise on the ring (fewer when the cluster is
  smaller).
- Reads of a hot key go round the copies in ring order, one read each: owner, first replica,
  second replica, owner... A copy that misses is refilled from the owner (or the database
  when the owner misses too).
- `invalidate` of a hot key removes every copy.
- Counting reads must not keep every key ever read in memory: only the current and the
  previous window matter.

**Coalesced misses**

- When many threads miss on the same key at once, only one calls `origin.load`; the others
  wait for its value. A load that fails throws in every waiting caller, and the next `get`
  tries again.
- Loads of different keys never wait for each other, and nothing waits on the database while
  holding a lock that other keys need.

From now on the cluster is called from many threads.
