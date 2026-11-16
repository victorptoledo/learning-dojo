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

function isJson(contentType: string | undefined): boolean {
  return contentType?.split(";")[0]?.trim().toLowerCase() === "application/json";
}

function create(request: ApiRequest, store: TodoStore): ApiResponse {
  if (!isJson(request.contentType)) {
    return problem(415, "/problems/unsupported-media-type", "Send the todo as application/json");
  }
  let json: unknown;
  try {
    json = JSON.parse(request.body);
  } catch {
    return problem(400, "/problems/malformed-json", "The body is not valid JSON");
  }
  const parsed = NewTodo.safeParse(json);
  if (!parsed.success) {
    return problem(422, "/problems/validation", "The todo is not valid", {
      errors: toFieldErrors(parsed.error),
    });
  }
  const todo = store.add(parsed.data.title, parsed.data.due ?? null);
  return {
    status: 201,
    headers: { "Content-Type": "application/json", Location: `/todos/${todo.id}` },
    body: todo,
  };
}

/** The routes of README.md. */
export function handleTodos(request: ApiRequest, store: TodoStore): ApiResponse {
  if (request.path === "/todos" && request.method === "GET") {
    return { status: 200, headers: { "Content-Type": "application/json" }, body: store.todos };
  }
  if (request.path === "/todos" && request.method === "POST") {
    return create(request, store);
  }
  return problem(404, "/problems/not-found", `No route for ${request.method} ${request.path}`);
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
