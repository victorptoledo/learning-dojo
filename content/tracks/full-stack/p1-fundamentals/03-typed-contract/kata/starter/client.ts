// The client side of the contract: never hand the UI data the contract does not allow.
import { z } from "zod";
import { Problem, Todo, type NewTodoInput } from "./contract.ts";

/** The server answered something the contract does not allow. */
export class ContractError extends Error {
  override name = "ContractError";
}

export type CreateResult = { ok: true; todo: Todo } | { ok: false; problem: Problem };

export type FetchFn = typeof fetch;

/** POST the todo as JSON; parse the answer as a Todo (2xx) or a Problem (anything else). */
export async function createTodo(
  baseUrl: string,
  input: NewTodoInput,
  fetchFn: FetchFn = fetch,
): Promise<CreateResult> {
  void Problem;
  throw new Error("todo: write createTodo()");
}

/** GET /todos, parsed as a list of Todos. */
export async function listTodos(baseUrl: string, fetchFn: FetchFn = fetch): Promise<Todo[]> {
  void z;
  throw new Error("todo: write listTodos()");
}
