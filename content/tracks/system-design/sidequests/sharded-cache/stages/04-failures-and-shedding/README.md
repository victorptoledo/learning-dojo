The cache exists to protect the database. When a cache node dies, or the database is already
struggling, the client must fail in a way that keeps the rest of the system alive.

**Failing nodes**

- A node call that throws `NodeDownException` never fails the `get`. When the owner fails, the
  value comes from the database (and is not stored anywhere for that read); a hot key copy
  that fails is treated like a copy that misses.
- `config.failureThreshold()` failures in a row eject the node for `config.ejectFor()`. An
  ejected node gets no calls: its keys are served by the next node clockwise that is not
  ejected, and `ownerOf` names that node. A success resets the count of failures.
- Once `ejectFor` has passed, the first request routed to the node is a **probe**: if it
  works, the node is back; if it fails, the node is ejected for another `ejectFor` at once.
- Hot key copies on an ejected node are skipped in the rotation.
- A migration step whose old node fails skips that node: its keys are lost (they will be
  cold), and the migration still finishes. During a migration, a previous owner that fails is
  treated like one that misses.

**Protecting the database**

- At most `config.originConcurrency()` loads may be running at once (a coalesced load counts
  once). A miss that would start one more throws `OverloadedException` at once, without
  calling the database: a fast failure the caller can handle, instead of a queue of threads
  piling up behind a database that is already slow.
- `OverloadedException` (new in the starter) is a `RuntimeException`. Callers that join a
  coalesced load already running are not refused.
