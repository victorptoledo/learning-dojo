# Task Board: DOM, Events and a Form

`index.html` and `style.css` are done. Write **`app.js`**: a small task board with a form that
adds tasks and buttons that move or delete them. No framework: the DOM API, events and the
browser's own form validation.

| Code | What it does |
| --- | --- |
| `countLabel(n)` | `"0 tasks left"`, `"1 task left"`, `"2 tasks left"` |
| `taskElement(task)` | For `{ id, title, done }`, returns `<li class="task" data-id="ID">` holding `<span class="task-title">TITLE</span>`, a toggle button and a delete button (below). Build it with `createElement` and `textContent`, **never `innerHTML`**: a title is user input |
| The form's `submit` | Adds a task with the trimmed title to `#todo`, clears the field, keeps the focus in it and updates `#count`. The page must not reload (`preventDefault`) |
| Blank titles | Add nothing. Call `input.setCustomValidity("...")` and `input.reportValidity()` so the browser shows why; the `input` listener in the starter clears the message again |
| One click listener on `.board` | Handles every task's buttons, also for tasks added later: **event delegation**. Find the button with `event.target.closest("button[data-action]")`. Do not add listeners to the task buttons |

The two buttons of a task (the accessible name must contain the visible text, WCAG "label in name"):

| Button | Text | Accessible name (`aria-label`) | Click |
| --- | --- | --- | --- |
| `data-action="toggle"` | `Done` (in to do) / `Undo` (in done) | `Done: TITLE` / `Undo: TITLE` | Moves the task to the other list, at the end, and moves the focus to the moved task's toggle button |
| `data-action="delete"` | `Delete` | `Delete TITLE` | Removes the task and moves the focus to the "New task" field |

`#count` always shows `countLabel(<tasks in #todo>)`. It has `role="status"`, so screen
readers announce the change.

Both buttons are `type="button"`: inside a form, a button without a type submits it.

To try it in your own browser:

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```

There are 14 tests: 1 in `tests/visible.test.js` and 13 hidden ones.
