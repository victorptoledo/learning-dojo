# useEffect, Cleanup and useRef: the Synchronisation Mindset

Rendering must be pure, and event handlers handle what the user does. But some code must run
because a component **is on the screen**: connect to a chat room while the room is shown,
subscribe to the window size, start a timer, sync a third-party widget. That is what
**effects** are for: they **synchronise** a component with a system outside React.

## useEffect

```tsx
import { useEffect, useState } from "react";

function ChatRoom({ roomId }: { roomId: string }) {
  const [messages, setMessages] = useState<string[]>([]);

  useEffect(() => {
    const connection = createConnection(roomId);       // start synchronising
    connection.onMessage((m) => setMessages((current) => [...current, m]));
    connection.connect();
    return () => connection.disconnect();              // stop synchronising
  }, [roomId]);                                        // re-sync when roomId changes

  return <ul>{messages.map((m, i) => <li key={i}>{m}</li>)}</ul>;
}
```

React runs the effect **after** the render has been committed to the screen. Read it as
"while this component shows room `roomId`, stay connected to it", not as "on mount".

## Dependencies

The second argument lists every reactive value the effect reads (props, state, and values
computed from them):

| Dependencies | The effect runs |
| --- | --- |
| `[roomId]` | after the first render and whenever `roomId` changed |
| `[]` | after the first render only |
| none | after **every** render |

React compares each dependency with `Object.is`. Do not lie about dependencies to make an
effect run less: if the effect reads `roomId`, list it, or it will keep using a stale value.
The `react-hooks/exhaustive-deps` lint rule checks this for you.

Objects and functions created during render are new on every render, so as dependencies
they make the effect run every time. Move them inside the effect, or out of the component.

### The infinite loop

```tsx
useEffect(() => {
  setCount(count + 1);    // ✗ sets state → re-render → effect runs again → ...
});
```

An effect that sets state on every render loops forever. If you set state in an effect, the
dependencies must stop the cycle, and usually the state should not be there at all (below).

## Cleanup

The function an effect returns is its **cleanup**. React calls it before running the effect
again with new dependencies, and when the component unmounts. Every effect that subscribes,
connects, starts a timer or adds a listener needs one:

```tsx
useEffect(() => {
  function onResize() {
    setWidth(window.innerWidth);
  }
  window.addEventListener("resize", onResize);
  return () => window.removeEventListener("resize", onResize);
}, []);

useEffect(() => {
  const id = setInterval(() => setSeconds((s) => s + 1), 1000);
  return () => clearInterval(id);
}, []);
```

In development, **StrictMode mounts every component twice** (mount, cleanup, mount). An effect
with a correct cleanup behaves the same; one without it shows the bug right away (two
connections, two intervals). Do not "fix" it with a ref that skips the second run; write the
cleanup.

## You might not need an effect

Effects are an escape hatch. Many effects in real code should not exist:

- **Derived values.** `useEffect(() => setFullName(first + " " + last), [first, last])` is
  an extra render and a chance for bugs. Compute `const fullName = first + " " + last` in
  render.
- **Reacting to user events.** "When the user clicks Buy, send a request" belongs in the
  click handler, not in an effect that watches a `bought` flag.
- **Resetting state when a prop changes.** Use a `key` (`<Profile key={userId} />`) instead of
  an effect that clears state.
- **Expensive calculations.** Compute in render, and memoise with `useMemo` if it is slow.

Keep effects for real synchronisation: network connections, subscriptions, timers, DOM APIs
that React does not manage, and (with care, see the data fetching lesson) fetching.

## useRef

A **ref** is a box `{ current: value }` that keeps its value between renders **without**
causing a render when it changes. Two common uses:

**1. A DOM node.** Pass the ref to the `ref` attribute; after the commit, `ref.current` is the
element:

```tsx
import { useRef } from "react";

function SearchBar() {
  const inputRef = useRef<HTMLInputElement>(null);
  return (
    <>
      <input ref={inputRef} />
      <button type="button" onClick={() => inputRef.current?.focus()}>Search</button>
    </>
  );
}
```

**2. A value that is not shown.** A timer id, the previous value, a flag for "a request is in
flight":

```tsx
const intervalRef = useRef<number | null>(null);
function start() {
  intervalRef.current = window.setInterval(tick, 1000);
}
function stop() {
  if (intervalRef.current !== null) clearInterval(intervalRef.current);
}
```

Rules: do not read or write `ref.current` **during render** (it breaks purity; use state for
what is displayed); read and write it in event handlers and effects. In React 19 a component
can receive `ref` as a normal prop, so you can pass a ref to your own `<TextInput ref={...} />`
without `forwardRef`.

## The lifecycle, seen through effects

A component **mounts** (first appears), **updates** (renders again with new props or state)
and **unmounts** (disappears). An effect does not follow that lifecycle; it starts and stops
synchronising, possibly many times while the component is mounted. Think about each effect on
its own: what does it start, what does it stop, which values make it restart?

## Key points

- Effects synchronise with systems outside React and run after the commit.
- List every reactive value the effect reads as a dependency; never silence the linter.
- Return a cleanup for anything you start; StrictMode's double mount checks that you did.
- Derived values, user-event logic and state resets do not need effects.
- `useRef` holds DOM nodes and mutable values that must not trigger renders; do not use it
  during render.

## Further reading

- react.dev, "Synchronizing with Effects": <https://react.dev/learn/synchronizing-with-effects>
- react.dev, "You Might Not Need an Effect": <https://react.dev/learn/you-might-not-need-an-effect>
- react.dev, "Lifecycle of Reactive Effects": <https://react.dev/learn/lifecycle-of-reactive-effects>
- react.dev, "Referencing Values with Refs": <https://react.dev/learn/referencing-values-with-refs>
- react.dev, "Manipulating the DOM with Refs": <https://react.dev/learn/manipulating-the-dom-with-refs>
- react.dev, `useEffect` reference: <https://react.dev/reference/react/useEffect>
