export type Todo = {
  readonly id: string;
  readonly title: string;
  readonly done: boolean;
};

export type Filter = "all" | "active" | "done";

/** The list with a new, not done todo at the end, or the same list when the title is blank. */
export function addTodo(_todos: readonly Todo[], _title: string, _id: string): readonly Todo[] {
  throw new Error("todo: write addTodo");
}

/** The list with the done flag of one todo flipped. */
export function toggleTodo(_todos: readonly Todo[], _id: string): readonly Todo[] {
  throw new Error("todo: write toggleTodo");
}

/** The list without the done todos. */
export function clearDone(_todos: readonly Todo[]): readonly Todo[] {
  throw new Error("todo: write clearDone");
}

/** The todos the filter shows. */
export function visibleTodos(_todos: readonly Todo[], _filter: Filter): readonly Todo[] {
  throw new Error("todo: write visibleTodos");
}

/** How many todos are not done. */
export function remaining(_todos: readonly Todo[]): number {
  throw new Error("todo: write remaining");
}
