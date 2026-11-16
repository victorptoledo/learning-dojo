# Hello, Components

Write three components. The types of their props are already in the files.

| Component | File | Renders |
| --- | --- | --- |
| `Badge` | `Badge.tsx` | `<span className="badge badge-<tone>">label</span>`. `tone` is `"neutral"`, `"success"` or `"warning"`, and defaults to `"neutral"` |
| `UserCard` | `UserCard.tsx` | An `<article aria-label={name}>` with the name in an `<h2>`, then the parts below |
| `TeamList` | `TeamList.tsx` | A `<section aria-label={title}>` with the title in an `<h1>`, the member count and the cards |

`UserCard` shows, in this order:

1. the name in an `<h2>`;
2. a `Badge`: `"Active"` with tone `success` when `user.active`, otherwise `"Inactive"` (neutral);
3. the email as a link `<a href="mailto:...">`, **only when the user has one**;
4. the skills as a `<ul>` with one `<li>` per skill, or `<p>No skills yet</p>` when there are none.

`TeamList` shows a `<p>` with `"1 member"` or `"<n> members"`, then either
`<p>No members yet</p>` or a `<ul>` with one `<li>` per user holding a `UserCard`, sorted by
name (`localeCompare`). Give every list item a stable `key` (the user's `id`, the skill
itself) and **do not mutate the props**: the tests pass frozen arrays.

```tsx
<Badge label="Admin" />                 // <span class="badge badge-neutral">Admin</span>
<Badge label="Online" tone="success" /> // <span class="badge badge-success">Online</span>
```

## How React katas are checked

1. `tsc` type-checks every `.ts` and `.tsx` file with strict settings. A type error means the
   code "does not compile", and no test runs.
2. `tsc` compiles the files to JavaScript and the tests in `tests/` run with `node:test` in a
   jsdom document. They use Testing Library: `render(<Badge label="x" />)`, then
   `screen.getByRole(...)` or `screen.getByText(...)`, like a user reading the page.

To run them on your machine (Node.js 26 or later): `npm install` once, then `npm test`. There
are 10 tests: 1 in `tests/visible.test.tsx` and 9 hidden ones. The components that still throw
`new Error("todo ...")` are yours to write.
