# Checkpoint 3: The Issue Tracker

A small issue tracker whose issues live in an **external store**, not in React state. The page
must stay correct when the store changes from outside, keep the filter in the address bar, and
meet a **render budget**. Every render of a row calls `trackRender("row:<id>")` (see
`renderLog.ts`; do not change that file).

## 1. The issues (`issues.ts`)

Pure functions that never mutate their input:

- `toggleIssue(state, id)` flips `done` of that issue;
- `renameIssue(state, id, title)` sets the **trimmed** title;
- `matchesQuery(issue, query)` is true when the title contains the trimmed query, ignoring case
  (an empty query matches everything);
- `openCount(issues)` counts the issues that are not done.

When `toggleIssue` or `renameIssue` change nothing (no such issue, an empty title, the same
title), they return **the same state object**. When they do change something, every issue that
did not change is **the same object** as before: the rows rely on it.

## 2. The store (`store.ts`)

`createStore(initial)` returns `{ getState, setState, subscribe }`:

- `setState(next)` takes a new state or an updater `(current) => next`, and calls every
  listener once after a change. Setting the same object (`Object.is`) calls nobody.
- `subscribe(listener)` returns a function that removes that listener.

`useStore(store, selector)` returns `selector(store.getState())` and re-renders the component
when that value changes. Use `useSyncExternalStore`.

## 3. The page (`IssueTracker.tsx`)

`<IssueTracker store={store} />` renders:

- a text field labelled **Filter**. It starts from `?q=` in the address bar, and every change
  writes the trimmed query back to `q` (no `q` when it is empty) **without adding history
  entries**;
- `<p role="status">` with `<open> of <total> open`, counting every issue, not only the
  visible ones;
- `<ul aria-label="Issues">` with one row per visible issue, in the store's order: a checkbox
  labelled `Done: <title>` that toggles the issue in the store, and a button with the title
  that selects it (`aria-pressed` tells which one is selected). When no issue matches:
  `<p>No issues match</p>`;
- below, `<p>Select an issue to edit it</p>`, or for the selected issue a form labelled
  **Edit issue** with a **Title** field that starts from the issue's title and a **Save**
  button that renames the issue in the store. An empty title is not saved: the field gets
  `aria-invalid="true"` and `aria-describedby` pointing to `<p role="alert">Title cannot be
  empty</p>`, and keeps the focus. An unsaved draft belongs to its issue: selecting another
  issue starts from that issue's title.

Rows are keyed so that each one keeps its own DOM when the store reorders the issues: the
focus stays on the checkbox of the same issue.

## The render budget

| When... | Row renders, at most |
| --- | --- |
| the page first renders | 1 per row |
| an issue is ticked or renamed (from the page or from outside) | 1, that issue's row |
| the user types in **Filter** | 0 for rows that stay visible |
| the user selects another issue | 2: the row that was selected and the one that is now |
| the user types in the **Title** field | 0 |

Do not use refs to hide state from React, and do not render rows outside the list. The tests
run without StrictMode, so counts are exact.

There are 15 tests: 1 in `tests/visible.test.tsx` and 14 hidden ones. Run them with
`npm install` once, then `npm test`.
