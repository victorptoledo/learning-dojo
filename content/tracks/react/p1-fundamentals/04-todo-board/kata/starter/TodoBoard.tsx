import type { ReactNode } from "react";
import type { Todo } from "./todos.ts";

export type TodoBoardProps = {
  initialTodos?: readonly Todo[];
  /** Makes the id of a new todo. */
  createId?: () => string;
};

/** The form, the filter buttons, the list of tasks, the counter and the Clear done button. */
export function TodoBoard(_props: TodoBoardProps): ReactNode {
  throw new Error("todo: write TodoBoard");
}
