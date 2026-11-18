Mistakes happen, especially with the keyboard. Add undo and redo.

- A toolbar named `History` (`role="toolbar"`) with two buttons, `Undo` and `Redo`, disabled
  when there is nothing to undo or redo, and while a card is carried.
- A **step** is one change: adding, deleting or renaming a card, or a whole move from
  pick-up to drop, however many keys it took. A cancelled move, or one dropped where it
  was picked up, is no step.
- `Undo` goes back one step and `Redo` goes forward again. A new step forgets what could be
  redone. Only the last 20 steps are kept.
- The live region says what happened: `Undid adding Ship.`, `Redid moving Write tests.`,
  `Undid deleting Review PR.`, `Undid renaming Fix bug to Fix login.`
- **Shortcuts:** with the focus anywhere on the board, Ctrl+Z undoes, and Ctrl+Y or
  Ctrl+Shift+Z redo (⌘ on a Mac). In a text field they are left to the field, which has its
  own undo.

The summary follows, as always. With a reducer, undo is mostly keeping the boards you
replaced: remember that they are never changed in place.
