# Sidequest: Accessible kanban board

Every team has a kanban board, and most of them only work with a mouse. Build one that a
keyboard and a screen reader can use as well as a mouse can, where every component reads
and changes the same board, and where nothing the user does is lost or half-saved.

Export three components from `Board.tsx`:

```tsx
import { Board, BoardProvider, BoardSummary } from "./Board.tsx";

<BoardProvider initial={board} createId={() => crypto.randomUUID()} api={api}>
  <main><Board /></main>
  <aside><BoardSummary /></aside>
</BoardProvider>
```

`BoardProvider` holds the state; `Board` and `BoardSummary` can sit anywhere under it and
always agree. The types of the board and of the API are in `board-types.ts`: keep them as
they are, the tests build boards of those types.

The way to get there is a **reducer** (`useReducer`): one pure function that takes the board
and an action (`add`, `delete`, `move`, `undo`...) and returns the next board, without
changing the old one. It makes moving, undoing and rolling back simple, and you can test it
without React. Put it behind a **context** with a small hook (`useBoard()`) that fails with
a clear message when it is used outside the provider.

The contract tests only use the page as a user would: regions, buttons and fields found by
their names, the keyboard, and what a screen reader would announce. `npm test` compiles the
project and runs the tests in `tests/`, which are yours too: test your reducer there.
