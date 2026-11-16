import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { test, type TestContext } from "node:test";
import { ContractError, createTodo, listTodos } from "../client.ts";
import { NewTodo, Todo } from "../contract.ts";
import { handleTodos, serve, TodoStore, type ApiRequest } from "../server.ts";

function post(body: string, contentType: string | undefined = "application/json"): ApiRequest {
  return { method: "POST", path: "/todos", contentType, body };
}

async function start(t: TestContext): Promise<string> {
  const server = serve();
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());
  return `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
}

function answering(status: number, body: unknown): typeof fetch {
  return async () => new Response(JSON.stringify(body), { status });
}

test("NewTodo refuses blank, too long and unknown fields", () => {
  assert.equal(NewTodo.safeParse({ title: "   " }).success, false);
  assert.equal(NewTodo.safeParse({ title: "x".repeat(121) }).success, false);
  assert.equal(NewTodo.safeParse({ title: ` ${"x".repeat(120)} ` }).success, true);
  assert.equal(NewTodo.safeParse({ title: "ok", done: true }).success, false);
  assert.equal(NewTodo.safeParse({ title: 7 }).success, false);
});

test("NewTodo accepts real ISO dates only", () => {
  assert.deepEqual(NewTodo.parse({ title: "Pay rent", due: "2026-03-01" }), { title: "Pay rent", due: "2026-03-01" });
  assert.equal(NewTodo.safeParse({ title: "x", due: "2026-02-30" }).success, false);
  assert.equal(NewTodo.safeParse({ title: "x", due: "01/03/2026" }).success, false);
});

test("Todo requires a positive integer id and a date or null", () => {
  assert.equal(Todo.safeParse({ id: 1, title: "a", due: null, done: false }).success, true);
  assert.equal(Todo.safeParse({ id: 0, title: "a", due: null, done: false }).success, false);
  assert.equal(Todo.safeParse({ id: 1.5, title: "a", due: null, done: false }).success, false);
  assert.equal(Todo.safeParse({ id: 1, title: "a", due: "soon", done: false }).success, false);
  assert.equal(Todo.safeParse({ id: 1, title: "a", due: null }).success, false);
});

test("a created todo has a Location, a trimmed title and its due date", () => {
  const store = new TodoStore();
  handleTodos(post('{"title": "first"}'), store);
  const answer = handleTodos(post('{"title": "  Pay rent ", "due": "2026-03-01"}', "application/json; charset=utf-8"), store);
  assert.equal(answer.status, 201);
  assert.equal(answer.headers["Location"], "/todos/2");
  assert.match(answer.headers["Content-Type"] ?? "", /^application\/json/);
  assert.deepEqual(answer.body, { id: 2, title: "Pay rent", due: "2026-03-01", done: false });
  assert.deepEqual(handleTodos({ method: "GET", path: "/todos", contentType: undefined, body: "" }, store).body, [
    { id: 1, title: "first", due: null, done: false },
    { id: 2, title: "Pay rent", due: "2026-03-01", done: false },
  ]);
});

test("a body that is not JSON is a 400 problem", () => {
  const answer = handleTodos(post("{title: oops"), new TodoStore());
  assert.equal(answer.status, 400);
  assert.equal(answer.headers["Content-Type"], "application/problem+json");
  assert.deepEqual(
    { ...(answer.body as object), title: "" },
    { type: "/problems/malformed-json", title: "", status: 400 },
  );
});

test("another media type is a 415 problem", () => {
  for (const type of ["text/plain", "application/x-www-form-urlencoded", undefined]) {
    const answer = handleTodos({ ...post('{"title": "x"}'), contentType: type }, new TodoStore());
    assert.equal(answer.status, 415, `for ${type}`);
    assert.equal((answer.body as { type: string }).type, "/problems/unsupported-media-type");
  }
});

test("an invalid todo is a 422 problem with one error per field, and nothing is stored", () => {
  const store = new TodoStore();
  const answer = handleTodos(post('{"title": "", "due": "2026-02-30", "owner": "ada"}'), store);
  assert.equal(answer.status, 422);
  assert.equal(answer.headers["Content-Type"], "application/problem+json");
  const body = answer.body as { type: string; status: number; title: string; errors: { path: string; message: string }[] };
  assert.equal(body.type, "/problems/validation");
  assert.equal(body.status, 422);
  assert.equal(typeof body.title, "string");
  assert.deepEqual(body.errors.map((error) => error.path).sort(), ["", "due", "title"]);
  assert.ok(body.errors.every((error) => error.message.length > 0));
  assert.deepEqual(store.todos, []);
});

test("unknown routes are a 404 problem", () => {
  const answer = handleTodos({ method: "DELETE", path: "/todos", contentType: undefined, body: "" }, new TodoStore());
  assert.equal(answer.status, 404);
  assert.equal((answer.body as { type: string }).type, "/problems/not-found");
});

test("the client creates and lists todos through the real server", async (t) => {
  const base = await start(t);
  const created = await createTodo(base, { title: " Ship it ", due: "2026-12-24" });
  assert.deepEqual(created, { ok: true, todo: { id: 1, title: "Ship it", due: "2026-12-24", done: false } });
  assert.deepEqual(await listTodos(base), [{ id: 1, title: "Ship it", due: "2026-12-24", done: false }]);
});

test("the client returns the server's problem when the input is refused", async (t) => {
  const base = await start(t);
  const result = await createTodo(base, { title: "" });
  assert.equal(result.ok, false);
  if (!result.ok) {
    assert.equal(result.problem.status, 422);
    assert.deepEqual(result.problem.errors?.map((error) => error.path), ["title"]);
  }
});

test("the client sends JSON with the right content type", async () => {
  let seen: RequestInit | undefined;
  const capture: typeof fetch = async (_url, init) => {
    seen = init;
    return new Response(JSON.stringify({ id: 3, title: "x", due: null, done: false }), { status: 201 });
  };
  await createTodo("http://api.test", { title: "x" }, capture);
  assert.equal(seen?.method, "POST");
  assert.equal(new Headers(seen?.headers).get("content-type"), "application/json");
  assert.deepEqual(JSON.parse(String(seen?.body)), { title: "x" });
});

test("a todo that breaks the contract is a ContractError, not data", async () => {
  await assert.rejects(
    createTodo("http://api.test", { title: "x" }, answering(201, { id: 3, title: "x", due: null })),
    ContractError,
  );
  await assert.rejects(listTodos("http://api.test", answering(200, [{ id: "1", title: "x", due: null, done: false }])), ContractError);
});

test("an error body that is not a problem is a ContractError", async () => {
  await assert.rejects(createTodo("http://api.test", { title: "x" }, answering(500, { message: "boom" })), ContractError);
});
