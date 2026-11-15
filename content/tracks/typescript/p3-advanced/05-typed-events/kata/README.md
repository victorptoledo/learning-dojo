# Typed Events

`TypedEmitter<Events>` is an event emitter whose type parameter maps each event name to its
payload:

```ts
type Events = { login: { name: string }; logout: undefined; tick: number };
const emitter = new TypedEmitter<Events>();
emitter.on("login", (user) => console.log(user.name)); // user: { name: string }
emitter.emit("tick", 1);
emitter.emit("tick", "1");   // does not compile
emitter.on("signup", ...);   // does not compile
```

The starter's methods take `name: string` and `payload: unknown`. Write the bodies **and** make
the signatures precise: a type parameter `K extends keyof Events` per method, payloads typed
`Events[K]`, and `Handlers<Events>` as a mapped type with key remapping.

| Member | Does |
| --- | --- |
| `on(name, listener)` | Adds the listener; returns a function that removes it (calling it twice is harmless) |
| `once(name, listener)` | Like `on`, but the listener is removed before its first call |
| `off(name, listener)` | Removes **one** registration of that listener (the same function may be added twice) |
| `emit(name, payload)` | Calls the event's listeners in the order they were added and returns how many it called. A listener added during an emit is not called by that emit. If listeners throw, the others still run, then `emit` throws an `AggregateError` with the errors in order |
| `listenerCount(name)` | The number of listeners of the event |
| `onAll(handlers)` | For `{ onLogin, onTick }`, listens to `login` and `tick`; returns one function that removes them all |
| `waitFor(name, { signal })` | A promise of the next payload; it stops listening once it settles, and rejects with `signal.reason` when the signal is (or becomes) aborted |

`Handlers<Events>` has one **optional** property per event, named `on` plus the capitalized
event name: `{ onLogin?: (payload: { name: string }) => void; ... }`.

Inside the class, keep the listeners in a private `Map` (`#listeners`). Storing listeners of
different payload types in one map needs one carefully placed cast; keep it in a single
private helper, so the public methods stay fully typed.

Run the tests with `npm test` after `npm install`. There are 18 tests: 1 in
`tests/visible.test.ts` and 17 hidden ones, 5 of them type-level.
