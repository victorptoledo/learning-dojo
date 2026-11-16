import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { test, type TestContext } from "node:test";
import { createApp } from "../app.ts";
import { openNotes, type NotesRepo } from "../db.ts";
import { validateNote } from "../notes.ts";
import { notesPage } from "../views.ts";

async function start(t: TestContext, repo: NotesRepo = openNotes()): Promise<{ base: string; repo: NotesRepo }> {
  const server = createApp(repo);
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());
  return { base: `http://127.0.0.1:${(server.address() as AddressInfo).port}`, repo };
}

function postForm(url: string, fields: Record<string, string>): Promise<Response> {
  return fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(fields).toString(),
    redirect: "manual",
  });
}

function postJson(url: string, body: string): Promise<Response> {
  return fetch(url, { method: "POST", headers: { "Content-Type": "application/json" }, body });
}

function ids(html: string): number[] {
  return [...html.matchAll(/<article class="note" data-id="(\d+)">/g)].map((match) => Number(match[1]));
}

test("validation trims, defaults the body and reports every field", () => {
  assert.deepEqual(validateNote({ title: "  Groceries ", body: " milk " }), { ok: true, note: { title: "Groceries", body: "milk" } });
  assert.deepEqual(validateNote({ title: "x" }), { ok: true, note: { title: "x", body: "" } });
  assert.deepEqual(validateNote({ title: "x".repeat(81), body: "y".repeat(2001) }), {
    ok: false,
    errors: { title: "Title must be at most 80 characters", body: "Body must be at most 2000 characters" },
  });
  assert.deepEqual(validateNote({ title: 42, body: ["no"] }), {
    ok: false,
    errors: { title: "Title is required", body: "Body must be at most 2000 characters" },
  });
});

test("the repository gets and removes by id", () => {
  const repo = openNotes();
  const note = repo.create({ title: "temp", body: "b" });
  assert.deepEqual(repo.get(note.id), { id: note.id, title: "temp", body: "b" });
  assert.equal(repo.get(999), undefined);
  assert.equal(repo.remove(note.id), true);
  assert.equal(repo.remove(note.id), false);
  assert.deepEqual(repo.list(), []);
});

test("search ignores case and treats % and _ literally", () => {
  const repo = openNotes();
  for (const title of ["50% off", "500 items", "Snake_case", "SnakeXcase", "Weekly PLAN"]) {
    repo.create({ title, body: "" });
  }
  assert.deepEqual(repo.list("50%").map((note) => note.title), ["50% off"]);
  assert.deepEqual(repo.list("e_c").map((note) => note.title), ["Snake_case"]);
  assert.deepEqual(repo.list("plan").map((note) => note.title), ["Weekly PLAN"]);
  assert.equal(repo.list("").length, 5);
});

test("values go to SQLite as parameters, never as SQL", () => {
  const repo = openNotes();
  const title = "it's'); DROP TABLE notes; --";
  repo.create({ title, body: "" });
  assert.deepEqual(repo.list("'); DROP").map((note) => note.title), [title]);
  assert.equal(repo.list().length, 1);
});

test("the page escapes user text and lists notes in order", () => {
  const html = notesPage({
    notes: [
      { id: 2, title: "<script>x</script>", body: "a & b" },
      { id: 1, title: "plain", body: "" },
    ],
    search: `"><b>`,
  });
  assert.deepEqual(ids(html), [2, 1]);
  assert.match(html, /<h2>&lt;script&gt;x&lt;\/script&gt;<\/h2>/);
  assert.match(html, /<p>a &amp; b<\/p>/);
  assert.match(html, /<input name="q" value="&quot;&gt;&lt;b&gt;">/);
  assert.doesNotMatch(html, /<script>x/);
  assert.match(html, /<form method="post" action="\/notes\/2\/delete">/);
  assert.match(html, /<form method="post" action="\/notes">/);
  assert.match(html, /<form method="get" action="\/">/);
});

test("the page shows errors, keeps the typed values and has an empty state", () => {
  const html = notesPage({ notes: [], errors: { title: "Title is required" }, values: { title: "  ", body: "<i>draft</i>" } });
  assert.match(html, /<p class="error">Title is required<\/p>/);
  assert.match(html, /<textarea name="body">&lt;i&gt;draft&lt;\/i&gt;<\/textarea>/);
  assert.match(html, /<input name="title" value="  ">/);
  assert.match(html, /<p class="empty">No notes yet<\/p>/);
});

test("GET / renders the stored notes as HTML, filtered by q", async (t) => {
  const { base, repo } = await start(t);
  repo.create({ title: "Groceries", body: "milk" });
  repo.create({ title: "Gym plan", body: "legs" });
  const all = await fetch(`${base}/`);
  assert.equal(all.status, 200);
  assert.match(all.headers.get("content-type") ?? "", /^text\/html/);
  assert.deepEqual(ids(await all.text()), [2, 1]);
  const filtered = await (await fetch(`${base}/?q=groc`)).text();
  assert.deepEqual(ids(filtered), [1]);
  assert.match(filtered, /<input name="q" value="groc">/);
});

test("a valid form post redirects with 303 and stores the note", async (t) => {
  const { base, repo } = await start(t);
  const response = await postForm(`${base}/notes`, { title: " Call mum ", body: "Sunday" });
  assert.equal(response.status, 303);
  assert.equal(response.headers.get("location"), "/");
  await response.text();
  assert.deepEqual(repo.list(), [{ id: 1, title: "Call mum", body: "Sunday" }]);
});

test("an invalid form post is a 422 page with the errors and the typed values", async (t) => {
  const { base, repo } = await start(t);
  repo.create({ title: "kept", body: "" });
  const response = await postForm(`${base}/notes`, { title: "", body: "<b>my draft</b>" });
  assert.equal(response.status, 422);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html/);
  const html = await response.text();
  assert.match(html, /<p class="error">Title is required<\/p>/);
  assert.match(html, /<textarea name="body">&lt;b&gt;my draft&lt;\/b&gt;<\/textarea>/);
  assert.deepEqual(ids(html), [1]);
  assert.equal(repo.list().length, 1);
});

test("the delete form removes a note, an unknown id is 404", async (t) => {
  const { base, repo } = await start(t);
  const note = repo.create({ title: "bye", body: "" });
  const deleted = await postForm(`${base}/notes/${note.id}/delete`, {});
  assert.equal(deleted.status, 303);
  assert.equal(deleted.headers.get("location"), "/");
  await deleted.text();
  assert.deepEqual(repo.list(), []);
  const missing = await postForm(`${base}/notes/${note.id}/delete`, {});
  assert.equal(missing.status, 404);
  await missing.text();
});

test("the JSON API lists, searches and reads notes", async (t) => {
  const { base, repo } = await start(t);
  repo.create({ title: "Alpha", body: "1" });
  repo.create({ title: "Beta", body: "2" });
  const list = await fetch(`${base}/api/notes`);
  assert.match(list.headers.get("content-type") ?? "", /application\/json/);
  assert.deepEqual(await list.json(), [
    { id: 2, title: "Beta", body: "2" },
    { id: 1, title: "Alpha", body: "1" },
  ]);
  assert.deepEqual(await (await fetch(`${base}/api/notes?q=alp`)).json(), [{ id: 1, title: "Alpha", body: "1" }]);
  assert.deepEqual(await (await fetch(`${base}/api/notes/2`)).json(), { id: 2, title: "Beta", body: "2" });
  const missing = await fetch(`${base}/api/notes/99`);
  assert.equal(missing.status, 404);
  assert.deepEqual(await missing.json(), { error: "not found" });
});

test("POST /api/notes creates with 201 and a Location", async (t) => {
  const { base } = await start(t);
  const response = await postJson(`${base}/api/notes`, JSON.stringify({ title: " From the SPA ", body: "json" }));
  assert.equal(response.status, 201);
  assert.equal(response.headers.get("location"), "/api/notes/1");
  assert.deepEqual(await response.json(), { id: 1, title: "From the SPA", body: "json" });
});

test("the JSON API shares the validation: 422 with the same messages, 400 for bad JSON", async (t) => {
  const { base, repo } = await start(t);
  const invalid = await postJson(`${base}/api/notes`, JSON.stringify({ title: "x".repeat(90) }));
  assert.equal(invalid.status, 422);
  assert.deepEqual(await invalid.json(), { errors: { title: "Title must be at most 80 characters" } });
  const broken = await postJson(`${base}/api/notes`, "{oops");
  assert.equal(broken.status, 400);
  assert.equal(typeof ((await broken.json()) as { error?: unknown }).error, "string");
  assert.deepEqual(repo.list(), []);
});

test("unknown routes are 404", async (t) => {
  const { base } = await start(t);
  for (const [method, path] of [["GET", "/admin"], ["DELETE", "/api/notes/1"], ["PUT", "/notes"]] as const) {
    const response = await fetch(`${base}${path}`, { method });
    assert.equal(response.status, 404, `${method} ${path}`);
    await response.text();
  }
});
