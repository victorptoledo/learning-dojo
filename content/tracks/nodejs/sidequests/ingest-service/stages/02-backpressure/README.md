The sink is slower than the network. If the service keeps reading while the sink falls behind,
the batches pile up in memory until the process dies. Push back instead.

```ts
export interface Stats {
  bufferedEvents: number;  // accepted events not written yet: the open batch plus the pending batches
  pendingBatches: number;  // batches handed to the sink and not written yet (the one being written too)
  batchesWritten: number;
}
// ServiceOptions gains maxPending?: number (default 4); Service gains stats(): Stats
```

- **Backpressure.** At most `maxPending` batches may be pending. When a batch fills and that
  many are already pending, the request that filled it **waits** for a write to finish before
  reading any further. Waiting inside your `for await` loop over the request is enough: the
  request stream stops being read, its buffer fills, TCP's window closes, and the **client's**
  writes stall. The tests check exactly that: with the sink stuck, a client trying to send
  40 MB stalls after a few MB, and at most `maxPending` batches plus one open batch sit in
  memory. When the sink catches up, the upload continues and every event is kept.
- **Load shedding.** A new `POST /events` that arrives while `maxPending` batches are pending
  is answered at once with `503` and `Retry-After: 1`, without reading its body: better to say
  "come back in a second" than to make it wait in memory too. Nothing of it is stored.
- `stats()` returns the three numbers above at any moment.
