# Checkpoint 1: Sign-up Form

## 1. The validator (`signup.ts`)

`validateSignup(values)` is a pure function. It returns an object with a message for each
invalid field, and `{}` when everything is valid:

| Field | Rule | Message |
| --- | --- | --- |
| `name` | not blank after trimming | `Enter your name` |
| `email` | trimmed, looks like `a@b.c` (no spaces, one `@`, a dot after it) | `Enter a valid email` |
| `password` | at least 8 characters and at least one digit | `Use at least 8 characters with a digit` |
| `confirm` | equal to `password` | `Passwords do not match` |
| `terms` | `true` | `Accept the terms` |

## 2. The form (`SignupForm.tsx`)

`<SignupForm onSubmit={...} />` renders a `<form>` with these controlled fields, each with a
visible label:

| Label | Field |
| --- | --- |
| `Name` | text input |
| `Email` | `type="email"` input |
| `Password` | `type="password"` input |
| `Confirm password` | `type="password"` input |
| `I accept the terms` | checkbox |

and a submit button **Create account**. Add `noValidate` to the form: your validator decides,
not the browser.

**On submit** (the page must not reload):

- When `validateSignup` finds errors, show each message in an element with an `id`, mark the
  field with `aria-invalid="true"` and point `aria-describedby` at the message. Move focus to
  the **first** invalid field, in the order of the table. Do not call `onSubmit`.
- After a failed submit, the errors follow the values: fixing a field removes its message as
  the user types. Before the first submit, no error is shown.
- When the values are valid, call `onSubmit({ name, email, password })` with the name and
  email trimmed. While the returned promise is pending, the button is `disabled` and reads
  **Creating account…** (with the `…` character).
- When it resolves, replace the form with `<p role="status">Welcome, <name>!</p>`.
- When it rejects with an `Error`, show its message in `<p role="alert">` and enable the
  button again, keeping the values.

There are 11 tests: 1 in `tests/visible.test.tsx` and 10 hidden ones. Run them with
`npm install` once, then `npm test`.
