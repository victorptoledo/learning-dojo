# React Reconciliation: Render, Commit and Fiber

You have written components that return JSX and seen the screen update. This lesson opens the
box: what React does between "state changed" and "the DOM changed", and why the rules about
purity, keys and positions follow from it.

## Trigger, render, commit

1. **Trigger.** A render starts on the first `root.render(...)`, and afterwards whenever a
   component's state changes (a setter, a dispatch), its context value changes, or its parent
   re-renders.
2. **Render.** React calls your components to get their new elements, from the component that
   changed down through its children, and **compares** the new tree with the previous one. This
   phase is pure computation: no DOM changes yet, which is why it can run twice, be paused or be
   thrown away.
3. **Commit.** React applies the **minimal** set of DOM changes the comparison found (insert,
   update attributes and text, remove), then runs layout effects, lets the browser paint, and
   runs effects.

"Re-rendered" therefore means "the function was called", not "the DOM was rebuilt". A parent's
render calls all its children by default, but if a child returns the same output, nothing in
its DOM changes. Rendering a lot is usually cheap; when it is not, the next lesson shows how to
skip it.

Updates are **batched**: every setter call during one event (or one promise callback,
timeout, ...) produces a single render.

## How the diff works

Comparing two arbitrary trees exactly is expensive, so React uses two heuristics that make it
linear:

1. **Different element types produce different trees.** If a `<div>` becomes a `<section>`,
   or `<Profile>` becomes `<Settings>`, React unmounts the old subtree (with all its state and
   DOM) and mounts a new one.
2. **Keys identify children in a list.** Among siblings, React matches old and new children by
   `key` (and by position when there are no keys).

For the same type at the same position, React keeps the component (and its state) and only
updates the props; for DOM elements it updates only the attributes that changed.

## State lives at a position

State is not stored "in" the component function; React stores it in the tree, at the
component's **position**. The consequences:

```tsx
{isFancy ? <Counter fancy /> : <Counter />}     // same type, same position: state kept
{isAdmin ? <Form key="admin" /> : <Form key="user" />}   // different keys: state reset
{showBanner && <Banner />}
<Editor />                                      // keeps its position: `false` holds the slot
```

- Switching between two `<Counter>` elements at the same place keeps the count: for React it
  is one component whose props changed.
- A different `key` tells React it is a different thing; the state is reset. This is the clean
  way to reset a form when the edited record changes.
- Wrapping a component in a different parent type (`<div>` vs `<section>`) moves it to a
  different tree, so its state is lost.
- This is also why a component **defined inside** another component loses its state on every
  render: it is a new type each time.

## Fiber

Since React 16 the tree React keeps is made of **fibers**: one object per component or element,
holding its type, props, state (the list of hooks), effects, and links to its parent, child and
sibling. A fiber is a **unit of work**.

Because the work is split into units, React can **pause** rendering after any fiber, check
whether something more urgent arrived (a keystroke), and continue later, or throw the
unfinished work away. During rendering React builds a work-in-progress tree next to the current
one, and the commit swaps them in one go, so the user never sees a half-updated screen.

## Concurrent rendering

With `createRoot` (React 18+), rendering is **concurrent**: updates have priorities.

- Urgent updates (typing, clicking) render synchronously-ish and first.
- **Transition** updates (`startTransition`, `useTransition`) are interruptible: if the user
  types while a heavy list renders, React drops the stale work and starts again with the new
  state.
- `useDeferredValue` renders a part of the UI with an older value until React has time for the
  new one.
- Suspense can show a fallback for a part of the tree that is waiting for data while the rest
  stays interactive.

All of this relies on render being pure: React may call your component, discard the result and
call it again.

## Key points

- Trigger → render (call components, diff) → commit (minimal DOM changes, then effects).
- Same type at the same position keeps state; a different type or key resets it.
- A parent's render re-renders its children, but the DOM only changes where output differs.
- Fiber splits rendering into units of work so React can pause, prioritise and discard it;
  that enables transitions, deferred values and Suspense.

## Further reading

- react.dev, "Render and Commit": <https://react.dev/learn/render-and-commit>
- react.dev, "Preserving and Resetting State": <https://react.dev/learn/preserving-and-resetting-state>
- legacy.reactjs.org, "Reconciliation" (the diffing heuristics): <https://legacy.reactjs.org/docs/reconciliation.html>
- react.dev, "React 18" release post (concurrent rendering): <https://react.dev/blog/2022/03/29/react-v18>
- Andrew Clark, "React Fiber Architecture": <https://github.com/acdlite/react-fiber-architecture>
