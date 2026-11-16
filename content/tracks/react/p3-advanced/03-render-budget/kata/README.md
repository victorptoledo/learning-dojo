# Render Budget

`ContactsPage.tsx` works: you can type a note, filter the contacts by name and select one.
But it is wasteful. Every render of a row calls `trackRender("row:<id>")`, and every call of
`sortContacts` is counted too (see `renderLog.ts` and `contacts.ts`; do not change those two
files, and keep the `trackRender` call in `ContactRow`).

**Keep the behaviour** (the behaviour tests already pass) and meet this **budget**:

| When the user... | At most |
| --- | --- |
| types in the **Note** field | 0 row renders and 0 calls of `sortContacts` |
| types in the **Filter** field | 0 calls of `sortContacts`, and 0 renders of rows that were visible before and still are |
| selects a contact | 2 row renders: the row that was selected and the one that is now |
| renders the page with a new `contacts` array | 1 call of `sortContacts` |

Each row still renders once when it first appears. Tools you may use: `memo`, `useMemo`,
`useCallback`, and moving state into smaller components. Do not use refs to hide state from
React, and do not render rows outside the list.

The tests run without StrictMode, so counts are exact. There are 9 tests: 1 in
`tests/visible.test.tsx` and 8 hidden ones (4 about behaviour, 4 about the budget). Run them
with `npm install` once, then `npm test`.
