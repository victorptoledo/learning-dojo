# Todo Board

## 1. Pure update functions (`todos.ts`)

Each function takes the list and returns a **new** list; none of them may change its input
(the tests pass frozen arrays).

| Function | Returns |
| --- | --- |
| `addTodo(todos, title, id)` | The list with `{ id, title: title.trim(), done: false }` at the end; **the same array** when the trimmed title is empty |
| `toggleTodo(todos, id)` | The list with the `done` of that todo flipped (a copy of that todo, the others unchanged) |
| `clearDone(todos)` | The list without the done todos |
| `visibleTodos(todos, filter)` | `"all"`: every todo; `"active"`: not done; `"done"`: done |
| `remaining(todos)` | How many todos are not done |

## 2. The board (`TodoBoard.tsx`)

`<TodoBoard initialTodos={...} createId={...} />` renders:

1. A `<form>` with an input labelled **New task** (`<label htmlFor>` or a wrapping
   `<label>`) and a submit button **Add**. Submitting adds the task with `createId()` as its
   id and empties the input. A blank title adds nothing. The page must not reload.
2. Three filter buttons **All**, **Active** and **Done** (`type="button"`). The selected one
   has `aria-pressed="true"`, the others `"false"`. The filter starts at All.
3. A `<ul aria-label="Tasks">` with one `<li>` per visible todo. Each holds a checkbox whose
   accessible name is the title (`<label><input type="checkbox" ... />{title}</label>`),
   checked when done. Clicking it toggles the todo. When no todo is visible, show
   `<p>Nothing to show</p>` instead of the list.
4. A `<p>` with `"1 item left"` or `"<n> items left"` (not done, whatever the filter).
5. A button **Clear done** that removes the done todos; it is `disabled` when none is done.

Store only what changes (the todos, the filter, the text of the input); compute the visible
list and the count during render. Use the todo's `id` as the key.

There are 12 tests: 1 in `tests/visible.test.tsx` and 11 hidden ones. Run them with
`npm install` once, then `npm test`.
