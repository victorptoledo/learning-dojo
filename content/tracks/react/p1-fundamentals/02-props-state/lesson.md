# Props, State and One-Way Data Flow

Props come from the parent. **State** is the component's own memory: values that change
while the user works (the text in a field, the open tab, the items in a cart). Changing state
makes React **re-render** the component, so the screen shows the new value.

## useState

```tsx
import { useState } from "react";

export function Counter() {
  const [count, setCount] = useState(0);
  return (
    <button type="button" onClick={() => setCount(count + 1)}>
      Clicked {count} times
    </button>
  );
}
```

`useState(initial)` returns the current value and a **setter**. Calling the setter schedules
a re-render with the new value. A plain `let count = 0` would not work: it is reset on every
render, and changing it does not tell React to render again.

`useState` is a **hook**. Hooks are called at the top level of a component, never inside
conditions or loops, because React matches them to the component by their call order.

In TypeScript the type is inferred from the initial value. Give it when it cannot be:
`useState<User | null>(null)`, `useState<string[]>([])`.

## State is a snapshot

Inside one render, `count` is a constant. The setter does not change it; it asks for the
**next** render:

```tsx
function handleClick() {
  setCount(count + 1);   // next render: 0 + 1
  setCount(count + 1);   // next render: 0 + 1 again
  console.log(count);    // still 0
}
```

When the next value depends on the previous one, pass an **updater function**. React queues
updaters and runs them in order:

```tsx
setCount((c) => c + 1);
setCount((c) => c + 1);  // next render: 2
```

React also **batches** all the setter calls of one event into a single re-render.

## Update objects and arrays immutably

React decides whether state changed by comparing references (`Object.is`). Mutating an object
or array in place keeps the same reference, so React may skip the re-render, and the old
value you mutated may be shown elsewhere. Always build a **new** value:

```tsx
const [user, setUser] = useState({ name: "Ada", email: "ada@example.com" });
setUser({ ...user, email: "ada@lovelace.dev" });            // ✓ copy, then change

const [todos, setTodos] = useState<Todo[]>([]);
setTodos([...todos, newTodo]);                               // add
setTodos(todos.filter((t) => t.id !== id));                  // remove
setTodos(todos.map((t) => (t.id === id ? { ...t, done: !t.done } : t))); // change one
setTodos(todos.toSorted((a, b) => a.title.localeCompare(b.title)));     // sort a copy
```

Avoid `push`, `splice`, `sort`, `reverse` and assignments to fields of state. `toSorted`,
`toReversed`, `toSpliced` and `with` are the non-mutating versions.

## Lifting state up

When two components need the same state, move it to their **closest common parent** and pass
it down as props, together with functions to change it:

```tsx
function Accordion() {
  const [openId, setOpenId] = useState<string | null>("a");
  return (
    <>
      <Panel title="A" open={openId === "a"} onOpen={() => setOpenId("a")} />
      <Panel title="B" open={openId === "b"} onOpen={() => setOpenId("b")} />
    </>
  );
}

function Panel({ title, open, onOpen }: { title: string; open: boolean; onOpen: () => void }) {
  return (
    <section>
      <h3>{title}</h3>
      {open ? <p>Content of {title}</p> : <button type="button" onClick={onOpen}>Show</button>}
    </section>
  );
}
```

This is **one-way data flow**: data goes down as props, changes go up as callbacks. Each piece
of state has one owner, so it is easy to know who changes it.

## Keep state minimal: derive the rest

Do not store what you can compute from props or other state. A `fullName` next to
`firstName` and `lastName`, or a `count` next to `items`, will drift out of sync. Compute it
during render:

```tsx
const [items, setItems] = useState<Item[]>([]);
const total = items.reduce((sum, item) => sum + item.price, 0);  // derived, always right
```

Questions to ask about each piece of state: does it change over time? Is it passed in by a
parent (then it is a prop)? Can it be computed from other state (then it is not state)?

## State belongs to a position in the tree

Each rendered component gets its own state. Two `<Counter />` elements have two independent
counts. React keeps a component's state as long as it stays at the same position in the tree;
render something else there, and the state is gone. You can reset state on purpose by giving a
component a different `key`: `<Profile key={userId} />` starts fresh for every user.

## Key points

- `useState` gives a value and a setter; setting state re-renders the component.
- A state variable is a snapshot of one render; use updaters when the next value depends on
  the previous one.
- Never mutate state; build new objects and arrays.
- Lift shared state to the closest common parent; pass data down and callbacks up.
- Store the minimum and derive the rest during render.

## Further reading

- react.dev, "State: A Component's Memory": <https://react.dev/learn/state-a-components-memory>
- react.dev, "State as a Snapshot": <https://react.dev/learn/state-as-a-snapshot>
- react.dev, "Queueing a Series of State Updates": <https://react.dev/learn/queueing-a-series-of-state-updates>
- react.dev, "Updating Objects in State" and "Updating Arrays in State": <https://react.dev/learn/updating-objects-in-state>
- react.dev, "Choosing the State Structure": <https://react.dev/learn/choosing-the-state-structure>
- react.dev, "Sharing State Between Components": <https://react.dev/learn/sharing-state-between-components>
