# Sidequest: Help desk

A support team lives in its help desk all day: hundreds of tickets, filters, a ticket open in
one tab and a link to another pasted in the chat. Build `HelpDesk.tsx`, the whole app, with
React 19 and nothing else: no router, no data library. Writing the small versions yourself
is how you learn what React Router, TanStack Query and a framework do for you, and what
they cost.

```tsx
import { HelpDesk } from "./HelpDesk.tsx";

createRoot(document.getElementById("root")!).render(<HelpDesk api={api} />);
```

The API is a prop, typed in `helpdesk-types.ts` (keep those types as they are). The tests
open the app on a URL, as a browser would, pass a fake API whose answers they control, and
then use the app only as a person does: links, buttons, fields, the keyboard, the Back button
and what a screen reader would hear. Split the app into as many files and components as you
like.

Five stages, each one about a thing large apps get wrong:

1. **Routes** on the History API: real links, Back and Forward, deep links, not found pages.
2. **Accessible navigation**: the tab title, the focus and an announcement on every page.
3. **A fast list**: filters kept in the URL and a render budget, measured.
4. **Suspense**: ticket pages read with `use`, errors caught by a boundary, transitions
   instead of loading flashes.
5. **Optimistic updates**: replies and status changes that show at once and roll back.

`npm test` compiles the project and runs the tests in `tests/`, which are yours too.
