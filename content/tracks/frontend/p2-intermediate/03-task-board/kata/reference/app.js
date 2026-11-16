// The task board: a form adds tasks, and ONE click listener on the board handles every task's
// buttons (event delegation), including tasks added later. See README.md.

const form = document.querySelector("#new-task");
const input = document.querySelector("#task-title");
const todo = document.querySelector("#todo");
const done = document.querySelector("#done");
const count = document.querySelector("#count");
const board = document.querySelector(".board");

/** "0 tasks left", "1 task left", "2 tasks left". */
export function countLabel(n) {
  return `${n} ${n === 1 ? "task" : "tasks"} left`;
}

function toggleLabel(task) {
  return task.done ? `Undo: ${task.title}` : `Done: ${task.title}`;
}

/**
 * The <li> for a task: { id, title, done }. Build it with DOM methods and textContent, never
 * innerHTML: a title is text typed by a user.
 */
export function taskElement(task) {
  const item = document.createElement("li");
  item.className = "task";
  item.dataset.id = String(task.id);

  const title = document.createElement("span");
  title.className = "task-title";
  title.textContent = task.title;

  const toggle = document.createElement("button");
  toggle.type = "button";
  toggle.dataset.action = "toggle";
  toggle.textContent = task.done ? "Undo" : "Done";
  toggle.setAttribute("aria-label", toggleLabel(task));

  const remove = document.createElement("button");
  remove.type = "button";
  remove.dataset.action = "delete";
  remove.textContent = "Delete";
  remove.setAttribute("aria-label", `Delete ${task.title}`);

  item.append(title, toggle, remove);
  return item;
}

function updateCount() {
  count.textContent = countLabel(todo.children.length);
}

let nextId = 1;

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const title = input.value.trim();
  if (title === "") {
    input.setCustomValidity("Write a task first.");
    input.reportValidity();
    return;
  }
  todo.append(taskElement({ id: nextId++, title, done: false }));
  form.reset();
  input.focus();
  updateCount();
});

input.addEventListener("input", () => {
  // A custom error message must be cleared, or the field stays invalid forever.
  input.setCustomValidity("");
});

board.addEventListener("click", (event) => {
  const button = event.target.closest("button[data-action]");
  const item = button?.closest("li.task");
  if (!item || !board.contains(item)) {
    return;
  }
  const title = item.querySelector(".task-title").textContent;
  if (button.dataset.action === "delete") {
    item.remove();
    input.focus();
  } else if (button.dataset.action === "toggle") {
    const isDone = item.parentElement === done;
    const task = { id: Number(item.dataset.id), title, done: !isDone };
    const replacement = taskElement(task);
    (task.done ? done : todo).append(replacement);
    item.remove();
    replacement.querySelector('[data-action="toggle"]').focus();
  }
  updateCount();
});
