Now the board lives on a server. `BoardProvider` gets `api`, whose `save(board)` stores the
whole board and rejects when it could not.

**Optimistic.** A change shows at once, without waiting for the server, and is saved in the
background:

- After every step (stage 3: add, delete, rename, a drop that moved a card, undo, redo),
  `api.save` is called with the whole board. Moves are saved when they are dropped, not on
  every key; a cancelled move saves nothing. Nothing is saved before the first change.
- **One save at a time.** Changes made while a save is in flight wait, and are saved
  together, as the board they made, when it ends.
- The board shows `Saving…` while a save is in flight and `All changes saved` otherwise.

**Rollback.** When a save fails, the changes since the last successful save are gone on the
server, so they must go on the screen too:

- the board goes back to the last board that was saved (or `initial`), including changes
  that were waiting behind the failed save; that board is not saved again;
- the history is cleared (it held changes that were never saved);
- an `alert` says `Could not save. The board was restored to the last saved version.`, with a
  `Dismiss` button that closes it. The board keeps working, and the next change is saved as
  usual.
