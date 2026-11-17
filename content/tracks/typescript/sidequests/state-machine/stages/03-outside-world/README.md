Events arrive as JSON from a queue or a request, and an order must survive a restart.

- `actor.receive(input: unknown)`: an event from outside, shaped `{ type, payload? }`. It
  returns a `ReceiveResult<M>`, which is a `SendResult<M>` or `{ ok: false; reason }` with
  two new reasons, checked first:
  - `"invalid-event"`: `input` is not a plain object with a string `type`;
  - `"unknown-event"`: `type` is not an event of the machine (in any state).

  Otherwise it behaves exactly like `send(type, payload)`, with the payload checked by the
  event's schema (an event without a schema ignores `payload`).
- `actor.history`: the transitions that happened, oldest first, as
  `{ from: StateOf<M>; event: EventOf<M>; to: StateOf<M> }` (`type Step<M>`); failed
  sends and receives leave no step.
- `actor.toJSON()`: `{ state, context }`, so `JSON.stringify(actor)` saves it.
- `restoreActor(machine, options, saved: unknown)`: the actor back from what `toJSON` gave,
  maybe after a trip through `JSON.parse`. `options` are those of `createActor` without
  `context` and with `contextSchema`, a zod schema for the context (the context type comes
  from it). It returns `{ ok: true; actor }` or `{ ok: false; error }`, with `error`:

| When | `error` |
|---|---|
| `saved` is not an object with a string `state` | `not a saved actor` |
| `state` is not a state of the machine | `unknown state <state>` |
| `contextSchema` rejects `context` | `invalid context` |

  The restored actor is in the saved state, with the context as the schema parsed it and an
  empty history, and works like any other (`events`, `guards` and `updates` included).
