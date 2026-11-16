# Testing with Testing Library and Accessible Components

Two topics, one idea: **build and test for the user**. Testing Library pushes you to find
elements the way users do (by role, label and text), and that only works if the component is
accessible. An accessible component is easier to test, and a good test checks accessibility
for free.

## Test behaviour, not implementation

A good component test renders the component, acts like a user and checks what the user would
see. It does not read state, call internal methods or depend on CSS classes. Then you can
refactor the component (split it, switch to a reducer, add memo) and the test still passes.

```tsx
import { render, screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";

test("adds a todo", async () => {
  const user = userEvent.setup();
  render(<TodoApp />);
  await user.type(screen.getByLabelText("New task"), "Write tests");
  await user.click(screen.getByRole("button", { name: "Add" }));
  expect(screen.getByRole("checkbox", { name: "Write tests" })).not.toBeChecked();
});
```

(The dojo's katas use `node:assert` instead of `expect`, but the queries are the same. In a
Vite project you would use Vitest, with `@testing-library/jest-dom` for matchers such as
`toBeChecked`.)

## Queries, in order of preference

1. `getByRole("button", { name: "Save" })`: what assistive technology sees. Use it almost always.
2. `getByLabelText("Email")`: form fields.
3. `getByPlaceholderText`, `getByText("Welcome")`, `getByDisplayValue`: when there is no role.
4. `getByAltText`, `getByTitle`.
5. `getByTestId`: last resort, for things users cannot perceive.

Variants: `getBy...` throws when there is no match (or more than one); `queryBy...` returns
`null`, for asserting that something is **absent**; `findBy...` returns a promise and waits,
for things that appear **asynchronously**. `getAllBy...` returns arrays. `within(element)`
scopes queries to a part of the page.

## user-event and async UI

`userEvent.setup()` then `await user.click(...)`, `user.type(...)`, `user.keyboard("{Enter}")`,
`user.tab()` simulate full interactions (focus, key down, input, key up, click) like a real
user, unlike `fireEvent`, which dispatches one event. Always `await` them.

For async results, prefer `await screen.findByRole(...)`; for other conditions,
`await waitFor(() => ...)` retries an assertion until it passes or times out. `act()` wraps
updates you trigger outside Testing Library (resolving a promise by hand, advancing timers).

## What to mock

Mock at the **edges**: the network (a fake `fetch`, a function passed as a prop, or Mock
Service Worker intercepting requests), timers, and browser APIs jsdom lacks. Do not mock your
own child components or hooks; that tests the wiring of mocks, not the app. Put providers
(context, router, query client) in a small `renderWithProviders` helper. For hooks,
`renderHook` with a `wrapper`.

jsdom is not a browser: no layout, no real CSS, no navigation. Visual and full user journeys
belong in browser tests (Playwright, Cypress, Storybook interaction tests).

## Accessibility basics

**Semantic HTML first.** A `<button>` is focusable, works with Enter and Space and is announced
as a button; a `<div onClick>` is none of that. Use `<a href>` for navigation, `<button>` for
actions, `<label>` for fields, headings in order, `<ul>` for lists, `<nav>`, `<main>`,
`<dialog>`.

**Every interactive element needs an accessible name**: its text, a `<label>`, `aria-label`,
or `aria-labelledby` pointing to visible text. Icon-only buttons need `aria-label`.

**ARIA when HTML is not enough.** Widgets like tabs, comboboxes and menus have no native
element, so you add roles and states: `role="tablist"`/`"tab"`/`"tabpanel"`, `aria-selected`,
`aria-expanded`, `aria-controls`, `aria-pressed`, `aria-current`, `aria-invalid`. The first
rule of ARIA: do not use it when a native element does the job, and never contradict the native
semantics.

**Keyboard.** Everything must work without a mouse. Tab moves between widgets; inside a
composite widget (tabs, menu, listbox), arrow keys move between items with a **roving
tabindex**: the active item has `tabIndex={0}`, the others `-1`. The WAI-ARIA Authoring
Practices describe the expected keys for each pattern.

**Focus management.** When a dialog opens, move focus into it and keep it there; when it
closes, return focus to the button that opened it. After an action that removes the focused
element, move focus somewhere sensible. Use refs and `.focus()` in effects or handlers.

**Announce changes.** Status messages go in a `role="status"` (polite) or `role="alert"`
(assertive) region.

Check with the keyboard, a screen reader (NVDA, VoiceOver), the browser's accessibility tree,
axe (as `@axe-core/react`, `jest-axe` or a browser extension) and `eslint-plugin-jsx-a11y`.

## Key points

- Test what the user sees and does; never internal state or CSS classes.
- Prefer `getByRole` with a name; `queryBy` for absence; `findBy` and `waitFor` for async.
- Use `user-event` and await it; mock only at the edges.
- Semantic HTML first, accessible names always, ARIA for custom widgets, full keyboard support,
  deliberate focus management, live regions for announcements.

## Further reading

- Testing Library, "Guiding Principles": <https://testing-library.com/docs/guiding-principles>
- Testing Library, "About Queries" (priority): <https://testing-library.com/docs/queries/about>
- Testing Library, user-event: <https://testing-library.com/docs/user-event/intro>
- react.dev, "Accessibility" (legacy docs, still accurate): <https://legacy.reactjs.org/docs/accessibility.html>
- WAI-ARIA Authoring Practices Guide, patterns: <https://www.w3.org/WAI/ARIA/apg/patterns/>
- MDN, "ARIA": <https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA>
