export type Todo = {
  readonly id: string;
  readonly title: string;
  readonly done: boolean;
};

export type Filter = "all" | "active" | "done";

/** The list with a new, not done todo at the end, or the same list when the title is blank. */
export function addTodo(todos: readonly Todo[], title: string, id: string): readonly Todo[] {
  const trimmed = title.trim();
  if (trimmed === "") {
    return todos;
  }
  return [...todos, { id, title: trimmed, done: false }];
}

/** The list with the done flag of one todo flipped. */
export function toggleTodo(todos: readonly Todo[], id: string): readonly Todo[] {
  return todos.map((todo) => (todo.id === id ? { ...todo, done: !todo.done } : todo));
}

/** The list without the done todos. */
export function clearDone(todos: readonly Todo[]): readonly Todo[] {
  return todos.filter((todo) => !todo.done);
}

/** The todos the filter shows. */
export function visibleTodos(todos: readonly Todo[], filter: Filter): readonly Todo[] {
  switch (filter) {
    case "all":
      return todos;
    case "active":
      return todos.filter((todo) => !todo.done);
    case "done":
      return todos.filter((todo) => todo.done);
  }
}

/** How many todos are not done. */
export function remaining(todos: readonly Todo[]): number {
  return todos.filter((todo) => !todo.done).length;
}
