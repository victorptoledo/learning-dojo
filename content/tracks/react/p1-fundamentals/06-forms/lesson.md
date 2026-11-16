# Forms: Controlled and Uncontrolled Inputs, Validation and Actions

Forms are where users give your app data. React offers two ways to read inputs, and React 19
adds **form actions** for the submit step.

## Controlled inputs

A **controlled** input gets its value from state and reports every change:

```tsx
function NameForm({ onSave }: { onSave: (name: string) => void }) {
  const [name, setName] = useState("");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave(name.trim());
  }

  return (
    <form onSubmit={handleSubmit}>
      <label htmlFor="name">Name</label>
      <input id="name" value={name} onChange={(event) => setName(event.target.value)} />
      <p>{name.length}/40</p>
      <button type="submit" disabled={name.trim() === ""}>Save</button>
    </form>
  );
}
```

State is the single source of truth, so you can validate as the user types, format the
value, enable buttons, or show a live character count. The cost is a re-render on every
keystroke, which is fine for normal forms.

`value` without `onChange` makes the field read-only, and React warns. Switching between
`value={undefined}` and a string turns a field from uncontrolled to controlled; start with
`""`, not `undefined`.

Other controls follow the same pattern:

| Control | Value prop | Read in onChange |
| --- | --- | --- |
| `<input>` text, email, number | `value` | `event.target.value` (always a string) |
| `<input type="checkbox">` | `checked` | `event.target.checked` |
| `<textarea>` | `value` (not children) | `event.target.value` |
| `<select>` | `value` on the select | `event.target.value` |

## Uncontrolled inputs and FormData

An **uncontrolled** input keeps its own value in the DOM; you read it when you need it,
usually on submit, with `FormData`:

```tsx
function Search({ onSearch }: { onSearch: (q: string) => void }) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    onSearch(String(data.get("q") ?? ""));
  }
  return (
    <form onSubmit={handleSubmit}>
      <label>Search <input name="q" defaultValue="react" /></label>
      <button type="submit">Go</button>
    </form>
  );
}
```

`defaultValue` (or `defaultChecked`) sets the starting value. Uncontrolled inputs are simpler
and cheaper when you only need the values at submit time.

## Labels and accessible errors

Every input needs a **label**: `<label htmlFor="email">` with a matching `id`, or an input
wrapped in a `<label>`. Clicking the label focuses the input, and screen readers announce it.
Placeholders are not labels. Use `useId()` to make unique ids for reusable components.

Show validation errors as text tied to the field:

```tsx
const errorId = useId();
<>
  <label htmlFor="email">Email</label>
  <input
    id="email"
    type="email"
    aria-invalid={error ? true : undefined}
    aria-describedby={error ? errorId : undefined}
    value={email}
    onChange={(event) => setEmail(event.target.value)}
  />
  {error && <p id={errorId} role="alert">{error}</p>}
</>
```

`aria-invalid` marks the field, `aria-describedby` makes the screen reader read the error with
the field, and `role="alert"` announces the message when it appears. On submit with errors,
move focus to the first invalid field (a ref and `.focus()`). Browser validation attributes
(`required`, `type="email"`, `minLength`) are a useful first layer, but always validate again
on the server.

A good pattern: keep validation in a **pure function** `validate(values) → errors` that you
can test without React, and call it on submit (and, if you like, on blur).

## Form actions (React 19)

In React 19 the `action` prop of a `<form>` can be a **function**. React calls it with the
`FormData` when the form is submitted, inside a transition, and resets uncontrolled fields
afterwards:

```tsx
import { useActionState } from "react";

type State = { error: string | null };

async function changeEmail(previous: State, formData: FormData): Promise<State> {
  const email = String(formData.get("email") ?? "");
  const result = await api.updateEmail(email);
  return result.ok ? { error: null } : { error: result.message };
}

function EmailForm() {
  const [state, formAction, isPending] = useActionState(changeEmail, { error: null });
  return (
    <form action={formAction}>
      <label>Email <input name="email" type="email" required /></label>
      <button type="submit" disabled={isPending}>Save</button>
      {state.error && <p role="alert">{state.error}</p>}
    </form>
  );
}
```

`useActionState(action, initialState)` returns the latest state returned by the action, a
wrapped action to pass to the form, and `isPending`. Inside a design-system button,
`useFormStatus()` (from `react-dom`) reads the pending state of the enclosing form without
props. `useOptimistic` shows the expected result immediately while the action runs. With
Server Components, the same `action` prop can call a **server function** directly.

## Choosing

- Controlled: the UI reacts to every keystroke (live validation, dependent fields, formatting).
- Uncontrolled + FormData or actions: values matter only on submit; less code and fewer renders.
- Large forms with many rules: a library such as React Hook Form (uncontrolled under the hood)
  with a schema validator such as zod.

## Key points

- Controlled: `value` + `onChange`, state is the truth. Uncontrolled: `defaultValue` and
  `FormData` on submit.
- Always label inputs; tie errors to fields with `aria-invalid` and `aria-describedby`.
- `preventDefault` in `onSubmit` handlers; actions handle that for you.
- React 19: `<form action={fn}>`, `useActionState`, `useFormStatus`, `useOptimistic`.

## Further reading

- react.dev, `<input>` (controlled and uncontrolled): <https://react.dev/reference/react-dom/components/input>
- react.dev, `<form>` and actions: <https://react.dev/reference/react-dom/components/form>
- react.dev, `useActionState`: <https://react.dev/reference/react/useActionState>
- react.dev, `useFormStatus`: <https://react.dev/reference/react-dom/hooks/useFormStatus>
- react.dev, `useId`: <https://react.dev/reference/react/useId>
- MDN, `aria-describedby`: <https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Attributes/aria-describedby>
