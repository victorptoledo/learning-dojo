The board, its cards and a shared state.

**`BoardProvider`** takes `initial` (the board to start from, which must never be changed in
place: the tests freeze it), `createId` (makes the id of a new card), `api` (stage 4) and
`children`. **`Board`** and **`BoardSummary`** read the board from it; used outside a
provider, they throw an error whose message mentions `BoardProvider`.

**`Board`** shows one region per column, in order: a `<section>` named by its `<h2>` title
(`aria-labelledby`). In each column:

- the cards in order, each one in a list item with its title and two buttons,
  `Edit <title>` and `Delete <title>`; a column without cards says `No cards`;
- a form named `Add a card to <column>` with a text field labelled `New card in <column>`
  and an `Add card` button. A title (trimmed) goes at the end of the column; the field is
  emptied and keeps the focus. A blank title adds nothing.

**Editing.** `Edit <title>` replaces the card's title with a field labelled
`Title of <title>`, holding the title and focused, and two buttons, `Save` and `Cancel`.
Enter or `Save` keeps the new title (trimmed; a blank one keeps the old title); Escape or
`Cancel` keeps the old one. Either way the focus goes back to the card's `Edit` button.

**Deleting.** `Delete <title>` removes the card and moves the focus to the `New card in
<column>` field of its column, so the focus is never lost on a button that is gone.

**`BoardSummary`** is a region named `Summary` with a list, one item per column:
`<column>: <cards>` (`To do: 2`), followed by `<n> cards in total` (`1 card in total`).
Rendered anywhere under the provider, it follows every change made on the board.
