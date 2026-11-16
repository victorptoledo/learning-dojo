import { useState, type FormEvent } from "react";
import {
  addTodo,
  clearDone,
  remaining,
  toggleTodo,
  visibleTodos,
  type Filter,
  type Todo,
} from "./todos.ts";

export type TodoBoardProps = {
  initialTodos?: readonly Todo[];
  /** Makes the id of a new todo. */
  createId?: () => string;
};

const FILTERS: readonly { value: Filter; label: string }[] = [
  { value: "all", label: "All" },
  { value: "active", label: "Active" },
  { value: "done", label: "Done" },
];

/** The form, the filter buttons, the list of tasks, the counter and the Clear done button. */
export function TodoBoard({
  initialTodos = [],
  createId = () => crypto.randomUUID(),
}: TodoBoardProps) {
  const [todos, setTodos] = useState(initialTodos);
  const [filter, setFilter] = useState<Filter>("all");
  const [title, setTitle] = useState("");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTodos((current) => addTodo(current, title, createId()));
    setTitle("");
  }

  const shown = visibleTodos(todos, filter);
  const left = remaining(todos);
  const anyDone = todos.some((todo) => todo.done);

  return (
    <div>
      <form onSubmit={handleSubmit}>
        <label htmlFor="new-task">New task</label>
        <input id="new-task" value={title} onChange={(event) => setTitle(event.target.value)} />
        <button type="submit">Add</button>
      </form>
      <div>
        {FILTERS.map(({ value, label }) => (
          <button
            key={value}
            type="button"
            aria-pressed={filter === value}
            onClick={() => setFilter(value)}
          >
            {label}
          </button>
        ))}
      </div>
      {shown.length > 0 ? (
        <ul aria-label="Tasks">
          {shown.map((todo) => (
            <li key={todo.id}>
              <label>
                <input
                  type="checkbox"
                  checked={todo.done}
                  onChange={() => setTodos((current) => toggleTodo(current, todo.id))}
                />
                {todo.title}
              </label>
            </li>
          ))}
        </ul>
      ) : (
        <p>Nothing to show</p>
      )}
      <p>{left === 1 ? "1 item left" : `${left} items left`}</p>
      <button
        type="button"
        disabled={!anyDone}
        onClick={() => setTodos((current) => clearDone(current))}
      >
        Clear done
      </button>
    </div>
  );
}
