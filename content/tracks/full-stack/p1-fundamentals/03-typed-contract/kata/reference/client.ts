// The client side of the contract: never hand the UI data the contract does not allow.
import { z } from "zod";
import { Problem, Todo, type NewTodoInput } from "./contract.ts";

/** The server answered something the contract does not allow. */
export class ContractError extends Error {
  override name = "ContractError";
}

export type CreateResult = { ok: true; todo: Todo } | { ok: false; problem: Problem };

export type FetchFn = typeof fetch;

function expect<T>(schema: z.ZodType<T>, value: unknown, what: string): T {
  const parsed = schema.safeParse(value);
  if (!parsed.success) {
    throw new ContractError(`the server sent an invalid ${what}: ${parsed.error.message}`);
  }
  return parsed.data;
}

async function json(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    throw new ContractError(`the server sent a body that is not JSON (status ${response.status})`);
  }
}

/** POST the todo as JSON; parse the answer as a Todo (2xx) or a Problem (anything else). */
export async function createTodo(
  baseUrl: string,
  input: NewTodoInput,
  fetchFn: FetchFn = fetch,
): Promise<CreateResult> {
  const response = await fetchFn(`${baseUrl}/todos`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  const body = await json(response);
  if (response.ok) {
    return { ok: true, todo: expect(Todo, body, "todo") };
  }
  return { ok: false, problem: expect(Problem, body, "problem") };
}

/** GET /todos, parsed as a list of Todos. */
export async function listTodos(baseUrl: string, fetchFn: FetchFn = fetch): Promise<Todo[]> {
  const response = await fetchFn(`${baseUrl}/todos`);
  const body = await json(response);
  if (!response.ok) {
    const problem = expect(Problem, body, "problem");
    throw new Error(`GET /todos failed: ${problem.status} ${problem.title}`);
  }
  return expect(z.array(Todo), body, "todo list");
}
