# Events, Conditional Rendering, Lists and Keys

## Event handlers

Pass a **function** to an event prop such as `onClick`, `onChange` or `onSubmit`. React calls
it with an event object when the event happens:

```tsx
function Toolbar() {
  function handleSave() {
    alert("Saved!");
  }
  return (
    <>
      <button type="button" onClick={handleSave}>Save</button>
      <button type="button" onClick={() => alert("Cancelled")}>Cancel</button>
    </>
  );
}
```

Pass the function, do not call it: `onClick={handleSave}` is right, `onClick={handleSave()}`
calls it during render. To pass arguments, wrap it: `onClick={() => remove(item.id)}`.

Event handlers are where **side effects belong**: changing state, sending a request, showing
a toast. They run because of a user action, not because of a render.

### Typing events

Inline handlers get their types from the prop. Separate handlers name the event type:

```tsx
import type { ChangeEvent, FormEvent, MouseEvent } from "react";

function handleChange(event: ChangeEvent<HTMLInputElement>) {
  console.log(event.target.value);
}
function handleSubmit(event: FormEvent<HTMLFormElement>) {
  event.preventDefault();   // stop the browser from reloading the page
}
```

### Handlers as props

A child component exposes events as callback props, named `onSomething` by convention, and
the parent decides what they do:

```tsx
function DeleteButton({ onDelete }: { onDelete: () => void }) {
  return <button type="button" onClick={onDelete}>Delete</button>;
}

<DeleteButton onDelete={() => setItems(items.filter((i) => i.id !== id))} />
```

### Propagation and default actions

Events **bubble** from the element to its ancestors: a click on a button inside a clickable
card also reaches the card's `onClick`. `event.stopPropagation()` stops it there.
`event.preventDefault()` cancels the browser's default action: following a link, submitting
a form. Buttons inside a `<form>` are submit buttons unless they say `type="button"`.

## Conditional rendering

Since JSX is expressions, use JavaScript to choose what to show:

```tsx
function Status({ user }: { user: User | null }) {
  if (user === null) {
    return <p>Please sign in.</p>;               // early return
  }
  return (
    <div>
      <p>Hello, {user.name}</p>
      {user.admin ? <AdminBadge /> : <p>Member</p>}   {/* either / or */}
      {user.unread > 0 && <p>{user.unread} new messages</p>}  {/* show or nothing */}
    </div>
  );
}
```

`null` renders nothing, so a component can return `null` to hide itself.

**The `&&` pitfall:** `count && <X />` renders `0` when `count` is `0`, because `0 && x`
evaluates to `0` and React renders numbers. Put a boolean on the left: `count > 0 && <X />`.

## Rendering lists

Turn arrays of data into arrays of elements with `map`, filter first with `filter`:

```tsx
type Todo = { id: string; title: string; done: boolean };

function TodoList({ todos }: { todos: readonly Todo[] }) {
  const open = todos.filter((todo) => !todo.done);
  return (
    <ul>
      {open.map((todo) => (
        <li key={todo.id}>{todo.title}</li>
      ))}
    </ul>
  );
}
```

### Keys

Every item of a list needs a **`key`**: a string or number that identifies it among its
siblings. React uses keys to match items between renders: it knows that the item with key
`"t7"` moved to the top, instead of thinking that every row changed. Keys also decide which
**state** belongs to which item.

Good keys are **stable** and **unique among siblings**: database ids, slugs, the value itself
for a list of unique strings. Bad keys:

- **the array index**, when the list can be reordered, filtered or have items inserted or
  removed: state and DOM (focus, input text) stay with the position, not with the item;
- `Math.random()` or `crypto.randomUUID()` called during render: a new key every render, so
  React remounts every item and loses its state.

The key goes on the outermost element returned by `map` (use `<Fragment key={...}>` when it is
a fragment). The component itself does not receive `key` as a prop.

## Key points

- Pass functions to event props; side effects belong in handlers.
- Type separate handlers with `ChangeEvent`, `FormEvent`, `MouseEvent` and the element type.
- `stopPropagation` stops bubbling; `preventDefault` stops the browser's default action.
- Conditional rendering: early return, ternary, `&&` with a real boolean, `null` to hide.
- Lists: `map` with a stable, unique key from the data, never the index for dynamic lists.

## Further reading

- react.dev, "Responding to Events": <https://react.dev/learn/responding-to-events>
- react.dev, "Conditional Rendering": <https://react.dev/learn/conditional-rendering>
- react.dev, "Rendering Lists": <https://react.dev/learn/rendering-lists>
- react.dev, "Preserving and Resetting State" (keys and state): <https://react.dev/learn/preserving-and-resetting-state>
- MDN, "Event bubbling": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Scripting/Event_bubbling>
