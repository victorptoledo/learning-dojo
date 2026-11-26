import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { test } from "node:test";
import { migrate } from "../migrations.ts";
import { splitFullName } from "../names.ts";

test("the first word is the first name, the rest is the last name", () => {
  assert.deepEqual(splitFullName("Ana Maria Souza"), { firstName: "Ana", lastName: "Maria Souza" });
});

test("the expand migration adds first_name and last_name, and runs once", () => {
  const db = new DatabaseSync(":memory:");
  assert.deepEqual(migrate(db), ["001_create_users", "002_split_name_expand"]);
  const names = (db.prepare("PRAGMA table_info(users)").all() as { name: string }[]).map((row) => row.name);
  assert.deepEqual(names, ["id", "full_name", "email", "first_name", "last_name"]);
  assert.deepEqual(migrate(db), []);
});
