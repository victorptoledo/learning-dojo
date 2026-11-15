import assert from "node:assert/strict";
import { test } from "node:test";
import { chunk, indexBy, keyed, maxBy, partition, pluck } from "../collections.ts";

type User = { id: number; name: string; admin: boolean };
const ada: User = { id: 1, name: "Ada", admin: true };
const grace: User = { id: 2, name: "Grace", admin: false };
const linus: User = { id: 3, name: "Linus", admin: false };
const users = [ada, grace, linus];

test("pluck keeps the order and works on empty lists", () => {
  assert.deepEqual(pluck(users, "id"), [1, 2, 3]);
  assert.deepEqual(pluck([] as User[], "id"), []);
});

test("indexBy maps each key to its item", () => {
  const byId = indexBy(users, (user) => user.id);
  assert.equal(byId.size, 3);
  assert.equal(byId.get(2), grace);
});

test("indexBy keeps the last item for a repeated key", () => {
  const byAdmin = indexBy(users, (user) => user.admin);
  assert.deepEqual([...byAdmin.entries()], [
    [true, ada],
    [false, linus],
  ]);
});

test("partition splits in order", () => {
  assert.deepEqual(partition([1, 2, 3, 4, 5], (n) => n % 2 === 1), [
    [1, 3, 5],
    [2, 4],
  ]);
  assert.deepEqual(partition(users, (user) => user.admin), [[ada], [grace, linus]]);
});

test("partition with a type guard", () => {
  const values: (string | number)[] = ["a", 1, "b", 2];
  assert.deepEqual(
    partition(values, (value): value is string => typeof value === "string"),
    [
      ["a", "b"],
      [1, 2],
    ],
  );
});

test("maxBy returns the first best item", () => {
  assert.equal(maxBy(users, (user) => user.name.length), grace);
  assert.equal(maxBy([], () => 1), undefined);
  assert.equal(maxBy([-5, -2, -9], (n) => n), -2);
});

test("chunk groups items and keeps a short last group", () => {
  assert.deepEqual(chunk([1, 2, 3, 4, 5], 2), [[1, 2], [3, 4], [5]]);
  assert.deepEqual(chunk([1, 2], 5), [[1, 2]]);
  assert.deepEqual(chunk([], 3), []);
});

test("chunk rejects sizes that are not positive integers", () => {
  for (const size of [0, -1, 1.5, Number.NaN]) {
    assert.throws(() => chunk([1, 2], size), RangeError, String(size));
  }
});

test("keyed builds one property per key", () => {
  assert.deepEqual(
    keyed(["a", "bb", "ccc"], (key) => key.length),
    { a: 1, bb: 2, ccc: 3 },
  );
  assert.deepEqual(keyed([], () => 0), {});
});
