The transition table and what the compiler can prove from it. Export from `machine.ts`:

- `defineMachine(config)`, where `config` is `{ initial, states }`: `states` maps each state
  name to `{ on?: { EVENT: "target state" } }`. It returns `{ initial, states }` as given,
  typed with the literal names (a `const` type parameter), so nobody writes `as const`. A
  target or an `initial` that is not a state name must not compile.
- The types, for `M` the type of a machine (`typeof order`):

| Type | Is | For the order above |
|---|---|---|
| `StateOf<M>` | every state name | `"open" \| "paid" \| "shipped" \| "cancelled"` |
| `EventsOf<M, S>` | the events state `S` accepts | `EventsOf<M, "open">` is `"PAY" \| "CANCEL"`, `never` for `shipped` |
| `EventOf<M>` | every event of the machine | `"PAY" \| "CANCEL" \| "SHIP"` |
| `Next<M, S, E>` | where `E` takes `S` | `Next<M, "open", "PAY">` is `"paid"` |

- `transition(machine, state, event)`: the next state. Its type is `Next<...>` of the
  arguments, and an event that `state` does not accept must not compile. At run time (with
  data the compiler did not see), an unknown state or event throws a `TransitionError`.
- `class TransitionError extends Error` with `state: string` and `event: string`, `name`
  `"TransitionError"` and message `cannot <event> in <state>`.
- `can(machine, state: string, event: string): boolean`: whether that transition exists,
  for strings that come from anywhere (it never throws).
- `finalStates(machine)`: the states that accept no event, in the order of `states`.
- `reachable(machine)`: the states you can get to from `initial`, `initial` first, in
  breadth-first order, following each state's events in the order they are written.

Only own properties count: `can(order, "open", "toString")` is `false`.
