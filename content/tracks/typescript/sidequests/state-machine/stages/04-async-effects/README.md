Real states do things: `loading` fetches, `paying` calls the bank. The work belongs to the
state: it starts when the actor enters it and is cancelled when the actor leaves.

- **`invoke`**, a new option of `createActor` and `restoreActor`:
  `{ [state]?: { run: (context: C, signal: AbortSignal) => Promise<unknown>; done: E; failed: E } }`,
  where `done` and `failed` must be events **of that state** (`EventsOf<M, S>`).
  - Whenever the actor **enters** a state with `invoke` (at creation, when restored, or by a
    transition, a transition to the same state included), it calls `run` with the current
    context and a fresh `AbortSignal`.
  - When the promise resolves with a value, the actor sends `done` with that value as the
    payload, exactly like `send` (so the event's schema checks it, and a rejected payload
    leaves the actor where it is). When it rejects, it sends `failed` with the error's
    message (`error.message` for an `Error`, `String(error)` otherwise) as the payload.
  - **Leaving** the state, or entering it again, aborts the signal of that run, and its
    result is ignored, even if it arrives later.
- **`actor.subscribe(listener)`**: `listener(step, snapshot)` is called after every
  transition, with the `Step<M>` and what `toJSON()` would return. It returns a function
  that unsubscribes.
- **`actor.waitFor(state, timeoutMs?)`**: a promise of the snapshot the first time the actor
  is in `state` (at once if it already is). It rejects with an `Error` whose message is
  `timed out waiting for <state>` after `timeoutMs`, or `actor stopped` when the actor stops.
- **`actor.stop()`**: aborts the running `run`, if any, and rejects the pending `waitFor`s.
  After it, `actor.stopped` is `true` (it starts `false`), every `send` and `receive` of a
  known event is `not-allowed`, `waitFor` rejects at once, and no listener is called again.
