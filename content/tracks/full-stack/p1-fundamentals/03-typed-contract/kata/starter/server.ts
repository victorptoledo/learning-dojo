// The server side of the contract: validate what comes in, answer with Todos or Problems.
import { createServer, type IncomingMessage, type Server } from "node:http";
import { NewTodo, toFieldErrors, type Problem, type Todo } from "./contract.ts";

export interface ApiRequest {
  method: string;
  path: string;
  contentType: string | undefined;
  /** The raw body text ("" when there is none). */
  body: string;
}

export interface ApiResponse {
  status: number;
  headers: Record<string, string>;
  /** Sent as JSON. */
  body: unknown;
}

/** Where the todos live; ids start at 1. */
export class TodoStore {
  readonly todos: Todo[] = [];

  add(title: string, due: string | null): Todo {
    const todo: Todo = { id: this.todos.length + 1, title, due, done: false };
    this.todos.push(todo);
    return todo;
  }
}

/** A problem-details answer: Content-Type application/problem+json. */
export function problem(status: number, type: string, title: string, extra: Partial<Problem> = {}): ApiResponse {
  return {
    status,
    headers: { "Content-Type": "application/problem+json" },
    body: { type, title, status, ...extra },
  };
}

/** The routes of README.md. */
export function handleTodos(request: ApiRequest, store: TodoStore): ApiResponse {
  // todo: GET /todos, POST /todos (415, 400, 422, 201) and 404 for anything else.
  // NewTodo.safeParse and toFieldErrors are imported for you.
  void NewTodo;
  void toFieldErrors;
  void store;
  return problem(501, "/problems/todo", `todo: ${request.method} ${request.path} is not written yet`);
}

async function readBody(request: IncomingMessage): Promise<string> {
  const chunks: Buffer[] = [];
  for await (const chunk of request) {
    chunks.push(chunk as Buffer);
  }
  return Buffer.concat(chunks).toString("utf8");
}

/** A node:http server around handleTodos (not listening yet). */
export function serve(store: TodoStore = new TodoStore()): Server {
  return createServer(async (request, response) => {
    const answer = handleTodos(
      {
        method: request.method ?? "GET",
        path: new URL(request.url ?? "/", "http://localhost").pathname,
        contentType: request.headers["content-type"],
        body: await readBody(request),
      },
      store,
    );
    response.writeHead(answer.status, { "Content-Type": "application/json", ...answer.headers });
    response.end(JSON.stringify(answer.body));
  });
}

if (import.meta.main) {
  serve().listen(3000, () => console.log("http://localhost:3000/todos"));
}
