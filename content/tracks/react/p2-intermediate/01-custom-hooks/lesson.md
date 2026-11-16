# Custom Hooks and the Rules of Hooks

Hooks are functions that let components use React features: `useState`, `useEffect`,
`useRef`, `useContext` and the rest. A **custom hook** is your own function that calls other
hooks, so you can give a name to a piece of stateful logic and reuse it.

## The rules of hooks

1. **Only call hooks at the top level** of a component or of another hook: not inside
   conditions, loops, nested functions, `try` blocks or after an early `return`.
2. **Only call hooks from React functions**: components and custom hooks, not ordinary
   functions or class components.

Why? React does not know your hooks by name. It keeps the state of a component as a **list**,
and gives the first `useState` call the first slot, the second call the second slot, and so
on. If a hook is called only sometimes, every later hook gets the wrong slot:

```tsx
function Profile({ user }: { user: User | null }) {
  if (user === null) return <p>Signed out</p>;   // ✗ early return before hooks
  const [tab, setTab] = useState("posts");        //   this hook runs only sometimes
}
```

Move every hook above the early returns, or move the conditional part into a child
component. The `eslint-plugin-react-hooks` rules catch these mistakes. (`use`, which you will
meet with Suspense, is the one exception that may be called conditionally.)

## Writing a custom hook

When two components repeat the same state + effect dance, extract it:

```tsx
import { useEffect, useState } from "react";

export function useWindowWidth(): number {
  const [width, setWidth] = useState(() => window.innerWidth);
  useEffect(() => {
    function onResize() {
      setWidth(window.innerWidth);
    }
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);
  return width;
}

function Layout() {
  const width = useWindowWidth();
  return width < 600 ? <MobileNav /> : <DesktopNav />;
}
```

(`useState(() => window.innerWidth)` passes an **initialiser function**, which React calls
only on the first render; use it when the initial value is costly to compute.)

Conventions:

- The name starts with **`use`** followed by a capital letter. This tells React's tooling to
  apply the rules of hooks inside it. A function that calls no hooks should **not** start with
  `use`: make it a plain function.
- Return what callers need: a single value, a tuple like `useState` (`[value, setValue] as
  const`) when callers will rename the parts, or an object (`{ data, error, isLoading }`) when
  there are several named parts.
- Accept the values the hook reacts to as parameters, and list them in its effects'
  dependencies.

## Hooks share logic, not state

Each call of a custom hook is **independent**. Two components that call `useWindowWidth()`
each get their own state and their own listener. A custom hook is a way to reuse code, like a
function; it is not a shared store. To share state, lift it up or use context (next lessons).

Because a custom hook re-runs on every render of its component, it always receives the
latest props and state. You can pass a value from one hook into another:

```tsx
const [query, setQuery] = useState("");
const debounced = useDebouncedValue(query, 300);
const results = useSearchResults(debounced);
```

## Handlers inside effects

A hook often takes a callback (`onMessage`, `onTick`). If you list it as a dependency, the
effect re-subscribes every time the parent passes a new function. Two ways out:

- the caller wraps it in `useCallback` (performance lesson);
- the hook keeps the latest callback in a ref, updated in an effect, and the subscription
  calls `ref.current(...)`. React is adding `useEffectEvent` for exactly this pattern.

## Testing a hook

Testing Library's `renderHook` renders a tiny component that calls your hook and gives you its
latest return value:

```tsx
import { act, renderHook } from "@testing-library/react";

test("counts up", () => {
  const { result } = renderHook(() => useCounter(5));
  assert.equal(result.current.count, 5);
  act(() => result.current.increment());
  assert.equal(result.current.count, 6);
});
```

`act` makes React apply the state updates before you assert. `rerender(newProps)` renders the
hook again with new arguments, and `unmount()` runs its cleanups, which is how you check that
your hook does not leak listeners or timers.

## When not to write a hook

Do not wrap every `useEffect` in a hook. Write one when it names a real concept
(`useOnlineStatus`, `useDebouncedValue`, `useLocalStorage`), hides a subscription with its
cleanup, or removes duplicated code. Avoid "lifecycle" hooks such as `useMount(fn)`: they hide
dependencies and invite bugs.

## Key points

- Call hooks at the top level, unconditionally, from components and hooks only: React
  identifies them by call order.
- A custom hook is a `use...` function that calls hooks; it shares logic, never state.
- Choose a clear return shape: a value, a tuple or an object.
- Test hooks with `renderHook`, `act`, `rerender` and `unmount`.

## Further reading

- react.dev, "Reusing Logic with Custom Hooks": <https://react.dev/learn/reusing-logic-with-custom-hooks>
- react.dev, "Rules of Hooks": <https://react.dev/reference/rules/rules-of-hooks>
- react.dev, `useState` (initialiser functions): <https://react.dev/reference/react/useState>
- Testing Library, `renderHook`: <https://testing-library.com/docs/react-testing-library/api#renderhook>
- eslint-plugin-react-hooks: <https://www.npmjs.com/package/eslint-plugin-react-hooks>
