# Quorum Store

`QuorumStore` coordinates `N` replicas (`Replica`, given). A replica stores, per key, a
`Versioned(value, version)` and keeps only the **highest** version it has been sent; a replica
that is down throws `ReplicaDownException` on every call.

## `QuorumStore(List<Replica> replicas, int w, int r)`

- At least one replica, and `1 ≤ w ≤ N`, `1 ≤ r ≤ N`; otherwise `IllegalArgumentException`.
- `overlapping()` is `true` when `w + r > N`.

## `put(key, value)` → the version written

- The coordinator numbers writes: 1, 2, 3, ... (one counter for the whole store, also for writes
  that fail).
- Send the `Versioned` to **every** replica; count those that did not throw.
- Fewer than `w` acknowledgements: throw `QuorumNotReachedException`. The copies that did land
  **stay** (this is how real quorum stores behave: a failed write is not rolled back).

## `get(key)` → `Optional<String>`

- Ask the replicas **in list order**, skipping those that are down, until `r` of them have
  answered (an answer may be "no value"). Do not ask more replicas than needed.
- Fewer than `r` answers: throw `QuorumNotReachedException`.
- Return the value of the **highest version** among the answers (`Optional.empty()` if none had
  the key).
- **Read repair**: send that newest `Versioned` to every replica **that answered** with an older
  version or with nothing. Count each repair write in `readRepairs()`. A replica that goes down
  during repair is simply skipped.

## What the tests show

With `N = 3, W = 2, R = 2`, a write made while replica 0 is down is still read after replica 0
comes back and replica 1 goes down, because the two sets overlap in replica 2, and replica 0 is
repaired by that read. With `W = 1, R = 1` the same dance can return an old value.

> Tip: `replica.write(key, versioned)` and `replica.read(key)` inside `try { ... } catch
> (ReplicaDownException e) { ... }`. For the read, keep a list of `(replica, Optional<Versioned>)`
> answers, then pick the max version with `Comparator.comparingLong(Versioned::version)`.
