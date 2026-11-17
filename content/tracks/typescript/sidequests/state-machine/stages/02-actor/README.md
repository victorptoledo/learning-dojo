A machine is a table; an **actor** is one running copy of it, with a current state and a
**context** (the data that travels with it, like the amount paid). Export from `machine.ts`:

- `createActor(machine, options)`, with these options (only `context` is required):

| Option | Type | What it does |
|---|---|---|
| `context` | any type `C` | the starting context |
| `events` | `{ [event]?: zod schema }` | the payload of an event; an event without a schema has none |
| `guards` | `{ ["state.EVENT"]?: (context, payload) => boolean }` | the transition only happens when it returns `true` |
| `updates` | `{ ["state.EVENT"]?: (context, payload) => C }` | the context after the transition |

  Keys of `events` are events of the machine, and keys of `guards` and `updates` are
  `"<state>.<event>"` for the transitions that exist (`"open.PAY"`), as a template literal
  type: anything else must not compile. Inside them, `context` is `C` and `payload` is the
  type the event's schema parses to (`z.infer`), or `undefined` without a schema.
- The actor has `state` (a `StateOf<M>`), `context` (a `C`), both read-only and current,
  and `send(event, payload)`:
  - `payload` is required when the event has a schema, of the schema's input type, and
    must not be passed when it has none; the event must be an event of the machine.
  - It returns `{ ok: true; state }` with the new state, or `{ ok: false; reason }`, with
    `reason` one of `"not-allowed"` (the current state does not accept the event),
    `"invalid-payload"` (the schema rejects it) or `"guard"` (the guard returned `false`),
    checked in that order. A failed send changes nothing.
  - Guards and updates get the payload **as the schema parsed it** (defaults filled in,
    unknown keys dropped). Without an update, the context stays the same object.
- `type SendResult<M>` is that result.
- Two actors of the same machine are independent.
