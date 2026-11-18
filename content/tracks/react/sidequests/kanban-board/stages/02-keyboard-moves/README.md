Drag and drop with a mouse leaves keyboard and screen reader users out. Give every card a
way to move that needs neither: the pattern GitHub, Trello and Atlassian use.

- Each card gets a third button, `Move <title>`, with `aria-pressed` (`false`) and
  `aria-describedby` pointing at an element (it may be hidden) that says exactly:
  `Press Space to pick up a card, the arrow keys to move it, Space to drop it and Escape to cancel.`
- Activating it (Space, Enter or a click) **picks the card up**: `aria-pressed` becomes
  `true`. Activating it again **drops** it.
- While a card is carried, the arrow keys on its `Move` button move it: Up and Down within its
  column, Left and Right to the next column, at the same position, or at the end when that
  column is shorter. At an edge nothing happens. **Escape** puts it back where it was picked
  up. Only one card is carried at a time.
- The card is shown where it is at every step, and the **focus stays on its `Move` button**,
  even when it changes column (and so is rendered again somewhere else) and after Escape.

What a sighted user sees, a screen reader user hears: the board has a `role="status"` live
region that says each step, with positions counted from 1:

| When | It says |
|---|---|
| picked up | `Picked up Write tests in To do, position 1 of 2.` |
| moved | `Write tests is in Doing, position 2 of 2.` |
| dropped | `Dropped Write tests in Doing, position 2 of 2.` |
| Escape | `Cancelled. Write tests is back in To do, position 1 of 2.` |
