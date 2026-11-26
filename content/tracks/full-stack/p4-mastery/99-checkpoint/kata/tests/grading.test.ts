import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { DatabaseSync } from "node:sqlite";
import { test, type TestContext } from "node:test";
import { createApp, READ_SPLIT_NAME } from "../app.ts";
import { checkUserForOldClients } from "../contract.ts";
import { bucket, fnv1a, isEnabled, type Flag } from "../flags.ts";
import { migrate, MIGRATIONS } from "../migrations.ts";
import { joinName, splitFullName } from "../names.ts";
import { usersRepo } from "../users.ts";

/** What the release still running in production does: it only knows full_name. */
const OLD_INSERT = "INSERT INTO users (full_name, email) VALUES (?, ?)";
const OLD_SELECT = "SELECT id, full_name, email FROM users WHERE id = ?";

function database(): DatabaseSync {
  const db = new DatabaseSync(":memory:");
  migrate(db);
  return db;
}

function oldInsert(db: DatabaseSync, fullName: string, email: string): number {
  return Number(db.prepare(OLD_INSERT).run(fullName, email).lastInsertRowid);
}

function columns(db: DatabaseSync): Map<string, { notnull: number; dflt_value: unknown }> {
  const rows = db.prepare("PRAGMA table_info(users)").all() as { name: string; notnull: number; dflt_value: unknown }[];
  return new Map(rows.map((row) => [row.name, row]));
}

function rollout(percent: number, name = READ_SPLIT_NAME, extra: Partial<Flag> = {}): Flag {
  return { name, enabled: true, percent, ...extra };
}

async function serve(t: TestContext, db: DatabaseSync, flags: () => Record<string, Flag>): Promise<string> {
  const server = createApp({ db, flags });
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => {
    server.closeAllConnections();
    server.close();
  });
  return `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
}

async function getUser(base: string, id: number): Promise<Record<string, unknown>> {
  const response = await fetch(`${base}/api/users/${id}`, { headers: { Connection: "close" } });
  assert.equal(response.status, 200);
  return (await response.json()) as Record<string, unknown>;
}

async function patch(base: string, id: number, body: unknown): Promise<Response> {
  return fetch(`${base}/api/users/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json", Connection: "close" },
    body: typeof body === "string" ? body : JSON.stringify(body),
  });
}

// ---- migrations -------------------------------------------------------------------------

test("migrate applies each migration once and records it", () => {
  const db = new DatabaseSync(":memory:");
  assert.deepEqual(migrate(db), ["001_create_users", "002_split_name_expand"]);
  assert.deepEqual(migrate(db), [], "a second run applies nothing");
  const recorded = db.prepare("SELECT name FROM schema_migrations ORDER BY name").all() as { name: string }[];
  assert.deepEqual(recorded.map((row) => row.name), ["001_create_users", "002_split_name_expand"]);
});

test("the expand migration only adds nullable columns without a default, and keeps full_name", () => {
  const db = database();
  const cols = columns(db);
  for (const name of ["first_name", "last_name"]) {
    const col = cols.get(name);
    assert.ok(col, `users.${name} is missing`);
    assert.equal(col.notnull, 0, `${name} must be nullable: the old release inserts rows without it`);
    assert.equal(col.dflt_value, null, `${name} must have no default: NULL is how the backfill finds its rows`);
  }
  assert.equal(cols.get("full_name")?.notnull, 1, "full_name stays: old clients and the old release still use it");
  const id = oldInsert(db, "Ana Souza", "ana@example.com");
  assert.deepEqual({ ...(db.prepare(OLD_SELECT).get(id) as object) }, { id, full_name: "Ana Souza", email: "ana@example.com" });
});

test("a failing migration is rolled back whole and not recorded", () => {
  const db = new DatabaseSync(":memory:");
  const broken = [
    ...MIGRATIONS,
    { name: "003_half_done", sql: "ALTER TABLE users ADD COLUMN nickname TEXT; SELECT nope FROM nowhere;" },
  ];
  assert.throws(() => migrate(db, broken));
  assert.equal(columns(db).has("nickname"), false, "the first statement of the failed migration was kept");
  const recorded = db.prepare("SELECT name FROM schema_migrations ORDER BY name").all() as { name: string }[];
  assert.deepEqual(recorded.map((row) => row.name), ["001_create_users", "002_split_name_expand"]);
  assert.deepEqual(migrate(db), [], "the migrations before the broken one stay applied");
});

// ---- names ------------------------------------------------------------------------------

test("splitFullName and joinName", () => {
  assert.deepEqual(splitFullName("Ana Maria  Souza"), { firstName: "Ana", lastName: "Maria Souza" });
  assert.deepEqual(splitFullName("  Cher "), { firstName: "Cher", lastName: "" });
  assert.deepEqual(splitFullName(""), { firstName: "", lastName: "" });
  assert.deepEqual(splitFullName("Jean\tLuc\nPicard"), { firstName: "Jean", lastName: "Luc Picard" });
  assert.equal(joinName(" Ana ", " Maria Souza "), "Ana Maria Souza");
  assert.equal(joinName("Cher", ""), "Cher");
  assert.equal(joinName("Cher", "   "), "Cher");
});

// ---- flags ------------------------------------------------------------------------------

test("fnv1a and bucket follow 32-bit FNV-1a", () => {
  assert.equal(fnv1a(""), 0x811c9dc5);
  assert.equal(fnv1a("a"), 0xe40c292c);
  assert.equal(fnv1a("foobar"), 0xbf9cf968);
  assert.equal(fnv1a("é"), 0x1e9de8c1, "hash the UTF-8 bytes (0xc3 0xa9), not the UTF-16 code unit");
  assert.equal(bucket("read-split-name", 42), fnv1a("read-split-name:42") % 100);
});

test("the kill switch beats the allow list, which beats the percentage", () => {
  assert.equal(isEnabled(undefined, 1), false, "an unknown flag is off");
  assert.equal(isEnabled(rollout(100, READ_SPLIT_NAME, { enabled: false, allow: [1] }), 1), false);
  assert.equal(isEnabled(rollout(0, READ_SPLIT_NAME, { allow: [7] }), 7), true);
  const users = Array.from({ length: 200 }, (_, i) => i + 1);
  assert.equal(users.filter((id) => isEnabled(rollout(0), id)).length, 0);
  assert.equal(users.filter((id) => isEnabled(rollout(100), id)).length, 200);
});

test("a rollout is stable and only grows: users in at 10% stay in at 50%", () => {
  const users = Array.from({ length: 1000 }, (_, i) => i + 1);
  const at10 = users.filter((id) => isEnabled(rollout(10), id));
  const at50 = new Set(users.filter((id) => isEnabled(rollout(50), id)));
  assert.ok(at10.length > 50 && at10.length < 150, `${at10.length} of 1000 users at 10%`);
  assert.ok(at50.size > 420 && at50.size < 580, `${at50.size} of 1000 users at 50%`);
  assert.ok(at10.every((id) => at50.has(id)), "someone who had the feature lost it when the rollout grew");
  assert.deepEqual(users.filter((id) => isEnabled(rollout(10), id)), at10, "the same answer every time");
});

test("two flags at 50% do not pick the same users", () => {
  const users = Array.from({ length: 1000 }, (_, i) => i + 1);
  const a = users.filter((id) => isEnabled(rollout(50, "new-checkout"), id));
  const b = new Set(users.filter((id) => isEnabled(rollout(50, "dark-mode"), id)));
  const both = a.filter((id) => b.has(id)).length;
  assert.ok(both > 150 && both < 350, `${both} users got both flags: the bucket must depend on the flag name`);
});

// ---- the repository ---------------------------------------------------------------------

test("the backfill fills NULL names in batches and picks up rows the old release adds later", () => {
  const db = database();
  for (const [i, name] of ["Ana Souza", "Bo", "Cy de la Cruz", "Di Lu", "Ed Ng"].entries()) oldInsert(db, name, `u${i}@example.com`);
  const users = usersRepo(db);
  assert.deepEqual([users.backfillNames(2), users.backfillNames(2), users.backfillNames(2), users.backfillNames(2)], [2, 2, 1, 0]);
  const row = db.prepare("SELECT first_name, last_name FROM users WHERE id = 3").get() as object;
  assert.deepEqual({ ...row }, { first_name: "Cy", last_name: "de la Cruz" });
  assert.deepEqual({ ...(db.prepare("SELECT last_name FROM users WHERE id = 2").get() as object) }, { last_name: "" });
  oldInsert(db, "Flo Rida", "flo@example.com");
  assert.equal(users.backfillNames(10), 1, "a row written by the old release after the first pass");
  assert.equal(users.backfillNames(10), 0);
});

test("the backfill never overwrites names that new clients wrote", () => {
  const db = database();
  const id = oldInsert(db, "Mary Ann Lee", "mary@example.com");
  const users = usersRepo(db);
  users.rename(id, { firstName: "Mary Ann", lastName: "Lee" });
  assert.equal(users.backfillNames(10), 0);
  assert.deepEqual(users.get(id, true), { id, fullName: "Mary Ann Lee", firstName: "Mary Ann", lastName: "Lee", email: "mary@example.com" });
});

test("rename writes both shapes, whichever client sent it", () => {
  const db = database();
  const id = oldInsert(db, "Ana Souza", "ana@example.com");
  const users = usersRepo(db);
  assert.deepEqual(users.rename(id, { fullName: "  Ana   Maria Souza " }), {
    id, fullName: "Ana Maria Souza", firstName: "Ana", lastName: "Maria Souza", email: "ana@example.com",
  });
  users.rename(id, { firstName: " Ana Maria ", lastName: "Souza" });
  assert.deepEqual({ ...(db.prepare("SELECT full_name, first_name, last_name FROM users WHERE id = ?").get(id) as object) }, {
    full_name: "Ana Maria Souza", first_name: "Ana Maria", last_name: "Souza",
  });
  assert.deepEqual({ ...(db.prepare(OLD_SELECT).get(id) as object) }, { id, full_name: "Ana Maria Souza", email: "ana@example.com" });
  assert.equal(users.rename(999, { fullName: "Nobody" }), undefined);
});

test("get reads the new columns only when asked to and when the row has them", () => {
  const db = database();
  const id = oldInsert(db, "Mary Ann Lee", "mary@example.com");
  const users = usersRepo(db);
  assert.deepEqual(users.get(id, true), { id, fullName: "Mary Ann Lee", firstName: "Mary", lastName: "Ann Lee", email: "mary@example.com" }, "not backfilled yet: derive from full_name");
  db.prepare("UPDATE users SET first_name = 'Mary Ann', last_name = 'Lee' WHERE id = ?").run(id);
  assert.equal(users.get(id, true)?.firstName, "Mary Ann");
  assert.equal(users.get(id, false)?.firstName, "Mary", "with the flag off, the columns are not read");
  assert.equal(users.get(404, true), undefined);
});

// ---- the API ----------------------------------------------------------------------------

test("the flag decides per user and per request, so the kill switch needs no deploy", async (t) => {
  const db = database();
  const ids = Array.from({ length: 40 }, (_, i) => oldInsert(db, `Mary${i} Ann Lee`, `m${i}@example.com`));
  db.exec("UPDATE users SET first_name = 'From', last_name = 'Columns'");
  let flags: Record<string, Flag> = { [READ_SPLIT_NAME]: rollout(50) };
  const base = await serve(t, db, () => flags);
  for (const id of ids) {
    const user = await getUser(base, id);
    const expected = isEnabled(flags[READ_SPLIT_NAME], id) ? "From" : `Mary${id - 1}`;
    assert.equal(user["firstName"], expected, `user ${id} at 50%`);
  }
  flags = { [READ_SPLIT_NAME]: rollout(100, READ_SPLIT_NAME, { enabled: false }) };
  for (const id of ids.slice(0, 10)) assert.equal((await getUser(base, id))["lastName"], "Ann Lee", `user ${id} after the kill switch`);
});

test("PATCH accepts exactly one shape and answers with the user", async (t) => {
  const db = database();
  const id = oldInsert(db, "Ana Souza", "ana@example.com");
  const base = await serve(t, db, () => ({ [READ_SPLIT_NAME]: rollout(100) }));
  for (const body of [{}, { fullName: "   " }, { fullName: 7 }, { firstName: "Ana" }, { firstName: " ", lastName: "Souza" },
    { fullName: "Ana Souza", firstName: "Ana", lastName: "Souza" }, [], "not json"]) {
    const response = await patch(base, id, body);
    assert.equal(response.status, 422, `PATCH ${JSON.stringify(body)}`);
    await response.body?.cancel();
  }
  assert.equal((await patch(base, 999, { fullName: "Nobody Here" })).status, 404);
  const response = await patch(base, id, { firstName: "Ana Maria", lastName: "Souza" });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { id, fullName: "Ana Maria Souza", firstName: "Ana Maria", lastName: "Souza", email: "ana@example.com" });
});

// ---- the consumer contract --------------------------------------------------------------

test("the contract check catches every change that would break old clients", () => {
  const good = { id: 1, fullName: "Ana Souza", email: "ana@example.com" };
  assert.deepEqual(checkUserForOldClients(good), []);
  assert.deepEqual(checkUserForOldClients({ ...good, firstName: "Ana", lastName: "Souza", extra: true }), [], "new fields are fine");
  const broken: [unknown, string][] = [
    [null, "body"], [[good], "body"], ["Ana", "body"],
    [{ id: 1, email: "ana@example.com" }, "fullName"],
    [{ ...good, fullName: "  " }, "fullName"],
    [{ ...good, fullName: null }, "fullName"],
    [{ ...good, id: "1" }, "id"],
    [{ ...good, id: 1.5 }, "id"],
    [{ ...good, email: "ana" }, "email"],
  ];
  for (const [body, field] of broken) {
    const problems = checkUserForOldClients(body);
    assert.ok(problems.length > 0, `${JSON.stringify(body)} passed the contract`);
    assert.ok(problems.some((p) => p.startsWith(field)), `${JSON.stringify(body)}: no problem about ${field} in ${JSON.stringify(problems)}`);
  }
  assert.equal(checkUserForOldClients({ id: "x" }).length, 3, "one problem per broken field");
});

test("the API keeps the contract for old clients with the flag on and off", async (t) => {
  const db = database();
  const id = oldInsert(db, "Cher", "cher@example.com");
  let percent = 0;
  const base = await serve(t, db, () => ({ [READ_SPLIT_NAME]: rollout(percent) }));
  assert.deepEqual(checkUserForOldClients(await getUser(base, id)), []);
  percent = 100;
  usersRepo(db).backfillNames(10);
  const user = await getUser(base, id);
  assert.deepEqual(checkUserForOldClients(user), []);
  assert.deepEqual(user, { id, fullName: "Cher", firstName: "Cher", lastName: "", email: "cher@example.com" });
});
