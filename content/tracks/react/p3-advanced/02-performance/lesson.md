# Performance: memo, useMemo, useCallback and the React Compiler

Most React apps are fast enough without any tricks. When one is not, the cause is usually one
of a few things: a component re-renders too often, a render does too much work, or the page
ships or mounts too much. Measure, then fix the cause.

## Measure first

Open the React DevTools **Profiler**, press record, do the slow interaction, stop. You see each
commit, which components rendered, how long each took, and (with "record why each component
rendered" enabled) **why**: props changed, state changed, hooks changed, parent rendered. In
code, `<Profiler id onRender>` reports the same numbers. Always profile a production build for
real numbers; development builds are slower.

## Why components re-render

A component renders when its state changes, its context changes, or **its parent renders**.
The last one is the usual source of waste: typing in a field at the top of a page re-renders
everything under the component that owns that field's state.

## Structural fixes first

Two changes fix most problems without any memoisation:

**Colocate state.** Move state down into the component that uses it. If only the search box
needs `query`, keep `query` in the search box, not in the page, and the rest of the page stops
re-rendering on each keystroke.

**Lift content up as children.** A component that changes often can receive the expensive
part as `children`; children created by the parent are the same elements on every render of the
wrapper, so React skips them:

```tsx
function ScrollTracker({ children }: { children: ReactNode }) {
  const [y, setY] = useState(0);         // changes often
  // ... subscribe to scroll, setY
  return <div data-y={y}>{children}</div>;  // children are not re-rendered by setY
}

<ScrollTracker><ExpensiveArticle /></ScrollTracker>
```

## memo

`memo(Component)` returns a component that **skips rendering** when its props are shallowly
equal (`Object.is` on each prop) to the previous ones:

```tsx
import { memo } from "react";

const Row = memo(function Row({ item, selected, onSelect }: RowProps) {
  return (
    <li aria-selected={selected} onClick={() => onSelect(item.id)}>
      {item.name}
    </li>
  );
});
```

memo only helps if the props really are equal. Objects, arrays and functions created during
the parent's render are **new every time**, so `onSelect={(id) => setSelected(id)}` or
`style={{ color: "red" }}` defeat memo.

## useCallback and useMemo

They keep a value **stable** between renders until its dependencies change:

```tsx
const handleSelect = useCallback((id: string) => setSelectedId(id), []);
const sorted = useMemo(() => rows.toSorted(byName), [rows]);
const columns = useMemo(() => [{ key: "name" }, { key: "price" }], []);
```

- `useCallback(fn, deps)` returns the same function while deps are unchanged: use it for
  callbacks passed to memoised children or used in effects' dependencies.
- `useMemo(() => value, deps)` caches the result of a computation: use it for **expensive**
  calculations (sorting or filtering thousands of items) and for objects passed to memoised
  children or context values.

Updater functions help keep callbacks stable: `setSelectedId(id)` needs no dependency, and
`setItems((items) => ...)` avoids depending on `items`.

Memoisation is not free: it costs memory and comparisons, and wrong dependencies give stale
bugs. Do not wrap everything by reflex; wrap what the Profiler shows to be slow, plus what a
memoised child or an effect needs to be stable.

## The React Compiler

The **React Compiler** (a Babel/Vite plugin, stable since 2025) analyses your components and
inserts the memoisation automatically, at a finer grain than you would by hand. With it,
most manual `memo`, `useMemo` and `useCallback` become unnecessary. It relies on your code
following the rules of React (pure renders, no mutation of props and state, rules of hooks);
code that breaks them is skipped. Existing manual memoisation keeps working.

## Other tools

- **Virtualise long lists.** Rendering 10,000 rows is slow however well memoised; libraries such
  as TanStack Virtual render only the visible rows.
- **Transitions.** Mark a heavy update as non-urgent with `startTransition` (or
  `useDeferredValue` for a value) so typing stays responsive while the list catches up.
- **Code splitting.** `lazy(() => import("./Editor.tsx"))` with `<Suspense>` loads a heavy
  component only when it is shown. Routes are the natural split points.
- **Avoid layout thrashing and huge DOMs**, which are browser costs, not React ones.

## Key points

- Profile before optimising; the Profiler says what rendered and why.
- First move state down and pass expensive parts as children.
- `memo` skips renders when props are shallowly equal; stabilise functions with `useCallback`
  and objects or expensive results with `useMemo`.
- The React Compiler automates memoisation for code that follows the rules of React.
- Virtualise long lists, use transitions for heavy updates, split code by route.

## Further reading

- react.dev, `memo`: <https://react.dev/reference/react/memo>
- react.dev, `useMemo`: <https://react.dev/reference/react/useMemo>
- react.dev, `useCallback`: <https://react.dev/reference/react/useCallback>
- react.dev, `<Profiler>`: <https://react.dev/reference/react/Profiler>
- react.dev, "React Compiler": <https://react.dev/learn/react-compiler>
- react.dev, `lazy`: <https://react.dev/reference/react/lazy>
