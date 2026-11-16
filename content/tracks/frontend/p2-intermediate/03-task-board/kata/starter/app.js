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
  throw new Error("todo: write countLabel()");
}

/**
 * The <li> for a task: { id, title, done }. Build it with DOM methods and textContent, never
 * innerHTML: a title is text typed by a user.
 */
export function taskElement(task) {
  throw new Error("todo: write taskElement()");
}

let nextId = 1;

form.addEventListener("submit", (event) => {
  event.preventDefault();
  throw new Error("todo: add the task");
});

input.addEventListener("input", () => {
  // A custom error message must be cleared, or the field stays invalid forever.
  input.setCustomValidity("");
});

board.addEventListener("click", (event) => {
  // todo: find the button that was clicked (event.target.closest) and toggle or delete its task
});
